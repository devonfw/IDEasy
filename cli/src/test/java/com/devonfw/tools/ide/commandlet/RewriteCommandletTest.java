package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.openrewrite.RecipeWrapper;
import com.devonfw.tools.ide.validation.ValidationResult;

/**
 * Tests for {@link RewriteCommandlet}.
 */
class RewriteCommandletTest extends AbstractIdeContextTest {

  private static final String RECIPE_JSON = """
      [
        {
          "id": "java.format_code",
          "description": "Format Java code.",
          "url": "https://docs.openrewrite.org/recipes/java/format/autoformat",
          "properties": {
            "rewrite.activeRecipes": "org.openrewrite.java.format.AutoFormat",
            "rewrite.exportDatatables": "true"
          }
        },
        {
          "id": "java.blank_lines",
          "description": "Add and/or remove blank lines.",
          "url": "https://docs.openrewrite.org/recipes/java/format/blanklines",
          "properties": {
            "rewrite.activeRecipes": "org.openrewrite.java.format.BlankLines"
          }
        }
      ]
      """;

  /**
   * Writes the recipe configuration into the context's {@code ide-settings} settings folder.
   *
   * @param context the {@link IdeTestContext}.
   */
  private void writeRecipeConfig(IdeTestContext context) {
    Path configPath = context.getSettingsPath().resolve("openrewrite").resolve("openrewrite.json");
    context.getFileAccess().writeFileContent(RECIPE_JSON, configPath, true);
  }

  /**
   * Tests that the RewriteCommandlet is registered and accessible via the commandlet manager.
   */
  @Test
  void testCommandletExists() {
    IdeTestContext context = newContext("basic");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    assertThat(commandlet).isNotNull();
    assertThat(commandlet.getName()).isEqualTo("rewrite");
  }

  /**
   * Tests that the RewriteCommandlet requires an IDEasy project context (settings folder).
   */
  @Test
  void testIdeHomeRequired() {
    IdeTestContext context = newContext("basic");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    assertThat(commandlet.isIdeHomeRequired()).isTrue();
  }

  /**
   * Tests that {@link RewriteCommandlet#doRun()} throws a {@link CliException} when the recipe
   * configuration is absent from the settings folder (settings-only).
   */
  @Test
  void testDoRunThrowsWhenNoRecipeConfig() {
    IdeTestContext context = newContext("basic");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    commandlet.command.setValue("java.format_code");
    assertThatThrownBy(commandlet::doRun).isInstanceOf(CliException.class)
        .hasMessageContaining("No OpenRewrite recipes configured");
  }

  /**
   * Tests that a valid recipe id passes validation and an unknown id is rejected.
   */
  @Test
  void testRecipeNameValidation() {
    IdeTestContext context = newContext("release");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    writeRecipeConfig(context);

    commandlet.command.setValue("java.format_code");
    assertThat(commandlet.command.validate().isValid()).isTrue();

    commandlet.command.setValue("DOES_NOT_EXIST");
    assertThat(commandlet.command.validate().isValid()).isFalse();
    assertThat(commandlet.command.validate().getErrorMessage()).contains("Unknown recipe");
  }

  /**
   * Tests the completion round trip: the auto-completion candidate keeps the recipe id and that same value
   * passes validation. Completing the technology prefix {@code java.format} narrows the candidates to the
   * matching id.
   */
  @Test
  void testCompletionCandidateKeepsIdAndValidates() {
    IdeTestContext context = newContext("release");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    writeRecipeConfig(context);

    // complete the recipe id with the technology prefix
    String[] argsArray = { "rewrite", "java.format" };
    Set<String> alreadyProvided = new HashSet<>(List.of("rewrite"));
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context, alreadyProvided);
    CliArguments args = CliArguments.ofCompletion(argsArray);
    List<CompletionCandidate> candidates = context.complete(args, collector, false);

    // the candidate is the recipe id
    assertThat(candidates.stream().map(CompletionCandidate::text)).containsExactly("java.format_code");

