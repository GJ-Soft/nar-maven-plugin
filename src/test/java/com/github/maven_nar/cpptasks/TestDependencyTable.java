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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import com.github.maven_nar.cpptasks.compiler.CompilerConfiguration;

/**
 * DependencyTable tests
 *
 * @author curta
 */
public class TestDependencyTable extends TestXMLConsumer {
  /**
   * Constructor
   * 
   * @param testName
   *          test name
   */

  /**
   * Loads a dependency file from OpenSHORE (http://www.openshore.org)
   * 
   * @throws IOException
   */
  @Test
  public void testLoadOpenshore() throws IOException, ParserConfigurationException, SAXException {
    final String tmpDir = System.getProperty("java.io.tmpdir");
    try {
      copyResourceToTmpDir("openshore/dependencies.xml", "dependencies.xml");
      final DependencyTable dependencies = new DependencyTable(new File(tmpDir));
      dependencies.load();
    } finally {
      deleteTmpFile("dependencies.xml");
    }
  }

  /**
   * Loads a dependency file from Xerces-C (http://xml.apache.org)
   * 
   * @throws IOException
   */
  @Test
  public void testLoadXerces() throws IOException, ParserConfigurationException, SAXException {
    final String tmpDir = System.getProperty("java.io.tmpdir");
    try {
      copyResourceToTmpDir("xerces-c/dependencies.xml", "dependencies.xml");
      final DependencyTable dependencies = new DependencyTable(new File(tmpDir));
      dependencies.load();
    } finally {
      deleteTmpFile("dependencies.xml");
    }
  }

  /**
   * Regression: a source includes a header of a dependency (unpacked under
   * target, not under src), and that header includes another one that changed.
   * The walk only went into headers under src/main or src/test, so the change
   * two levels down was missed and the object was left stale.
   */
  @Test
  public void testChangeInIncludeOfDependencyHeaderForcesRebuild(@TempDir final File base) throws Exception {
    final File obj = new File(base, "obj");
    assertTrue(obj.mkdirs());
    final String sep = File.separator;
    final String source = ".." + sep + "src" + sep + "main" + sep + "a.cpp";
    final String header = ".." + sep + "dep" + sep + "A.h";
    final String nested = ".." + sep + "dep" + sep + "B.h";
    final long old = 1_600_000_000_000L;
    final long recent = old + 3_600_000L;
    touch(new File(obj, source), old);
    touch(new File(obj, header), old);
    touch(new File(obj, nested), recent);
    Files.writeString(new File(obj, "dependencies.xml").toPath(), "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<dependencies>\n"
        + "<includePath signature=\"ID\">\n"
        + entry(source, old, header)
        + entry(header, old, nested)
        + entry(nested, recent, null)
        + "</includePath>\n"
        + "</dependencies>\n");

    final DependencyTable table = new DependencyTable(obj);
    table.load();
    final DependencyInfo sourceInfo = table.getDependencyInfo(source, "ID");
    assertNotNull(sourceInfo);
    final DependencyTable.TimestampChecker checker = table.new TimestampChecker(old + 60_000L, true);
    table.walkDependencies(new CCTask(), sourceInfo, compilerWithIncludePath("ID"),
        new DependencyInfo[50], checker);
    assertTrue(checker.getMustRebuild());
  }

  private static void touch(final File file, final long lastModified) throws IOException {
    file.getParentFile().mkdirs();
    Files.writeString(file.toPath(), "");
    assertTrue(file.setLastModified(lastModified));
  }

  private static String entry(final String file, final long lastModified, final String include) {
    return "<source file=\"" + file + "\" lastModified=\"" + Long.toHexString(lastModified) + "\">\n"
        + (include == null ? "" : "<include file=\"" + include + "\"/>\n")
        + "</source>\n";
  }

  /** The walk only asks the compiler for its include path identifier. */
  private static CompilerConfiguration compilerWithIncludePath(final String identifier) {
    return (CompilerConfiguration) Proxy.newProxyInstance(CompilerConfiguration.class.getClassLoader(),
        new Class<?>[] { CompilerConfiguration.class }, (proxy, method, args) -> {
          if (method.getName().equals("getIncludePathIdentifier")) {
            return identifier;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }
}
