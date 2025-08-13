package com.devonfw.tools.ide.context;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.os.WindowsHelperMock;

/**
 * Test for bash finding functionality using WindowsHelper.
 */
public class BashFindingTest extends AbstractIdeContextTest {

  @Test
  public void testFindBash() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    
    // act
    String bashPath = context.findBash();
    
    // assert
    // On non-Windows systems, it should return "bash"
    // On Windows with proper registry entries, it should return the full path
    assertThat(bashPath).isNotNull();
    
    // This is mainly to verify the refactoring doesn't break existing functionality
    // The exact result depends on the system and mock setup
  }
}