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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * @author Mark Donszelmann
 */
public class NarInfo {

  public static final String NAR_PROPERTIES = "nar.properties";

  private final String groupId, artifactId, version;

  private final Properties info;

  private final Log log;

  public NarInfo(final String groupId, final String artifactId, final String version, final Log log)
      throws MojoExecutionException {
    this(groupId, artifactId, version, log, null);
  }

  @SuppressWarnings("this-escape") // getNarInfoFileName() is a stable overridable helper
  public NarInfo(final String groupId, final String artifactId, final String version, final Log log, File propertiesFile)
      throws MojoExecutionException {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.log = log;
    this.info = new Properties();

    // Fill with general properties.nar file
    if (propertiesFile != null) {
      try {
        if (propertiesFile.isDirectory()) {
          propertiesFile = new File(propertiesFile, getNarInfoFileName());
        }
        this.info.load(new FileInputStream(propertiesFile));
      } catch (final FileNotFoundException e) {
        // ignored
      } catch (final IOException e) {
        throw new MojoExecutionException("Problem loading " + propertiesFile, e);
      }
    }
  }

  public final void addNar(final AOL aol, final String type, final String nar) {
    String nars = getProperty(aol, NarConstants.NAR + "." + type);
    nars = nars == null ? nar : nars + ", " + nar;
    setProperty(aol, NarConstants.NAR + "." + type, nars);
  }

  public final boolean exists(final JarFile jar) {
    return getNarPropertiesEntry(jar) != null;
  }

  public final AOL getAOL(final AOL aol) {
    return aol == null ? null : new AOL(getProperty(aol, aol.toString(), aol.toString()));
  }

  // FIXME replace with list of AttachedNarArtifacts
  public final String[] getAttachedNars(final AOL aol, final String type) {
    final String attachedNars = getProperty(aol, NarConstants.NAR + "." + type);
    return attachedNars != null ? attachedNars.split(",") : null;
  }

  /**
   * No binding means default binding.
   *
   * @param aol
   * @return
   */
  public final String getBinding(final AOL aol, final String defaultBinding) {
    return getProperty(aol, "libs.binding", defaultBinding);
  }

  public final String getExactProperty(final AOL aol, final String key, final String defaultValue) {
    if (key == null) {
      throw new NullPointerException();
    }
    final String value = this.info.getProperty((aol == null ? "" : aol.toString() + ".") + key, defaultValue);
    this.log.debug("getExactProperty(" + aol + ", " + key + ", " + defaultValue + ") = " + value);
    return value;
  }

  public final String getLibs(final AOL aol) {
    // resolve output Vs libs.names
    return getProperty(aol, "libs.names", getOutput(aol, this.artifactId + "-" + this.version));
  }
  
  public final String getIncludesType(final AOL aol) {
    return getProperty(aol, "includes.type", "local");
  }

  public String getNarInfoFileName() {
    return "META-INF/nar/" + this.groupId + "/" + this.artifactId + "/" + NAR_PROPERTIES;
  }

  private JarEntry getNarPropertiesEntry(final JarFile jar) {
    return jar.getJarEntry(getNarInfoFileName());
  }

  public final String getOptions(final AOL aol) {
    return getProperty(aol, "linker.options");
  }

  public final String getOutput(final AOL aol, final String defaultOutput) {
    return getExactProperty(aol, "output", defaultOutput);
  }

  public final String getProperty(final AOL aol, final String key) {
    return getProperty(aol, key, (String) null);
  }

  public final boolean getProperty(final AOL aol, final String key, final boolean defaultValue) {
    return Boolean.valueOf(getProperty(aol, key, String.valueOf(defaultValue))).booleanValue();
  }

  public final File getProperty(final AOL aol, final String key, final File defaultValue) {
    return new File(getProperty(aol, key, defaultValue.getPath()));
  }

  public final int getProperty(final AOL aol, final String key, final int defaultValue) {
    return Integer.parseInt(getProperty(aol, key, Integer.toString(defaultValue)));
  }

  public final String getProperty(final AOL aol, final String key, final String defaultValue) {
    if (key == null) {
      return defaultValue;
    }
    String value = this.info.getProperty(key, defaultValue);
    value = aol == null ? value : this.info.getProperty(aol.toString() + "." + key, value);
    this.log.debug("getProperty(" + aol + ", " + key + ", " + defaultValue + ") = " + value);
    return value;
  }

  public final String getSysLibs(final AOL aol) {
    return getProperty(aol, "syslibs.names");
  }

