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
package com.github.maven_nar.cpptasks;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinkerConfiguration;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.gcc.GccLinker;
import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LinkerArgument;
import com.github.maven_nar.cpptasks.types.SystemLibrarySet;

/**
 * Tests for LinkerDef class.
 */
public final class TestLinkerDef extends TestProcessorDef {
  /**
   * Creates a processor.
   *
   * @return new linker
   */
  @Override
  protected ProcessorDef create() {
    return new LinkerDef();
  }

  /**
   * Gets the command line arguments that appear before the filenames.
   *
   * @param processor
   *          processor under test
   * @return command line arguments
   */
  @Override
  protected String[] getPreArguments(final ProcessorDef processor) {
    return ((CommandLineLinkerConfiguration) getConfiguration(processor)).getPreArguments();
  }

  /**
   * Tests that the entry attribute in the base linker is effective when
   * creating the command line for a linker that extends it.
   */
  @Test
  public void testExtendsEntry() {
    final LinkerDef baseLinker = new LinkerDef();
    baseLinker.setEntry("foo");
    final LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(baseLinker);
    final String[] preArgs = getPreArguments(extendedLinker);
    assertEquals("-e", preArgs[0]);
    assertEquals("foo", preArgs[1]);
  }

  /**
   * Tests that fileset's that appear in the base linker are effective when
   * creating the command line for a linker that extends it.
   * 
   * @throws IOException
   *           if unable to create or delete temporary file
   */
  @Test
  public void testExtendsFileSet() throws IOException {
    super.testExtendsFileSet(File.createTempFile("cpptaskstest", ".o"));
  }

  /**
   * Tests that libset's that appear in the base linker are effective when
   * creating the command line for a linker that extends it.
   */
  @Test
  public void testExtendsLibSet() {
    final LinkerDef baseLinker = new LinkerDef();
    final LibrarySet libset = new LibrarySet();
    final LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(baseLinker);
    libset.setProject(baseLinker.getProject());
    final CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("advapi32");
    libset.setLibs(libs);
    baseLinker.addLibset(libset);
    final CommandLineLinkerConfiguration config = (CommandLineLinkerConfiguration) getConfiguration(extendedLinker);
    final String[] libnames = config.getLibraryNames();
    assertEquals(1, libnames.length);
    assertEquals("advapi32", libnames[0]);
  }

  /**
   * Tests that linkerarg's that appear in the base linker are effective when
   * creating the command line for a linker that extends it.
   */
  @Test
  public void testExtendsLinkerArgs() {
    final LinkerDef baseLinker = new LinkerDef();
    final LinkerArgument linkerArg = new LinkerArgument();
    linkerArg.setValue("/base");
    baseLinker.addConfiguredLinkerArg(linkerArg);
    final LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(baseLinker);
    final String[] preArgs = getPreArguments(extendedLinker);
    assertEquals(1, preArgs.length);
    assertEquals("/base", preArgs[0]);
  }

  /**
   * Verify linkerarg's that appear in the base linker are effective when
   * creating the command line for a linker that extends it, even if the
   * linker is brought in through a reference.
   */
  @Test
  public void testExtendsLinkerArgsViaReference() {
    final Project project = new Project();
    final LinkerDef baseLinker = new LinkerDef();
    baseLinker.setProject(project);
    baseLinker.setId("base");
    project.addReference("base", baseLinker);
    final LinkerArgument linkerArg = new LinkerArgument();
    linkerArg.setValue("/base");
    baseLinker.addConfiguredLinkerArg(linkerArg);

    final LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(baseLinker);
    extendedLinker.setProject(project);
    extendedLinker.setId("extended");
    project.addReference("extended", extendedLinker);

    final LinkerDef linkerRef = new LinkerDef();
    linkerRef.setProject(project);
    linkerRef.setRefid(new Reference(project, "extended"));
    final String[] preArgs = getPreArguments(linkerRef);
    assertEquals(1, preArgs.length);
    assertEquals("/base", preArgs[0]);
  }

  /**
   * Tests that the rebuild attribute in the base linker is effective when
   * creating the command line for a linker that extends it.
   */
  @Test
  public void testExtendsRebuild() {
    testExtendsRebuild(new LinkerDef());
  }

  /**
   * Tests that syslibset's that appear in the base linker are effective when
   * creating the command line for a linker that extends it.
   */
  @Test
  public void testExtendsSysLibSet() {
    final LinkerDef baseLinker = new LinkerDef();
    final SystemLibrarySet libset = new SystemLibrarySet();
    final LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(baseLinker);
    libset.setProject(baseLinker.getProject());
    final CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("advapi32");
    libset.setLibs(libs);
    baseLinker.addSyslibset(libset);
    final CommandLineLinkerConfiguration config = (CommandLineLinkerConfiguration) getConfiguration(extendedLinker);
    final String[] libnames = config.getLibraryNames();
    assertEquals(1, libnames.length);
    assertEquals("advapi32", libnames[0]);
  }

  /**
   * Test if setting the classname attribute to the name of the GCC linker
   * results in the singleton GCC linker.
   */
  @Test
  public void testGetGcc() {
    final LinkerDef linkerDef = (LinkerDef) create();
    linkerDef.setClassname("com.github.maven_nar.cpptasks.gcc.GccLinker");
    final Linker comp = (Linker) linkerDef.getProcessor();
    assertNotNull(comp);
    assertSame(GccLinker.getInstance(), comp);
  }

  /**
   * Tests if setting the classname attribute to an bogus classname results in
   * a BuildException.
   *
   */
  @Test
  public void testUnknownClass() {
    final LinkerDef linkerDef = (LinkerDef) create();
    try {
      linkerDef.setClassname("com.github.maven_nar.cpptasks.bogus.BogusLinker");
    } catch (final BuildException ex) {
      return;
    }
    fail("should have thrown exception");
  }

  /**
   * Tests if setting the classname to the name of a class that doesn't
   * support Linker throws a BuildException.
   *
   */
  @Test
  public void testWrongType() {
    final LinkerDef linkerDef = (LinkerDef) create();
    try {
      linkerDef.setClassname("com.github.maven_nar.cpptasks.CCTask");
    } catch (final ClassCastException ex) {
      return;
    }
    fail("should have thrown exception");
  }
}
