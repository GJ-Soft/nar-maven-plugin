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
package com.github.maven_nar.cpptasks.gcc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for gcc compatible compilers
 *
 * @author CurtA
 */
public abstract class TestGccCompatibleCCompiler {
  /**
   * Constructor
   * 
   * @param name
   *          test case name
   */

  /**
   * Compiler creation method
   * 
   * Must be overriden by extending classes
   * 
   * @return GccCompatibleCCompiler
   */
  protected abstract GccCompatibleCCompiler create();

  /**
   * Tests command lines switches for warning = 0
   */
  @Test
  public void testWarningLevel0() {
    final GccCompatibleCCompiler compiler = create();
    final List<String> args = new ArrayList<>();
    compiler.addWarningSwitch(args, 0);
    assertEquals(1, args.size());
    assertEquals("-w", args.get(0));
  }

  /**
   * Tests command lines switches for warning = 1
   */
  @Test
  public void testWarningLevel1() {
    final GccCompatibleCCompiler compiler = create();
    final List<String> args = new ArrayList<>();
    compiler.addWarningSwitch(args, 1);
    assertEquals(0, args.size());
  }

  /**
   * Tests command lines switches for warning = 2
   */
  @Test
  public void testWarningLevel2() {
    final GccCompatibleCCompiler compiler = create();
    final List<String> args = new ArrayList<>();
    compiler.addWarningSwitch(args, 2);
    assertEquals(0, args.size());
  }

  /**
   * Tests command lines switches for warning = 3
   */
  @Test
  public void testWarningLevel3() {
    final GccCompatibleCCompiler compiler = create();
    final List<String> args = new ArrayList<>();
    compiler.addWarningSwitch(args, 3);
    assertEquals(1, args.size());
    assertEquals("-Wall", args.get(0));
  }

  /**
   * Tests command lines switches for warning = 4
   */
  @Test
  public void testWarningLevel4() {
    final GccCompatibleCCompiler compiler = create();
    final List<String> args = new ArrayList<>();
    compiler.addWarningSwitch(args, 4);
    assertEquals(2, args.size());
    assertEquals("-W", args.get(0));
    assertEquals("-Wall", args.get(1));
  }

  /**
   * Tests command lines switches for warning = 5
   */
  @Test
  public void testWarningLevel5() {
    final GccCompatibleCCompiler compiler = create();
    final List<String> args = new ArrayList<>();
    compiler.addWarningSwitch(args, 5);
    assertEquals(3, args.size());
    assertEquals("-Werror", args.get(0));
    assertEquals("-W", args.get(1));
    assertEquals("-Wall", args.get(2));
  }
}
