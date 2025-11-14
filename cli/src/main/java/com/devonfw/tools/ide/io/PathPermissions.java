package com.devonfw.tools.ide.io;

import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * Simple but efficient container for {@link PosixFilePermission}s for a {@link java.nio.file.Path}.
 */
public class PathPermissions {

  /** {@link PathPermissions} for rwxr-xr-x so only owner can write but everything else allowed (e.g. common default for folder). */
  public static final PathPermissions MODE_RWX_RX_RX = of(0_755);

  /** {@link PathPermissions} for rw-r--r-- so only owner can write but reading is allowed for everyone (e.g. common default for files). */
  public static final PathPermissions MODE_RW_R_R = of(0_644);

  private static final String ALL_PERMISSIONS = "rwxrwxrwx";
 
  private static final int MASK_EXECUTABLE = 0_111;

  private final int permissions;

  private PathPermissions(int permissions) {
    super();
    this.permissions = permissions &= 0b111111111; // Ensure that only the last 9 bits are considered
  }

  /**
   * @return a new instance of {@link PathPermissions} with executable permissions set for everyone.
   */
  public PathPermissions makeExecutable() {
    int newPermissions = this.permissions | MASK_EXECUTABLE;
    if (newPermissions == this.permissions) {
      return this;
    }
    return of(newPermissions);
  }

  /**
   * @return the mode of this {@link PathPermissions}. The mode is a bit-mask representing the actual flags. It is often displayed in octal notation to make it
   *     more readable since every part (user, group, others) is represented by an octal.
   */
  public int toMode() {
    return this.permissions;
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
