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

import com.github.maven_nar.cpptasks.compiler.Compiler;
import com.github.maven_nar.cpptasks.gcc.GccCCompiler;
import com.github.maven_nar.cpptasks.gcc.WindresResourceCompiler;

/**
 * Enumeration of supported compilers
 *
 * <table border="1">
 * <caption>Supported compilers</caption>
 * <tr>
 * <td>gcc (default)</td>
 * <td>GCC C compiler</td>
 * </tr>
 * <tr>
 * <td>g++</td>
 * <td>GCC C++ compiler</td>
 * </tr>
 * <tr>
 * <td>c++</td>
 * <td>GCC C++ compiler</td>
 * </tr>
 * <tr>
 * <td>clang</td>
 * <td>clang / llvm C compiler</td>
 * </tr>
 * <tr>
 * <td>clang++</td>
 * <td>clang++ / llvm C++ compiler</td>
 * </tr>
 * <tr>
 * <td>windres</td>
 * <td>GNU resource compiler</td>
 * </tr>
 * </table>
 *
 * @author Curt Arnold
 * 
 */
public class CompilerEnum extends EnumeratedAttribute {
	private static final ProcessorEnumValue[] compilers = new ProcessorEnumValue[] {
			new ProcessorEnumValue("gcc", GccCCompiler.getInstance()),
			new ProcessorEnumValue("g++", GccCCompiler.getGppInstance()),
			new ProcessorEnumValue("clang", GccCCompiler.getCLangInstance()),
			new ProcessorEnumValue("clang++", GccCCompiler.getCLangppInstance()),
			new ProcessorEnumValue("c++", GccCCompiler.getCppInstance()),
			new ProcessorEnumValue("windres", WindresResourceCompiler.getInstance()) };

	public Compiler getCompiler() {
		return (Compiler) compilers[getIndex()].getProcessor();
	}

	@Override
	public String[] getValues() {
		return ProcessorEnumValue.getValues(compilers);
	}
}
