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
package com.github.maven_nar.cpptasks.types;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.MockBuildListener;

/**
 * Tests for the LibrarySet class.
 */
public class TestLibrarySet {

  /**
   * Evaluate isActive when "if" specifies a property that is set.
   */
  @Test
  public final void testIsActive1() {
    final LibrarySet libset = new LibrarySet();
    final Project project = new Project();
    project.setProperty("windows", "");
    libset.setProject(project);
    libset.setIf("windows");
    final CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("kernel32");
    libset.setLibs(libs);
    final boolean isActive = libset.isActive(project);
    assertTrue(isActive);
  }

  /**
   * Evaluate isActive when "if" specifies a property whose value suggests the
   * user thinks the value is significant.
   *
   */
  @Test
  public final void testIsActive2() {
    final LibrarySet libset = new LibrarySet();
    final Project project = new Project();
    //
    // setting the value to false should throw
    // exception to warn user that they are misusing if
    //
    project.setProperty("windows", "false");
    libset.setIf("windows");
    try {
      final boolean isActive = libset.isActive(project);
    } catch (final BuildException ex) {
      return;
    }
    fail();
  }

  /**
   * Evaluate isActive when "if" specifies a property that is not set.
   */
  @Test
  public final void testIsActive3() {
    final LibrarySet libset = new LibrarySet();
    final Project project = new Project();
    libset.setIf("windows");
    final boolean isActive = libset.isActive(project);
    assertTrue(!isActive);
  }

  /**
   * Evaluate isActive when "unless" specifies a property that is set.
   *
   */
  @Test
  public final void testIsActive4() {
    final LibrarySet libset = new LibrarySet();
    final Project project = new Project();
    project.setProperty("windows", "");
    libset.setUnless("windows");
    final boolean isActive = libset.isActive(project);
    assertTrue(!isActive);
  }

  /**
   * Evaluate isActive when "unless" specifies a property whose value suggests
   * the user thinks the value is significant.
   *
   */
  @Test
  public final void testIsActive5() {
    final LibrarySet libset = new LibrarySet();
    final Project project = new Project();
    //
    // setting the value to false should throw
    // exception to warn user that they are misusing if
    //
    project.setProperty("windows", "false");
    libset.setUnless("windows");
    try {
      final boolean isActive = libset.isActive(project);
    } catch (final BuildException ex) {
      return;
    }
    fail();
  }

  /**
   * Evaluate isActive when "unless" specifies a property that is not set.
   */
  @Test
  public final void testIsActive6() {
    final LibrarySet libset = new LibrarySet();
    final Project project = new Project();
    libset.setProject(project);
    libset.setUnless("windows");
    final CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("kernel32");
    libset.setLibs(libs);
    final boolean isActive = libset.isActive(project);
    assertTrue(isActive);
  }

  /**
   * The libs parameter should not end with .lib, .so, .a etc New behavior is
   * to warn if it ends in a suspicious extension.
   */
  @Test
  public final void testLibContainsDot() {
    final LibrarySet libset = new LibrarySet();
    final Project p = new Project();
    final MockBuildListener listener = new MockBuildListener();
    p.addBuildListener(listener);
    libset.setProject(p);
    final CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("mylib1.1");
    libset.setLibs(libs);
    assertEquals(0, listener.getMessageLoggedEvents().size());
  }

  /**
   * The libs parameter should not end with .lib, .so, .a (that is,
   * should be kernel, not kernel.lib). Previously the libset would
   * warn on configuration, now provides more feedback
   * when library is not found.
   */
  @Test
  public final void testLibContainsDotLib() {
    final LibrarySet libset = new LibrarySet();
    final Project p = new Project();
    final MockBuildListener listener = new MockBuildListener();
    p.addBuildListener(listener);
    libset.setProject(p);
    final CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("mylib1.lib");
    libset.setLibs(libs);
    assertEquals(0, listener.getMessageLoggedEvents().size());
  }

  /**
   * Use of a libset or syslibset without a libs attribute should log a
   * warning message.
   */
  @Test
  public final void testLibNotSpecified() {
    final LibrarySet libset = new LibrarySet();
    final Project p = new Project();
    final MockBuildListener listener = new MockBuildListener();
    p.addBuildListener(listener);
    libset.setProject(p);
    final boolean isActive = libset.isActive(p);
    assertEquals(false, isActive);
    assertEquals(1, listener.getMessageLoggedEvents().size());
  }

  /**
   * this threw an exception prior to 2002-09-05 and started to throw one
   * again 2002-11-19 up to 2002-12-11.
   */
  @Test
  public final void testShortLibName() {
    final LibrarySet libset = new LibrarySet();
    final CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("li");
    libset.setProject(new Project());
    libset.setLibs(libs);
  }

  /**
   * The libs parameter should contain not a lib prefix (that is,
   * pthread not libpthread). Previously the libset would
   * warn on configuration, now provides more feedback
   * when library is not found.
   */
  @Test
  public final void testStartsWithLib() {
    final LibrarySet libset = new LibrarySet();
    final Project p = new Project();
    final MockBuildListener listener = new MockBuildListener();
    p.addBuildListener(listener);
    libset.setProject(p);
    final CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("libmylib1");
    libset.setLibs(libs);
    assertEquals(0, listener.getMessageLoggedEvents().size());
  }

}
