# Gjs project

Gjs NAR Maven plugin


## General information


| Element  | Link                                                 |
|----------|------------------------------------------------------|
| Git      | https://github.com/GJ-Soft/nar-maven-plugin          |
| Wiki     | -                                                    |
| Issues   | https://github.com/GJ-Soft/nar-maven-plugin/issues   |
| Releases | -                                                    |


## Description

The GJS NAR plugin for Maven allows you to compile native code (C++, C and
Fortran) with GNU g++, gcc. This plugin inherits from thr classic NAR plugin for Maven. For the moment, it focuses only on GNU compilers.

The output produced is wrapped up in Native ARchive files (.nar) some of which are machine independent (-noarch), while others are machine specific and thus depend on a combination of machine architecture(A), operating-system(O) and linker(L) identified as AOL. These nar files can be installed in the local Maven repository and deployed to a standard Maven (web) server, using the standard 
`maven-install-plugin` and `maven-deploy-plugin`.


## Usage

In your POM:

```xml
<build>
	<plugins>
		<plugin>
			<groupId>org.gjs.maven.plugins</groupId>
			<artifactId>nar-maven-plugin</artifactId>
			<version>4.0.0-SNAPSHOT</version>
			<extensions>true</extensions>
			<configuration>
				...
			</configuration>
		</plugin>
	</plugins>
</build>
```

    
## Requirements

* gcc       13.4.0
* g++       13.4.0   
* Java      21
* Maven     3.9.x


## License

GNU GENERAL PUBLIC LICENSE.



## Goals

The plugin contributes its own packaging type, `nar`, and binds one goal to
almost every phase of the build lifecycle (see
`src/main/resources/META-INF/plexus/components.xml`). To activate this lifecycle,
declare the plugin with `<extensions>true</extensions>` and set
`<packaging>nar</packaging>` in your POM. Individual goals can also be invoked
directly, e.g. `mvn nar:nar-compile`.

The plugin exposes **19 goals**, each implemented by a Mojo class in the
`com.github.maven_nar` package:

### Goal list

| Goal | Default phase | Implementing Mojo class | Description |
|------|---------------|-------------------------|-------------|
| `nar-assembly` | process-resources | `com.github.maven_nar.NarAssemblyMojo` | Assemble libraries of NAR files. |
| `nar-compile` | compile | `com.github.maven_nar.NarCompileMojo` | Compiles native source files. |
| `nar-download` | initialize | `com.github.maven_nar.NarDownloadMojo` | List all dependencies needed by the project (compilation, tests, execution) and download the NAR files to the local repository. |
| `nar-download-dependencies` | generate-sources | `com.github.maven_nar.NarDownloadDependenciesMojo` | List all dependencies of the project and download the `noarch`/`aol` NAR files to the local repository if needed. |
| `nar-gnu-configure` | process-sources | `com.github.maven_nar.NarGnuConfigureMojo` | Copies the GNU style source files to a target area, autogens and configures them. |
| `nar-gnu-make` | compile | `com.github.maven_nar.NarGnuMakeMojo` | Runs `make` on the GNU style generated Makefile. |
| `nar-gnu-process` | process-classes | `com.github.maven_nar.NarGnuProcess` | Move the GNU style output into the correct directories for `nar-package`. |
| `nar-gnu-resources` | process-resources | `com.github.maven_nar.NarGnuResources` | Move the GNU style include/lib to some output directory. |
| `nar-package` | package | `com.github.maven_nar.NarPackageMojo` | Jar up the NAR files and attach them to the project's main artifact. |
| `nar-prepare-package` | prepare-package | `com.github.maven_nar.NarPreparePackageMojo` | Create the `nar.properties` file. |
| `nar-process-libraries` | process-classes | `com.github.maven_nar.NarProcessLibraries` | Run arbitrary command line tools to post-process the compiled output (ranlib/ar/etc). |
| `nar-resources` | process-resources | `com.github.maven_nar.NarResourcesMojo` | Copies resources, including AOL specific distributions, to the target area for packaging. |
| `nar-system-generate` | generate-resources | `com.github.maven_nar.NarSystemMojo` | Generates a `NarSystem` class with static methods to use from the Java part of the library. |
| `nar-test` | test | `com.github.maven_nar.NarTestMojo` | Tests NAR files. Runs native tests and executables if produced. |
| `nar-test-unpack` | generate-test-sources | `com.github.maven_nar.NarTestUnpackMojo` | Unpacks NAR files needed for test compilation and execution. |
| `nar-testCompile` | test-compile | `com.github.maven_nar.NarTestCompileMojo` | Compiles native test source files. |
| `nar-unpack` | generate-sources | `com.github.maven_nar.NarUnpackMojo` | Unpacks NAR files needed for compilation into the project target folder. |
| `nar-unpack-dependencies` | generate-sources | `com.github.maven_nar.NarUnpackDependenciesMojo` | Download and unpack all the dependencies' NAR files into the project target folder. |
| `nar-validate` | validate | `com.github.maven_nar.NarValidateMojo` | Validates the configuration of the NAR project (aol and pom). |

