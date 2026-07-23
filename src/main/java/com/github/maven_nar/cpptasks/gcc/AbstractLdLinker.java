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
package com.github.maven_nar.cpptasks.gcc;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinkerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

/**
 * Abstract adapter for ld-like linkers
 *
 * @author Curt Arnold
 */
public abstract class AbstractLdLinker extends CommandLineLinker {
  private final String outputPrefix;

  protected AbstractLdLinker(final String command, final String identifierArg, final String[] extensions,
      final String[] ignoredExtensions, final String outputPrefix, final String outputSuffix, final boolean isLibtool,
      final AbstractLdLinker libtoolLinker) {
    super(command, identifierArg, extensions, ignoredExtensions, outputSuffix, isLibtool, libtoolLinker);
    this.outputPrefix = outputPrefix;
  }

  @Override
  protected void addBase(final CCTask task, final long base, final List<String> args) {
    if (base >= 0) {
      args.add("--image-base");
      args.add(Long.toHexString(base));
    }
  }

  @Override
  protected void addEntry(final CCTask task, final String entry, final List<String> args) {
    if (entry != null) {
      args.add("-e");
      args.add(entry);
    }
  }

  @Override
  protected void addImpliedArgs(final CCTask task, final boolean debug, final LinkType linkType,
      final List<String> args) {
    if (debug) {
      args.add("-g");
    }
    if (isDarwin()) {
      if (linkType.isPluginModule()) {
        args.add("-bundle");
        // BEGINFREEHEP
      } else if (linkType.isJNIModule()) {
        args.add("-dynamic");
        args.add("-bundle");
        // ENDFREEHEP
      } else {
        if (linkType.isSharedLibrary()) {
          // FREEHEP no longer needed for 10.4+
          // args.add("-prebind");
          args.add("-dynamiclib");
        }
      }
    } else {
      if (linkType.isStaticRuntime()) {
        args.add("-static");
      }
      if (linkType.isPluginModule()) {
        args.add("-shared");
      } else {
        if (linkType.isSharedLibrary()) {
          args.add("-shared");
        }
      }
    }
  }

  @Override
  protected void addIncremental(final CCTask task, final boolean incremental, final List<String> args) {
    if (incremental) {
      args.add("-i");
    }
  }

  @Override
  protected void addLibraryPath(final List<String> preargs, final String path) {
    preargs.add("-L" + path);
  }

  protected int addLibraryPatterns(final String[] libnames, final StringBuilder buf, final String prefix,
      final String extension, final String[] patterns, final int offset) {
    for (int i = 0; i < libnames.length; i++) {
      buf.setLength(0);
      buf.append(prefix);
      buf.append(libnames[i]);
      buf.append(extension);
      patterns[offset + i] = buf.toString();
    }
    return offset + libnames.length;
  }

  @Override
  protected String[] addLibrarySets(final CCTask task, final LibrarySet[] libsets, final List<String> preargs,
      final List<String> midargs, final List<String> endargs) {
    final List<String> libnames = new ArrayList<>();
    super.addLibrarySets(task, libsets, preargs, midargs, endargs);
    LibraryTypeEnum previousLibraryType = null;
    for (final LibrarySet libset : libsets) {
      final LibrarySet set = libset;
      final File libdir = set.getDir(null);
      final String[] libs = set.getLibs();
      if (libdir != null) {
        String relPath = libdir.getAbsolutePath();
        // File outputFile = task.getOutfile();
        final File currentDir = new File(".");
        if (currentDir.getParentFile() != null) {
          relPath = CUtil.getRelativePath(currentDir.getParentFile().getAbsolutePath(), libdir);
        }
        if (set.getType() != null && "framework".equals(set.getType().getValue()) && isDarwin()) {
          endargs.add("-F" + relPath);
        } else {
          endargs.add("-L" + relPath);
        }
      }
      //
      // if there has been a change of library type
      //
      if (set.getType() != previousLibraryType) {
        if (set.getType() != null && "static".equals(set.getType().getValue())) {
          // BEGINFREEHEP not on MacOS X
          if (!isDarwin()) {
            endargs.add(getStaticLibFlag());
            previousLibraryType = set.getType();
          }
          // ENDFREEHEP
        } else {
          // FREEHEP not on MacOS X, recheck this!
          if (set.getType() == null || !"framework".equals(set.getType().getValue()) && !isDarwin()) {
            endargs.add(getDynamicLibFlag());
            previousLibraryType = set.getType();
          }
        }
      }
      final StringBuilder buf = new StringBuilder("-l");
      if (set.getType() != null && "framework".equals(set.getType().getValue()) && isDarwin()) {
        buf.setLength(0);
        // FREEHEP, added as endarg w/o trailing space to avoid quoting!
        endargs.add("-framework");
      }
      final int initialLength = buf.length();
      for (final String lib : libs) {
        //
        // reset the buffer to just "-l"
        //
        buf.setLength(initialLength);
        //
        // add the library name
        buf.append(lib);
        libnames.add(lib);
        //
        // add the argument to the list
        endargs.add(buf.toString());
      }
    }

    // BEGINFREEHEP if last was -Bstatic reset it to -Bdynamic so that libc and
    // libm can be found as shareables
    if (previousLibraryType != null && previousLibraryType.getValue().equals("static") && !isDarwin()) {
      endargs.add(getDynamicLibFlag());
    }
    // ENDFREEHEP

    final String rc[] = new String[libnames.size()];
    for (int i = 0; i < libnames.size(); i++) {
      rc[i] = libnames.get(i);
    }
    return rc;
  }

