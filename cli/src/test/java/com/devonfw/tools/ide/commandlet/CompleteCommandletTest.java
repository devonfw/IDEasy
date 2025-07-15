package com.devonfw.tools.ide.commandlet;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test to verify that CompleteCommandlet does not produce duplicates.
 */
public class CompleteCommandletTest extends AbstractIdeContextTest {

  /** Test that CompleteCommandlet does not produce duplicate completion candidates. */
  @Test
  public void testCompleteCommandletNoDuplicates() {
    // arrange
    IdeTestContext context = new IdeTestContext();
    
    // Capture output
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(outputStream));
    
    try {
      // Create CompleteCommandlet and set args
      CompleteCommandlet completeCmd = new CompleteCommandlet(context);
      completeCmd.args.setValue("in");
      
      // act
      completeCmd.run();
      
      // assert
      String output = outputStream.toString();
      String[] lines = output.split("\n");
      
      // Filter out empty lines
      String[] nonEmptyLines = Arrays.stream(lines)
          .filter(line -> !line.trim().isEmpty())
          .toArray(String[]::new);
      
      Set<String> uniqueLines = Arrays.stream(nonEmptyLines)
          .collect(Collectors.toSet());
      
      assertThat(nonEmptyLines.length).as("Should not have duplicate completion lines").isEqualTo(uniqueLines.size());
      
    } finally {
      System.setOut(originalOut);
    }
  }
}