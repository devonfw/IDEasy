package com.devonfw.tools.ide.tool.mvn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.InstallCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * Integration test of {@link Mvn}.
 */
public class MvnTest extends AbstractIdeContextTest {

  private static final String PROJECT_MVN = "mvn";

  //private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\[(.*?)\\]");
  private static final Pattern VARIABLE_PATTERN = Pattern.compile("<([^>]+)>(.*?)</\\1>");

  /**
   * Tests the installation of {@link Mvn}
   *
   * @throws IOException if an I/O error occurs during the installation process
   */
  @Test
  public void testMvnInstall() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_MVN);
    context.setInputValues(List.of("testLogin", "testPassword"));
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("mvn", context);

    // act
    install.run();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests the execution of {@link Mvn}
   *
   * @throws IOException if an I/O error occurs during the installation process
   */
  @Test
  public void testMvnRun() throws IOException {
    // arrange
    IdeTestContext context = newContext(PROJECT_MVN);
    context.setInputValues(List.of("testLogin", "testPassword"));
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("mvn", context);
    Mvn commandlet = (Mvn) install.tool.getValue();
    commandlet.arguments.addValue("foo");
    commandlet.arguments.addValue("bar");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessage("mvn " + "foo bar");
    checkInstallation(context);
  }

  private void checkInstallation(IdeTestContext context) throws IOException {

    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();

    assertThat(context.getSoftwarePath().resolve("mvn/.ide.software.version")).exists().hasContent("3.9.7");
    assertThat(context).logAtSuccess().hasMessage("Successfully installed mvn in version 3.9.7");

    Path settingsFile = context.getConfPath().resolve(Mvn.MVN_CONFIG_FOLDER).resolve(Mvn.SETTINGS_FILE);
    assertThat(settingsFile).exists();
    assertFileContent(settingsFile, List.of("${env.M2_REPO}", "repository", "testLogin", "testPassword"));

    Path settingsSecurityFile = context.getConfPath().resolve(Mvn.MVN_CONFIG_FOLDER).resolve(Mvn.SETTINGS_SECURITY_FILE);
    assertThat(settingsSecurityFile).exists();
    assertFileContent(settingsSecurityFile, List.of("masterPassword"));
  }

  /**
   * Tests if the user is starting IDEasy without a Maven repository, IDEasy should fall back to USER_HOME/.m2/repository.
   * <p>
   * See: <a href="https://github.com/devonfw/IDEasy/issues/463">#463</a>
   */
  @Test
  public void testMavenRepositoryPathFallsBackToUserHome() {
    // arrange
    String path = "project/workspaces";
    // act
    IdeTestContext context = newContext(PROJECT_MVN, path, false);
    Path mavenRepository = context.getUserHome().resolve(".m2").resolve("repository");
    // assert
    assertThat(IdeVariables.M2_REPO.get(context)).isEqualTo(mavenRepository);
  }

  private void assertFileContent(Path filePath, List<String> expectedValues) throws IOException {

    String content = new String(Files.readAllBytes(filePath));
    Matcher matcher = VARIABLE_PATTERN.matcher(content);
    List<String> values = matcher.results().map(matchResult -> matchResult.group(2)).collect(Collectors.toList());

    assertThat(values).containsExactlyInAnyOrderElementsOf(expectedValues);
  }
}
