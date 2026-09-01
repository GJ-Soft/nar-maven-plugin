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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.maven_nar.cpptasks.CompilerDef;
import com.github.maven_nar.cpptasks.types.CommandLineArgument;
import com.github.maven_nar.cpptasks.types.UndefineArgument;

/**
 * Publishes the directories holding the headers a scope of this project
 * compiles against: its own, and those of every NAR dependency unpacked under
 * the target area. The point is to hand a tool, an IDE in particular, what it
 * needs to resolve the includes of the sources, without it having to know
 * anything about the NAR layout.
 * <p>
 * The result is offered three ways at once, so that each consumer takes the one
 * that suits it: a plain list of directories, a Visual Studio Code C/C++
 * configuration, and a project property. All of them land in the target
 * directory of the scope, which is what "mvn clean" wipes and what no one else
 * owns. Where the configuration of an IDE has to end up depends on which folder
 * it was opened on, something only that IDE knows, so composing these into it is
 * left to whoever does know.
 * <p>
 * There is one goal per scope, so that each is bound where its headers are
 * ready and still ahead of the compilation that needs them. Reporting on both at
 * a single point would leave the sources waiting behind the compilation of the
 * project, which is of no use to whoever is trying to edit a project that does
 * not compile yet.
 */
public abstract class AbstractNarIncludePathMojo extends AbstractCompileMojo {

	/**
	 * Name of the file the include directories are written to, one absolute
	 * directory per line. It is the plainest of the outputs, and the one meant to be
	 * read by anything that is not Maven. Set to an empty value to write none.
	 */
	@Parameter(property = "nar.includePath.fileName", defaultValue = "nar-include-path.txt")
	private String includePathFileName;

	/**
	 * Name of the Visual Studio Code C/C++ configuration file. Set to an empty value
	 * to write none.
	 */
	@Parameter(property = "nar.includePath.vscodeFileName", defaultValue = "c_cpp_properties.json")
	private String vscodeFileName;

	/**
	 * Name of the configuration inside the Visual Studio Code file. Defaults to the
	 * artifact id, so that the configurations of the several modules of a project
	 * can be told apart once gathered together. The one of the tests takes a "-test"
	 * suffix.
	 */
	@Parameter(property = "nar.includePath.vscodeConfigName", defaultValue = "${project.artifactId}")
	private String vscodeConfigName;

	/**
	 * Name of the project property the include directories are published under,
	 * joined by the path separator of the platform. The one of the tests takes a
	 * ".test" suffix. Only the plugins running later in the same build can read it.
	 */
	@Parameter(property = "nar.includePath.property", defaultValue = "nar.include.path")
	private String includePathProperty;

	/**
	 * The compiler written to the Visual Studio Code configuration. Defaults to the
	 * name of the configured C compiler, which is looked up on the PATH the same way
	 * the build does. Set it to an absolute path when several toolchains are on the
	 * PATH and the IDE has to be pointed at a particular one.
	 */
	@Parameter(property = "nar.includePath.compilerPath")
	private String compilerPath;

	/**
	 * The IntelliSense mode written to the Visual Studio Code configuration.
	 * Defaults to one composed from the operating system, the compiler and the
	 * architecture, such as "windows-gcc-x64".
	 */
	@Parameter(property = "nar.includePath.intelliSenseMode")
	private String intelliSenseMode;

	/**
	 * Skips the publishing of the include path altogether. These goals sit in the
	 * NAR lifecycle so that the files are there for an IDE without anyone having to
	 * configure it, which is of no use to a build that has no IDE looking at it.
	 */
	@Parameter(property = "nar.includePath.skip", defaultValue = "false")
	private boolean skipIncludePath;

	/**
	 * When true the dependencies are unpacked before being reported, so that the
	 * goal is of use on its own against a project that has never been built:
	 *
	 * <pre>
	 * mvn nar:nar-include-path -Dnar.includePath.unpack=true
	 * </pre>
	 *
	 * It defaults to false because unpacking is neither cached nor cheap, it
	 * extracts every NAR afresh and runs ranlib over the libraries, and inside a
	 * build the unpack goals have already done it.
	 */
	@Parameter(property = "nar.includePath.unpack", defaultValue = "false")
	private boolean unpack;

	/**
	 * When true nothing is printed to the log, the files and the property are still
	 * produced.
	 */
	@Parameter(property = "nar.includePath.quiet", defaultValue = "false")
	private boolean quiet;