> The goal-to-phase bindings above are those declared in the Mojo annotations.
> The default `nar` lifecycle wiring (which phase actually runs which goal) is
> defined in `components.xml`; the Eclipse m2e mapping lives in
> `src/main/resources/META-INF/m2e/lifecycle-mapping-metadata.xml`.


## Configuration

Configuration is supplied through the plugin's `<configuration>` element (or per
`<execution>`). Across all goals the plugin declares **82 parameters**, of which
9 are read-only values injected by Maven (`project`, `session`,
`mojoExecution`, `localRepository`, `remoteArtifactRepositories`, `mavenProject`,
`classesDirectory`, `classpathElements`, `javaHome`) and cannot be set by the
user. Most user-facing parameters are inherited from the shared abstract bases
`AbstractNarMojo` &rarr; `AbstractCompileMojo` / `AbstractDependencyMojo` /
`AbstractResourcesMojo` / `AbstractGnuMojo`.

> **Note &mdash; parameters declared in several Mojos.** A few parameters are *not*
> inherited but re-declared as independent `@Parameter` fields in each Mojo that
> needs them (so their default value / property can differ per goal):
> - `tests` &rarr; `AbstractCompileMojo`, `NarDownloadMojo`, `NarDownloadDependenciesMojo`, `NarUnpackMojo`, `NarUnpackDependenciesMojo`, `NarTestUnpackMojo`
> - `gnuSourceDirectory` &rarr; `AbstractGnuMojo`, `NarValidateMojo`
> - `runtime` &rarr; `AbstractCompileMojo` (and, as a nested element, `Java`)

### Configuration schema (XML tree)

The tree below shows the editable elements accepted inside `<configuration>`.
Simple leaf elements map directly to a parameter; complex elements
(`<linker>`, the compilers, `<libraries>`, `<tests>`, ...) are backed by their
own helper classes and are expanded further down.

