package com.devonfw.tools.ide.context;

import java.nio.file.Path;

import com.devonfw.tools.ide.log.IdeTestLogger;

/**
 * Implementation of {@link IdeContext} for testing.
 */
public class IdeTestContext extends AbstractIdeTestContext {

  /**
   * The constructor.
   *
   * @param userDir the optional {@link Path} to current working directory.
   * @param answers the automatic answers simulating a user in test.
   */
  public IdeTestContext(Path userDir, String... answers) {

    super(level -> new IdeTestLogger(level), userDir, answers);
  }

}
