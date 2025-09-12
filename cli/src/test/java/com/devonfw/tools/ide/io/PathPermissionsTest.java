package com.devonfw.tools.ide.io;

import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link PathPermissions}.
 */
public class PathPermissionsTest extends Assertions {

  /**
   * Test of {@link PathPermissions#of(int)} and {@link PathPermissions#toString()}.
   */
  @Test
  public void testOfMode() {

    assertThat(PathPermissions.of(0b000000000)).hasToString("---------");
    assertThat(PathPermissions.of(0b000000001)).hasToString("--------x");
    assertThat(PathPermissions.of(0b000000010)).hasToString("-------w-");
    assertThat(PathPermissions.of(0b000000100)).hasToString("------r--");
    assertThat(PathPermissions.of(0b000001000)).hasToString("-----x---");
    assertThat(PathPermissions.of(0b000010000)).hasToString("----w----");
    assertThat(PathPermissions.of(0b000100000)).hasToString("---r-----");
    assertThat(PathPermissions.of(0b001000000)).hasToString("--x------");
    assertThat(PathPermissions.of(0b010000000)).hasToString("-w-------");
    assertThat(PathPermissions.of(0b100000000)).hasToString("r--------");
    assertThat(PathPermissions.of(0b111111111)).hasToString("rwxrwxrwx");
  }

  /**
   * Test of {@link PathPermissions#of(Set)} and {@link PathPermissions#toString()}.
   */
  @Test
  public void testOfPosix() {

    assertThat(PathPermissions.of(Set.of())).hasToString("---------");
    assertThat(PathPermissions.of(Set.of(PosixFilePermission.OWNER_EXECUTE))).hasToString("--------x");
    assertThat(PathPermissions.of(Set.of(PosixFilePermission.OWNER_WRITE))).hasToString("-------w-");
    assertThat(PathPermissions.of(Set.of(PosixFilePermission.OWNER_READ))).hasToString("------r--");
    assertThat(PathPermissions.of(Set.of(PosixFilePermission.GROUP_EXECUTE))).hasToString("-----x---");
    assertThat(PathPermissions.of(Set.of(PosixFilePermission.GROUP_WRITE))).hasToString("----w----");
    assertThat(PathPermissions.of(Set.of(PosixFilePermission.GROUP_READ))).hasToString("---r-----");
    assertThat(PathPermissions.of(Set.of(PosixFilePermission.OTHERS_EXECUTE))).hasToString("--x------");
    assertThat(PathPermissions.of(Set.of(PosixFilePermission.OTHERS_WRITE))).hasToString("-w-------");
    assertThat(PathPermissions.of(Set.of(PosixFilePermission.OTHERS_READ))).hasToString("r--------");
    assertThat(PathPermissions.of(EnumSet.allOf(PosixFilePermission.class))).hasToString("rwxrwxrwx");
  }

  /**
   * Test of {@link PathPermissions#of(Set)} and {@link PathPermissions#toString()}.
   */
  @Test
  public void testToPosix() {

    assertThat(PathPermissions.of(0b000000000).toPosix()).isEmpty();
    assertThat(PathPermissions.of(0b000000001).toPosix()).containsExactly(PosixFilePermission.OWNER_EXECUTE);
    assertThat(PathPermissions.of(0b000000010).toPosix()).containsExactly(PosixFilePermission.OWNER_WRITE);
    assertThat(PathPermissions.of(0b000000100).toPosix()).containsExactly(PosixFilePermission.OWNER_READ);
    assertThat(PathPermissions.of(0b000001000).toPosix()).containsExactly(PosixFilePermission.GROUP_EXECUTE);
    assertThat(PathPermissions.of(0b000010000).toPosix()).containsExactly(PosixFilePermission.GROUP_WRITE);
    assertThat(PathPermissions.of(0b000100000).toPosix()).containsExactly(PosixFilePermission.GROUP_READ);
    assertThat(PathPermissions.of(0b001000000).toPosix()).containsExactly(PosixFilePermission.OTHERS_EXECUTE);
    assertThat(PathPermissions.of(0b010000000).toPosix()).containsExactly(PosixFilePermission.OTHERS_WRITE);
    assertThat(PathPermissions.of(0b100000000).toPosix()).containsExactly(PosixFilePermission.OTHERS_READ);
    assertThat(PathPermissions.of(0b111111111).toPosix()).containsExactlyElementsOf(EnumSet.allOf(PosixFilePermission.class));
  }
}
