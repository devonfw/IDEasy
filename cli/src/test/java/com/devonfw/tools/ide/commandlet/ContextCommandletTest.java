package com.devonfw.tools.ide.commandlet;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.property.FlagProperty;

public class ContextCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link ContextCommandlet} has name context.
   */
  @Test
  public void testNameIsContext() {
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
    assertThat(cxt.isIdeHomeRequired()).isFalse();
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

  /**
   * Test of {@link ContextCommandlet} run with all flag options enabled
   */
  @Test
  public void testRunWithOptions() {

    // arrange
    ContextCommandlet cxt = new ContextCommandlet();
    ((FlagProperty) cxt.getOption("--force")).setValue(Boolean.TRUE);
    ((FlagProperty) cxt.getOption("--batch")).setValue(Boolean.TRUE);
    ((FlagProperty) cxt.getOption("--quiet")).setValue(Boolean.TRUE);
    ((FlagProperty) cxt.getOption("--offline")).setValue(Boolean.TRUE);
    // act
    cxt.run();
    // assert
    assertThat(cxt.getIdeContext()).isInstanceOf(IdeContextConsole.class);
    assertThat(cxt.getIdeContext().isForceMode()).isTrue();
    assertThat(cxt.getIdeContext().isBatchMode()).isTrue();
    assertThat(cxt.getIdeContext().isQuietMode()).isTrue();
    assertThat(cxt.getIdeContext().isOfflineMode()).isTrue();

  }
}
