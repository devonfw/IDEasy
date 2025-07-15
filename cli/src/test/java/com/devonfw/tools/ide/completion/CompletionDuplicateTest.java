package com.devonfw.tools.ide.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Test to verify that completion candidates are not duplicated.
 */
public class CompletionDuplicateTest extends AbstractIdeContextTest {

  /** 
   * Test to verify that completion candidates are not duplicated.
   * This test checks that the fix for issue #536 is working correctly.
   */
  @Test
  public void testCompletionNoDuplicates() {
    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    context.level(IdeLogLevel.TRACE);
    
    // Test various completion scenarios that could potentially produce duplicates
    String[] testInputs = {"i", "in", "install", "h", "help", "j", "java"};
    
    for (String input : testInputs) {
      // act
      CliArguments args = CliArguments.ofCompletion(input);
      List<CompletionCandidate> candidates = context.complete(args, false);
      
      // assert
      Map<String, Integer> textCounts = new HashMap<>();
      for (CompletionCandidate candidate : candidates) {
        String text = candidate.text();
        textCounts.put(text, textCounts.getOrDefault(text, 0) + 1);
      }
      
      // Check for duplicates
      long uniqueCount = candidates.stream().map(CompletionCandidate::text).distinct().count();
      
      // Show duplicates if any
      List<String> duplicates = textCounts.entrySet().stream()
          .filter(entry -> entry.getValue() > 1)
          .map(entry -> entry.getKey() + " (x" + entry.getValue() + ")")
          .collect(java.util.stream.Collectors.toList());
      
      if (!duplicates.isEmpty()) {
        System.out.println("DUPLICATES found for input '" + input + "': " + duplicates);
      }
      
      assertThat(candidates.size()).as("Input '%s' should not have duplicate candidates", input).isEqualTo(uniqueCount);
    }
  }
}