```xml
<configuration>
  <!-- ===== General / layout (AbstractNarMojo) ===== -->
  <skip>false</skip>                     <!-- skip all NAR goals -->
  <skipTests>false</skipTests>
  <skipRanlib>false</skipRanlib>
  <ignore>false</ignore>                 <!-- ignore errors and failures -->
  <dryRun>false</dryRun>
  <layout>...</layout>                   <!-- artifact layout -->
  <aol>arch-os-linker</aol>
  <architecture>...</architecture>       <!-- x86, i386, amd64, ppc, sparc... -->
  <os>...</os>                           <!-- Windows, Linux, MacOSX, SunOS, AIX... -->
  <output>${project.artifactId}-${project.version}</output>
  <libsName>...</libsName>
  <includesType>local</includesType>     <!-- system | local -->
  <javaHome>...</javaHome>
  <windowsSdkDir>...</windowsSdkDir>
  <windowsSdkVersion>...</windowsSdkVersion>
  <targetDirectory>${project.build.directory}/nar</targetDirectory>
  <unpackDirectory>${targetDirectory}</unpackDirectory>
  <testTargetDirectory>${project.build.directory}/test-nar</testTargetDirectory>
  <testUnpackDirectory>${testTargetDirectory}</testUnpackDirectory>
  <classifiers>...</classifiers>
  <versionInfo>...</versionInfo>         <!-- NARVersionInfo, Windows binaries -->

  <!-- ===== Dependency selection (AbstractDependencyMojo) ===== -->
  <includeGroupIds/>  <excludeGroupIds/>
  <includeArtifactIds/> <excludeArtifactIds/>

  <!-- ===== Compilation (AbstractCompileMojo) ===== -->
  <failOnError>true</failOnError>
  <libtool>false</libtool>
  <runtime>dynamic</runtime>             <!-- dynamic | static -->
  <maxCores>0</maxCores>                 <!-- 0 = unlimited -->
  <commandLogLevel>...</commandLogLevel>
  <directDepsOnly>false</directDepsOnly>
  <decorateLinkerOptions>false</decorateLinkerOptions>
  <onlySpecifiedCompilers>false</onlySpecifiedCompilers>
  <syslibsFromDependencies>...</syslibsFromDependencies>
  <sharedObjectName>...</sharedObjectName>   <!-- AIX only -->
  <fortifyID>...</fortifyID>
  <embedManifest>true</embedManifest>        <!-- NarCompileMojo -->

  <cpp>...</cpp>            <!-- C++ compiler   (Cpp -> Compiler) -->
  <c>...</c>               <!-- C compiler     (C -> Compiler) -->
  <fortran>...</fortran>   <!-- Fortran        (Fortran -> Compiler) -->
  <assembler>...</assembler>  <!-- (Assembler -> Compiler) -->
  <idl>...</idl>           <!-- IDL            (IDL -> Compiler) -->
  <message>...</message>   <!-- Message        (Message -> Compiler) -->
  <resource>...</resource> <!-- Resource compiler -->
  <linker>...</linker>     <!-- Linker -->
  <java>...</java>         <!-- Java info for includes and linking -->
  <libraries>              <!-- List<Library> -->
    <library>...</library>
  </libraries>
  <tests>                  <!-- List<Test> -->
    <test>...</test>
  </tests>
  <replay>...</replay>

  <!-- ===== Resources (AbstractResourcesMojo / NarResourcesMojo) ===== -->
  <resourceDirectory>src/nar/resources</resourceDirectory>
  <resourceBinDir>...</resourceBinDir>
  <resourceIncludeDir>...</resourceIncludeDir>
  <resourceLibDir>...</resourceLibDir>
  <resourcesCopyAOL>...</resourcesCopyAOL>
  <testResourceDirectory>src/test/resources</testResourceDirectory>

  <!-- ===== GNU autotools goals (AbstractGnuMojo / NarGnu*Mojo) ===== -->
  <gnuUseOnWindows>...</gnuUseOnWindows>
  <gnuSourceDirectory>...</gnuSourceDirectory>
  <gnuTargetDirectory>...</gnuTargetDirectory>
  <gnuAutogenSkip>false</gnuAutogenSkip>
  <gnuBuildconfArgs>...</gnuBuildconfArgs>
  <gnuConfigureArgs>...</gnuConfigureArgs>
  <gnuConfigureSkip>false</gnuConfigureSkip>
  <gnuConfigureInPlace>false</gnuConfigureInPlace>
  <gnuMakeArgs>...</gnuMakeArgs>
  <gnuMakeEnv>...</gnuMakeEnv>
  <gnuMakeSkip>false</gnuMakeSkip>
  <gnuMakeInstallSkip>false</gnuMakeInstallSkip>

  <!-- ===== Post-processing (NarProcessLibraries / NarTestCompileMojo) ===== -->
  <commands>...</commands>
  <skipNar>false</skipNar>
</configuration>
```

#### Complex element: `<cpp>` / `<c>` / `<fortran>` / `<assembler>` / `<idl>` / `<message>` (class `Compiler`)

All compiler elements share the same schema (`C`, `Cpp`, `Fortran`,
`Assembler`, `IDL` and `Message` all extend `Compiler`):

```xml
<cpp>
  <name>...</name>  <prefix>...</prefix>  <toolPath>...</toolPath>
  <sourceDirectory>...</sourceDirectory>
  <testSourceDirectory>...</testSourceDirectory>
  <gccFileAbsolutePath>false</gccFileAbsolutePath>
  <includes>...</includes>       <excludes>...</excludes>
  <testIncludes>...</testIncludes>  <testExcludes>...</testExcludes>
  <ccache>false</ccache>
  <debug>false</debug>
  <exceptions>true</exceptions>
  <rtti>true</rtti>
  <optimize>...</optimize>
  <multiThreaded>false</multiThreaded>
  <defines>...</defines>      <defineSet>...</defineSet>      <clearDefaultDefines>false</clearDefaultDefines>
  <undefines>...</undefines>  <undefineSet>...</undefineSet>  <clearDefaultUndefines>false</clearDefaultUndefines>
  <includePaths>...</includePaths>
  <testIncludePaths>...</testIncludePaths>
  <systemIncludePaths>...</systemIncludePaths>
  <options>...</options>      <testOptions>...</testOptions>  <optionSet>...</optionSet>  <clearDefaultOptions>false</clearDefaultOptions>
  <compileOrder>...</compileOrder>
</cpp>
```

