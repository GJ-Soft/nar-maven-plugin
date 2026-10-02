# Changelog

All major changes are referenced in this file

[Added|Changed|Deprecated|Removed|Fixed|Security]


## Initial
This project is an update to Java 21 from version 3.10.1 as it was on 17/04/2020.

## 4.0.0-RC.1

### Added
- `languageStandard` and `bitsArchitecture` options for `<c>` and `<cpp>`, so the
  language standard (`-std=`) and the word size (`-m`) no longer have to be smuggled
  in as raw compiler options.
- `threadFlag` and `multiThreaded` options, which pick the right switch per platform
  (`-mthreads` on MinGW, `-pthread` elsewhere, nothing on Cygwin).
- External libraries declared in `<linker><libs>` are now propagated to consumers
  through `nar.properties`, so a project no longer has to repeat the libraries of
  the projects it depends on.
- Projects with `executable` packaging can test their `main()`: the objects are
  archived into an intermediate static library of test scope, which the test then
  links against.
- Two goals that publish the directories holding the headers a project compiles
  against, `nar-include-path` and `nar-test-include-path`, each writing a plain
  list, a Visual Studio Code C/C++ configuration and a project property. They are
  bound in the NAR lifecycle, so they need no configuration, and they run before
  anything is compiled so that an IDE can resolve the includes of a project that
  does not build yet.

### Changed
- Java 21, Maven 3.9 and Resolver 1.9.x. Tests migrated to JUnit 5. Documentation
  reduced to the GNU toolchains, which are the ones actually supported.
- Coordinates moved to `org.gjsoft.maven.plugins`.
- `nar-test-unpack` and `nar-test-include-path` moved from `generate-test-sources`
  and `process-test-sources` to `generate-sources` and `process-sources`. Both
  phases they used to sit in come after `compile`, which made the include path of
  the tests unavailable until the project had been built. **`nar-test-unpack` now
  honours `skipTests`**, so a build that skips the tests no longer unpacks their
  dependencies; it used to do so regardless.
- The unpacking done by the include path goals decides on its own whether it is
  needed, instead of an opt-in flag. It reads what each dependency publishes in its
  `nar.properties` to tell a dependency that carries no headers from one whose
  headers are not unpacked yet, and only unpacks for the latter. The `unpack`
  parameter now takes three states: unset decides, `true` always, `false` never.
- Include directories and object files are handed to the compiler relative to the
  working directory, the treatment the source file already got. **This changes the
  command line of every compilation**; what the compiler resolves does not change,
  and the command comes out shorter. It can be turned off with
  `gccFileAbsolutePath`.

### Fixed
- The name of every `<test>` was excluded from the library as well, not just from
  the test compilation, so a project whose library and test shared a file name lost
  it from the library.
- A `<test>` declared without a source file of its own name is no longer compiled
  nor run.
- A project's own static library is linked into the test executable as an object,
  not through `-l`.
- `nar-testCompile` ignored `decorateLinkerOptions` and kept the default of
  `CCTask`, so the same `<options>` came out untouched when linking the library and
  wrapped in `-Wl,` when linking the test, which handed them to `ld` instead of to
  the compiler driver.
- `CCTask` set the working directory of the compiler *after* building its
  configuration. Compilers are singletons shared by every definition in a build, so
  a configuration read whatever the previous one had left behind, and nothing built
  there could depend on the working directory.
- The warnings gcc emits on Windows for libraries named `libX.a` / `libX.so` are no
  longer printed.

## 4.0.0

### Added
- A `run-its` profile to run the integration tests under `src/it`
  ( `mvn -Prun-its verify` ). The configuration used to come from the parent of
  the original NAR plugin, so since the move to the new coordinates they had not
  been run. They are copied to `target/it` and use a repository of their own,
  `target/it-repo`. On Windows `it-parent` sets the linker to `g++`, since the
  default in `aol.properties` is still `msvc`.

### Fixed
- `nar-compile` failed with a `NullPointerException` ( `this.session` is null )
  whenever it needed the dependency tree, that is with `directDepsOnly` or
  `pushDepsToLowestOrder`. `AbstractDependencyMojo` got a `session` field in the
  move to Maven 3.9 while `NarCompileMojo` kept its own of the same name, and Maven
  only injected the one of the subclass.

### Removed
- Options that had no use with the GNU toolchains, the only ones this plugin
  supports. A pom that still sets one of the top level ones gets a Maven warning
  about an unknown parameter; the nested ones ( `<linkFortran>` /
  `<linkFortranMain>` in `<library>`, `<generateManifest>` in `<linker>` ) make
  the plugin configuration fail, and have to be removed from the pom.
  - `<fortran>`, `<assembler>`, `<idl>` and `<message>`, with their classes, the
    `g77` and `gfortran` compilers, the Fortran dependency parser and the
    Fortran entries of `aol.properties`; and `<linkFortran>` / `<linkFortranMain>`
    in `<library>`.
  - `libtool`, down to the libtool variants every gcc compiler and linker kept
    alongside the normal one.
  - `fortifyID`, the wrapper that put `sourceanalyzer` ( HPE Fortify ) in front of
    the compiler.
  - `embedManifest` and `generateManifest`, which nothing read.
- `runtime` and `subSystem` stay: with gcc they do take effect ( `-static`,
  `-static-libgcc` and the static `libstdc++` for the first, `-mconsole` /
  `-mwindows` on MinGW for the second ).
- `src/xdocs`, the Maven 1 location of the site pages, which the current site
  plugin does not read. It held the `maven.nar.*` properties of the Maven 1
  plugin and an old copy of `narDependencies.apt`.

### Changed
- The copy of `aol.properties` in the site page `aol.apt` was out of date; it is
  now the current file.
