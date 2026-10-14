package com.devonfw.tools.ide.tool.gcloud;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.commandlet.EnvironmentCommandlet;
import com.devonfw.tools.ide.commandlet.HelpCommandlet;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.claude.RecordingEnvironmentContext;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link Gcloud}.
 */
class GcloudTest extends AbstractIdeContextTest {

  private static final String PROJECT_GCLOUD = "gcloud";

  private static final String GCLOUD_VERSION = "520.0.0";

  private final IdeTestContext context = newContext(PROJECT_GCLOUD);

  /**
   * Tests if the {@link Gcloud} can be installed properly on platforms that ship a bundled Python.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "linux" })
  void testGcloudInstall(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Gcloud gcloud = new Gcloud(this.context);

    // act
    gcloud.install();

    // assert
    checkInstallation(this.context);
    assertThat(gcloud.getName()).isEqualTo(PROJECT_GCLOUD);
    assertThat(gcloud.getTags()).containsExactly(Tag.CLOUD);

    // if tool already installed
    gcloud.install();
    assertThat(this.context).logAtDebug()
        .hasMessageContaining("Version " + GCLOUD_VERSION + " of tool gcloud is already installed");
  }

  /**
   * Tests that installing {@link Gcloud} on macOS also installs the required Python dependency automatically and
   * exports {@code CLOUDSDK_PYTHON}.
   */
  @Test
  void testGcloudInstallOnMacInstallsPythonDependency() {

    // arrange
    this.context.setSystemInfo(SystemInfoMock.MAC_X64);
    Gcloud gcloud = new Gcloud(this.context);
    EnvironmentCommandlet envCommandlet = new EnvironmentCommandlet(this.context);

    // act
    gcloud.install();
    envCommandlet.run();

    // assert
    checkInstallation(this.context);
    assertThat(this.context.getSoftwarePath().resolve("python")).exists();
    assertThat(this.context).logAtSuccess().hasMessageContaining("Successfully installed python");
    Path python3 = this.context.getSoftwarePath().resolve("python").resolve("bin").resolve("python3");
    assertThat(this.context).log()
        .hasEntries(IdeLogEntry.ofProcessable("export CLOUDSDK_PYTHON=\"" + python3 + "\""));
  }

  /**
   * Tests if the environment variables are correctly set after {@link Gcloud} is installed.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "linux" })
  void testGcloudSetEnvironmentCloudSdkConfig(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Gcloud gcloud = new Gcloud(this.context);
    EnvironmentCommandlet envCommandlet = new EnvironmentCommandlet(this.context);

    // act
    gcloud.install();
    envCommandlet.run();

    // assert
    Path configDir = this.context.getConfPath().resolve(".gcloud");
    if (os.equals("windows")) {
      assertThat(this.context).log().hasEntries(IdeLogEntry.ofProcessable("CLOUDSDK_CONFIG=" + configDir));
    } else {
      assertThat(this.context).log()
          .hasEntries(IdeLogEntry.ofProcessable("export CLOUDSDK_CONFIG=\"" + configDir + "\""));
    }
  }

  /**
   * Tests that {@link Gcloud#setEnvironment} points {@code CLOUDSDK_PYTHON} at the IDEasy python when the gcloud
   * installation has no bundled python.
   */
  @Test
  void testGcloudSetEnvironmentCloudSdkPythonWhenNoBundledPython() throws Exception {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of("mac");
    this.context.setSystemInfo(systemInfo);
    Path gcloudRoot = this.context.getSoftwarePath().resolve("gcloud");
    Path pythonBin = this.context.getSoftwarePath().resolve("python").resolve("bin");
    Files.createDirectories(gcloudRoot.resolve("bin"));
    Files.createDirectories(pythonBin);
    Path python3 = pythonBin.resolve("python3");
    Files.writeString(python3, "#!/bin/bash\n");
    python3.toFile().setExecutable(true);
    Files.writeString(gcloudRoot.resolve(".ide.software.version"), GCLOUD_VERSION);

    Gcloud gcloud = new Gcloud(this.context);
    ToolInstallation installation = new ToolInstallation(gcloudRoot, gcloudRoot, gcloudRoot.resolve("bin"),
        VersionIdentifier.of(GCLOUD_VERSION), false);
    RecordingEnvironmentContext environmentContext = new RecordingEnvironmentContext();

    // act
    gcloud.setEnvironment(environmentContext, installation, false);

    // assert
    assertThat(environmentContext.set).containsEntry("CLOUDSDK_CONFIG",
        this.context.getConfPath().resolve(".gcloud").toString());
    assertThat(environmentContext.set).containsEntry("CLOUDSDK_PYTHON", python3.toString());
  }

  /**
   * Tests that {@link Gcloud#setEnvironment} does not set {@code CLOUDSDK_PYTHON} when gcloud ships a bundled python.
   */
  @Test
  void testGcloudSetEnvironmentSkipsCloudSdkPythonWhenBundled() throws Exception {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of("linux");
    this.context.setSystemInfo(systemInfo);
    Path gcloudRoot = this.context.getSoftwarePath().resolve("gcloud");
    Path bundledPython = gcloudRoot.resolve("platform").resolve("bundledpythonunix").resolve("bin").resolve("python3");
    Files.createDirectories(bundledPython.getParent());
    Files.writeString(bundledPython, "#!/bin/bash\n");
    Files.createDirectories(gcloudRoot.resolve("bin"));

    Gcloud gcloud = new Gcloud(this.context);
    ToolInstallation installation = new ToolInstallation(gcloudRoot, gcloudRoot, gcloudRoot.resolve("bin"),
        VersionIdentifier.of(GCLOUD_VERSION), false);
    RecordingEnvironmentContext environmentContext = new RecordingEnvironmentContext();

    // act
    gcloud.setEnvironment(environmentContext, installation, false);

    // assert
    assertThat(environmentContext.set).containsEntry("CLOUDSDK_CONFIG",
        this.context.getConfPath().resolve(".gcloud").toString());
    assertThat(environmentContext.set).doesNotContainKey("CLOUDSDK_PYTHON");
  }

  /**
   * Tests if the output of {@link Gcloud#printHelp(NlsBundle)} is correct.
   */
  @Test
  void testGcloudPrintHelp() {

    // arrange
    HelpCommandlet helpCommandlet = new HelpCommandlet(this.context);
    helpCommandlet.commandlet.setValueAsString(PROJECT_GCLOUD, this.context);

    // act
    helpCommandlet.run();

    // assert
    assertThat(this.context).logAtInfo()
        .hasMessage("To get detailed help about the usage of the Google Cloud CLI, use \"gcloud help\"");
  }

  private void checkInstallation(IdeTestContext context) {

    Gcloud commandlet = context.getCommandletManager().getCommandlet(Gcloud.class);
    assertThat(commandlet.getInstalledVersion().toString()).isEqualTo(GCLOUD_VERSION);
    assertThat(context).logAtSuccess()
        .hasMessageContaining("Successfully installed gcloud in version " + GCLOUD_VERSION);
    assertThat(context.getConfPath().resolve(".gcloud")).exists();
  }

}
