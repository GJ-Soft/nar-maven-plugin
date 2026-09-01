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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.LinkerDef;

/**
 * @author Mark Donszelmann
 */
public abstract class AbstractCompileMojo extends AbstractDependencyMojo {

	/**
	 * C++ Compiler
	 */
	@Parameter
	private Cpp cpp;

	/**
	 * C Compiler
	 */
	@Parameter
	private C c;

	/**
	 * Fortran Compiler
	 */
	@Parameter
	private Fortran fortran;

	/**
	 * Assembler Compiler
	 *
	 */
	@Parameter
	private Assembler assembler;

	/**
	 * Resource Compiler
	 */
	@Parameter
	private Resource resource;

	/**
	 * IDL Compiler
	 */
	@Parameter
	private IDL idl;

	/**
	 * Message Compiler
	 */
	@Parameter
	private Message message;

	/**
	 * By default NAR compile will attempt to compile using all known compilers
	 * against files in the directories specified by convention. This allows
	 * configuration to a reduced set, you will have to specify each compiler to use
	 * in the configuration.
	 */
	@Parameter(defaultValue = "false")
	protected boolean onlySpecifiedCompilers;

	/**
	 * Do we log commands that is executed to produce the end-result? Conception was
	 * to allow eclipse to sniff out include-paths from compile.
	 */
	@Parameter
	protected int commandLogLevel = Project.MSG_VERBOSE;

	/**
	 * Maximum number of Cores/CPU's to use. 0 means unlimited.
	 */
	@Parameter
	private int maxCores = 0;

	/**
	 * Fail on compilation/linking error.
	 */
	@Parameter(defaultValue = "true", required = true)
	private boolean failOnError;

	/**
	 * Sets the type of runtime library, possible values "dynamic", "static".
	 */
	@Parameter(defaultValue = "dynamic", required = true)
	private String runtime;

	/**
	 * Set use of libtool. If set to true, the "libtool " will be prepended to the
	 * command line for compatible processors.
	 */
	@Parameter(defaultValue = "false", required = true)
	private boolean libtool;

	/**
	 * Forces project to specify all it's dependencies and not inherit transitive
	 * dependencies.
	 * 
	 * @since 3.5.3
	 */
	@Parameter(defaultValue = "false")
	protected boolean directDepsOnly;

	/**
	 * This parameter only has an effect on the AIX operating system for shared
	 * library projects. If set, the linker will output a shared object of the given
	 * name, and that shared object will be added to a shared archive using the
	 * normal output name.
	 */
	@Parameter(defaultValue = "")
	protected String sharedObjectName;
	/**
	 * List of tests to create
	 */
	@Parameter
	private List<Test> tests;

	/**
	 * Java info for includes and linking
	 */
	@Parameter
	private Java java;

	/**
	 * To support scanning the code with HPE Fortify.
	 * <p>
	 * The attribute functions as a flag that indicates Fortify is required, and the
	 * value is an ID, prepended to the command line as
	 * {@code sourceanalyzer –b <fortifyID>}.
	 * </p>
	 */
	@Parameter(defaultValue = "")
	private String fortifyID;

	/**
	 * Flag to cpptasks to indicate whether linker options should be decorated or
	 * not
	 */
	@Parameter
	protected boolean decorateLinkerOptions;

	/**
	 * Whether to automatically gather syslibs from dependencies
	 */
	@Parameter(defaultValue = "false", required = true)
	protected boolean syslibsFromDependencies;

	private List<String> dependencyLibOrder;

	private Project antProject;

	protected final List<String[]> compileCommands = new ArrayList<>();
	protected final List<String[]> linkCommands = new ArrayList<>();
	protected final List<String[]> testCompileCommands = new ArrayList<>();
	protected final List<String[]> testLinkCommands = new ArrayList<>();

	protected final boolean failOnError(final AOL aol) throws MojoExecutionException {
		return getNarInfo().getProperty(aol, "failOnError", this.failOnError);
	}

	protected final Project getAntProject() {
		if (this.antProject == null) {
			// configure ant project
			this.antProject = new Project();
			this.antProject.setName("NARProject");
			this.antProject.addBuildListener(new NarLogger(getLog()));
		}
		return this.antProject;
	}

	protected final C getC() {
		if (this.c == null && !this.onlySpecifiedCompilers) {
			setC(new C());
		}
		return this.c;
	}

