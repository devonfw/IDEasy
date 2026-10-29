package com.devonfw.tools.ide.migration.v2026;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.migration.IdeVersionMigration;
import com.devonfw.tools.ide.tool.IdeasyCommandlet;

/**
 * Refreshes desktop shortcuts after icon rename.
 */
public class Mig202610001 extends IdeVersionMigration {

  /**
   * The constructor.
   */
  public Mig202610001() {

    super("2026.10.001");
  }

  @Override
  public void run(IdeContext context) {

    IdeasyCommandlet ideasy = new IdeasyCommandlet(context);
    ideasy.refreshDesktopShortcut();
  }
}