	/**
	 * When true the include directories are written to the standard output, joined
	 * by the path separator of the platform and with nothing else around them. It is
	 * meant to be combined with the quiet flag of Maven, so that the whole of the
	 * output is the one line a script is after:
	 *
	 * <pre>
	 * mvn -q nar:nar-include-path -Dnar.includePath.stdout=true
	 * </pre>
	 */
	@Parameter(property = "nar.includePath.stdout", defaultValue = "false")
	private boolean stdout;

	/**
	 * The scope this goal reports on, {@link Compiler#MAIN} or
	 * {@link Compiler#TEST}.
	 */
	protected abstract String getScope();

	/**
	 * The directory the files of this scope are written to.
	 */
	protected abstract File getPublishDirectory();

	/**
	 * The executables whose dependencies are unpacked for this scope, the libraries
	 * of the project or its tests.
	 */
	protected abstract List<? extends Executable> getExecutables();

	/**
	 * Tells whether there is anything to report on at all, so that a scope switched
	 * off elsewhere, the tests through skipTests, is left alone.
	 */
	protected boolean shouldPublish() {
		return true;
	}

	@Override
	public final void narExecute() throws MojoExecutionException, MojoFailureException {
		if (this.skipIncludePath) {
			getLog().debug("Not publishing the include path, it is skipped");
			return;
		}
		if (!shouldPublish()) {
			return;
		}

		if (this.unpack) {
			unpackAttachedNars(getAttachedNarArtifacts(getExecutables()));
		}

		final Set<String> includeDirectories = collectIncludeDirectories();
		if (includeDirectories.isEmpty()) {
			getLog().warn("No include directories found for the " + getScope() + " scope. Dependencies are only"
					+ " visible once unpacked, so either let the unpack goals run first or ask this goal to do it"
					+ " with -Dnar.includePath.unpack=true.");
		}

		publishProperty(includeDirectories);
		writeFiles(includeDirectories);

		if (!this.quiet) {
			getLog().info("NAR " + getScope() + " include path (" + includeDirectories.size() + " directories):");
			for (final String includeDirectory : includeDirectories) {
				getLog().info("  " + includeDirectory);
			}
		}

		if (this.stdout) {
			// deliberately not through the log: the point is to leave the console holding
			// the bare string and nothing else
			System.out.println(String.join(File.pathSeparator, includeDirectories));
		}
	}

	/**
	 * Gathers every directory holding headers this scope compiles against, in the
	 * order the compiler is given them and without repetitions.
	 * <p>
	 * The headers of the project itself are taken from their source location rather
	 * than from the copy the build leaves under the target area: that copy is only
	 * made at compile time, and pointing an IDE at the original is what makes
	 * jumping to a definition land on the file that can be edited.
	 * <p>
	 * The tests see the headers of the project as well as their own, which is what
	 * lets a test include what it is testing.
	 */
	private Set<String> collectIncludeDirectories() throws MojoExecutionException, MojoFailureException {
		final Set<String> includeDirectories = new LinkedHashSet<>();

		// the include paths of the project, as configured on each compiler
		for (final Compiler compiler : getConfiguredCompilers()) {
			if (Compiler.TEST.equals(getScope())) {
				addExisting(includeDirectories, activeIncludePaths(compiler, Compiler.TEST));
			}
			addExisting(includeDirectories, activeIncludePaths(compiler, Compiler.MAIN));
		}

		// the include directories of the dependencies, as unpacked for this scope:
		// nar-unpack fills the target area, nar-test-unpack the test one
		for (final File directory : getDependencyIncludeDirectories(getUnpackDirectory())) {
			includeDirectories.add(directory.getAbsolutePath());
		}

		// the system include paths go last, as they do on the command line
		for (final Compiler compiler : getConfiguredCompilers()) {
			final CompilerDef compilerDef = compilerDef(compiler);
			if (compilerDef != null) {
				addExisting(includeDirectories, compilerDef.getActiveSysIncludePaths());
			}
		}

		return includeDirectories;
	}

	private String[] activeIncludePaths(final Compiler compiler, final String type)
			throws MojoExecutionException, MojoFailureException {
		final CompilerDef compilerDef = Compiler.TEST.equals(type) ? compiler.getTestCompiler(type, null)
				: compiler.getCompiler(type, null);
		return compilerDef == null ? null : compilerDef.getActiveIncludePaths();
	}

	/**
	 * The definition of the given compiler for this scope. The test scope goes
	 * through the test compiler, so that whatever the {@code <testOptions>} carry, a
	 * define among it, is accounted for.
	 */
	private CompilerDef compilerDef(final Compiler compiler) throws MojoExecutionException, MojoFailureException {
		return Compiler.TEST.equals(getScope()) ? compiler.getTestCompiler(getScope(), null)
				: compiler.getCompiler(getScope(), null);
	}

