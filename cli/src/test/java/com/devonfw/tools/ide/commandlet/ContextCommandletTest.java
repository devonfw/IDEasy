package com.devonfw.tools.ide.commandlet;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContextConsole;

public class ContextCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link ContextCommandlet} has name context.
   */
  @Test
  public void testNameIsContext(){
    //arrange
    ContextCommandlet cxt = new ContextCommandlet();
    //act & assert
    assertThat(cxt.getName()).isEqualTo("context");
  }

  /**
   * Test of {@link ContextCommandlet} does not require home.
   */
  @Test
  public void testThatHomeIsNotReqired() {

    // arrange
    ContextCommandlet cxt = new ContextCommandlet();
    //act & assert
    assertThat(cxt.isIdeHomeRequired()).isEqualTo(false);
  }

  /**
   * Test of {@link ContextCommandlet} getIdeContext.
   */
  @Test
  public void testGetIdeContext() {

    // arrange
    ContextCommandlet cxt = new ContextCommandlet();
    cxt.run();
    // act
    // assert
    assertThat(cxt.getIdeContext()).isInstanceOf(AbstractIdeContext.class);
  }

  /**
   * Test of {@link ContextCommandlet} run.
   */
  @Test
  public void testRun() {

    // arrange
    ContextCommandlet cxt = new ContextCommandlet();
    // act
    cxt.run();
    // assert
    assertThat(cxt.getIdeContext()).isInstanceOf(IdeContextConsole.class);
    assertThat(cxt.getIdeContext().isForceMode()).isFalse();
    assertThat(cxt.getIdeContext().isBatchMode()).isFalse();
    assertThat(cxt.getIdeContext().isQuietMode()).isFalse();
    assertThat(cxt.getIdeContext().isOfflineMode()).isFalse();
    assertThat(cxt.getIdeContext().getLocale()).isEqualTo(Locale.getDefault());

  }
}
