package com.devonfw.tools.ide.io;

import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * Simple but efficient container for {@link PosixFilePermission}s for a {@link java.nio.file.Path}.
 */
public class PathPermissions {

  private static final String ALL_PERMISSIONS = "rwxrwxrwx";

  private static final int MASK_EXECUTABLE = 0111;

  private final int permissions;

  private PathPermissions(int permissions) {
    super();
    this.permissions = permissions &= 0b111111111; // Ensure that only the last 9 bits are considered
  }

  /**
   * @return a new instance of {@link PathPermissions} with executable permissions set for everyone.
   */
  public PathPermissions makeExecutable() {
    int newPermissions = this.permissions & MASK_EXECUTABLE;
    if (newPermissions == this.permissions) {
      return this;
    }
    return of(newPermissions);
  }

  /**
   * @return this {@link PathPermissions} as {@link Set} of {@link PosixFilePermission}s.
   */
  public Set<PosixFilePermission> toPosix() {

    Set<PosixFilePermission> perms = EnumSet.noneOf(PosixFilePermission.class);
    for (PosixFilePermission permission : PosixFilePermission.values()) {
      int mask = mask(permission);
      if ((this.permissions & mask) != 0) {
        perms.add(permission);
      }
    }
    return perms;
  }

  private static int mask(PosixFilePermission permission) {
    return 1 << (8 - permission.ordinal());
  }

  @Override
  public String toString() {

    char[] permissionChars = ALL_PERMISSIONS.toCharArray();
    for (int i = 0; i < permissionChars.length; i++) {
      int mask = 1 << i;
      if ((this.permissions & mask) == 0) {
        permissionChars[8 - i] = '-';
      }
    }
    return new String(permissionChars);
  }

  /**
   * @param permissions the mode mask.
   * @return the according {@link PathPermissions}.
   */
  public static PathPermissions of(int permissions) {
    return new PathPermissions(permissions);
  }

  /**
   * @param permissions the {@link Set} of {@link java.nio.file.attribute.PosixFilePermission}s.
   * @return the according {@link PathPermissions}.
   */
  public static PathPermissions of(Set<PosixFilePermission> permissions) {

    int mode = 0;
    for (PosixFilePermission permission : permissions) {
      mode = mode | mask(permission);
    }
    return new PathPermissions(mode);
  }
}
