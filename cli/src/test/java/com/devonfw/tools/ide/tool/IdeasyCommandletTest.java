package com.devonfw.tools.ide.tool;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileAccessImpl;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.os.WindowsHelper;
import com.devonfw.tools.ide.os.WindowsPathSyntax;

/**
 * Test of {@link IdeasyCommandlet}.
 */
public class IdeasyCommandletTest extends AbstractIdeContextTest {

  /** Test of {@link IdeasyCommandlet#removeObsoleteEntryFromWindowsPath(String)}. */
  @Test
  public void testRemoveObsoleteEntryFromWindowsPath() {

    assertThat(IdeasyCommandlet.removeObsoleteEntryFromWindowsPath("C:\\projects\\_ide\\bin;C:\\Windows\\System32;C:\\Program Files\\Git\\cmd")).isEqualTo(
        "C:\\Windows\\System32;C:\\Program Files\\Git\\cmd");
    assertThat(IdeasyCommandlet.removeObsoleteEntryFromWindowsPath("C:\\Windows\\System32;C:\\Program Files\\Git\\cmd;C:\\projects\\_ide\\bin")).isEqualTo(
        "C:\\Windows\\System32;C:\\Program Files\\Git\\cmd");
    assertThat(IdeasyCommandlet.removeObsoleteEntryFromWindowsPath("C:\\Windows\\System32;C:\\projects\\_ide\\bin;C:\\Program Files\\Git\\cmd")).isEqualTo(
        "C:\\Windows\\System32;C:\\Program Files\\Git\\cmd");
    String path = "C:\\Windows\\System32;C:\\projects\\_ide\\installation\\bin;C:\\Program Files\\Git\\cmd";
    assertThat(IdeasyCommandlet.removeObsoleteEntryFromWindowsPath(
        "C:\\Windows\\System32;C:\\projects\\_ide\\bin;C:\\projects\\_ide\\installation\\bin;C:\\Program Files\\Git\\cmd")).isEqualTo(path);
    assertThat(IdeasyCommandlet.removeObsoleteEntryFromWindowsPath(path)).isEqualTo(path);
  }

  /**
   * Tests if the installation on windows did not add a 2nd path to IDEasy to the PATH environment variable and instead removed the old one and updated it. See:
   * <a href="https://github.com/devonfw/IDEasy/issues/1559">#1559</a> for reference.
   */
  @Test
  public void testInstallDoesNotAddPathEntryTwiceOnWindows() {
    // arrange
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    IdeTestContext context = newContext("install");
    context.setIdeRoot(null);
    context.setSystemInfo(systemInfo);
    context.getStartContext().setForceMode(true);
    WindowsHelper helper = context.getWindowsHelper();
    String originalPath = helper.getUserEnvironmentValue("PATH");
    IdeasyCommandlet ideasy = new IdeasyCommandlet(context);
    // act
    ideasy.installIdeasy(context.getUserHome().resolve("Downloads/ide-cli"));
    String newPath = originalPath.replace("C:\\projects\\_ide\\installation\\bin;", "");
    // assert
    assertThat(helper.getUserEnvironmentValue("PATH")).isEqualTo(newPath + ";" + context.getUserHome().resolve("projects/_ide/installation/bin"));
  }

  /**
   * Test of {@link IdeasyCommandlet#installIdeasy(Path)}.
   *
   * @param os to use
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testInstallIdeasy(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    IdeTestContext context = newContext("install");
    context.setIdeRoot(null);
    context.setSystemInfo(systemInfo);
    context.getStartContext().setForceMode(true);
    WindowsHelper helper = context.getWindowsHelper();
    String originalPath = helper.getUserEnvironmentValue("PATH");
    if (systemInfo.isWindows()) {
      helper.setUserEnvironmentValue("PATH", "C:\\projects\\_ide\\bin;" + originalPath);
    }
    Path ideRoot = context.getUserHome().resolve("projects");
    String addedRcLines =
        "source \"$IDE_ROOT/_ide/installation/functions\"\n";
    if (!systemInfo.isWindows()) {
      addedRcLines = "export IDE_ROOT=\"" + WindowsPathSyntax.MSYS.format(ideRoot) + "\"\n" + addedRcLines;
    }
    Path idePath = ideRoot.resolve("_ide");
    Path installationPath = idePath.resolve("installation");
    Path releasePath = idePath.resolve("software/maven/ideasy/ideasy/SNAPSHOT");
    Path gitconfigPath = context.getUserHome().resolve(".gitconfig");
    FileAccess fileAccess = new FileAccessImpl(context);
    fileAccess.writeFileContent("", gitconfigPath);
    IdeasyCommandlet ideasy = new IdeasyCommandlet(context);
    // act
    ideasy.installIdeasy(context.getUserHome().resolve("Downloads/ide-cli"));
    // assert
    verifyInstallation(installationPath);
    verifyInstallation(releasePath);
    if (systemInfo.isWindows()) {
      assertThat(helper.getUserEnvironmentValue("IDE_ROOT")).isEqualTo(ideRoot.toString());
      assertThat(helper.getUserEnvironmentValue("PATH")).isEqualTo(originalPath + ";" + context.getUserHome().resolve("projects/_ide/installation/bin"));
      assertThat(context.getUserHome().resolve(".gitconfig")).hasContent("[core]\nlongpaths = true");
    }
    assertThat(context.getUserHome().resolve(".bashrc")).hasContent(addedRcLines);
    assertThat(context.getUserHome().resolve(".zshrc")).hasContent("#already exists\n"
        + "autoload -U +X bashcompinit && bashcompinit\n"
        + "alias devon=\"source ~/.devon/devon\"\n"
        + "devon\n"
        + "source ~/.devon/autocomplete\n"
        + addedRcLines);
  }

  /** Test of {@link IdeasyCommandlet#configureWindowsTerminalGitBash()}. */
  @Test
  public void testConfigureWindowsTerminalGitBash() {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    IdeTestContext context = newContext("install");
    IdeasyCommandlet ideasy = new IdeasyCommandlet(context);

    // act
    ideasy.configureWindowsTerminalGitBash();

    // assert
    Path settingsPath = ideasy.getWindowsTerminalSettingsPath();
    assertThat(settingsPath).exists();
    assertThat(settingsPath).content().contains("\"name\" : \"Git Bash\"");
    assertThat(settingsPath).content().contains("\"guid\" : \"{2ece5bfe-50ed-5f3a-ab87-5cd4baafed2b}\"");
  }

  private void verifyInstallation(Path installationPath) {

    assertThat(installationPath).isDirectory();
    assertThat(installationPath.resolve("bin/ideasy.exe")).hasContent("ideasy.exe mock");
    assertThat(installationPath.resolve("bin/ide.bat")).hasContent("ide.bat mock");
    assertThat(installationPath.resolve("internal/eclipse-import.groovy")).hasContent("eclipse-import.groovy mock");
    assertThat(installationPath.resolve("system")).isDirectory();
    assertThat(installationPath.resolve("functions")).hasContent("functions mock");
    assertThat(installationPath.resolve("IDEasy.pdf")).hasContent("IDEasy.pdf mock");
    assertThat(installationPath.resolve("setup")).hasContent("setup mock");
    assertThat(installationPath.resolve("setup.bat")).hasContent("setup.bat mock");
    assertThat(installationPath.resolve(IdeContext.FILE_SOFTWARE_VERSION)).hasContent("SNAPSHOT");
  }

}