  @Override
  protected void addMap(final CCTask task, final boolean map, final List<String> args) {
    if (map) {
      args.add("-M");
    }
  }

  @Override
  protected void addStack(final CCTask task, final int stack, final List<String> args) {
    if (stack > 0) {
      args.add("--stack");
      args.add(Integer.toString(stack));
    }
  }

  @Override
  public String getCommandFileSwitch(final String commandFile) {
    throw new IllegalStateException("ld does not support command files");
  }

  protected String getDynamicLibFlag() {
    return "-Bdynamic";
  }

  /**
   * Returns library path.
   *
   */
  protected File[] getEnvironmentIncludePath() {
    return CUtil.getPathFromEnvironment("LIB", ":");
  }

  @Override
  public String getLibraryKey(final File libfile) {
    final String libname = libfile.getName();
    final int lastDot = libname.lastIndexOf('.');
    if (lastDot >= 0) {
      return libname.substring(0, lastDot);
    }
    return libname;
  }

  /**
   * Returns library path.
   *
   */
  @Override
  public File[] getLibraryPath() {
    return new File[0];
  }

  @Override
  public String[] getLibraryPatterns(final String[] libnames, final LibraryTypeEnum libType) {
    final StringBuilder buf = new StringBuilder();
    int patternCount = libnames.length;
    if (libType == null) {
      patternCount *= 2;
    }
    final String[] patterns = new String[patternCount];
    int offset = 0;
    if (libType == null || "static".equals(libType.getValue())) {
      offset = addLibraryPatterns(libnames, buf, "lib", ".a", patterns, 0);
    }
    if (libType != null && "framework".equals(libType.getValue()) && isDarwin()) {
      for (final String libname : libnames) {
        buf.setLength(0);
        buf.append(libname);
        buf.append(".framework/");
        buf.append(libname);
        patterns[offset++] = buf.toString();
      }
    } else {
      if (libType == null || !"static".equals(libType.getValue())) {
        if (isHPUX()) {
          offset = addLibraryPatterns(libnames, buf, "lib", ".sl", patterns, offset);
        } else {
          offset = addLibraryPatterns(libnames, buf, "lib", ".so", patterns, offset);
        }
      }
    }
    return patterns;
  }

  @Override
  public int getMaximumCommandLength() {
    // FREEHEP
    return isWindows() ? 20000 : Integer.MAX_VALUE;
  }

  @Override
  public String[] getOutputFileNames(final String baseName, final VersionInfo versionInfo) {
    final String[] baseNames = super.getOutputFileNames(baseName, versionInfo);
    if (this.outputPrefix.length() > 0) {
      for (int i = 0; i < baseNames.length; i++) {
        baseNames[i] = this.outputPrefix + baseNames[i];
      }
    }
    return baseNames;
  }

  @Override
  public String[] getOutputFileSwitch(final String outputFile) {
    return GccProcessor.getOutputFileSwitch("-o", outputFile);
  }

  protected String getStaticLibFlag() {
    return "-Bstatic";
  }

  @Override
  public boolean isCaseSensitive() {
    return true;
  }

  protected boolean isHPUX() {
    final String osname = System.getProperty("os.name").toLowerCase();
    if (osname.contains("hp") && osname.contains("ux")) {
      return true;
    }
    return false;
  }

  /**
   * Prepares argument list for exec command. Will return null if command
   * line would exceed allowable command line buffer.
   *
   * @param outputFile
   *          linker output file
   * @param sourceFiles
   *          linker input files (.obj, .o, .res)
   * @param config
   *          linker configuration
   * @return arguments for runTask
   */
  @Override
  public String[] prepareArguments(final CCTask task, final String outputDir, final String outputFile,
      final String[] sourceFiles, final CommandLineLinkerConfiguration config) {
    //
    // need to suppress sources that correspond to
    // library set entries since they are already
    // in the argument list
    final String[] libnames = config.getLibraryNames();
    if (libnames == null || libnames.length == 0) {
      return super.prepareArguments(task, outputDir, outputFile, sourceFiles, config);
    }
    //
    //
    // null out any sources that correspond to library names
    //
    final String[] localSources = sourceFiles.clone();
    int extra = 0;
    for (final String libname : libnames) {
      for (int j = 0; j < localSources.length; j++) {
        if (localSources[j] != null && localSources[j].indexOf(libname) > 0 && localSources[j].indexOf("lib") > 0) {
          final String filename = new File(localSources[j]).getName();
          if (filename.startsWith("lib") && filename.substring(3).startsWith(libname)) {
            final String extension = filename.substring(libname.length() + 3);
            if (extension.equals(".a") || extension.equals(".so") || extension.equals(".sl")) {
              localSources[j] = null;
              extra++;
            }
          }
        }
      }
    }
    if (extra == 0) {
      return super.prepareArguments(task, outputDir, outputFile, sourceFiles, config);
    }
    final String[] finalSources = new String[localSources.length - extra];
    int index = 0;
    for (final String localSource : localSources) {
      if (localSource != null) {
        finalSources[index++] = localSource;
      }
    }
    return super.prepareArguments(task, outputDir, outputFile, finalSources, config);
  }
}
