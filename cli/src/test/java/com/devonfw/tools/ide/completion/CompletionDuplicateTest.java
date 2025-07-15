package com.devonfw.tools.ide.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;

/**
 * Test to check for duplicate completion candidates in IDEasy complete.
 */
public class CompletionDuplicateTest extends AbstractIdeContextTest {

  /** Test that completion for "in" does not produce duplicates. */
  @Test
  public void testCompleteInNoDuplicates() {
    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("in");
    
    // act
    List<CompletionCandidate> candidates = context.complete(args, false);
    
    // assert
    System.out.println("Completion candidates for 'in':");
    for (CompletionCandidate candidate : candidates) {
      System.out.println("  " + candidate.text());
    }
    
    // Check for duplicates
    long uniqueCount = candidates.stream().map(CompletionCandidate::text).distinct().count();
    System.out.println("Total candidates: " + candidates.size());
    System.out.println("Unique candidates: " + uniqueCount);
    
    assertThat(candidates.size()).isEqualTo(uniqueCount);
  }
  
  /** Test that completion for "install" does not produce duplicates. */
  @Test
  public void testCompleteInstallNoDuplicates() {
    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("install");
    
    // act
    List<CompletionCandidate> candidates = context.complete(args, false);
    
    // assert
    System.out.println("Completion candidates for 'install':");
    for (CompletionCandidate candidate : candidates) {
      System.out.println("  " + candidate.text());
    }
    
    // Check for duplicates
    long uniqueCount = candidates.stream().map(CompletionCandidate::text).distinct().count();
    System.out.println("Total candidates: " + candidates.size());
    System.out.println("Unique candidates: " + uniqueCount);
    
    assertThat(candidates.size()).isEqualTo(uniqueCount);
  }
}