	/**
	 * The compilers that are actually configured, so that the ones left at their
	 * defaults for a language this project does not use contribute nothing.
	 */
	private List<Compiler> getConfiguredCompilers() throws MojoExecutionException, MojoFailureException {
		final List<Compiler> compilers = new ArrayList<>();
		for (final Compiler compiler : new Compiler[] {
				getCpp(), getC(), getFortran()
		}) {
			if (compiler != null && compiler.getName() != null) {
				compilers.add(compiler);
			}
		}
		return compilers;
	}

	private void addExisting(final Set<String> includeDirectories, final String[] paths) {
		if (paths == null) {
			return;
		}
		for (final String path : paths) {
			final File directory = new File(path);
			if (directory.exists()) {
				includeDirectories.add(directory.getAbsolutePath());
			} else {
				getLog().debug("Skipping non-existing include directory " + directory);
			}
		}
	}

	/**
	 * The preprocessor definitions the compilers are given, in the "NAME" or
	 * "NAME=VALUE" shape an IDE expects. They matter as much as the directories do:
	 * headers that switch on a define resolve to the wrong branch without them.
	 */
	private Set<String> collectDefines() throws MojoExecutionException, MojoFailureException {
		final Set<String> defines = new LinkedHashSet<>();

		for (final Compiler compiler : getConfiguredCompilers()) {
			final CompilerDef compilerDef = compilerDef(compiler);
			if (compilerDef == null) {
				continue;
			}

			// those declared through <defines> and <defineSet>, plus the defaults of the AOL
			for (final UndefineArgument define : compilerDef.getActiveDefines()) {
				if (define.isDefine() && define.getName() != null) {
					defines.add(
							define.getValue() == null ? define.getName() : define.getName() + "=" + define.getValue());
				}
			}

			// and those smuggled in as plain "-D" compiler options, which is how the
			// platform switches tend to be passed
			for (final CommandLineArgument argument : compilerDef.getActiveProcessorArgs()) {
				final String value = argument.getValue();
				if (value != null && value.startsWith("-D") && value.length() > 2) {
					defines.add(value.substring(2));
				}
			}
		}
		return defines;
	}

	/**
	 * The language standard given to a compiler through "-std=", or null when none
	 * is configured.
	 */
	private String getLanguageStandard(final Compiler compiler) throws MojoExecutionException, MojoFailureException {
		final CompilerDef compilerDef = compilerDef(compiler);
		if (compilerDef == null) {
			return null;
		}
		for (final CommandLineArgument argument : compilerDef.getActiveProcessorArgs()) {
			final String value = argument.getValue();
			if (value != null && value.startsWith("-std=")) {
				return value.substring("-std=".length());
			}
		}
		return null;
	}

	private void publishProperty(final Set<String> includeDirectories) {
		if (this.includePathProperty == null || this.includePathProperty.isEmpty()) {
			return;
		}
		final String property = Compiler.TEST.equals(getScope()) ? this.includePathProperty + ".test"
				: this.includePathProperty;
		final String joined = String.join(File.pathSeparator, includeDirectories);
		getMavenProject().getProperties().setProperty(property, joined);
		getLog().debug("Set property " + property + " to " + joined);
	}

	private void writeFiles(final Set<String> includeDirectories) throws MojoExecutionException, MojoFailureException {
		final File directory = getPublishDirectory();
		if (directory == null) {
			return;
		}

		if (this.includePathFileName != null && !this.includePathFileName.isEmpty()) {
			final StringBuilder contents = new StringBuilder();
			for (final String includeDirectory : includeDirectories) {
				contents.append(includeDirectory).append(System.lineSeparator());
			}
			write(new File(directory, this.includePathFileName), contents.toString());
		}

		if (this.vscodeFileName != null && !this.vscodeFileName.isEmpty()) {
			write(new File(directory, this.vscodeFileName), buildVscodeConfiguration(includeDirectories));
		}
	}

