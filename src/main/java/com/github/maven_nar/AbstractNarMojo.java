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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.gjs.java.maven.common.GjsAbstractMojo;

/**
 * @author Mark Donszelmann
 */
public abstract class AbstractNarMojo extends GjsAbstractMojo implements NarConstants {

	/**
	 * Skip running of NAR plugins (any) altogether.
	 */
	@Parameter(property = "nar.skip", defaultValue = "false")
	protected boolean skip;

	/**
	 * Skip the tests. Listens to Maven's general 'maven.skip.test'.
	 */
	@Parameter(property = "maven.test.skip")
	protected boolean skipTests;

	/**
	 * Ignore errors and failures.
	 */
	@Parameter(property = "nar.ignore", defaultValue = "false")
	protected boolean ignore;

	/**
	 * The Architecture for the nar, Some choices are: "x86", "i386", "amd64",
	 * "ppc", "sparc", ... Defaults to a derived value from ${os.arch}
	 */
	@Parameter(property = "nar.arch")
	protected String architecture;

	/**
	 * The Operating System for the nar. Some choices are: "Windows", "Linux",
	 * "MacOSX", "SunOS","AIX" ... Defaults to a derived value from ${os.name} FIXME
	 * table missing
	 */
	@Parameter(property = "nar.os")
	protected String os;

	/**
	 * Architecture-OS-Linker name. Defaults to: arch-os-linker.
	 */
	@Parameter(defaultValue = "")
	protected String aol;

	/**
	 * Linker
	 */
	@Parameter
	protected Linker linker;

	// these could be obtained from an injected project model.

	@Parameter(property = "project.build.outputDirectory", readonly = true)
	protected File classesDirectory;

	/**
	 * Name of the output - for jni
	 * default-value="${project.artifactId}-${project.version}" - for libs
	 * default-value="${project.artifactId}-${project.version}" - for exe
	 * default-value="${project.artifactId}" -- for tests
	 * default-value="${test.name}"
	 * 
	 */
	@Parameter
	protected String output;

	/**
	 * Target directory for Nar file construction. Defaults to
	 * "${project.build.directory}/nar" for "nar-compile" goal
	 */
	@Parameter
	protected File targetDirectory;

	/**
	 * Target directory for Nar test construction. Defaults to
	 * "${project.build.directory}/test-nar" for "nar-testCompile" goal
	 */
	@Parameter
	protected File testTargetDirectory;

	/**
	 * Target directory for Nar file unpacking. Defaults to "${targetDirectory}"
	 */
	@Parameter
	protected File unpackDirectory;

	/**
	 * Target directory for Nar test unpacking. Defaults to "${testTargetDirectory}"
	 */
	@Parameter
	protected File testUnpackDirectory;

	/**
	 * NARVersionInfo for Windows binaries
	 *
	 */
	@Parameter
	protected NARVersionInfo versionInfo;

	/**
	 * List of classifiers which you want download/unpack/assemble Example
	 * ppc-MacOSX-g++, x86-Windows-msvc, i386-Linux-g++. Not setting means all.
	 */
	@Parameter
	protected List<String> classifiers;

	/**
	 * List of libraries to create
	 */
	@Parameter
	protected List<Library> libraries;

	/**
	 * Name of the libraries included
	 */
	@Parameter
	protected String libsName;

	/**
	 * Skip running ranlib if this artifact is a library
	 */
	@Parameter(defaultValue = "false", required = true)
	protected boolean skipRanlib;

	/**
	 * Specifies the type of include this artifact may contain -- system or local
	 * (default)
	 */
	@Parameter(defaultValue = "local", required = true)
	protected String includesType;

	/**
	 * Indicates that the NAR plugin should do everything besides actually compiling
	 * or linking any source or object files. Typically used in conjunction with
	 * "replay" functionality.
	 */
	@Parameter(defaultValue = "false", required = true)
	protected boolean dryRun;

	/**
	 * Layout to be used for building and unpacking artifacts
	 */
	@Parameter(property = "nar.layout", defaultValue = "com.github.maven_nar.NarLayout21", required = true)
	protected String layout;

	protected NarLayout narLayout;

	@Parameter(defaultValue = "${project}", readonly = true)
	protected MavenProject mavenProject;

	protected AOL aolId;

	protected NarInfo narInfo;

	/**
	 * The home of the Java system. Defaults to a derived value from ${java.home}
	 * which is OS specific.
	 */
	@Parameter(readonly = true)
	protected File javaHome;

	@Parameter
	protected Replay replay;

	@Override
	protected boolean getSkip() {
		return skip;
	}

	@Override
	protected boolean getFail() {
		return !ignore;
	}

	@Override
	protected void goalExecution(Log log, List<String> errorMessages) throws Throwable {
		validate();
		narExecute();
	}

	protected final AOL getAOL() throws MojoFailureException, MojoExecutionException {
		return this.aolId;
	}

	protected final String getArchitecture() {
		return this.architecture;
	}

	protected final File getBasedir() {
		return this.baseDirectory;
	}

	protected final File getJavaHome(final AOL aol) throws MojoExecutionException {
		// FIXME should be easier by specifying default...
		return getNarInfo().getProperty(aol, "javaHome", NarUtil.getJavaHome(this.javaHome));
	}

