package com.devonfw.ide.gui;

import java.nio.file.Path;

import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.ApplicationTest;

/**
 * Provides the handling of mocking the IDE_ROOT directory to subclasses.
 */
public class IdeGuiMockRootTest extends ApplicationTest {

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  private static Path mockIdeRoot;

  /**
   * @return a mock {@link Path} to a temporary IDE_ROOT directory.
   */
  public static Path getMockIdeRoot() {

    return mockIdeRoot;
  }
}
