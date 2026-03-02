package com.devonfw.tools.ide.tool;

import java.util.List;

/**
 * Represents a command to be executed by a package manager. Each command consists of a {@link NativePackageManager} and a list of commands to be executed by
 * that package manager.
 *
 * @param packageManager The package manager associated with this command.
 * @param commands The list of commands to be executed by the package manager.
 */
public record PackageManagerCommand(NativePackageManager packageManager, List<String> commands) {

  /**
   * Constructs a {@code PackageManagerCommand} based on the provided command string. The package manager is retrieved from the command string using
   * {@link NativePackageManager#extractPackageManager(String)}.
   *
   * @param command The command string.
   * @return A {@code PackageManagerCommand} based on the provided command string.
   */
  public static PackageManagerCommand of(String command) {

    NativePackageManager pm = NativePackageManager.extractPackageManager(command);
    return new PackageManagerCommand(pm, List.of(command));
  }
}
