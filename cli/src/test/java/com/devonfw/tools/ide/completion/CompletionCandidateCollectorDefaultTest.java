package com.devonfw.tools.ide.completion;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.VersionCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.property.VersionProperty;
import org.junit.jupiter.api.Test;

class CompletionCandidateCollectorDefaultTest extends AbstractIdeContextTest {

  /**
   * Test of {@link CompletionCandidateCollectorDefault#addAllMatches(String, String[], Property, Commandlet)}
   */
  @Test
  public void testAddAllMatches() {

    String[] sortedCandidates = { "1", "2.0", "2.1", "3", "20" };
    String input = "2";
    String[] expectedCandidates = { "2.0", "2.1", "20" };

    VersionProperty versionProperty = new VersionProperty("", false, "version");
    IdeContext context = IdeTestContextMock.get();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);

    int matches = collector.addAllMatches(input, sortedCandidates, versionProperty, new VersionCommandlet(context));

    assertThat(matches).isEqualTo(3);
    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text)).containsExactly(expectedCandidates);

  }
}