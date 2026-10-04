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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Linker#findVersion(String)}, with the first line that each
 * toolchain prints for '--version'.
 */
public class TestLinker {

	@Test
	public void testPlainGcc() {
		assertEquals("14.2.0", Linker.findVersion("gcc (GCC) 14.2.0\nCopyright (C) 2024"));
	}

	/**
	 * Regression: with the dots unescaped, the "x86_64" of the prefix came out as
	 * version "86_64".
	 */
	@Test
	public void testPrefixedMingwGcc() {
		assertEquals("14.2.0", Linker.findVersion("x86_64-w64-mingw32-gcc (GCC) 14.2.0 20240801"));
	}

	@Test
	public void testPrefixedCygwinGcc() {
		assertEquals("12.4.0", Linker.findVersion("x86_64-pc-cygwin-gcc (GCC) 12.4.0"));
	}

	@Test
	public void testMsys2Gcc() {
		assertEquals("14.2.0", Linker.findVersion("g++.exe (Rev2, Built by MSYS2 project) 14.2.0"));
	}

	@Test
	public void testClang() {
		assertEquals("18.1.3", Linker.findVersion("Ubuntu clang version 18.1.3 (1ubuntu1)\nTarget: x86_64-pc-linux-gnu"));
	}

	@Test
	public void testTwoComponents() {
		assertEquals("4.9", Linker.findVersion("gcc version 4.9"));
	}

	@Test
	public void testNoVersion() {
		assertNull(Linker.findVersion("command not found"));
	}
}