    // and that completed value passes validation (round trip: complete -> validate)
    String completedName = candidates.get(0).text();
    commandlet.command.setValue(completedName);
    assertThat(commandlet.command.validate().isValid()).isTrue();
  }

  /**
   * Tests that when the recipe configuration is absent the validation passes (no "Unknown recipe"
   * error) so that {@link RewriteCommandlet#doRun()} can report the helpful "No OpenRewrite recipes
   * configured" hint instead.
   */
  @Test
  void testValidationPassesWithoutRecipeConfig() {
    IdeTestContext context = newContext("basic");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    commandlet.command.setValue("java.format_code");

    ValidationResult result = commandlet.command.validate();
    assertThat(result.isValid()).isTrue();
    assertThat(result.getErrorMessage()).isNull();
  }

  /**
   * Tests that {@link RewriteCommandlet#doRun()} throws a {@link CliException} for a recipe id that is not
   * present in the settings configuration.
   */
  @Test
  void testDoRunThrowsForUnknownRecipe() {
    IdeTestContext context = newContext("basic");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    writeRecipeConfig(context);
    commandlet.command.setValue("DOES_NOT_EXIST");
    assertThatThrownBy(commandlet::doRun).isInstanceOf(CliException.class).hasMessageContaining("Unknown recipe");
  }

  /**
   * Tests that {@link RewriteCommandlet#buildRunArgs(boolean, RecipeWrapper, Path)} builds the dry-run
   * command: the {@code -f} flag with the project root, the {@code rewrite:dryRun} goal, and one
   * {@code -D} argument per property (sorted by key for a stable command line).
   */
  @Test
  void testBuildRunArgsDryRun() {
    IdeTestContext context = newContext("release");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    Path projectRoot = context.getWorkspacePath();
    RecipeWrapper wrapper = new RecipeWrapper("java.format_code", "Format Java code.",
        "https://docs.openrewrite.org/recipes/java/format/autoformat",
        Map.of("rewrite.exportDatatables", "true", "rewrite.activeRecipes", "org.openrewrite.java.format.AutoFormat"));

    List<String> args = commandlet.buildRunArgs(true, wrapper, projectRoot);

    assertThat(args).containsExactly("-f", projectRoot.toString(), "org.openrewrite.maven:rewrite-maven-plugin:dryRun",
        "-Drewrite.activeRecipes=org.openrewrite.java.format.AutoFormat", "-Drewrite.exportDatatables=true");
  }

  /**
   * Tests that {@link RewriteCommandlet#buildRunArgs(boolean, RecipeWrapper, Path)} builds the run command
   * with the fully-qualified {@code rewrite:run} goal when the user confirmed applying the change.
   */
  @Test
  void testBuildRunArgsRun() {
    IdeTestContext context = newContext("release");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    Path projectRoot = context.getWorkspacePath();
    RecipeWrapper wrapper = new RecipeWrapper("java.format_code", "Format Java code.",
        "https://docs.openrewrite.org/recipes/java/format/autoformat", Map.of("rewrite.activeRecipes",
            "org.openrewrite.java.format.AutoFormat"));

    List<String> args = commandlet.buildRunArgs(false, wrapper, projectRoot);

    assertThat(args).containsExactly("-f", projectRoot.toString(), "org.openrewrite.maven:rewrite-maven-plugin:run",
        "-Drewrite.activeRecipes=org.openrewrite.java.format.AutoFormat");
  }

  /**
   * Tests that {@link RewriteCommandlet#buildRunArgs(boolean, RecipeWrapper, Path)} builds the run command
   * without any {@code -D} argument when the recipe has no properties.
   */
  @Test
  void testBuildRunArgsWithoutProperties() {
    IdeTestContext context = newContext("release");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    Path projectRoot = context.getWorkspacePath();
    RecipeWrapper wrapper = new RecipeWrapper("java.format_code", "Format Java code.",
        "https://docs.openrewrite.org/recipes/java/format/autoformat", Map.of());

    List<String> args = commandlet.buildRunArgs(false, wrapper, projectRoot);

    assertThat(args).containsExactly("-f", projectRoot.toString(), "org.openrewrite.maven:rewrite-maven-plugin:run");
  }

  /**
   * Tests that {@link RewriteCommandlet#findProjectRoot()} locates the nearest pom.xml when the current
   * working directory contains one.
   */
  @Test
  void testFindProjectRootFindsPomInCurrentDirectory() {
    IdeTestContext context = newContext("release");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    // mvn directory contains pom.xml
    Path mvnDir = context.getWorkspacePath().resolve("mvn");
    context.setCwd(mvnDir, "main", context.getIdeHome());
    Path projectRoot = commandlet.findProjectRoot();
    assertThat(projectRoot).isEqualTo(mvnDir);
  }

  /**
   * Tests that {@link RewriteCommandlet#findProjectRoot()} walks up the directory tree to find the nearest
   * pom.xml.
   */
  @Test
  void testFindProjectRootWalksUpDirectoryTree() {
    IdeTestContext context = newContext("release");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    // empty directory does NOT contain pom.xml - should walk up to find it
    Path emptyDir = context.getWorkspacePath().resolve("empty");
    context.setCwd(emptyDir, "main", context.getIdeHome());
    Path projectRoot = commandlet.findProjectRoot();
    // Should find the pom.xml in the parent workspaces/main directory or above
    assertThat(projectRoot.resolve("pom.xml")).exists();
  }

  /**
   * Tests that {@link RewriteCommandlet#findProjectRoot()} throws when cwd is not set.
   */
  @Test
  void testFindProjectRootThrowsWhenNoCwd() {
    IdeTestContext context = newContext("basic");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    context.setCwd(null, "main", context.getIdeHome());
    assertThatThrownBy(commandlet::findProjectRoot).isInstanceOf(CliException.class).hasMessageContaining("current working directory");
  }
}
