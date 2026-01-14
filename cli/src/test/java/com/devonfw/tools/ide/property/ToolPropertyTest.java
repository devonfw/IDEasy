package com.devonfw.tools.ide.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.tool.java.Java;

/**
 * Test of {@link ToolProperty}.
 */
class ToolPropertyTest {

  @Test
  void testCompleteValue() {
    IdeContext context = IdeTestContextMock.get();
    String[] expectedCandidates = { "az", "android-studio", "aws" };
    String input = "a";
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);

    ToolProperty toolProp = new ToolProperty("", false, "");
    toolProp.completeValue(input, context, new ContextCommandlet(), collector);

    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text)).containsExactlyInAnyOrder(expectedCandidates);
  }

  @Test
  void testParse() {
    IdeContext context = IdeTestContextMock.get();
    ToolProperty toolProp = new ToolProperty("", false, "");

    assertThat(toolProp.parse("intellij", context)).isInstanceOf(Intellij.class);
    assertThat(toolProp.parse("java", context)).isInstanceOf(Java.class);

    assertThrows(IllegalArgumentException.class, () -> toolProp.parse("install", context));
  }
}
