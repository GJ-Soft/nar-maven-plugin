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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;

/**
 * Publishes the include directories the tests of this project compile against,
 * into its test target directory.
 * <p>
 * It runs in process-sources, next to the goal that does the same for the
 * sources of the project, and not in process-test-sources: that phase comes
 * after compile, and a path published there is of no use to whoever is editing
 * tests of a module that does not compile yet, which is the whole point. Its
 * dependencies are unpacked just before, in generate-sources.
 *
 * @see NarIncludePathMojo
 */
@Mojo(name = "nar-test-include-path", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST)
public class NarTestIncludePathMojo extends AbstractNarIncludePathMojo {

	@Override
	protected String getGoalName() {
		return "nar-test-include-path";
	}

	@Override
	protected String getScope() {
		return Compiler.TEST;
	}

	@Override
	protected File getPublishDirectory() {
		return getTestTargetDirectory();
	}

	@Override
	protected List<? extends Executable> getExecutables() {
		return getTests();
	}

	/**
	 * The tests are unpacked into their own area, the way nar-test-unpack puts them
	 * there and the test compilation looks for them.
	 */
	@Override
	protected File getUnpackDirectory() {
		return getTestUnpackDirectory() == null ? super.getUnpackDirectory() : getTestUnpackDirectory();
	}

	/**
	 * Nothing to publish when the tests are not being built at all.
	 */
	@Override
	protected boolean shouldPublish() {
		if (this.skipTests) {
			getLog().debug("Tests are skipped, publishing no include path for them");
			return false;
		}
		return true;
	}

	/**
	 * The dependencies needed to compile and run the tests.
	 */
	@Override
	protected ScopeFilter getArtifactScopeFilter() {
		return new ScopeFilter(Artifact.SCOPE_TEST, null);
	}
}
