package com.devonfw.tools.ide.context;

import java.nio.file.Path;

import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListenerNone;

/**
 * Mock instance of {@link com.devonfw.tools.ide.context.IdeContext}.
 *
 * @see #get()
 */
public class IdeTestContextMock extends AbstractIdeTestContext {

  private static final IdeTestContextMock INSTANCE = new IdeTestContextMock();

  private static final Path PATH_MOCK = Path.of("/");

  private IdeTestContextMock() {

    super(new IdeStartContextImpl(IdeLogLevel.TRACE, IdeLogListenerNone.INSTANCE), PATH_MOCK, null);
  }

  @Override
  protected boolean isMutable() {

    return false;
  }

  /**
   * @return the singleton mock instance of {@link com.devonfw.tools.ide.context.IdeContext}. Does NOT have {@link #getIdeHome() IDE_HOME}.
   */
  public static IdeTestContextMock get() {

    return INSTANCE;
  }

}
