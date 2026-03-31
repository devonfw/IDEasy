package com.devonfw.tools.ide.property;

import java.util.Locale;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link LocaleProperty}.
 */
class LocalePropertyTest extends Assertions {

  /** Test of {@link LocaleProperty#setValueAsString(String, IdeContext)}. */
  @Test
  void testGermany() {

    // arrange
    IdeContext context = new IdeTestContext();
    Locale germany = Locale.GERMANY;
    // act
    LocaleProperty property = new LocaleProperty("--locale", true, null);
    property.setValueAsString(germany.toLanguageTag(), context);
    // assert
    assertThat(property.getValue()).isSameAs(germany);
  }

  /**
   * Test of
   * {@link LocaleProperty#completeValue(String, IdeContext, com.devonfw.tools.ide.commandlet.Commandlet, CompletionCandidateCollector) auto-completion}.
   */
  @Test
  void testCompletion() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    String[] expectedCandidates = { "de", "de-AT", "de-BE", "de-CH", "de-DE", "de-IT", "de-LI", "de-LU", "de-Latn-DE" };
    String input = "de";
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);
    // act
    LocaleProperty property = new LocaleProperty("--locale", true, null);
    property.completeValue(input, context, new ContextCommandlet(), collector);
    // assert
    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text)).containsExactly(expectedCandidates);
  }

}
