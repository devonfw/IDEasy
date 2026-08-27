package com.devonfw.tools.ide.tool.gui;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.WindowsSymlinkTestHelper;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link Gui}.
 */
class GuiTest extends AbstractIdeContextTest {

  private ToolInstallation newJavaInstallation(IdeTestContext context) {

    Path binDir = context.getSoftwarePath().resolve("java/bin");
    context.getFileAccess().mkdirs(binDir);
    Path javaExecutable = binDir.resolve("java");
    try {
      Files.createFile(javaExecutable);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return new ToolInstallation(binDir.getParent(), binDir.getParent(), binDir, VersionIdentifier.of("25.0.0"), false);
  }

  @Test
  void testGetGuiExecutableOnMacCreatesAppBundleWithSymlinkNamedIDEasy() throws Exception {

    // arrange
    WindowsSymlinkTestHelper.assumeSymlinksSupported();
    IdeTestContext context = newContext(PROJECT_BASIC, null, true);
    context.setSystemInfo(SystemInfoMock.MAC_ARM64);
    ToolInstallation javaInstallation = newJavaInstallation(context);
    Gui gui = new Gui(context);

    // act
    String executable = gui.getGuiExecutable(javaInstallation);

    // assert
    Path contentsDir = context.getTempPath().resolve("IDEasy.app").resolve("Contents");
    Path launcher = contentsDir.resolve("MacOS").resolve("IDEasy");
    assertThat(executable).isEqualTo(launcher.toString());
    assertThat(Files.isSymbolicLink(launcher)).isTrue();
    assertThat(launcher.toRealPath()).isEqualTo(javaInstallation.binDir().resolve("java").toRealPath());
    Path infoPlist = contentsDir.resolve("Info.plist");
    assertThat(infoPlist).exists();
    assertThat(Files.readString(infoPlist)).contains("<key>CFBundleName</key>", "<string>IDEasy</string>",
        "<key>CFBundleIdentifier</key>");
  }

  @Test
  void testGetGuiExecutableOnWindowsReturnsPlainJava() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, true);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    ToolInstallation javaInstallation = newJavaInstallation(context);
    Gui gui = new Gui(context);

    // act
    String executable = gui.getGuiExecutable(javaInstallation);

    // assert
    assertThat(executable).isEqualTo("java");
  }

  @Test
  void testGetGuiExecutableOnLinuxReturnsPlainJava() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, true);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    ToolInstallation javaInstallation = newJavaInstallation(context);
    Gui gui = new Gui(context);

    // act
    String executable = gui.getGuiExecutable(javaInstallation);

    // assert
    assertThat(executable).isEqualTo("java");
  }

}
