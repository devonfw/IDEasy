package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xmlunit.assertj3.XmlAssert;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.WindowsSymlinkTestHelper;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.tool.extra.ExtraToolInstallation;
import com.devonfw.tools.ide.tool.extra.ExtraTools;
import com.devonfw.tools.ide.tool.extra.ExtraToolsMapper;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link AbstractIdeToolCommandlet}.
 */
class IdeToolCommandletTest extends AbstractIdeContextTest {

  /**
   * Tests that launching an IDE configures its workspace exactly once.
   *
   * @param installed whether the IDE is already installed before launch.
   */
  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void testConfigureWorkspace(boolean installed) {

    WindowsSymlinkTestHelper.assumeSymlinksSupported();
    // arrange
    IdeTestContext context = newContext("intellij");
    Path workspace = context.getWorkspacePath();
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    if (installed) {
      intellij.install();
      assertThat(workspace.resolve(".editorconfig")).exists();
      context.getTestStartContext().getEntries().clear();
    }
    // act
    intellij.run();
    // assert
    assertThat(context.getTestStartContext().getEntries()).extracting(IdeLogEntry::message)
        .containsOnlyOnce("Start: Configuring workspace main for IDE intellij");
    assertThat(workspace.resolve(".editorconfig")).exists();
    assertThat(workspace.resolve(".intellij/config/idea.key")).exists();
    assertThat(workspace.resolve("user.properties")).exists().content().contains("ijversion=2023.3.3");
  }

  /**
   * Tests that launching an installed IDE applies changed templates, restores managed properties and preserves additional user properties.
   *
   * @param os the operating system to simulate.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  void testUpdateAndRestoreWorkspaceAfterInstallation(String os) {

    WindowsSymlinkTestHelper.assumeSymlinksSupported();
    // arrange
    IdeTestContext context = newContext("intellij");
    context.setSystemInfo(SystemInfoMock.of(os));
    FileAccess fileAccess = context.getFileAccess();
    Path template = context.getSettingsPath().resolve("intellij/workspace/update/managed.properties");
    Path workspaceFile = context.getWorkspacePath().resolve("managed.properties");
    fileAccess.writeFileContent("managed.value=v1\n", template);
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    intellij.install();
    assertThat(fileAccess.readProperties(workspaceFile)).containsEntry("managed.value", "v1");

    fileAccess.writeFileContent("managed.value=v2\n", template);
    fileAccess.writeFileContent("managed.value=broken\npersonal.note=keep-me\n", workspaceFile);

    // act
    intellij.run();

    // assert
    Properties properties = fileAccess.readProperties(workspaceFile);
    assertThat(properties).containsEntry("managed.value", "v2").containsEntry("personal.note", "keep-me");
  }

  /**
   * Tests that launching an installed IDE registers an extra Java SDK added after installation without removing existing SDKs.
   *
   * @param os the operating system to simulate.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  void testRegisterExtraJavaSdkAfterInstallation(String os) {

    WindowsSymlinkTestHelper.assumeSymlinksSupported();
    // arrange
    IdeTestContext context = newContext("intellij");
    context.setSystemInfo(SystemInfoMock.of(os));
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    intellij.install();
    Path jdkTable = context.getWorkspacePath().resolve(".intellij/config/options/jdk.table.xml");
    String sdkName = "additional-java";
    String sdkPath = "/application/component[@name='ProjectJdkTable']/jdk[name/@value='" + sdkName + "']";
    XmlAssert.assertThat(jdkTable).valueByXPath("count(" + sdkPath + ")").isEqualTo("0");

    Path extraJavaHome = context.getSoftwareExtraPath().resolve("java").resolve(sdkName);
    context.getFileAccess().mkdirs(extraJavaHome);
    ExtraToolsMapper mapper = ExtraToolsMapper.get();
    ExtraTools extraTools = new ExtraTools();
    extraTools.addExtraInstallations("java", new ExtraToolInstallation(sdkName, VersionIdentifier.of("21"), null));
    mapper.saveJsonToFolder(extraTools, context.getSettingsPath());

    // act
    intellij.run();

    // assert
    XmlAssert.assertThat(jdkTable).valueByXPath("count(" + sdkPath + ")").isEqualTo("1");
    XmlAssert.assertThat(jdkTable).valueByXPath(sdkPath + "/homePath/@value").isEqualTo(extraJavaHome.toString().replace('\\', '/'));
    XmlAssert.assertThat(jdkTable).valueByXPath(sdkPath + "/version/@value").isEqualTo("21");
    XmlAssert.assertThat(jdkTable).valueByXPath("count(/application/component[@name='ProjectJdkTable']/jdk[name/@value='Java'])").isEqualTo("1");
  }

  /**
   * Tests that {@link AbstractIdeToolCommandlet#getIdeMetadataPath()} resolves to {@code $IDE_HOME/.ide/«ide»/«workspace»} instead of the workspace itself.
   */
  @Test
  void testGetIdeMetadataPath() {
    // arrange
    IdeContext context = newContext("intellij");
    AbstractIdeToolCommandlet ide = context.getCommandletManager().getCommandlet(Intellij.class);
    // act
    Path metadataPath = ide.getIdeMetadataPath();
    // assert
    assertThat(metadataPath).startsWithRaw(context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE));
    assertThat(metadataPath).endsWithRaw(Path.of("intellij", context.getWorkspaceName()));
    assertThat(metadataPath).isNotEqualTo(context.getWorkspacePath());
  }
}
