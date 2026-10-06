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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A JSON compilation database (<code>compile_commands.json</code>), the format
 * that clangd, clang-tidy and the SonarQube C/C++ analyzer read to learn how
 * each source file is compiled.
 * <p>
 * Every compile task of a module (the library and each test) writes to the
 * same file, so {@link #write()} merges: it keeps the entries already in the
 * file and replaces those for the same source and object file. The file is
 * written one entry per line, which is what lets it read its own output back
 * without a JSON parser; a line it does not recognise is dropped.
 * </p>
 */
public final class CompileCommands {

  /** One entry per line: the merge reads back these two keys. */
  private static final Pattern FILE_KEY = Pattern.compile("\"file\": \"((?:[^\"\\\\]|\\\\.)*+)\"");
  private static final Pattern OUTPUT_KEY = Pattern.compile("\"output\": \"((?:[^\"\\\\]|\\\\.)*+)\"");

  private final File file;

  /** The entries recorded by this task, by source and object file. */
  private final Map<String, String> entries = new LinkedHashMap<>();

  /**
   * @param file
   *          the compilation database to write
   */
  public CompileCommands(final File file) {
    this.file = file;
  }

  public File getFile() {
    return this.file;
  }

  /**
   * Records how a source file is compiled.
   *
   * @param directory
   *          the working directory of the compiler process
   * @param source
   *          the source file
   * @param arguments
   *          the whole command line, the compiler first
   * @param output
   *          the object file
   */
  public void add(final File directory, final File source, final List<String> arguments, final File output) {
    final StringBuilder entry = new StringBuilder();
    entry.append("{\"directory\": ").append(quote(directory.getAbsolutePath()));
    entry.append(", \"file\": ").append(quote(source.getAbsolutePath()));
    entry.append(", \"arguments\": [");
    for (int i = 0; i < arguments.size(); i++) {
      if (i > 0) {
        entry.append(", ");
      }
      entry.append(quote(arguments.get(i)));
    }
    entry.append("], \"output\": ").append(quote(output.getAbsolutePath())).append('}');
    this.entries.put(key(source.getAbsolutePath(), output.getAbsolutePath()), entry.toString());
  }

  /**
   * Writes the database, keeping the entries of other tasks already in the
   * file.
   *
   * @throws IOException
   *           if the file cannot be read or written
   */
  public void write() throws IOException {
    final Map<String, String> merged = new LinkedHashMap<>();
    if (this.file.isFile()) {
      for (final String line : Files.readAllLines(this.file.toPath(), StandardCharsets.UTF_8)) {
        final String entry = line.trim().replaceFirst(",$", "");
        final String key = keyOf(entry);
        if (key != null) {
          merged.put(key, entry);
        }
      }
    }
    merged.putAll(this.entries);

    final List<String> lines = new ArrayList<>();
    lines.add("[");
    int i = 0;
    for (final String entry : merged.values()) {
      i++;
      lines.add("  " + entry + (i < merged.size() ? "," : ""));
    }
    lines.add("]");
    final File parent = this.file.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    Files.write(this.file.toPath(), lines, StandardCharsets.UTF_8);
  }

  /** The key of an entry written by {@link #add}, or null if the line is not one. */
  static String keyOf(final String entry) {
    if (!entry.startsWith("{") || !entry.endsWith("}")) {
      return null;
    }
    final Matcher source = FILE_KEY.matcher(entry);
    final Matcher output = OUTPUT_KEY.matcher(entry);
    if (!source.find() || !output.find()) {
      return null;
    }
    return key(unquote(source.group(1)), unquote(output.group(1)));
  }

  private static String key(final String source, final String output) {
    return source + '\n' + output;
  }

  static String quote(final String value) {
    final StringBuilder quoted = new StringBuilder(value.length() + 2).append('"');
    for (int i = 0; i < value.length(); i++) {
      final char c = value.charAt(i);
      switch (c) {
        case '"':
          quoted.append("\\\"");
          break;
        case '\\':
          quoted.append("\\\\");
          break;
        case '\n':
          quoted.append("\\n");
          break;
        case '\r':
          quoted.append("\\r");
          break;
        case '\t':
          quoted.append("\\t");
          break;
        default:
          if (c < 0x20) {
            quoted.append(String.format("\\u%04x", (int) c));
          } else {
            quoted.append(c);
          }
      }
    }
    return quoted.append('"').toString();
  }

  static String unquote(final String value) {
    final StringBuilder plain = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      final char c = value.charAt(i);
      if (c != '\\' || i + 1 >= value.length()) {
        plain.append(c);
        continue;
      }
      final char next = value.charAt(++i);
      switch (next) {
        case 'n':
          plain.append('\n');
          break;
        case 'r':
          plain.append('\r');
          break;
        case 't':
          plain.append('\t');
          break;
        case 'u':
          plain.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
          i += 4;
          break;
        default:
          plain.append(next);
      }
    }
    return plain.toString();
  }
}
