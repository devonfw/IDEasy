package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.validation.ValidationResult;

/**
 * Test of {@link DockerSandboxCommandlet}.
 */
class DockerSandboxCommandletTest extends AbstractIdeContextTest {

  private static final String LAUNCHER_FOLDER = "docker-sandbox";
  private static final String LAUNCHER = "docker-sandbox.sh";

  private final IdeTestContext context = newContext("basic");

  private DockerSandboxCommandlet commandlet;

  private Path settingsPath;

  @BeforeEach
  void setUp() {
    this.commandlet = new DockerSandboxCommandlet(this.context);
    this.settingsPath = this.context.getSettingsPath();
    // clean up anything a previous test may have created inside the settings folder
    this.context.getFileAccess().delete(this.settingsPath.resolve(LAUNCHER_FOLDER));
  }

  @Test
  void testCommandletIsRegistered() {

    AbstractCommandlet registered = this.context.getCommandletManager().getCommandlet("docker-sandbox");
    assertThat(registered).isInstanceOf(DockerSandboxCommandlet.class);
    assertThat(this.commandlet.getFirstKeyword().getName()).isEqualTo("docker-sandbox");
  }

  @Test
  void testFindCommandletRoutesKeyword() {

    Iterator<AbstractCommandlet> it = this.context.getCommandletManager()
        .findCommandlet(argsAfterKeyword(new String[] { "docker-sandbox" }), null);
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next()).isInstanceOf(DockerSandboxCommandlet.class);
  }

  @Test
  void testApplyCapturesArgsAfterKeyword() {

    applyArgs("docker-sandbox", "--repo", "/x", "--image", "foo");
    assertThat(capturedArgs()).containsExactly("--repo", "/x", "--image", "foo");
  }

  @Test
  void testApplyCapturesAgentArgs() {

    // the IDEasy CLI strips the "--" end-of-options token, so the captured arguments are exactly the agent arguments
    applyArgs("docker-sandbox", "-p", "hello");
    assertThat(capturedArgs()).containsExactly("-p", "hello");
  }

  @Test
  void testApplyIsValid() {

    assertThat(applyArgs("docker-sandbox", "-p", "hi").isValid()).isTrue();
    assertThat(applyArgs("docker-sandbox").isValid()).isTrue();
  }

  @ParameterizedTest
  @MethodSource("forwardArgsCases")
  void testForwardArgsReinsertsEndOfOptions(List<String> input, List<String> expected) {

    assertThat(DockerSandboxCommandlet.forwardArgs(input)).isEqualTo(expected);
  }

  private static Stream<Arguments> forwardArgsCases() {

    return Stream.of(
        // no launcher option -> the first (agent) argument is preceded by the re-inserted "--"
        Arguments.of(List.of("-p", "hi"), List.of("--", "-p", "hi")),
        // launcher value option first, then the agent arguments
        Arguments.of(List.of("--repo", "/x", "-p", "hi"), List.of("--repo", "/x", "--", "-p", "hi")),
        // launcher flag option first, then the agent arguments
        Arguments.of(List.of("--pull", "-p", "hi"), List.of("--pull", "--", "-p", "hi")),
        // a launcher flag that takes no value and no further arguments -> nothing re-inserted
        Arguments.of(List.of("--help"), List.of("--help")),
        // only launcher value options and no agent arguments -> nothing re-inserted
        Arguments.of(List.of("--repo", "/x", "--image", "foo"), List.of("--repo", "/x", "--image", "foo")),
        // a literal "--" is already present (defensive) -> it is kept
        Arguments.of(List.of("--", "-p", "hi"), List.of("--", "-p", "hi")),
        // no arguments at all
        Arguments.of(List.of(), List.of()));
  }

  @Test
  void testRunInvokesLauncherFromSettingsFolder() {

    Path bash = this.context.findBash();
    Assumptions.assumeTrue((bash != null) && Files.isExecutable(bash), "no bash available for the end-to-end test");

    createFakeLauncher();
    applyArgs("docker-sandbox", "--repo", "/x", "-p", "hi");
    this.commandlet.run();

    List<String> recorded = this.context.getFileAccess().readFileLines(this.settingsPath.resolve(LAUNCHER_FOLDER).resolve(LAUNCHER + ".out"));
    // the fake launcher records the arguments it received, i.e. everything the commandlet forwarded after the launcher path
    assertThat(recorded).containsExactly("--repo", "/x", "--", "-p", "hi");
  }

  @Test
  void testRunFailsWhenLauncherIsMissing() {

    // nothing is created by the default setUp, so the launcher is absent
    applyArgs("docker-sandbox", "--check");
    assertThatThrownBy(this.commandlet::run).isInstanceOf(CliException.class);
  }

  private void createFakeLauncher() {

    Path launcher = this.settingsPath.resolve(LAUNCHER_FOLDER).resolve(LAUNCHER);
    this.context.getFileAccess().writeFileContent("#!/usr/bin/env bash\nprintf '%s\\n' \"$@\" > \"${0}.out\"\nexit 0\n", launcher, true);
  }

  private ValidationResult applyArgs(String... args) {

    return this.context.apply(argsAfterKeyword(args), this.commandlet);
  }

  /**
   * Creates a {@link CliArguments} positioned on the first real argument (past the start token), as the CLI dispatch does before applying the arguments
   * to a commandlet.
   *
   * @param args the CLI arguments (starting with the commandlet keyword).
   * @return the {@link CliArguments}.
   */
  private static CliArguments argsAfterKeyword(String[] args) {

    CliArguments arguments = new CliArguments(CliArgument.of(args));
    if (arguments.current().isStart()) {
      arguments.next();
    }
    return arguments;
  }

  private String[] capturedArgs() {

    Property<?> argsProperty = this.commandlet.getValues().stream().filter(property -> "args".equals(property.getAlias())).findFirst().orElseThrow();
    return ((StringProperty) argsProperty).asArray();
  }
}
