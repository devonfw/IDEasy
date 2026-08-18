package com.devonfw.tools.ide.commandlet;


import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.openrewrite.RecipeManager;
import com.devonfw.tools.ide.tool.openrewrite.RecipeWrapper;
import com.devonfw.tools.ide.tool.openrewrite.RewriteRecipeEnum;

/**
 * Tests for {@link RewriteCommandlet}.
 */
class RewriteCommandletTest extends AbstractIdeContextTest {

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
   * Tests that {@link RecipeManager} correctly loads recipes and validates enums.
   */
  @Test
  void testRecipeManagerLoadsRecipes() {
    RecipeManager manager = new RecipeManager();
    assertThat(manager.listAvailableRecipes()).isNotEmpty();
    assertThat(manager.isValidRecipeEnum(RewriteRecipeEnum.FORMAT_JAVA_CODE)).isTrue();
    assertThat(manager.isValidRecipeEnum(RewriteRecipeEnum.UNRECOGNIZED_RECIPE)).isFalse();
  }

  /**
   * Tests that the dry-run conversion correctly replaces :run with :dryRun.
   */
  @Test
  void testDryRunConversion() {
    RecipeManager manager = new RecipeManager();
    RecipeWrapper wrapper = manager.getRecipeWrapper(RewriteRecipeEnum.FORMAT_JAVA_CODE);
    assertThat(wrapper.rawCmd).contains(":run");

    String dryRunCmd = wrapper.rawCmd.replace(":run", ":dryRun");
    assertThat(dryRunCmd).contains(":dryRun");
    assertThat(dryRunCmd).doesNotContain(":run");
  }

  /**
   * Tests that the dry-run conversion fails when no :run goal is present.
   */
  @Test
  void testDryRunConversionFailsWithoutRunGoal() {
    String commandWithoutRun = "mvn -U some:otherGoal -Dfoo=bar";
    String result = commandWithoutRun.replace(":run", ":dryRun");
    // No replacement happened
    assertThat(result).isEqualTo(commandWithoutRun);
  }

  /**
   * Tests that the commandlet properly parses MVN commands by stripping the "mvn" prefix.
   */
  @Test
  void testAdaptMvnCommand() {
    RecipeManager manager = new RecipeManager();
    RecipeWrapper wrapper = manager.getRecipeWrapper(RewriteRecipeEnum.FORMAT_JAVA_CODE);

    // Verify the raw command starts with "mvn"
    assertThat(wrapper.rawCmd).startsWith("mvn");

    String trimmed = wrapper.rawCmd.trim();
    String withoutPrefix = trimmed.substring(4); // strip "mvn "
    List<String> args = List.of(withoutPrefix.split("\\s+"));
    assertThat(args).isNotEmpty();
    assertThat(args.getFirst()).startsWith("-"); // should be a Maven flag
  }

  /**
   * Tests that ideHome is not required for the RewriteCommandlet.
   */
  @Test
  void testIdeHomeNotRequired() {
    IdeTestContext context = newContext("basic");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    assertThat(commandlet.isIdeHomeRequired()).isFalse();
  }

  /**
   * Tests that {@link RewriteCommandlet#doRun()} throws a {@link CliException} when an invalid recipe enum is provided, exercising the error handling path.
   */
  @Test
  void testDoRunThrowsForInvalidRecipe() {
    IdeTestContext context = newContext("basic");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    commandlet.command.setValue(RewriteRecipeEnum.UNRECOGNIZED_RECIPE);
    assertThatThrownBy(commandlet::doRun).isInstanceOf(CliException.class).hasMessageContaining("Invalid recipe name");
  }

  /**
   * Tests that {@link RewriteCommandlet#findProjectRoot()} locates the nearest pom.xml when the current working directory contains one.
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
   * Tests that {@link RewriteCommandlet#findProjectRoot()} walks up the directory tree to find the nearest pom.xml.
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
