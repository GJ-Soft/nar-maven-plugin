/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.Project;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.LinkerDef;
import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

/**
 * Keeps info on a library
 *
 * @author Mark Donszelmann
 */
public class Lib {

  /**
   * Name of the library, or a dependency groupId:artifactId if this library
   * contains sublibraries
   */
  @Parameter(required = true)
  private String name;

  /**
   * Type of linking for this library
   */
  @Parameter(defaultValue = "shared", required = true)
  private String type = Library.SHARED;

  /**
   * Location for this library
   */
  @Parameter(required = true)
  private File directory;

  /**
   * Optional location of the runtime shared library (e.g. the {@code bin}
   * directory holding the actual {@code .dll}/{@code .so}), used to build the
   * runtime library path when running tests and executables. Useful for
   * Cygwin/MinGW layouts where {@link #directory} holds the import library
   * ({@code .dll.a}) while the runtime {@code .dll} lives elsewhere. When set, it
   * is used instead of {@link #directory} for the runtime path.
   */
  @Parameter
  private File binDirectory;

  /**
   * Sub libraries for this library
   */
  @Parameter
  private List<Lib> libs;

  public Lib() {
  }

  /**
   * Creates a leaf (external/system) library. Used to reconstruct a &lt;lib&gt;
   * propagated through a dependency's nar.properties.
   *
   * @param name         library name
   * @param type         link type (e.g. shared, static)
   * @param directory    location of the (import) library
   * @param binDirectory optional location of the runtime shared library
   */
  public Lib(final String name, final String type, final File directory, final File binDirectory) {
    this.name = name;
    if (type != null) {
      this.type = type;
    }
    this.directory = directory;
    this.binDirectory = binDirectory;
  }

  public final String getName() {
    return this.name;
  }

  public final String getType() {
    return this.type;
  }

  public final File getDirectory() {
    return this.directory;
  }

  public final File getBinDirectory() {
    return this.binDirectory;
  }

  public final List<Lib> getLibs() {
    return this.libs;
  }

  public final void addLibSet(final AbstractDependencyMojo mojo, final LinkerDef linker, final Project antProject)
      throws MojoFailureException, MojoExecutionException {
    if (this.name == null) {
      throw new MojoFailureException("NAR: Please specify <Name> as part of <Lib> for library \"" + this.name + "\"");
    }
    addLibSet(mojo, linker, antProject, this.name, this.directory);
  }

  private void addLibSet(final AbstractDependencyMojo mojo, final LinkerDef linker, final Project antProject,
      final String name, final File dir) throws MojoFailureException, MojoExecutionException {
    if (this.libs == null) {
      addSingleLibSet(linker, antProject, name, dir);
    } else {
      addMultipleLibSets(mojo, linker, antProject, name);
    }
  }

  private void addMultipleLibSets(final AbstractDependencyMojo mojo, final LinkerDef linker, final Project antProject,
      final String name) throws MojoFailureException, MojoExecutionException {
    final List<NarArtifact> dependencies = mojo.getNarArtifacts();
    for (final Object lib1 : this.libs) {
      final Lib lib = (Lib) lib1;
      final String[] ids = name.split(":", 2);
      if (ids.length != 2) {
        throw new MojoFailureException("NAR: Please specify <Name> as part of <Lib> in format 'groupId:artifactId'");
      }
      for (final Object dependency1 : dependencies) {
        final Artifact dependency = (Artifact) dependency1;
        if (dependency.getGroupId().equals(ids[0]) && dependency.getArtifactId().equals(ids[1])) {
          // FIXME NAR-90
          final File narDir = new File(dependency.getFile().getParentFile(),
              "nar/lib/" + mojo.getAOL() + "/" + lib.type);
          final String narName = dependency.getArtifactId() + "-" + lib.name + "-" + dependency.getBaseVersion();
          lib.addLibSet(mojo, linker, antProject, narName, narDir);
        }
      }
    }
  }

  private void addSingleLibSet(final LinkerDef linker, final Project antProject, final String name, final File dir)
      throws MojoFailureException, MojoExecutionException {
    if (!this.type.equals("framework") && dir == null) {
      throw new MojoFailureException("NAR: Please specify <Directory> as part of <Lib> for library \"" + name + "\"");
    }
    final LibrarySet libSet = new LibrarySet();
    libSet.setProject(antProject);
    libSet.setLibs(new CUtil.StringArrayBuilder(name));
    final LibraryTypeEnum libType = new LibraryTypeEnum();
    libType.setValue(this.type);
    libSet.setType(libType);
    libSet.setDir(dir);
    linker.addLibset(libSet);
  }
}
