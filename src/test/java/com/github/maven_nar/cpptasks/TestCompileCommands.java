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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link CompileCommands}, the compilation database.
 */
public class TestCompileCommands {

  @TempDir
  File dir;

  private List<String> read(final File file) throws IOException {
    return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
  }

  private static long entries(final List<String> lines) {
    return lines.stream().filter(l -> l.trim().startsWith("{")).count();
  }

  @Test
  public void testWritesOneEntryPerLine() throws IOException {
    final File file = new File(this.dir, "compile_commands.json");
    final CompileCommands commands = new CompileCommands(file);
    commands.add(this.dir, new File(this.dir, "a.c"), Arrays.asList("gcc", "-c", "-o", "a.o", "a.c"),
        new File(this.dir, "a.o"));
    commands.add(this.dir, new File(this.dir, "b.c"), Arrays.asList("gcc", "-c", "b.c"), new File(this.dir, "b.o"));
    commands.write();

    final List<String> lines = read(file);
    assertEquals("[", lines.get(0));
    assertEquals("]", lines.get(lines.size() - 1));
    assertEquals(2, entries(lines));
    assertTrue(lines.get(1).endsWith("},"), "every entry but the last ends in a comma");
    assertTrue(lines.get(2).endsWith("}"));
    assertFalse(lines.get(2).endsWith(","));
    assertTrue(lines.get(1).contains("\"arguments\": [\"gcc\", \"-c\", \"-o\", \"a.o\", \"a.c\"]"));
  }

  @Test
  public void testEmptyDatabaseIsAnEmptyArray() throws IOException {
    final File file = new File(this.dir, "compile_commands.json");
    new CompileCommands(file).write();
    assertEquals(Arrays.asList("[", "]"), read(file));
  }

  @Test
  public void testCreatesTheDirectory() throws IOException {
    final File file = new File(this.dir, "target/sub/compile_commands.json");
    new CompileCommands(file).write();
    assertTrue(file.isFile());
  }

  @Test
  public void testMergesWithOtherTasks() throws IOException {
    final File file = new File(this.dir, "compile_commands.json");
    final CompileCommands library = new CompileCommands(file);
    library.add(this.dir, new File(this.dir, "lib.c"), Arrays.asList("gcc", "lib.c"), new File(this.dir, "lib.o"));
    library.write();

    final CompileCommands test = new CompileCommands(file);
    test.add(this.dir, new File(this.dir, "test.c"), Arrays.asList("gcc", "test.c"), new File(this.dir, "test.o"));
    test.write();

    final String text = String.join("\n", read(file));
    assertEquals(2, entries(read(file)));
    assertTrue(text.contains("lib.c\""), "the library entry is kept");
    assertTrue(text.contains("test.c\""));
  }

  @Test
  public void testReplacesTheSameSourceAndObject() throws IOException {
    final File file = new File(this.dir, "compile_commands.json");
    final CompileCommands first = new CompileCommands(file);
    first.add(this.dir, new File(this.dir, "a.c"), Arrays.asList("gcc", "-O0", "a.c"), new File(this.dir, "a.o"));
    first.write();

    final CompileCommands second = new CompileCommands(file);
    second.add(this.dir, new File(this.dir, "a.c"), Arrays.asList("gcc", "-O2", "a.c"), new File(this.dir, "a.o"));
    second.write();

    final List<String> lines = read(file);
    assertEquals(1, entries(lines));
    assertTrue(lines.get(1).contains("-O2"));
    assertFalse(lines.get(1).contains("-O0"));
  }

  @Test
  public void testSameSourceToAnotherObjectIsAnotherEntry() throws IOException {
    final File file = new File(this.dir, "compile_commands.json");
    final CompileCommands commands = new CompileCommands(file);
    commands.add(this.dir, new File(this.dir, "a.c"), Arrays.asList("gcc", "a.c"), new File(this.dir, "shared/a.o"));
    commands.add(this.dir, new File(this.dir, "a.c"), Arrays.asList("gcc", "a.c"), new File(this.dir, "static/a.o"));
    commands.write();
    assertEquals(2, entries(read(file)));
  }

  @Test
  public void testEscapesAndReadsBackPathsWithSpecialCharacters() throws IOException {
    final File file = new File(this.dir, "compile_commands.json");
    final File source = new File(this.dir, "with space\\and \"quote\".c");
    final CompileCommands first = new CompileCommands(file);
    first.add(this.dir, source, Arrays.asList("gcc", "-DTEXT=\"a\\tb\""), new File(this.dir, "x.o"));
    first.write();

    // Read back: the same key is recognised and replaced, not duplicated.
    final CompileCommands second = new CompileCommands(file);
    second.add(this.dir, source, Arrays.asList("gcc"), new File(this.dir, "x.o"));
    second.write();
    assertEquals(1, entries(read(file)));
  }

  @Test
  public void testQuoteAndUnquote() {
    final String value = "C:\\dir\\\"q\"\n\r\t\u0001end";
    final String quoted = CompileCommands.quote(value);
    assertEquals("\"C:\\\\dir\\\\\\\"q\\\"\\n\\r\\t\\u0001end\"", quoted);
    assertEquals(value, CompileCommands.unquote(quoted.substring(1, quoted.length() - 1)));
  }

  @Test
  public void testKeyOfIgnoresOtherLines() {
    assertNull(CompileCommands.keyOf("["));
    assertNull(CompileCommands.keyOf("]"));
    assertNull(CompileCommands.keyOf("{\"file\": \"a.c\"}"), "without output it is not an entry of ours");
    assertEquals("a.c\na.o", CompileCommands.keyOf("{\"file\": \"a.c\", \"output\": \"a.o\"}"));
  }
}