	private String buildVscodeConfiguration(final Set<String> includeDirectories)
			throws MojoExecutionException, MojoFailureException {
		final String configName = Compiler.TEST.equals(getScope()) ? this.vscodeConfigName + "-test"
				: this.vscodeConfigName;

		final StringBuilder json = new StringBuilder();
		json.append("{").append(System.lineSeparator());
		json.append("  \"version\": 4,").append(System.lineSeparator());
		json.append("  \"configurations\": [").append(System.lineSeparator());
		json.append("    {").append(System.lineSeparator());
		json.append("      \"name\": ").append(quote(configName)).append(",").append(System.lineSeparator());
		appendJsonArray(json, "includePath", includeDirectories);
		appendJsonArray(json, "defines", collectDefines());

		final Compiler c = getC();
		final Compiler cpp = getCpp();

		final String compiler = this.compilerPath != null && !this.compilerPath.isEmpty() ? this.compilerPath
				: c != null ? c.getName() : null;
		if (compiler != null) {
			json.append("      \"compilerPath\": ").append(quote(compiler)).append(",").append(System.lineSeparator());
		}

		// The very same <languageStandard> tends to be configured on both compilers,
		// so each standard is only written where it belongs: an IDE given a C standard
		// as its C++ one just rejects the configuration.
		if (c != null && c.getName() != null) {
			final String standard = getLanguageStandard(c);
			if (isCStandard(standard)) {
				json.append("      \"cStandard\": ").append(quote(standard)).append(",")
						.append(System.lineSeparator());
			}
		}
		if (cpp != null && cpp.getName() != null) {
			final String standard = getLanguageStandard(cpp);
			if (isCppStandard(standard)) {
				json.append("      \"cppStandard\": ").append(quote(standard)).append(",")
						.append(System.lineSeparator());
			}
		}

		json.append("      \"intelliSenseMode\": ").append(quote(getIntelliSenseMode()))
				.append(System.lineSeparator());
		json.append("    }").append(System.lineSeparator());
		json.append("  ]").append(System.lineSeparator());
		json.append("}").append(System.lineSeparator());

		return json.toString();
	}

	private void appendJsonArray(final StringBuilder json, final String name, final Set<String> values) {
		json.append("      ").append(quote(name)).append(": [").append(System.lineSeparator());
		int remaining = values.size();
		for (final String value : values) {
			json.append("        ").append(quote(value));
			if (--remaining > 0) {
				json.append(",");
			}
			json.append(System.lineSeparator());
		}
		json.append("      ],").append(System.lineSeparator());
	}

	/**
	 * A C++ standard is the only thing an IDE accepts as such, and it is told apart
	 * by its shape: "c++17", "gnu++20" and the like.
	 */
	private boolean isCppStandard(final String standard) {
		return standard != null && (standard.startsWith("c++") || standard.startsWith("gnu++"));
	}

	/**
	 * A C standard is anything not shaped like a C++ one, that is "c17", "gnu2x" and
	 * the like.
	 */
	private boolean isCStandard(final String standard) {
		return standard != null && !standard.isEmpty() && !isCppStandard(standard);
	}

	/**
	 * The IntelliSense mode of the Visual Studio Code extension, composed the way it
	 * expects it: operating system, compiler family and architecture.
	 */
	private String getIntelliSenseMode() throws MojoExecutionException, MojoFailureException {
		if (this.intelliSenseMode != null && !this.intelliSenseMode.isEmpty()) {
			return this.intelliSenseMode;
		}

		final String os;
		if (OS.WINDOWS.equals(getOS())) {
			os = "windows";
		} else if (OS.MACOSX.equals(getOS())) {
			os = "macos";
		} else {
			os = "linux";
		}

		final String linker = getLinker() == null ? null : getLinker().getName();
		final String family;
		if (linker != null && linker.contains("clang")) {
			family = "clang";
		} else if (linker != null && linker.contains("msvc")) {
			family = "msvc";
		} else {
			family = "gcc";
		}

		final String architecture = getArchitecture();
		final String bits = architecture != null && architecture.contains("64") ? "x64" : "x86";

		return os + "-" + family + "-" + bits;
	}

	private String quote(final String value) {
		final StringBuilder quoted = new StringBuilder("\"");
		for (int i = 0; i < value.length(); i++) {
			final char c = value.charAt(i);
			switch (c) {
				case '"':
					quoted.append("\\\"");
					break;
				case '\\':
					quoted.append("\\\\");
					break;
				case '\n':
					quoted.append("\\n");
					break;
				case '\r':
					quoted.append("\\r");
					break;
				case '\t':
					quoted.append("\\t");
					break;
				default:
					quoted.append(c);
			}
		}
		return quoted.append("\"").toString();
	}

	private void write(final File file, final String contents) throws MojoExecutionException {
		try {
			final File parent = file.getParentFile();
			if (parent != null) {
				parent.mkdirs();
			}
			Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
			getLog().info("Wrote " + file);
		} catch (final IOException e) {
			throw new MojoExecutionException("NAR: Could not write " + file, e);
		}
	}
}
