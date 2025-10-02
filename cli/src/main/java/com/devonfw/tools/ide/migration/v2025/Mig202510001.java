package com.devonfw.tools.ide.migration.v2025;

import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.migration.IdeVersionMigration;

/**
 * Migration to 2025.10.001. Removes "npm" folder from software path as the {@link com.devonfw.tools.ide.tool.npm.Npm} commandlet has changed entirely so the
 * installation of node based tools will now always happen in the "node" software folder.
 */
public class Mig202510001 extends IdeVersionMigration {

  /**
   * The constructor.
   */
  public Mig202510001() {

    super("2025.10.001");
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
