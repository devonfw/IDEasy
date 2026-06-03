package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.openrewrite.RecipeManager;
import com.devonfw.tools.ide.tool.openrewrite.RecipeWrapper;
import com.devonfw.tools.ide.tool.openrewrite.RefactorRecipeEnum;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Scanner;

/**
 * {@link ToolCommandlet} for <a href="https://docs.openrewrite.org/">Refactor</a>.
 */
public class RefactorCommandlet extends Commandlet {

  public final EnumProperty<RefactorRecipeEnum> command;
  public final StringProperty arguments;
  private RecipeManager recipeManager;
  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public RefactorCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.command = add(new EnumProperty<>("", true, "recipe_name", RefactorRecipeEnum.class));
    this.arguments = new StringProperty("", false, true, "recipe-extra-arguments");
    recipeManager = new RecipeManager();
    add(this.arguments);
    //this.recipe = context.
  }

  @Override
  public String getName() {
    //this indicates the command name
    return "refactor";
  }

  private String[] adaptMVNCommand(String recipeRawCommands) {
    if(recipeRawCommands.startsWith("mvn")) {
      return recipeRawCommands.replaceFirst("\\Qmvn\\E", "").split("\\s+");
    } else {
      return recipeRawCommands.split("\\s+");
    }
  }

  private String changeToDryRunCommand(String recipeRawCommands) {
    return recipeRawCommands.replaceAll(":run\\b", ":dryrun");
  }

  private void showInfo(RecipeWrapper wrapper) {
    context.info("Recipe [{}], {} ", wrapper.ideasy_command.name(), wrapper.description);
    context.info("Reference {}", wrapper.url);
    context.info("Raw command: {}", wrapper.raw_cmd);
  }

  private boolean confirmApplyChange() {
    context.info("***Before making actual changes to the code, please confirm it seriously. It is strongly recommended to perform a DRY-RUN first***");
    context.info("Type yes to apply changes, or press other keys to perform DRY-RUN: ");

    Scanner scanner = new Scanner(System.in);
    String input = scanner.nextLine();

    return (input.equalsIgnoreCase("yes"));
  }

  @Override
  public void run() {

    context.info("{} called", getClass().getSimpleName());

    RefactorRecipeEnum command = this.command.getValue();
    String option = this.arguments.getValue();

    if(!recipeManager.isValidRecipeEnum(command)) {
      context.error("INVALID recipe name: {}", command);
      return;
    }

    RecipeWrapper wrapper = recipeManager.getRecipeWrapper(command);

    showInfo(wrapper);

    String commandLine = wrapper.raw_cmd;

    if(!confirmApplyChange()) {
      commandLine = changeToDryRunCommand(commandLine);
    }

    context.info("Actual command line: {}", commandLine);

    getCommandlet(Mvn.class).runTool(adaptMVNCommand(commandLine));

  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }
}