#### Complex element: `<linker>` (class `Linker`)

```xml
<linker>
  <name>...</name>  <prefix>...</prefix>  <toolPath>...</toolPath>
  <incremental>false</incremental>
  <map>false</map>
  <skipDepLink>false</skipDepLink>
  <options>...</options>  <testOptions>...</testOptions>  <optionSet>...</optionSet>  <clearDefaultOptions>false</clearDefaultOptions>
  <libs>                     <!-- List<Lib>: name / type / directory / nested libs -->
    <lib><name>...</name><type>...</type><directory>...</directory></lib>
  </libs>
  <libSet>...</libSet>
  <sysLibs>                  <!-- List<SysLib>: name / type -->
    <sysLib><name>...</name><type>...</type></sysLib>
  </sysLibs>
  <sysLibSet>...</sysLibSet>
  <narDependencyLibOrder>...</narDependencyLibOrder>
  <narDefaultDependencyLibOrder>...</narDefaultDependencyLibOrder>
  <pushDepsToLowestOrder>false</pushDepsToLowestOrder>
  <generateManifest>...</generateManifest>
</linker>
```

#### Complex element: `<java>` (class `Java`)

```xml
<java>
  <include>false</include>
  <includePaths>...</includePaths>
  <link>false</link>
  <runtimeDirectory>...</runtimeDirectory>
  <runtime>...</runtime>
</java>
```

#### Complex elements: `<libraries>` (class `Library`) and `<tests>` (class `Test`)

```xml
<libraries>
  <library>
    <type>...</type>                <!-- shared, static, jni, executable, plugin, none -->
    <subSystem>...</subSystem>
    <linkCPP>true</linkCPP>
    <linkFortran>...</linkFortran>  <linkFortranMain>...</linkFortranMain>
    <narSystemPackage>...</narSystemPackage>
    <narSystemName>...</narSystemName>
    <narSystemDirectory>...</narSystemDirectory>
    <run>...</run>
    <args>...</args>
    <dependencyBindings>...</dependencyBindings>
  </library>
</libraries>

<tests>
  <test>
    <name>...</name>
    <link>...</link>
    <run>true</run>
    <type>...</type>
    <args>...</args>
    <dependencyBindings>...</dependencyBindings>
    <environmentVariables>...</environmentVariables>
  </test>
</tests>
```

### Full parameter list

The table lists every parameter reported by the generated plugin descriptor
(`target/classes/META-INF/maven/plugin.xml`) together with the class in which
the backing `@Parameter` field is declared. Read-only/injected values show up as
*(Maven built-in)*.

