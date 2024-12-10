package com.devonfw.tools.ide.property;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.commandlet.HelpCommandlet;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.tool.helm.Helm;
import com.devonfw.tools.ide.tool.intellij.Intellij;

class CommandletPropertyTest {

  @Test
  public void testCompleteValue() {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    String[] expectedCandidates = { "help", "helm" };
    String input = "he";
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);
    CommandletProperty cmdProp = new CommandletProperty("", false, "");

    // act
    cmdProp.completeValue(input, context, new ContextCommandlet(), collector);

    // assert
    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text)).containsExactlyInAnyOrder(expectedCandidates);
  }

  @Test
  public void testParse() {

    // arrange
    IdeContext context = IdeTestContextMock.get();

    // act
    CommandletProperty cmdProp = new CommandletProperty("", false, "");

    // act + assert
    assertThat(cmdProp.parse("help", context)).isInstanceOf(HelpCommandlet.class);
    assertThat(cmdProp.parse("helm", context)).isInstanceOf(Helm.class);
    assertThat(cmdProp.parse("intellij", context)).isInstanceOf(Intellij.class);
  }
}
