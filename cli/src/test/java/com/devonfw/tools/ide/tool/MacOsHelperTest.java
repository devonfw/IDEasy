package com.devonfw.tools.ide.tool;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.SystemInformationMock;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link MacOsHelper}.
 */
public class MacOsHelperTest extends Assertions {

  private static final IdeContext CONTEXT = new IdeTestContext(IdeContextTest.PATH_PROJECTS.resolve("basic"));

  private static final Path APPS_DIR = Paths.get("src/test/resources/mac-apps");

  /** Test "java" structure. */
  @Test
  public void testJava() {

    // arrange
    Path rootDir = APPS_DIR.resolve("java");
    MacOsHelper helper = new MacOsHelper(CONTEXT.getFileAccess(), SystemInformationMock.MAC_X64, CONTEXT);
    // act
    Path linkDir = helper.findLinkDir(rootDir);
    // assert
    assertThat(linkDir).isEqualTo(rootDir.resolve("Contents/Resources/app"));
  }

  /** Test "special" structure. */
  @Test
  public void testSpecial() {

    // arrange
    Path rootDir = APPS_DIR.resolve("special");
    MacOsHelper helper = new MacOsHelper(CONTEXT.getFileAccess(), SystemInformationMock.MAC_X64, CONTEXT);
    // act
    Path linkDir = helper.findLinkDir(rootDir);
    // assert
    assertThat(linkDir).isEqualTo(rootDir.resolve("Special.app/Contents/CorrectFolder"));
  }

  /** Test if OS is not Mac. */
  @Test
  public void testNotMac() {

    // arrange
    Path rootDir = APPS_DIR.resolve("java");
    MacOsHelper helper = new MacOsHelper(CONTEXT.getFileAccess(), SystemInformationMock.LINUX_X64, CONTEXT);
    // act
    Path linkDir = helper.findLinkDir(rootDir);
    // assert
    assertThat(linkDir).isSameAs(rootDir);
  }
}
