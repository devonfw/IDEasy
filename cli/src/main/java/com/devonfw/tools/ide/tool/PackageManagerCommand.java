package com.devonfw.tools.ide.tool;

import java.util.List;

/**
 * Represents a command to be executed by a package manager.
 * Each command consists of a {@link PackageManager} and a list of commands to be executed by that package manager.
 */
public class PackageManagerCommand {

  /** The package manager associated with this command. */
  private final PackageManager packageManager;

  /** The list of commands to be executed by the package manager. */
  private final List<String> commands;

  /**
   * Constructs a new {@code PackageManagerCommand} with the specified package manager and commands.
   *
   * @param packageManager The package manager associated with this command.
   * @param commands The list of commands to be executed by the package manager.
   */
  public PackageManagerCommand(PackageManager packageManager, List<String> commands) {
    this.packageManager = packageManager;
    this.commands = commands;
  }

  /**
   * Constructs a {@code PackageManagerCommand} based on the provided command string.
   * The package manager is retrieved from the command string using {@link PackageManager#extractPackageManager(String)}.
   *
   * @param command The command string.
   * @return A {@code PackageManagerCommand} based on the provided command string.
   */
  public static PackageManagerCommand of(String command) {
    PackageManager pm = PackageManager.extractPackageManager(command);
    return new PackageManagerCommand(pm, List.of(command));
  }

  public PackageManager getPackageManager() {
    return packageManager;
  }

  public List<String> getCommands() {
    return commands;
  }
}
