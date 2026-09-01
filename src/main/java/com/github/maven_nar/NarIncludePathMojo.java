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
 * Publishes the include directories the sources of this project compile
 * against, into its target directory.
 * <p>
 * It runs in process-sources, which comes after the dependencies have been
 * unpacked and before anything is compiled, so that the headers can be resolved
 * by an IDE on a project that does not compile yet.
 *
 * @see NarTestIncludePathMojo
 */
@Mojo(name = "nar-include-path", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresProject = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class NarIncludePathMojo extends AbstractNarIncludePathMojo {

	@Override
	protected String getGoalName() {
		return "nar-include-path";
	}

	@Override
	protected String getScope() {
		return Compiler.MAIN;
	}

	@Override
	protected File getPublishDirectory() {
		return getTargetDirectory();
	}

	@Override
	protected List<? extends Executable> getExecutables() {
		return getLibraries();
	}

	/**
	 * The dependencies needed to compile the sources.
	 */
	@Override
	protected ScopeFilter getArtifactScopeFilter() {
		return new ScopeFilter(Artifact.SCOPE_COMPILE, null);
	}
}