	protected final Cpp getCpp() {
		if (this.cpp == null && !this.onlySpecifiedCompilers) {
			setCpp(new Cpp());
		}
		return this.cpp;
	}

	protected final List<String> getDependencyLibOrder() {
		return this.dependencyLibOrder;
	}

	protected final Fortran getFortran() {
		if (this.fortran == null && !this.onlySpecifiedCompilers) {
			setFortran(new Fortran());
		}
		return this.fortran;
	}

	protected final Assembler getAssembler() {
		if (assembler == null) {
			assembler = new Assembler();
		}
		assembler.setAbstractCompileMojo(this);
		return assembler;
	}

	protected final IDL getIdl() {
		if (this.idl == null && !this.onlySpecifiedCompilers) {
			setIdl(new IDL());
		}
		return this.idl;
	}

	protected final Java getJava() {
		if (this.java == null) {
			this.java = new Java();
		}
		this.java.setAbstractCompileMojo(this);
		return this.java;
	}

	protected final int getMaxCores(final AOL aol) throws MojoExecutionException {
		return getNarInfo().getProperty(aol, "maxCores", this.maxCores);
	}

	/**
	 * Get value of the directDepsOnly flag.
	 * 
	 * @return {@code true} if directDepsOnly is true, {@code false} otherwise.
	 * @since 3.5.3
	 */
	protected boolean getDirectDepsOnly() {
		return this.directDepsOnly;
	}

	protected final Message getMessage() {
		if (this.message == null && !this.onlySpecifiedCompilers) {
			setMessage(new Message());
		}
		return this.message;
	}

	protected final String getOutput(final AOL aol, final String type) throws MojoExecutionException {
		return getNarInfo().getOutput(aol, getOutput(!Library.EXECUTABLE.equals(type)));
	}

	/**
	 * Prefixes an archiver may prepend to the name of a static library.
	 */
	private static final String[] ARCHIVE_PREFIXES = { "", "lib" };

	/**
	 * Extensions an archiver may give to a static library.
	 */
	private static final String[] ARCHIVE_EXTENSIONS = { ".a", ".lib" };

	/**
	 * Directory holding the test scoped archive built out of this project's own
	 * objects. Only produced for "executable" projects, which have no library for
	 * their test executable to link against. It is deliberately kept outside the
	 * NAR layout, since it is an intermediate build artifact and must never be
	 * packaged nor installed.
	 *
	 * @return the directory, which may not exist.
	 */
	protected final File getTestArchiveDirectory() throws MojoFailureException, MojoExecutionException {
		return new File(new File(getTestTargetDirectory(), "lib"), getAOL().toString());
	}

	/**
	 * Locates the static library built under the given base name. Which prefix and
	 * extension the file ends up with is decided by the archiver of the toolchain
	 * in use ("lib" and ".a" for gcc, none and ".lib" for msvc), so the file is
	 * searched for rather than composed.
	 *
	 * @param dir      directory to search.
	 * @param baseName output name the library was built with, without prefix or
	 *                 extension.
	 * @return the library file, or null if it was not found.
	 */
	protected final File findStaticLibrary(final File dir, final String baseName) {
		for (final String prefix : ARCHIVE_PREFIXES) {
			for (final String extension : ARCHIVE_EXTENSIONS) {
				final File candidate = new File(dir, prefix + baseName + extension);
				if (candidate.exists()) {
					return candidate;
				}
			}
		}
		return null;
	}

	protected final Resource getResource() {
		if (this.resource == null && !this.onlySpecifiedCompilers) {
			setResource(new Resource());
		}
		return this.resource;
	}

	protected final String getRuntime(final AOL aol) throws MojoExecutionException {
		return getNarInfo().getProperty(aol, "runtime", this.runtime);
	}

	protected final List<Test> getTests() {
		if (this.tests == null) {
			this.tests = Collections.emptyList();
		}
		return this.tests;
	}

	/**
	 * The include directory of the given NAR dependency, looked up in the given
	 * unpack directories in order of preference. A dependency unpacked into more
	 * than one of them, which is what happens to the ones needed to build the
	 * project as well as its tests, is only taken from the first.
	 *
	 * @param dependency        the dependency to locate.
	 * @param unpackDirectories the directories the NARs were unpacked into. A null
	 *                          one is passed over.
	 * @return the directory, or null when the dependency carries no headers or has
	 *         not been unpacked yet. Headers are not required of a NAR, so having
	 *         none is not in itself an error.
	 */
	protected final File getDependencyIncludeDirectory(final NarArtifact dependency, final File... unpackDirectories)
			throws MojoExecutionException, MojoFailureException {
		for (final File unpackDirectory : unpackDirectories) {
			if (unpackDirectory == null) {
				continue;
			}
			final File include = getLayout().getIncludeDirectory(unpackDirectory, dependency.getArtifactId(),
					dependency.getBaseVersion());
			getLog().debug("Looking for include directory: " + include);
			if (include.exists()) {
				return include;
			}
		}
		return null;
	}

