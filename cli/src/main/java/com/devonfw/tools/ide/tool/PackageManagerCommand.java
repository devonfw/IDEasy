package com.devonfw.tools.ide.tool;

import java.util.List;

public class PackageManagerCommand {
  private final PackageManager packageManager;
  private final List<String> commands;

  public PackageManagerCommand(PackageManager packageManager, List<String> commands) {
    this.packageManager = packageManager;
    this.commands = commands;
  }

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
