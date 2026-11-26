package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.RecipeNameProperty;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.openrewrite.RecipeManager;
import com.devonfw.tools.ide.tool.openrewrite.RecipeWrapper;
import com.devonfw.tools.ide.validation.PropertyValidator;

/**
 * {@link AbstractCommandlet} for <a href="https://docs.openrewrite.org/">OpenRewrite</a> refactoring.
 */
public class RewriteCommandlet extends AbstractCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(RewriteCommandlet.class);

  /** The relative location of the recipe configuration inside the {@code ide-settings} settings folder. */
  private static final Path RECIPE_CONFIG_PATH = Path.of("openrewrite").resolve("openrewrite.json");

  /** The {@code groupId:artifactId} of the OpenRewrite Maven plugin (the latest version is resolved by Maven). */
  private static final String REWRITE_PLUGIN = "org.openrewrite.maven:rewrite-maven-plugin";

  /** The fully-qualified plugin goal to actually apply the recipes. */
  private static final String GOAL_RUN = REWRITE_PLUGIN + ":run";

  /** The fully-qualified plugin goal to perform a dry run. */
  private static final String GOAL_DRY_RUN = REWRITE_PLUGIN + ":dryRun";

  /** The recipe to run, selected by its id from the {@code ide-settings} recipe configuration. */
  public final RecipeNameProperty command;

  private final RecipeManager recipeManager;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public RewriteCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    // Resolve the settings path lazily: the commandlet is constructed before the context has resolved
    // its settings folder, so we must not capture getSettingsPath() here (it is still null).
    this.recipeManager = new RecipeManager(() -> {
      Path settingsPath = context.getSettingsPath();
      return settingsPath == null ? null : settingsPath.resolve(RECIPE_CONFIG_PATH);
    });
    PropertyValidator<String> validator = (value, state) -> {
      // If the recipe configuration is missing, validation must pass so that doRun() can report the
      // helpful "No OpenRewrite recipes configured" hint. Reporting "Unknown recipe" here would shadow
      // that hint because doRun() is only reached when the properties validate successfully.
      if (this.recipeManager.isLoaded() && !this.recipeManager.hasRecipe(value)) {
        state.addErrorMessage("Unknown recipe: " + value);
      }
    };
    this.command = add(new RecipeNameProperty("", true, "recipe_name", this.recipeManager::getRecipeNames, validator));
  }

  @Override
  public String getName() {

    return "rewrite";
  }

  /**
   * Builds the arguments for the OpenRewrite Maven plugin invocation.
   *
   * @param dryRun {@code true} to run the {@code rewrite:dryRun} goal (no changes applied), {@code false}
   *     to run {@code rewrite:run}.
   * @param wrapper the {@link RecipeWrapper} holding the properties to pass to OpenRewrite.
   * @param projectRoot the path of the Maven project (nearest {@code pom.xml}).
   * @return the arguments to pass to the Maven tool, in invocation order.
   */
  List<String> buildRunArgs(boolean dryRun, RecipeWrapper wrapper, Path projectRoot) {

    List<String> args = new ArrayList<>();
    args.add("-f");
    args.add(projectRoot.toString());
    args.add(dryRun ? GOAL_DRY_RUN : GOAL_RUN);
    Map<String, String> properties = (wrapper.properties == null) ? Map.of() : wrapper.properties;
    for (String key : properties.keySet().stream().sorted(Comparator.naturalOrder()).toList()) {
      args.add("-D" + key + "=" + properties.get(key));
    }
    return args;
  }

  private void showInfo(RecipeWrapper wrapper) {
    LOG.info("Recipe [{}], {}", wrapper.id, wrapper.description);
    LOG.info("Reference {}", wrapper.url);
  }

  private boolean confirmApplyChange() {

    String input = this.context.askForInput(
        "***Before making actual changes to the code, please confirm it seriously."
            + " It is strongly recommended to perform a DRY-RUN first***\n" +
            "Type yes to apply changes, or press other keys to perform DRY-RUN: ");

    return input.equalsIgnoreCase("yes");

  }

  /**
   * Searches up from the current working directory to find the nearest pom.xml.
   *
   * @return the path to the project root containing pom.xml.
   * @throws CliException if no pom.xml is found or cwd is not set.
   */
  Path findProjectRoot() {
    Path dir = this.context.getCwd();
    if (dir == null) {
      throw new CliException("Cannot determine current working directory");
    }
    while (dir != null) {
      if (Files.exists(dir.resolve("pom.xml"))) {
        return dir;
      }
      dir = dir.getParent();
    }
    throw new CliException("No pom.xml found in current directory or any parent directory");
  }

  @Override
  public void doRun() {

    LOG.info("{} called", getClass().getSimpleName());

    // The property validator only skips the recipe check when nothing is loaded, so this is the single
    // place that reports a missing recipe configuration (see the validator in the constructor).
    if (!this.recipeManager.isLoaded()) {
      throw new CliException("No OpenRewrite recipes configured. Expected the recipe configuration at "
          + this.recipeManager.getConfigPath()
          + ". Make sure your ide-settings settings folder is up to date (e.g. run 'ide update').");
    }

    String recipeName = this.command.getValue();
    RecipeWrapper wrapper = this.recipeManager.getRecipe(recipeName);
    if (wrapper == null) {
      throw new CliException("Unknown recipe: " + recipeName);
    }

    Path projectRoot = findProjectRoot();
    LOG.info("Target project: {}", projectRoot);

    showInfo(wrapper);

    boolean dryRun = !confirmApplyChange();

    List<String> args = buildRunArgs(dryRun, wrapper, projectRoot);
    LOG.info("Actual command line: {}", String.join(" ", args));

    getCommandlet(Mvn.class).runTool(args);

  }

  @Override
  public boolean isIdeHomeRequired() {

    return true;
  }
}
