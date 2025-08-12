package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.property.FlagProperty;

/**
 * Test class for {@link ContextCommandlet}.
 */
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
  public void testThatHomeIsNotRequired() {

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
    assertThat(cxt.getStartContext().isForceMode()).isFalse();
    assertThat(cxt.getStartContext().isBatchMode()).isFalse();
    assertThat(cxt.getStartContext().isQuietMode()).isFalse();
    assertThat(cxt.getStartContext().isOfflineMode()).isFalse();
    assertThat(cxt.getStartContext().isPrivacyMode()).isFalse();
    assertThat(cxt.getStartContext().getLocale()).isNull();
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
    ((FlagProperty) cxt.getOption("--privacy")).setValue(Boolean.TRUE);
    ((FlagProperty) cxt.getOption("--offline")).setValue(Boolean.TRUE);
    // act
    cxt.run();
    // assert
    assertThat(cxt.getStartContext().isForceMode()).isTrue();
    assertThat(cxt.getStartContext().isBatchMode()).isTrue();
    assertThat(cxt.getStartContext().isQuietMode()).isTrue();
    assertThat(cxt.getStartContext().isPrivacyMode()).isTrue();
    assertThat(cxt.getStartContext().isOfflineMode()).isTrue();

  }
}
