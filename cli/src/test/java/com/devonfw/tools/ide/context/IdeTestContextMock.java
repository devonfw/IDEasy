package com.devonfw.tools.ide.context;

import java.nio.file.Path;

/**
 * Mock instance of {@link com.devonfw.tools.ide.context.IdeContext}.
 *
 * @see #get()
 */
public class IdeTestContextMock extends IdeSlf4jContext {

  private static final IdeTestContextMock INSTANCE = new IdeTestContextMock();

  private IdeTestContextMock() {

    super(Path.of("/"));
  }

  /**
   * @return the singleton mock instance of {@link com.devonfw.tools.ide.context.IdeContext}. Does NOT have {@link #getIdeHome() IDE_HOME}.
   */
  public static IdeTestContextMock get() {

    return INSTANCE;
  }

}
