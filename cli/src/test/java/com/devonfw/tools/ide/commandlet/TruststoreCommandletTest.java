package com.devonfw.tools.ide.commandlet;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.util.TruststoreUtil;

/**
 * Test of {@link TruststoreCommandlet}.
 */
class TruststoreCommandletTest extends AbstractIdeContextTest {

  private static final String IDE_OPTIONS = "IDE_OPTIONS";


  private String previousTruststore;

  private String previousTruststorePassword;

  @AfterEach
  void cleanSystemProperties() {

    restoreSystemProperty("javax.net.ssl.trustStore", this.previousTruststore);
    restoreSystemProperty("javax.net.ssl.trustStorePassword", this.previousTruststorePassword);
  }

  @Test
  void testRunWithInvalidEndpointThrowsCliException(@TempDir Path tempDir) {

    IdeTestContext context = newIsolatedContext(tempDir);
    TruststoreCommandlet commandlet = context.getCommandletManager().getCommandlet(TruststoreCommandlet.class);
    setUrl(commandlet, context, "http://github.com");

    assertThatThrownBy(commandlet::run).isInstanceOf(CliException.class)
        .hasMessageContaining("Invalid target URL/host")
        .hasMessageContaining("Only HTTPS URLs are supported");
  }

  @Test
  void testRunWithUnreachableEndpointLogsHintAndKeepsTruststoreUntouched(@TempDir Path tempDir) {

    IdeTestContext context = newIsolatedContext(tempDir);
    TruststoreCommandlet commandlet = context.getCommandletManager().getCommandlet(TruststoreCommandlet.class);
    setUrl(commandlet, context, "https://127.0.0.1:9");

    Path customTruststorePath = context.getSettingsPath().resolve("truststore").resolve("truststore.p12");
    commandlet.run();

    assertThat(customTruststorePath).doesNotExist();
    assertThat(context).logAtInfo().hasMessageContaining("is not reachable/valid without certificate changes");
    assertThat(context).logAtInteraction().hasMessageContaining("proxy-support.adoc#tls-certificate-issues");
  }

  @Test
  void testConfigureIdeOptionsReplacesExistingTruststoreOptions(@TempDir Path tempDir) throws Exception {

    IdeTestContext context = newIsolatedContext(tempDir);
    TruststoreCommandlet commandlet = context.getCommandletManager().getCommandlet(TruststoreCommandlet.class);
    rememberSystemProperties();

    EnvironmentVariables userVariables = context.getVariables().getByType(EnvironmentVariablesType.USER);
    userVariables.set(IDE_OPTIONS,
        "-Xmx512m -Djavax.net.ssl.trustStore=/old/path.p12 -Djavax.net.ssl.trustStorePassword=old-secret");

    Path newTruststorePath = context.getSettingsPath().resolve("truststore").resolve("truststore.p12");

    invokeConfigureIdeOptions(commandlet, newTruststorePath);

    String options = userVariables.getFlat(IDE_OPTIONS);
    String expectedPassword = Arrays.toString(TruststoreUtil.CUSTOM_TRUSTSTORE_PASSWORD);

    assertThat(options).contains("-Xmx512m");
    assertThat(options).contains("-Djavax.net.ssl.trustStore=" + newTruststorePath.toAbsolutePath());
    assertThat(options).contains("-Djavax.net.ssl.trustStorePassword=" + expectedPassword);
    assertThat(options).doesNotContain("/old/path.p12");
    assertThat(options).doesNotContain("old-secret");

    assertThat(System.getProperty("javax.net.ssl.trustStore")).isEqualTo(newTruststorePath.toAbsolutePath().toString());
    assertThat(System.getProperty("javax.net.ssl.trustStorePassword")).isEqualTo(expectedPassword);
  }

  private IdeTestContext newIsolatedContext(Path tempDir) {

    IdeTestContext context = newContext(PROJECT_BASIC);
    Path isolatedSettingsPath = tempDir.resolve("settings");
    assertThat(isolatedSettingsPath.startsWith(tempDir)).isTrue();
    context.setSettingsPath(isolatedSettingsPath);
    assertThat(context.getSettingsPath()).isEqualTo(isolatedSettingsPath);
    return context;
  }

  private static void setUrl(TruststoreCommandlet commandlet, IdeTestContext context, String url) {

    Property<?> endpointValue = commandlet.getValues().get(1);
    endpointValue.setValueAsString(url, context);
  }

  private static void invokeConfigureIdeOptions(TruststoreCommandlet commandlet, Path customTruststorePath) throws Exception {

    Method method = TruststoreCommandlet.class.getDeclaredMethod("configureIdeOptions", Path.class);
    method.setAccessible(true);
    method.invoke(commandlet, customTruststorePath);
  }

  private void rememberSystemProperties() {

    this.previousTruststore = System.getProperty("javax.net.ssl.trustStore");
    this.previousTruststorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
  }

  private static void restoreSystemProperty(String key, String value) {

    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }

}







