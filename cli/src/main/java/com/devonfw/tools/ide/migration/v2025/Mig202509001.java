package com.devonfw.tools.ide.migration.v2025;

import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.migration.IdeVersionMigration;

/**
 * Migration for 2025.02.001-beta. Removes old entries of IDEasy without "installation" folder from Windows PATH and old entries from .bashrc and .zshrc.
 */
public class Mig202509001 extends IdeVersionMigration {

  /**
   * The constructor.
   */
  public Mig202509001() {

    super("2025.09.001");
  }

  @Override
  public void run(IdeContext context) {

    Path softwarePath = context.getSoftwarePath();
    if (softwarePath != null) {
      Path npm = softwarePath.resolve("npm");
      context.getFileAccess().backup(npm);
    }
  }

}
