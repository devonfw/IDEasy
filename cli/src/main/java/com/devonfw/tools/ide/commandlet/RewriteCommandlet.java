package com.devonfw.tools.ide.commandlet;

import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.openrewrite.RecipeManager;
import com.devonfw.tools.ide.tool.openrewrite.RecipeWrapper;
import com.devonfw.tools.ide.tool.openrewrite.RewriteRecipeEnum;

/**
 * {@link ToolCommandlet} for <a href="https://docs.openrewrite.org/">Refactor</a>.
 */
public class RewriteCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(RewriteCommandlet.class);


  public final EnumProperty<RewriteRecipeEnum> command;
  public final StringProperty arguments;
  private RecipeManager recipeManager;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public RewriteCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.command = add(new EnumProperty<>("", true, "recipe_name", RewriteRecipeEnum.class));
    this.arguments = new StringProperty("", false, true, "recipe-extra-arguments");
    recipeManager = new RecipeManager();
    add(this.arguments);
    //this.recipe = context.
  }

  @Override
  public String getName() {
    //this indicates the command name
    return "rewrite";
  }

  private String[] adaptMVNCommand(String recipeRawCommands) {
    if (recipeRawCommands.startsWith("mvn")) {
      return recipeRawCommands.replaceFirst("^mvn\\s+", "").split("\\s+");
    } else {
      return recipeRawCommands.trim().split("\\s+");
    }
  }

  private String changeToDryRunCommand(String recipeRawCommands) {
    return recipeRawCommands.replaceAll(":run\\b", ":dryRun");
  }

  private void showInfo(RecipeWrapper wrapper) {
    LOG.info("Recipe [{}], {} ", wrapper.ideasyCommand.name(), wrapper.description);
    LOG.info("Reference {}", wrapper.url);
    LOG.info("Raw command: {}", wrapper.rawCmd);
  }

  private boolean confirmApplyChange() {
    LOG.info("***Before making actual changes to the code, please confirm it seriously. It is strongly recommended to perform a DRY-RUN first***");
    LOG.info("Type yes to apply changes, or press other keys to perform DRY-RUN: ");

    try (Scanner scanner = new Scanner(System.in)) {
      String input = scanner.nextLine();
      return input.equalsIgnoreCase("yes");
    }
  }

  @Override
  public void doRun() {

    LOG.info("{} called", getClass().getSimpleName());

    RewriteRecipeEnum command = this.command.getValue();
    String option = this.arguments.getValue();

    if (!recipeManager.isValidRecipeEnum(command)) {
      LOG.error("INVALID recipe name: {}", command);
      return;
    }

    RecipeWrapper wrapper = recipeManager.getRecipeWrapper(command);

    showInfo(wrapper);

    String commandLine = wrapper.rawCmd;

    if (!confirmApplyChange()) {
      commandLine = changeToDryRunCommand(commandLine);
    }

    LOG.info("Actual command line: {}", commandLine);

    getCommandlet(Mvn.class).runTool(List.of(adaptMVNCommand(commandLine)));

  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }
}
