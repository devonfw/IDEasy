package com.devonfw.tools.ide.context;

import java.nio.file.Path;

import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLoggerOut;

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
   * @param workingDirectory the optional {@link Path} to current working directory.
   */
  public IdeSlf4jContext(Path workingDirectory) {

    super(new IdeStartContextImpl(IdeLogLevel.TRACE, level -> new IdeSubLoggerOut(level, null, true, IdeLogLevel.TRACE, null)), workingDirectory, null);
  }

}
