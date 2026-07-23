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
import org.junit.jupiter.api.AfterEach;

import com.github.maven_nar.cpptasks.OutputTypeEnum;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;

/**
 * @author CurtA
 */
public class TestGccLinker {
  private final String realOSName;

  /**
   * Constructor
   * 
   * @param name
   *          test name
   */
  public TestGccLinker() {
    this.realOSName = System.getProperty("os.name");
  }

  @AfterEach
  protected void tearDown() throws java.lang.Exception {
    System.setProperty("os.name", this.realOSName);
  }

  @Test
  public void testGetLinkerDarwinPlugin() {
    System.setProperty("os.name", "Mac OS X");
    final GccLinker linker = GccLinker.getInstance();
    final OutputTypeEnum outputType = new OutputTypeEnum();
    outputType.setValue("plugin");
    final LinkType linkType = new LinkType();
    linkType.setOutputType(outputType);
    final Linker pluginLinker = linker.getLinker(linkType);
    assertEquals("libfoo.bundle", pluginLinker.getOutputFileNames("foo", null)[0]);
  }

  @Test
  public void testGetLinkerDarwinShared() {
    System.setProperty("os.name", "Mac OS X");
    final GccLinker linker = GccLinker.getInstance();
    final OutputTypeEnum outputType = new OutputTypeEnum();
    outputType.setValue("shared");
    final LinkType linkType = new LinkType();
    linkType.setOutputType(outputType);
    final Linker sharedLinker = linker.getLinker(linkType);
    assertEquals("libfoo.dylib", sharedLinker.getOutputFileNames("foo", null)[0]);
  }

  @Test
  public void testGetLinkerNonDarwinPlugin() {
    System.setProperty("os.name", "Microsoft Windows");
    final GccLinker linker = GccLinker.getInstance();
    final OutputTypeEnum outputType = new OutputTypeEnum();
    outputType.setValue("plugin");
    final LinkType linkType = new LinkType();
    linkType.setOutputType(outputType);
    final Linker pluginLinker = linker.getLinker(linkType);
    assertEquals("libfoo.so", pluginLinker.getOutputFileNames("foo", null)[0]);
  }

  @Test
  public void testGetLinkerNonDarwinShared() {
    System.setProperty("os.name", "Microsoft Windows");
    final GccLinker linker = GccLinker.getInstance();
    final OutputTypeEnum outputType = new OutputTypeEnum();
    outputType.setValue("shared");
    final LinkType linkType = new LinkType();
    linkType.setOutputType(outputType);
    final Linker sharedLinker = linker.getLinker(linkType);
    assertEquals("libfoo.so", sharedLinker.getOutputFileNames("foo", null)[0]);
  }
}
