package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Extends {@link CommandletManagerImpl} to make {@link #add(Commandlet)} method visible for testing and mocking.
 */
public class TestCommandletManager extends CommandletManagerImpl {

  /**
   * @param context the {@link IdeContext}.
   */
  public TestCommandletManager(IdeContext context) {

    super(context);
  }

  @Override
  public void add(Commandlet commandlet) {

    super.add(commandlet);
  }


}
