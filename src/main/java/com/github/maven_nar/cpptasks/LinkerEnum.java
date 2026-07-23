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
package com.github.maven_nar.cpptasks;

import org.apache.tools.ant.types.EnumeratedAttribute;

import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.gcc.GccLibrarian;
import com.github.maven_nar.cpptasks.gcc.GccLinker;
import com.github.maven_nar.cpptasks.gcc.GppLinker;
import com.github.maven_nar.cpptasks.gcc.LdLinker;

/**
 * Enumeration of supported linkers
 *
 * @author Curt Arnold
 * 
 */
public class LinkerEnum extends EnumeratedAttribute {
	private final static ProcessorEnumValue[] linkers = new ProcessorEnumValue[] {
			new ProcessorEnumValue("gcc", GccLinker.getInstance()),
			new ProcessorEnumValue("g++", GppLinker.getInstance()),
			new ProcessorEnumValue("clang", GccLinker.getCLangInstance()),
			new ProcessorEnumValue("clang++", GppLinker.getCLangInstance()),
			new ProcessorEnumValue("ld", LdLinker.getInstance()),
			new ProcessorEnumValue("ar", GccLibrarian.getInstance()) };

	public Linker getLinker() {
		return (Linker) linkers[getIndex()].getProcessor();
	}

	@Override
	public String[] getValues() {
		return ProcessorEnumValue.getValues(linkers);
	}
}
