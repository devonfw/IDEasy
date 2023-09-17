package com.devonfw.tools.ide.context;

import java.nio.file.Path;

import com.devonfw.tools.ide.log.IdeSlf4jLogger;

/**
 * Implementation of {@link IdeContext} for testing.
 */
public class IdeSlf4jContext extends AbstractIdeTestContext {

  /**
   * The constructor.
   *
   * @param userDir the optional {@link Path} to current working directory.
   * @param answers the automatic answers simulating a user in test.
   */
  public IdeSlf4jContext(Path userDir, String... answers) {

    super(level -> new IdeSlf4jLogger(level), userDir, answers);
  }

}
