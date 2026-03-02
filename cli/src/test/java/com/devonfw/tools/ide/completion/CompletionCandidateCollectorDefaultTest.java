package com.devonfw.tools.ide.completion;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.VersionCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.property.VersionProperty;

/**
 * Test of {@link CompletionCandidateCollectorDefault}.
 */
class CompletionCandidateCollectorDefaultTest extends AbstractIdeContextTest {

  /**
   * Test of {@link CompletionCandidateCollectorDefault#addAllMatches(String, String[], Property, Commandlet)}
   */
  @Test
  void testAddAllMatches() {

    // arrange
    String[] sortedCandidates = { "1", "2.0", "2.1", "3", "20", "30", "200" };
    String input = "2";
    String[] expectedCandidates = { "2.0", "2.1", "20", "200" };

    VersionProperty versionProperty = new VersionProperty("", false, "version");
    IdeContext context = IdeTestContextMock.get();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);

    // act
    int matches = collector.addAllMatches(input, sortedCandidates, versionProperty, new VersionCommandlet(context));

    // assert
    assertThat(matches).isEqualTo(expectedCandidates.length);
    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text)).containsExactly(expectedCandidates);
  }

  @Test
  void testAddAllMatchesEmptyInput() {

    // arrange
    String[] sortedCandidates = { "1", "2.0", "2.1", "3", "20", "30", "200" };
    String input = "";

    VersionProperty versionProperty = new VersionProperty("", false, "version");
    IdeContext context = IdeTestContextMock.get();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);

    // act
    int matches = collector.addAllMatches(input, sortedCandidates, versionProperty, new VersionCommandlet(context));

    // assert
    assertThat(matches).isEqualTo(sortedCandidates.length);
    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text)).containsExactly(sortedCandidates);
  }

  @Test
  void testClearCandidates() {

    // arrange
    String[] sortedCandidates = { "11" };
    String input = "1";
    String[] expectedCandidates = sortedCandidates;

    VersionProperty versionProperty = new VersionProperty("", false, "version");
    IdeContext context = IdeTestContextMock.get();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);

    // act
    collector.addAllMatches(input, sortedCandidates, versionProperty, new VersionCommandlet(context));
    assertThat(collector.getCandidates()).isNotEmpty();
    collector.clear();

    // assert
    assertThat(collector.getCandidates()).isEmpty();
  }
}
