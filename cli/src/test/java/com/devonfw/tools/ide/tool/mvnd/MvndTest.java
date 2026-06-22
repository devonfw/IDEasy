package com.devonfw.tools.ide.tool.mvnd;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.property.Property;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Mvnd}.
 */
@WireMockTest
class MvndTest extends AbstractIdeContextTest {

  private static final String MVND_PROJECT = "mvnd";

  @Test
  void testMvndBasicProperties(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(MVND_PROJECT, wireMockRuntimeInfo);
    Mvnd mvnd = new Mvnd(context);

    // assert
    assertThat(mvnd.getName()).isEqualTo("mvnd");
    assertThat(mvnd.getTags()).contains(Tag.JAVA, Tag.BUILD);
    assertThat(mvnd.getToolHelpArguments()).isEqualTo("--help");
  }

  @Test
  void testMvndAutoCompletion(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(MVND_PROJECT, wireMockRuntimeInfo);
    Mvnd mvnd = new Mvnd(context);
    CompletionCandidateCollectorDefault collector = new CompletionCandidateCollectorDefault(context);
    Property<?> property = mvnd.arguments;

    // act
    mvnd.completeToolArguments("", collector, property);

    // assert
    java.util.List<String> candidates = collector.getCandidates().stream().map(c -> c.text()).collect(Collectors.toList());
    assertThat(candidates).contains("clean", "compile", "dependency:tree", "-DskipTests", "--status", "--stop");
  }

  @Test
  void testMvndInstallAndRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(MVND_PROJECT, wireMockRuntimeInfo);
    Mvnd mvnd = new Mvnd(context);

    // act
    mvnd.install();

    // assert
    assertThat(context.getSoftwarePath().resolve("mvnd/bin/mvnd")).exists();
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed mvnd");

    // act
    mvnd.arguments.addValue("foo");
    mvnd.arguments.addValue("bar");
    mvnd.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("mvnd foo bar");
  }

}
