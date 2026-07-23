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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NarUtil}.
 */
public class TestNarUtil {

  @Test
  public void testParseOctalPermissionNull() {
    assertNull(NarUtil.parseOctalPermission(null));
  }

  @Test
  public void testParseOctalPermissionNone() {
    assertTrue(NarUtil.parseOctalPermission("000").isEmpty());
  }

  @Test
  public void testParseOctalPermission755() {
    final Set<PosixFilePermission> permissions = NarUtil.parseOctalPermission("755");
    assertEquals(PosixFilePermissions.fromString("rwxr-xr-x"), permissions);
  }

  @Test
  public void testParseOctalPermission644() {
    final Set<PosixFilePermission> permissions = NarUtil.parseOctalPermission("644");
    assertEquals(PosixFilePermissions.fromString("rw-r--r--"), permissions);
  }

  @Test
  public void testParseOctalPermission777() {
    final Set<PosixFilePermission> permissions = NarUtil.parseOctalPermission("777");
    assertEquals(PosixFilePermissions.fromString("rwxrwxrwx"), permissions);
  }

  /**
   * Regression: octal 020 is the group-write bit and must map to
   * {@link PosixFilePermission#GROUP_WRITE} (previously it wrongly produced
   * GROUP_READ).
   */
  @Test
  public void testParseOctalPermissionGroupWrite() {
    final Set<PosixFilePermission> permissions = NarUtil.parseOctalPermission("020");
    assertEquals(PosixFilePermissions.fromString("----w----"), permissions);
  }

  /**
   * Octal 040 is the group-read bit, kept distinct from group-write.
   */
  @Test
  public void testParseOctalPermissionGroupRead() {
    final Set<PosixFilePermission> permissions = NarUtil.parseOctalPermission("040");
    assertEquals(PosixFilePermissions.fromString("---r-----"), permissions);
  }
}