| Parameter | Type | Req. | Declared in | Description |
|-----------|------|:----:|-------------|-------------|
| `aol` | String |  | `AbstractNarMojo` | Architecture-OS-Linker name. Defaults to: arch-os-linker. |
| `architecture` | String |  | `AbstractNarMojo` | The Architecture for the nar. Some choices: "x86", "i386", "amd64", "ppc", "sparc"... |
| `assembler` | Assembler |  | `AbstractCompileMojo` | Assembler Compiler. |
| `baseDirectory` | File | Yes | *(Maven built-in)* | Base directory of the project. |
| `c` | C |  | `AbstractCompileMojo` | C Compiler. |
| `classesDirectory` | File |  | `AbstractNarMojo` | *(injected)* |
| `classifiers` | List |  | `AbstractNarMojo` | List of classifiers to download/unpack/assemble (e.g. ppc-MacOSX-g++, x86-Windows-msvc...). |
| `classpathElements` | List | Yes | `NarTestMojo` | *(injected)* The classpath elements of the project being tested. |
| `commandLogLevel` | int |  | `AbstractCompileMojo` | Log level for the commands that are executed. |
| `commands` | List |  | `NarProcessLibraries` | List of commands to execute. |
| `cpp` | Cpp |  | `AbstractCompileMojo` | C++ Compiler. |
| `decorateLinkerOptions` | boolean |  | `AbstractCompileMojo` | Whether linker options should be decorated or not. |
| `directDepsOnly` | boolean |  | `AbstractCompileMojo` | Force the project to specify all its dependencies and not inherit transitive ones. |
| `dryRun` | boolean | Yes | `AbstractNarMojo` | Do everything besides actually compiling or linking any sources. |
| `embedManifest` | boolean |  | `NarCompileMojo` | Whether the final manifest should be embedded in the output (default true). |
| `excludeArtifactIds` | String |  | `AbstractDependencyMojo` | Comma separated list of Artifact names to exclude. |
| `excludeGroupIds` | String |  | `AbstractDependencyMojo` | Comma separated list of GroupId names to exclude. |
| `failOnError` | boolean | Yes | `AbstractCompileMojo` | Fail on compilation/linking error. |
| `finalName` | String | Yes | *(Maven built-in)* | Final name of the artifact. |
| `fortifyID` | String |  | `AbstractCompileMojo` | Support scanning the code with HPE Fortify. |
| `fortran` | Fortran |  | `AbstractCompileMojo` | Fortran Compiler. |
| `gnuAutogenSkip` | boolean |  | `NarGnuConfigureMojo` | Skip running of autogen.sh (aka buildconf). |
| `gnuBuildconfArgs` | String |  | `NarGnuConfigureMojo` | Arguments to pass to GNU buildconf. |
| `gnuConfigureArgs` | String |  | `NarGnuConfigureMojo` | Arguments to pass to GNU configure. |
| `gnuConfigureInPlace` | boolean |  | `NarGnuConfigureMojo` | Run ./configure in the source directory instead of copying to target. |
| `gnuConfigureSkip` | boolean |  | `NarGnuConfigureMojo` | Skip running of configure (and therefore autogen.sh). |
| `gnuMakeArgs` | String |  | `NarGnuMakeMojo` | Space delimited list of arguments to pass to make. |
| `gnuMakeEnv` | String |  | `NarGnuMakeMojo` | Comma delimited list of environment variables to set before running make. |
| `gnuMakeInstallSkip` | boolean |  | `NarGnuMakeMojo` | Skip 'make install' after the make. |
| `gnuMakeSkip` | boolean |  | `NarGnuMakeMojo` | Skip running of make. |
| `gnuSourceDirectory` | File |  | `AbstractGnuMojo`, `NarValidateMojo` | Source directory for GNU style project. |
| `gnuTargetDirectory` | File |  | `AbstractGnuMojo` | Directory in which gnu sources are copied and "configured". |
| `gnuUseOnWindows` | boolean | Yes | `AbstractGnuMojo` | Use GNU goals on Windows. |
| `idl` | IDL |  | `AbstractCompileMojo` | IDL Compiler. |
| `ignore` | boolean |  | `AbstractNarMojo` | Ignore errors and failures. |
| `includeArtifactIds` | String |  | `AbstractDependencyMojo` | Comma separated list of Artifact names to include. |
| `includeGroupIds` | String |  | `AbstractDependencyMojo` | Comma separated list of GroupIds to include. |
| `includesType` | String | Yes | `AbstractNarMojo` | Type of include this artifact may contain -- system or local (default). |
| `java` | Java |  | `AbstractCompileMojo` | Java info for includes and linking. |
| `javaHome` | File |  | `AbstractNarMojo` | *(injected)* Home of the Java system (derived from ${java.home}). |
| `layout` | String | Yes | `AbstractNarMojo` | Layout to be used for building and unpacking artifacts. |
| `libraries` | List |  | `AbstractNarMojo` | List of libraries to create. |
| `libsName` | String |  | `AbstractNarMojo` | Name of the libraries included. |
| `libtool` | boolean | Yes | `AbstractCompileMojo` | Set use of libtool ("libtool " is prepended to the command line). |
| `linker` | Linker |  | `AbstractNarMojo` | Linker. |
| `localRepository` | LocalRepository | Yes | *(Maven built-in)* | *(injected)* |
| `mavenProject` | MavenProject |  | `AbstractNarMojo` | *(injected)* |
| `maxCores` | int |  | `AbstractCompileMojo` | Maximum number of Cores/CPUs to use. 0 means unlimited. |
| `message` | Message |  | `AbstractCompileMojo` | Message Compiler. |
| `mojoExecution` | MojoExecution | Yes | *(Maven built-in)* | *(injected)* |
| `onlySpecifiedCompilers` | boolean |  | `AbstractCompileMojo` | Compile only with the explicitly configured compilers instead of all known ones. |
| `os` | String |  | `AbstractNarMojo` | The Operating System for the nar (Windows, Linux, MacOSX, SunOS, AIX...). |
| `output` | String |  | `AbstractNarMojo` | Name of the output (jni/libs defaults derived from artifactId/version). |
| `outputDirectory` | File | Yes | `Replay` | Output directory. |
| `project` | MavenProject | Yes | *(Maven built-in)* | *(injected)* |
| `projectRepos` | List |  | `AbstractDependencyMojo` | Repositories queried by the verbose dependency graph collection request. |
| `remoteArtifactRepositories` | List | Yes | *(Maven built-in)* | *(injected)* Remote repositories searched for nar attachments. |
| `replay` | Replay |  | `AbstractNarMojo` | Replay configuration. |
| `repoSession` | RepositorySystemSession |  | `AbstractDependencyMojo` | Session object controlling the dependency graph collection request. |
| `resource` | Resource |  | `AbstractCompileMojo` | Resource Compiler. |
| `resourceBinDir` | String | Yes | `AbstractResourcesMojo` | Binary directory. |
| `resourceDirectory` | File | Yes | `NarResourcesMojo` | Directory for nar resources. Defaults to src/nar/resources. |
| `resourceIncludeDir` | String | Yes | `AbstractResourcesMojo` | Include directory. |
| `resourceLibDir` | String | Yes | `AbstractResourcesMojo` | Library directory. |
| `resourcesCopyAOL` | boolean | Yes | `NarResourcesMojo` | Use given AOL only; if false, copy for all available AOLs. |
| `runtime` | String | Yes | `AbstractCompileMojo` | Type of runtime library: "dynamic" or "static". |
| `session` | MavenSession | Yes | *(Maven built-in)* | *(injected)* |
| `sharedObjectName` | String |  | `AbstractCompileMojo` | AIX-only: name for shared library projects. |
| `skip` | boolean |  | `AbstractNarMojo` | Skip running of NAR plugins (any) altogether. |
| `skipNar` | boolean |  | `NarTestCompileMojo` | Skip running of NAR integration test plugins. |
| `skipRanlib` | boolean | Yes | `AbstractNarMojo` | Skip running ranlib if this artifact is a library. |
| `skipTests` | boolean |  | `AbstractNarMojo` | Skip the tests. Listens to Maven's general 'maven.skip.test'. |
| `syslibsFromDependencies` | boolean | Yes | `AbstractCompileMojo` | Whether to automatically gather syslibs from dependencies. |
| `targetDirectory` | File |  | `AbstractNarMojo` | Target directory for Nar file construction (default ${project.build.directory}/nar). |
| `testResourceDirectory` | File | Yes | `NarTestMojo` | Directory for test resources. Defaults to src/test/resources. |
| `testTargetDirectory` | File |  | `AbstractNarMojo` | Target directory for Nar test construction. |
| `testUnpackDirectory` | File |  | `AbstractNarMojo` | Target directory for Nar test unpacking (default ${testTargetDirectory}). |
| `tests` | List |  | `AbstractCompileMojo`, `NarDownloadDependenciesMojo`, `NarDownloadMojo`, `NarTestUnpackMojo`, `NarUnpackDependenciesMojo`, `NarUnpackMojo` | List of tests to create. |
| `unpackDirectory` | File |  | `AbstractNarMojo` | Target directory for Nar file unpacking (default ${targetDirectory}). |
| `versionInfo` | NARVersionInfo |  | `AbstractNarMojo` | NARVersionInfo for Windows binaries. |
| `windowsSdkDir` | String |  | `AbstractNarMojo` | Specific path for the Windows Platform SDK. |
| `windowsSdkVersion` | String |  | `AbstractNarMojo` | Version of the Windows Platform SDK to use. |

_82 configurable parameters across 19 goals (9 of them read-only Maven injections)._



## Documentation
-------------
* [Wiki](https://github.com/GJ-Soft/nar-maven-plugin/wiki)
    * [How to contribute](https://github.com/maven-nar/nar-maven-plugin/wiki/How-to-contribute)
    * [FAQ](https://github.com/maven-nar/nar-maven-plugin/wiki/Frequently_Asked_Questions)
* [Maven site](http://maven-nar.github.io/)

   
## References

* [Maven Build Lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)

