package com.devonfw.tools.ide.tool.aws;

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

/**
 * Test of {@link Aws}.
 */
class AwsTest extends AbstractIdeContextTest {

  private static final String PROJECT_AWS = "aws";
  private final IdeTestContext context = newContext(PROJECT_AWS);

  /**
   * Tests if the {@link Aws} can be installed properly.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac" })
  void testAwsInstall(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Aws awsCommandlet = new Aws(context);

    // act
    awsCommandlet.install();

    // assert
    checkInstallation(context);
    assertThat(awsCommandlet.getName()).isEqualTo(PROJECT_AWS);
    assertThat(awsCommandlet.getTags()).containsExactly(Tag.CLOUD);

    //if tool already installed
    awsCommandlet.install();
    assertThat(context).logAtDebug().hasMessageContaining("Version 2.24.15 of tool aws is already installed");
  }

  /**
   * Tests if the environment variables are correctly set after {@link Aws} is installed.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac" })
  void testAwsSetEnvironment(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Aws awsCommandlet = new Aws(context);
    EnvironmentCommandlet envCommandlet = new EnvironmentCommandlet(context);

    // act
    awsCommandlet.install();
    envCommandlet.run();

    // assert
    if (os.equals("mac")) {
      assertThat(context).log().hasEntries(
          IdeLogEntry.ofProcessable("export AWS_CONFIG_FILE=\"" + context.getConfPath().resolve(PROJECT_AWS).resolve("config") + "\""), //
          IdeLogEntry.ofProcessable("export AWS_SHARED_CREDENTIALS_FILE=\"" + context.getConfPath().resolve(PROJECT_AWS).resolve("credentials") + "\"")
      );
    } else {
      assertThat(context).log().hasEntries(
          IdeLogEntry.ofProcessable("AWS_CONFIG_FILE=" + context.getConfPath().resolve(PROJECT_AWS).resolve("config")), //
          IdeLogEntry.ofProcessable("AWS_SHARED_CREDENTIALS_FILE=" + context.getConfPath().resolve(PROJECT_AWS).resolve("credentials"))
      );
    }

  }

  /**
   * Tests if the output of {@link Aws#printHelp(NlsBundle)} is correct.
   */
  @Test
  void testAwsPrintHelp() {

    // arrange
    HelpCommandlet helpCommandlet = new HelpCommandlet(context);
    helpCommandlet.commandlet.setValueAsString(PROJECT_AWS, context);

    // act
    helpCommandlet.run();

    // assert
    assertThat(context).logAtInfo()
        .hasMessage("To get detailed help about the usage of the AWS CLI, use \"aws help\"");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("aws/.ide.software.version")).exists().hasContent("2.24.15");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed aws in version 2.24.15");
    assertThat(context.getConfPath().resolve(PROJECT_AWS)).exists();
  }

}
