package com.devonfw.tools.ide.tool.pip;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link Pip}.
 */
public class PipTest extends AbstractIdeContextTest {

  /**
   * Tests that the {@link Pip} commandlet is properly instantiated and delegates to UV.
   */
  @Test
  public void testPipCommandletCreation() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    
    // act
    Pip pip = new Pip(context);

    // assert
    assertThat(pip).isNotNull();
    assertThat(pip.getName()).isEqualTo("pip");
  }
}
