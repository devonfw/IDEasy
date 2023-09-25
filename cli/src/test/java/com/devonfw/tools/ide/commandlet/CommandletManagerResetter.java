package com.devonfw.tools.ide.commandlet;

/**
 * Class to allow {@link #reset()} for testing.
 */
public class CommandletManagerResetter {

  /**
   * Make {@link CommandletManagerImpl#reset()} visible for testing.
   */
  public static void reset() {

    CommandletManagerImpl.reset();
  }

}
