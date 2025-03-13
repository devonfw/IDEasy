package com.devonfw.tools.ide.migration.v2025;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.migration.IdeVersionMigration;

/**
 * Migration for 2025.02.001-beta. Removes old entries of IDEasy without "installation" folder from Windows PATH and old entries from .bashrc and .zshrc.
 */
public class Mig202502001 extends IdeVersionMigration {

  /**
   * The constructor.
   */
  public Mig202502001() {

    super("2025.02.001-beta");
  }

  @Override
  public void run(IdeContext context) {

    // nothing left to do, just used as first base-line.
  }

}
