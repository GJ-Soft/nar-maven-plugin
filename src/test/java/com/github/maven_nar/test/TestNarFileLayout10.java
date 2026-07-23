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
package com.github.maven_nar.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import com.github.maven_nar.Library;
import com.github.maven_nar.NarFileLayout;
import com.github.maven_nar.NarFileLayout10;

/**
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 * @version $Id$
 */
public class TestNarFileLayout10 {
  protected NarFileLayout fileLayout;

  protected String artifactId;

  protected String version;

  protected String aol;

  protected String type;

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  @BeforeEach
  protected void setUp() throws Exception {
    this.fileLayout = new NarFileLayout10();
    this.artifactId = "artifactId";
    this.version = "version";
    this.aol = "x86_64-MacOSX-g++";
    this.type = Library.SHARED;
  }

  @Test
  public final void testGetBinDirectory() {
    assertEquals("bin" + File.separator + this.aol, this.fileLayout.getBinDirectory(this.aol));
  }

  @Test
  public final void testGetIncludeDirectory() {
    assertEquals("include", this.fileLayout.getIncludeDirectory());
  }

  @Test
  public final void testGetLibDirectory() {
    assertEquals("lib" + File.separator + this.aol + File.separator + this.type,
        this.fileLayout.getLibDirectory(this.aol, this.type));
  }
}
