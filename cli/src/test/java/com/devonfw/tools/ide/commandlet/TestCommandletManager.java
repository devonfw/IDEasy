package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Extends {@link CommandletManagerImpl} to make {@link #add(AbstractCommandlet)} method visible for testing and mocking.
 */
public class TestCommandletManager extends CommandletManagerImpl {

  /**
   * @param context the {@link IdeContext}.
   */
  public TestCommandletManager(IdeContext context) {

    super(context);
  }

  @Override
  public void add(AbstractCommandlet commandlet) {

    super.add(commandlet);
  }


}
