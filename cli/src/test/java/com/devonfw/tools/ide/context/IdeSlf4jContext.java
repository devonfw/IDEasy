package com.devonfw.tools.ide.context;

import java.nio.file.Path;

import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLoggerSlf4j;

/**
 * Implementation of {@link IdeContext} for testing.
 */
public class IdeSlf4jContext extends AbstractIdeTestContext {

  private static final Path PATH_MOCK = Path.of("/");

  /**
   * The constructor.
   */
  public IdeSlf4jContext() {
    this(PATH_MOCK);
  }

  /**
   * The constructor.
   *
   * @param userDir the optional {@link Path} to current working directory.
   */
  public IdeSlf4jContext(Path userDir) {

    super(new IdeStartContextImpl(IdeLogLevel.TRACE, level -> new IdeSubLoggerSlf4j(level)), userDir);
  }

}