	/**
	 * Hands the include directory of a dependency over to the task, as the kind of
	 * include that dependency asks to be reached by: a "system" one goes in through
	 * the switch that tells the compiler it is looking at third party code, whose
	 * warnings are none of this project's business.
	 */
	protected final void addDependencyIncludePath(final CCTask task, final NarArtifact dependency,
			final File include) {
		if ("system".equals(dependency.getNarInfo().getIncludesType(null))) {
			task.createSysIncludePath().setPath(include.getPath());
		} else {
			task.createIncludePath().setPath(include.getPath());
		}
	}

	/**
	 * Include directories of every NAR dependency, as unpacked under the given
	 * directories. It mirrors what the compile goals hand over to the compiler, so
	 * that the very headers the build sees can also be offered to other consumers,
	 * an IDE among them.
	 *
	 * @param unpackDirectories the directories the NARs were unpacked into, in order
	 *                          of preference.
	 * @return the directories that do exist, in dependency order.
	 */
	protected final List<File> getDependencyIncludeDirectories(final File... unpackDirectories)
			throws MojoExecutionException, MojoFailureException {
		final List<File> includeDirectories = new ArrayList<>();
		for (final NarArtifact dependency : getNarArtifacts()) {
			final File include = getDependencyIncludeDirectory(dependency, unpackDirectories);
			if (include != null) {
				includeDirectories.add(include);
			}
		}
		return includeDirectories;
	}

	/**
	 * Base names (file name without extension) of the test sources, collected on
	 * the first call and kept for the remaining tests.
	 */
	private Set<String> testSourceBaseNames = null;

	/**
	 * Tells whether the sources of the given test are there. A test is built out of
	 * the source file named after it, which is also what keeps the sources of the
	 * other tests out of its own compilation (see
	 * {@link Compiler#getCompiler(String, String)}). Without that file there is
	 * nothing to build the test from, so it is neither compiled nor run.
	 *
	 * @param test the test to look for.
	 * @return true when a source file named after the test exists under the test
	 *         source directories.
	 */
	protected final boolean hasTestSources(final Test test) throws MojoFailureException, MojoExecutionException {
		if (this.testSourceBaseNames == null) {
			this.testSourceBaseNames = new HashSet<>();
			for (final Compiler compiler : new Compiler[] {
					getCpp(), getC(), getFortran()
			}) {
				if (compiler != null && compiler.getName() != null) {
					collectTestSourceBaseNames(compiler, this.testSourceBaseNames);
				}
			}
			getLog().debug("Test sources found: " + this.testSourceBaseNames);
		}
		return this.testSourceBaseNames.contains(test.getName());
	}

	/**
	 * Collects the base names of the source files the given compiler would compile
	 * for the tests. The very same directories and include patterns the compilation
	 * uses are applied here, so that what is looked for is exactly what would be
	 * built.
	 *
	 * @param compiler  the compiler to ask for its test sources.
	 * @param baseNames the set to contribute to.
	 */
	private void collectTestSourceBaseNames(final Compiler compiler, final Set<String> baseNames)
			throws MojoFailureException, MojoExecutionException {
		final Set<String> includes = compiler.getIncludes(Compiler.TEST);
		final Set<String> excludes = compiler.getExcludes(Compiler.TEST);

		for (final File sourceDirectory : compiler.getSourceDirectories(Compiler.TEST)) {
			if (!sourceDirectory.exists()) {
				continue;
			}

			final DirectoryScanner scanner = new DirectoryScanner();
			scanner.setBasedir(sourceDirectory);
			// An empty set of includes means "every file", exactly what it means on the
			// Ant fileset the compilation builds. Plenty of AOLs, all the Windows gcc/g++
			// ones among them, define no default include patterns at all, and the fileset
			// is then left with the excludes as its only filter.
			if (!includes.isEmpty()) {
				scanner.setIncludes(includes.toArray(new String[0]));
			}
			if (!excludes.isEmpty()) {
				scanner.setExcludes(excludes.toArray(new String[0]));
			}
			scanner.addDefaultExcludes();
			scanner.scan();

			for (final String includedFile : scanner.getIncludedFiles()) {
				// the exclusion of the other tests is done on the file name alone
				// ("**/<name>.*"), so the directory the file sits in is irrelevant here too
				final String fileName = new File(includedFile).getName();
				final int dot = fileName.lastIndexOf('.');
				baseNames.add(dot < 0 ? fileName : fileName.substring(0, dot));
			}
		}
	}

