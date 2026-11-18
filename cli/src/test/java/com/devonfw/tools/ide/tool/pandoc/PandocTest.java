package com.devonfw.tools.ide.tool.pandoc;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.CapturingProcessContextMock;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Pandoc}.
 */
@WireMockTest
public class PandocTest extends AbstractIdeContextTest {

  private static final String PANDOC_PROJECT = "pandoc";

  private static final String PANDOC_VERSION = "3.12";

  @Test
  void testPandocInstallsFromMockArchive(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PANDOC_PROJECT, wireMockRuntimeInfo);
    Pandoc pandoc = new Pandoc(context);

    // act
    pandoc.install();

    // assert
    assertInstalled(context);
  }

  @Test
  void testPandocRunInstallsAndPassesArguments(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PANDOC_PROJECT, wireMockRuntimeInfo);
    Pandoc pandoc = new Pandoc(context);
    pandoc.arguments.addValue("hello");
    pandoc.arguments.addValue("world");

    // act
    pandoc.run();

    // assert
    assertInstalled(context);
    assertThat(context).logAtInfo().hasMessage("pandoc --data-dir=" + context.getConfPath().resolve("pandoc") + " hello world");
  }

  @ParameterizedTest
  @MethodSource("argumentsWithoutProjectDataDirectory")
  void testPandocPreservesOverridesAndSubcommands(List<String> args) {

    IdeTestContext context = newContext(PANDOC_PROJECT);
    Pandoc pandoc = new Pandoc(context);
    CapturingProcessContextMock process = new CapturingProcessContextMock(context);

    pandoc.runTool(process, ProcessMode.DEFAULT, args);

    assertThat(process.getExecutedCommands()).containsExactly("pandoc " + String.join(" ", args));
  }

  private static Stream<List<String>> argumentsWithoutProjectDataDirectory() {

    return Stream.of(List.of("--data-dir=custom data", "input.md"), List.of("--data-dir", "custom data", "input.md"),
        List.of("lua", "-e", "print('ok')"), List.of("server", "--help"));
  }

  private void assertInstalled(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("pandoc/.ide.software.version")).exists().hasContent(PANDOC_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed pandoc in version " + PANDOC_VERSION);
  }
}