  /**
   * Returns the external libraries (&lt;linker&gt;&lt;libs&gt;) recorded for this
   * nar, so a consuming project can link them too. Stored as indexed keys to
   * avoid delimiter clashes with paths (which contain ':' and '\\').
   *
   * @param aol the AOL to read for.
   * @return the list of external libraries, possibly empty (never null).
   */
  public final List<Lib> getExternalLibs(final AOL aol) {
    final List<Lib> result = new ArrayList<>();
    final int count = getProperty(aol, "external.libs.count", 0);
    for (int i = 0; i < count; i++) {
      final String prefix = "external.libs." + i + ".";
      final String name = getProperty(aol, prefix + "name");
      if (name == null || name.isEmpty()) {
        continue;
      }
      final String type = getProperty(aol, prefix + "type", Library.SHARED);
      final String directory = getProperty(aol, prefix + "directory");
      final String binDirectory = getProperty(aol, prefix + "binDirectory");
      result.add(new Lib(name, type, directory == null ? null : new File(directory),
          binDirectory == null ? null : new File(binDirectory)));
    }
    return result;
  }

  /**
   * Records the external libraries (&lt;linker&gt;&lt;libs&gt;) so consumers can
   * link them. Only leaf libraries (a name with a directory) are stored; grouped
   * libraries with nested &lt;libs&gt; are skipped.
   *
   * @param aol  the AOL to write for.
   * @param libs the libraries to record (may be null).
   */
  public final void setExternalLibs(final AOL aol, final List<Lib> libs) {
    if (libs == null) {
      return;
    }
    int index = 0;
    for (final Lib lib : libs) {
      if (lib.getName() == null || lib.getLibs() != null) {
        continue;
      }
      final String prefix = "external.libs." + index + ".";
      setProperty(aol, prefix + "name", lib.getName());
      setProperty(aol, prefix + "type", lib.getType());
      if (lib.getDirectory() != null) {
        setProperty(aol, prefix + "directory", lib.getDirectory().getPath());
      }
      if (lib.getBinDirectory() != null) {
        setProperty(aol, prefix + "binDirectory", lib.getBinDirectory().getPath());
      }
      index++;
    }
    if (index > 0) {
      setProperty(aol, "external.libs.count", Integer.toString(index));
    }
  }

  /**
   * Get the Group Id of the NarInfo instance
   * @return {@link String} representing a NarInfo object's Group Id.
   * @since 3.5.3
   */
  public String getGroupId() {
      return this.groupId;
  }

  /**
   * Get the Artifact Id of the NarInfo instance
   * @return {@link String} representing a NarInfo object's Artifact Id.
   * @since 3.5.3
   */
  public String getArtifactId() {
      return this.artifactId;
  }

  /**
   * Get the version of the NarInfo instance
   * @return {@link String} representing a NarInfo object's version.
   * @since 3.5.3
   */
  public String getVersion() {
    return this.version;
  }

  /**
   * Get the properties of the NarInfo instance
   * @return {@link Properties} representing a NarInfo object's properties.
   * @since 3.5.4
   */
  public Properties getInfo() {
    return this.info;
  }

  public final void read(final JarFile jar) throws IOException {
    this.info.load(jar.getInputStream(getNarPropertiesEntry(jar)));
  }

  public final void setBinding(final AOL aol, final String value) {
    setProperty(aol, "libs.binding", value);
  }

  public final void setNar(final AOL aol, final String type, final String nar) {
    setProperty(aol, NarConstants.NAR + "." + type, nar);
  }

  public final void setOutput(final AOL aol, final String value) {
    setProperty(aol, "output", value);
  }

  public final void setLibs(final AOL aol, final String value) {
    setProperty(aol, "libs.names", value);
  }
  
  public final void setIncludesType(final AOL aol, final String value) {
    setProperty(aol, "includes.type", value);
  }

  public final void setSysLibs(final AOL aol, final String value) {
    setProperty(aol, "syslibs.names", value);
  }

  private void setProperty(final AOL aol, final String key, final String value) {
    if (aol == null) {
      this.info.setProperty(key, value);
    } else {
      this.info.setProperty(aol.toString() + "." + key, value);
    }
  }

  public void mergeProperties(final Properties properties){
    this.getInfo().putAll(properties);
  }


  @Override
  public final String toString() {
    final StringBuilder s = new StringBuilder("NarInfo for ");
    s.append(this.groupId);
    s.append(":");
    s.append(this.artifactId);
    s.append("-");
    s.append(this.version);
    s.append(" {\n");

    for (final Object element : this.info.keySet()) {
      final String key = (String) element;
      s.append("   ");
      s.append(key);
      s.append("='");
      s.append(this.info.getProperty(key, "<null>"));
      s.append("'\n");
    }

    s.append("}\n");
    return s.toString();
  }

  public final void writeToDirectory(final File... directories) throws MojoExecutionException {
    for(File directory:directories) {
      try {
        writeToFile(new File(directory, getNarInfoFileName()));
      }
      catch (final IOException ioe) {
        throw new MojoExecutionException("Cannot write nar properties file to " + directory, ioe);
      }
    }
  }

  public final void writeToFile(final File file) throws IOException {
    final File parent = file.getParentFile();
    if (parent != null) {
      parent.mkdirs();
    }
    log.debug("Write NAR Properties: " + file.toString());
    this.info.store(new FileOutputStream(file), "NAR Properties for " + this.groupId + "." + this.artifactId + "-"
        + this.version);
  }
}