	public void setC(final C c) {
		this.c = c;
		c.setAbstractCompileMojo(this);
	}

	public void setCpp(final Cpp cpp) {
		this.cpp = cpp;
		cpp.setAbstractCompileMojo(this);
	}

	protected final String getfortifyID() {
		return this.fortifyID;
	}

	public final void setDependencyLibOrder(final List<String> order) {
		this.dependencyLibOrder = order;
	}

	public void setFortran(final Fortran fortran) {
		this.fortran = fortran;
		fortran.setAbstractCompileMojo(this);
	}

	public void setIdl(final IDL idl) {
		this.idl = idl;
		idl.setAbstractCompileMojo(this);
	}

	public void setMessage(final Message message) {
		this.message = message;
		message.setAbstractCompileMojo(this);
	}

	public void setResource(final Resource resource) {
		this.resource = resource;
		resource.setAbstractCompileMojo(this);
	}

	protected final boolean useLibtool(final AOL aol) throws MojoExecutionException {
		return getNarInfo().getProperty(aol, "libtool", this.libtool);
	}

	public List<SysLib> getDependecySysLib(final NarArtifact dependency)
			throws MojoExecutionException, MojoFailureException {

		final String sysLibs = dependency.getNarInfo().getSysLibs(getAOL());
		List<SysLib> l = new ArrayList<SysLib>();

		if (sysLibs != null && !sysLibs.isEmpty()) {
			getLog().debug("Using SYSLIBS = " + sysLibs);

			String[] split = sysLibs.split(",");

			for (String s : split) {
				String[] typeAndValue = s.split(":", 2);
				if (typeAndValue.length != 2)
					throw new MojoExecutionException("Malformed syslib from dependency: " + s);

				SysLib sysLib = new SysLib();
				sysLib.setName(typeAndValue[0]);
				sysLib.setType(typeAndValue[1]);
				l.add(sysLib);
			}
		}

		return l;
	}

	private static String externalLibKey(final Lib lib) {
		return lib.getName() + "|" + lib.getType() + "|"
				+ (lib.getDirectory() == null ? "" : lib.getDirectory().getPath());
	}

	/**
	 * Builds a dedup set seeded with this project's own &lt;linker&gt;&lt;libs&gt;,
	 * so external libraries propagated from dependencies are not linked twice.
	 *
	 * @return a mutable set of keys for the own external libraries.
	 */
	protected Set<String> getOwnExternalLibKeys() {
		final Set<String> seen = new HashSet<>();
		final List<Lib> own = getLinker().getLibs();
		if (own != null) {
			for (final Lib lib : own) {
				seen.add(externalLibKey(lib));
			}
		}
		return seen;
	}

	/**
	 * Adds the external libraries (&lt;linker&gt;&lt;libs&gt;) declared in a
	 * dependency's nar.properties to the given linker, skipping any already present
	 * in {@code seen} (by name/type/directory). Approach (a): the directory is the
	 * absolute path recorded by the producer, so it must resolve on the consumer's
	 * machine too.
	 *
	 * @param dependency the nar dependency to read external libs from.
	 * @param aol        the AOL to read for.
	 * @param linker     the linker definition to add the libraries to.
	 * @param antProject the Ant project.
	 * @param seen       dedup set (see {@link #getOwnExternalLibKeys()}).
	 */
	protected void addDependencyExternalLibs(final NarArtifact dependency, final AOL aol, final LinkerDef linker,
			final Project antProject, final Set<String> seen) throws MojoFailureException, MojoExecutionException {
		for (final Lib lib : dependency.getNarInfo().getExternalLibs(aol)) {
			if (seen.add(externalLibKey(lib))) {
				getLog().debug("Adding external lib from dependency " + dependency.getArtifactId() + ": "
						+ lib.getName() + " (" + lib.getDirectory() + ")");
				lib.addLibSet(this, linker, antProject);
			}
		}
	}
}
