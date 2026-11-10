package com.devonfw.tools.ide.property;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Tests for {@link RecipeNameProperty}.
 */
class RecipeNamePropertyTest extends Assertions {

  private static final List<String> RECIPE_NAMES = List.of("FORMAT_JAVA_CODE", "REMOVE_BLANK_LINES");

  private RecipeNameProperty newProperty() {

    return new RecipeNameProperty("RECIPE", true, "recipe_name", () -> RECIPE_NAMES, null);
  }

  /**
   * Tests that {@link RecipeNameProperty#parse(String, IdeContext)} returns the value unchanged (no
   * case normalization) so the lookup in {@code RecipeManager} can use the original casing.
   */
  @Test
  void testParse() {
    IdeContext context = new IdeTestContext();

    assertThat(newProperty().parse("format_java_code", context)).isEqualTo("format_java_code");
  }

  /**
   * Tests that the completion candidates keep the original casing of the recipe names, which are
   * case-sensitive keys in the {@code RecipeManager}.
   */
  @Test
  void testCompleteValueKeepsOriginalCasing() {
    IdeContext context = new IdeTestContext();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);

    newProperty().completeValue("", context, new ContextCommandlet(), collector);

    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text)).containsExactlyElementsOf(RECIPE_NAMES);
  }

  /**
   * Tests that the completion is case-insensitive with respect to the given argument.
   */
  @Test
  void testCompleteValueIsCaseInsensitive() {
    IdeContext context = new IdeTestContext();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);

    newProperty().completeValue("format", context, new ContextCommandlet(), collector);

    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text)).containsExactly("FORMAT_JAVA_CODE");
  }

  /**
   * Tests that no candidate is suggested for an argument that does not match any recipe name.
   */
  @Test
  void testCompleteValueNoMatch() {
    IdeContext context = new IdeTestContext();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);

    newProperty().completeValue("zz", context, new ContextCommandlet(), collector);

    assertThat(collector.getCandidates()).isEmpty();
  }

  /**
   * Tests that a {@code null} supplier or a {@code null} list of names yields no candidates.
   */
  @Test
  void testCompleteValueNoCandidates() {
    IdeContext context = new IdeTestContext();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);

    RecipeNameProperty property = new RecipeNameProperty("RECIPE", true, "recipe_name", () -> null, null);
    property.completeValue("", context, new ContextCommandlet(), collector);

    assertThat(collector.getCandidates()).isEmpty();
  }
}