	protected final NarLayout getLayout() throws MojoExecutionException {
		if (this.narLayout == null) {
			this.narLayout = AbstractNarLayout.getLayout(this.layout, getLog());
		}
		return this.narLayout;
	}

	protected final List<Library> getLibraries() {
		if (this.libraries == null) {
			this.libraries = Collections.emptyList();
		}
		return this.libraries;
	}

	protected final Linker getLinker() {
		return this.linker;
	}

	protected final NARVersionInfo getNARVersionInfo() {
		return versionInfo;
	}

	protected final MavenProject getMavenProject() {
		return this.mavenProject;
	}

	protected NarInfo getNarInfo() throws MojoExecutionException {
		if (this.narInfo == null) {
			final String groupId = getMavenProject().getGroupId();
			final String artifactId = getMavenProject().getArtifactId();
			final String path = "META-INF/nar/" + groupId + "/" + artifactId + "/" + NarInfo.NAR_PROPERTIES;
			File propertiesFile = new File(this.classesDirectory, path);
			// should not need to try and read from source.
			if (!propertiesFile.exists()) {
				propertiesFile = new File(getMavenProject().getBasedir(), "src/main/resources/" + path);
			}

			this.narInfo = new NarInfo(groupId, artifactId, getMavenProject().getVersion(), getLog(), propertiesFile);
		}
		return this.narInfo;
	}

	protected final String getOS() {
		return this.os;
	}

	protected final String getOutput(final boolean versioned) throws MojoExecutionException {
		if (this.output != null && !this.output.trim().isEmpty()) {
			return this.output;
		} else {
			if (versioned) {
				return getMavenProject().getArtifactId() + "-" + getMavenProject().getVersion();
			} else {
				return getMavenProject().getArtifactId();
			}
		}
	}

	protected final String getLibsName() throws MojoExecutionException {
		if (this.libsName != null && !this.libsName.trim().isEmpty()) {
			return this.libsName;
		} else {
			return null;
		}
	}

	protected final boolean isSkipRanlib() throws MojoExecutionException {
		return this.skipRanlib;
	}

	protected final String getIncludesType() throws MojoExecutionException {
		return this.includesType;
	}

	protected final File getOutputDirectory() {
		return this.outputDirectory;
	}

	protected final File getTargetDirectory() {
		return this.targetDirectory;
	}

	protected final File getTestTargetDirectory() {
		return this.testTargetDirectory;
	}

	protected final File getTestUnpackDirectory() {
		return this.testUnpackDirectory;
	}

	protected File getUnpackDirectory() {
		return this.unpackDirectory;
	}

	protected boolean isDryRun() {
		return dryRun;
	}

	protected void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}

	public abstract void narExecute() throws MojoFailureException, MojoExecutionException;

	protected final void validate() throws MojoFailureException, MojoExecutionException {

		this.architecture = NarUtil.getArchitecture(this.architecture);
		this.os = NarUtil.getOS(this.os);
		this.linker = NarUtil.getLinker(this.linker, getLog()); // linker name set in NarUtil.getAOL if not configured
		this.aolId = NarUtil.getAOL(this.mavenProject, this.architecture, this.os, this.linker, this.aol, getLog());

		final Model model = this.mavenProject.getModel();
		final Properties properties = model.getProperties();
		properties.setProperty("nar.arch", getArchitecture());
		properties.setProperty("nar.os", getOS());
		properties.setProperty("nar.linker", getLinker().getName());
		properties.setProperty("nar.aol", this.aolId.toString());
		properties.setProperty("nar.aol.key", this.aolId.getKey());
		model.setProperties(properties);

		if (this.targetDirectory == null) {
			this.targetDirectory = new File(this.mavenProject.getBuild().getDirectory(), "nar");
		}
		if (this.testTargetDirectory == null) {
			this.testTargetDirectory = new File(this.mavenProject.getBuild().getDirectory(), "test-nar");
		}

		if (this.unpackDirectory == null) {
			this.unpackDirectory = this.targetDirectory;
		}
		if (this.testUnpackDirectory == null) {
			this.testUnpackDirectory = this.testTargetDirectory;
		}
		if (this.replay != null) {
			if (this.replay.getOutputDirectory() == null) {
				this.replay.setOutputDirectory(new File(targetDirectory, "nar-replay"));
			}
			if (this.replay.getScriptDirectory() == null) {
				this.replay.setScriptDirectory(new File(this.replay.getOutputDirectory(), "scripts"));
			}
		}

		if (this.replay != null && this.replay.getScripts() != null) {
			Set<String> replayIds = new HashSet<String>();
			for (Script s : this.replay.getScripts()) {
				if (replayIds.contains(s.getId()))
					throw new MojoFailureException("Replay id must be unique");
				replayIds.add(s.getId());
			}
		}
	}

	public Replay getReplay() {
		return replay;
	}

	public void setReplay(Replay replay) {
		this.replay = replay;
	}

	protected void createReplayDirs() {
		if (replay != null) {

			replay.getOutputDirectory().mkdirs();
			replay.getScriptDirectory().mkdirs();
		}
	}
}
