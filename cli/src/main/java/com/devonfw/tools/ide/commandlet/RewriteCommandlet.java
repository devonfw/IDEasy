package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.openrewrite.RecipeManager;
import com.devonfw.tools.ide.tool.openrewrite.RecipeWrapper;
import com.devonfw.tools.ide.tool.openrewrite.RewriteRecipeEnum;

/**
 * {@link Commandlet} for <a href="https://docs.openrewrite.org/">OpenRewrite</a> refactoring.
 */
public class RewriteCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(RewriteCommandlet.class);

  public final EnumProperty<RewriteRecipeEnum> command;
  private final RecipeManager recipeManager;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public RewriteCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.command = add(new EnumProperty<>("", true, "recipe_name", RewriteRecipeEnum.class));
    this.recipeManager = new RecipeManager();
  }

  @Override
  public String getName() {

    return "rewrite";
  }

  /**
   * Parses the raw MVN command from JSON into individual argument tokens, stripping the leading "mvn" if present.
   *
   * @param recipeRawCommands the raw command string from the recipe configuration.
   * @return list of individual MVN argument tokens.
   */
  private List<String> adaptMVNCommand(String recipeRawCommands) {
    String trimmed = recipeRawCommands.trim();
    if (trimmed.startsWith("mvn ")) {
      trimmed = trimmed.substring(4);
    } else if (trimmed.equals("mvn")) {
      trimmed = "";
    }
    List<String> args = new ArrayList<>();
    for (String token : trimmed.split("\\s+")) {
      if (!token.isEmpty()) {
        args.add(token);
      }
    }
    return args;
  }

  /**
   * Converts a raw command to dry-run mode by replacing the ":run" goal with ":dryRun".
   *
   * @param recipeRawCommands the raw command string.
   * @return the command with dry-run mode applied.
   * @throws CliException if the command does not contain a ":run" goal to replace.
   */
  private String changeToDryRunCommand(String recipeRawCommands) {
    String result = recipeRawCommands.replace(":run", ":dryRun");
    if (result.equals(recipeRawCommands)) {
      throw new CliException("Cannot convert to dry-run: command does not contain ':run' goal: " + recipeRawCommands);
    }
    return result;
  }

  private void showInfo(RecipeWrapper wrapper) {
    LOG.info("Recipe [{}], {}", wrapper.ideasyCommand.name(), wrapper.description);
    LOG.info("Reference {}", wrapper.url);
    LOG.info("Raw command: {}", wrapper.rawCmd);
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

    RewriteRecipeEnum recipeEnum = this.command.getValue();

    if (!recipeManager.isValidRecipeEnum(recipeEnum)) {
      throw new CliException("Invalid recipe name: " + recipeEnum);
    }

    RecipeWrapper wrapper = recipeManager.getRecipeWrapper(recipeEnum);

    Path projectRoot = findProjectRoot();
    LOG.info("Target project: {}", projectRoot);

    showInfo(wrapper);

    String commandLine = wrapper.rawCmd;

    if (!confirmApplyChange()) {
      commandLine = changeToDryRunCommand(commandLine);
    }

    LOG.info("Actual command line: {}", commandLine);

    List<String> args = adaptMVNCommand(commandLine);
    // Inject -f <project-root> so Maven runs in the correct project
    args.add(0, "-f");
    args.add(1, projectRoot.toString());
    getCommandlet(Mvn.class).runTool(args);

  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }
}
