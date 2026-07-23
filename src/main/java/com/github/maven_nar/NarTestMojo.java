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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Tests NAR files. Runs Native Tests and executables if produced.
 *
 * @author Mark Donszelmann
 */
@Mojo(name = "nar-test", defaultPhase = LifecyclePhase.TEST, requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST)
public class NarTestMojo extends AbstractCompileMojo {

	@Override
	protected String getGoalName() {
		return "nar-test";
	}

	/**
	 * The classpath elements of the project being tested.
	 */
	@Parameter(defaultValue = "${project.testClasspathElements}", required = true, readonly = true)
	private List<String> classpathElements;

	/**
	 * Directory for test resources. Defaults to src/test/resources
	 */
	@Parameter(defaultValue = "${basedir}/src/test/resources", required = true)
	private File testResourceDirectory;

	private String[] generateEnvironment(Map<String, String> environmentVariables)
			throws MojoExecutionException, MojoFailureException {
		List<String> env = new ArrayList<>();

		// Add the default configured environment
		Collections.addAll(env, generateEnvironment());

		// Add manually specified environment variables
		for (Map.Entry<String, String> envVar : environmentVariables.entrySet()) {
			getLog().debug("Adding environment variable: " + envVar.getKey() + " with value: " + envVar.getValue());
			env.add(envVar.getKey() + "=" + envVar.getValue());
		}

		return env.toArray(new String[env.size()]);
	}

	private String[] generateEnvironment() throws MojoExecutionException, MojoFailureException {
		final List<String> env = new ArrayList<>();

		final Set<File> sharedPaths = new HashSet<>();

		// add all shared libraries of this package
		for (final Library lib : getLibraries()) {
			if (lib.getType().equals(Library.SHARED)) {
				final File path = getLayout().getLibDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
						getMavenProject().getVersion(), getAOL().toString(), lib.getType());
				getLog().debug("Adding path to shared library: " + path);
				sharedPaths.add(path);
			}
		}

		// add dependent shared libraries
		final String classifier = getAOL() + "-shared";
		final List<NarArtifact> narArtifacts = getNarArtifacts();
		final List<AttachedNarArtifact> dependencies = getNarManager().getAttachedNarDependencies(narArtifacts,
				classifier);
		for (final AttachedNarArtifact dependency : dependencies) {
			getLog().debug("Looking for dependency " + dependency);

			// FIXME reported to maven developer list, isSnapshot
			// changes behaviour
			// of getBaseVersion, called in pathOf.
			dependency.isSnapshot();

			final File libDirectory = getLayout().getLibDirectory(getUnpackDirectory(), dependency.getArtifactId(),
					dependency.getBaseVersion(), getAOL().toString(), Library.SHARED);
			sharedPaths.add(libDirectory);
		}

		// set environment
		if (!sharedPaths.isEmpty()) {
			final StringBuilder sharedPath = new StringBuilder();
			for (final File path : sharedPaths) {
				if (sharedPath.length() > 0) {
					sharedPath.append(File.pathSeparator);
				}
				sharedPath.append(path.getPath());
			}

			final String sharedEnv = NarUtil.addLibraryPathToEnv(sharedPath.toString(), null, getOS());
			env.add(sharedEnv);
		}

		// necessary to find WinSxS
		if (getOS().equals(OS.WINDOWS)) {
			env.add("SystemRoot=" + NarUtil.getEnv("SystemRoot", "SystemRoot", "C:\\Windows"));
		}

		// add CLASSPATH
		env.add("CLASSPATH=" + StringUtils.join(this.classpathElements.iterator(), File.pathSeparator));

		return env.toArray(new String[0]);
	}

	/**
	 * List the dependencies needed for tests executions and for executables
	 * executions, those dependencies are used to declare the paths of shared
	 * libraries for execution.
	 */
	@Override
	protected ScopeFilter getArtifactScopeFilter() {
		return new ScopeFilter(Artifact.SCOPE_TEST, null);
	}

	@Override
	protected File getUnpackDirectory() {
		return getTestUnpackDirectory() == null ? super.getUnpackDirectory() : getTestUnpackDirectory();
	}

	@Override
	public final void narExecute() throws MojoExecutionException, MojoFailureException {
		if (this.skipTests || this.dryRun) {
			getLog().info("Tests are skipped");
		} else {

			// run all tests
			for (final Test test : getTests()) {
				runTest(test);
			}

			for (final Library library : getLibraries()) {
				runExecutable(library);
			}
		}
	}

	private void runExecutable(final Library library) throws MojoExecutionException, MojoFailureException {
		if (library.getType().equals(Library.EXECUTABLE) && library.shouldRun()) {
			final MavenProject project = getMavenProject();
			// FIXME NAR-90, we could make sure we get the final name from layout
			final String extension = getOS().equals(OS.WINDOWS) ? ".exe" : "";
			final File executable = new File(getLayout().getBinDirectory(getTargetDirectory(),
					getMavenProject().getArtifactId(), getMavenProject().getVersion(), getAOL().toString()),
					project.getArtifactId() + extension);
			if (!executable.exists()) {
				getLog().warn("Skipping non-existing executable " + executable);
				return;
			}
			getLog().info("Running executable " + executable);
			final List<String> args = library.getArgs();
			final int result = NarUtil.runCommand(executable.getPath(), args.toArray(new String[0]), null,
					generateEnvironment(), getLog());
			if (result != 0) {
				throw new MojoFailureException("Test " + executable + " failed with exit code: " + result + " 0x"
						+ Integer.toHexString(result));
			}
		}
	}

	private void runTest(final Test test) throws MojoExecutionException, MojoFailureException {
		// run if requested
		if (test.shouldRun()) {
			// NOTE should we use layout here ?
			final String name = test.getName() + (getOS().equals(OS.WINDOWS) ? ".exe" : "");
			File path = new File(getTestTargetDirectory(), "bin");
			path = new File(path, getAOL().toString());
			path = new File(path, name);
			if (!path.exists()) {
				getLog().warn("Skipping non-existing test " + path);
				return;
			}

			final File workingDir = new File(getTestTargetDirectory(), "test-reports");
			workingDir.mkdirs();

			// Copy test resources
			try {
				int copied = 0;
				if (this.testResourceDirectory.exists()) {
					copied += NarUtil.copyDirectoryStructure(this.testResourceDirectory, workingDir, null,
							NarUtil.DEFAULT_EXCLUDES);
				}
				getLog().info("Copied " + copied + " test resources");
			} catch (final IOException e) {
				throw new MojoExecutionException("NAR: Could not copy test resources", e);
			}

			getLog().info("Running test " + name + " in " + workingDir);

			final List<String> args = test.getArgs();
			final int result = NarUtil.runCommand(path.toString(), args.toArray(new String[0]),
					workingDir, generateEnvironment(test.getEnvironmentVariables()), getLog());
			if (result != 0) {
				throw new MojoFailureException(
						"Test " + name + " failed with exit code: " + result + " 0x" + Integer.toHexString(result));
			}
		}
	}
}
