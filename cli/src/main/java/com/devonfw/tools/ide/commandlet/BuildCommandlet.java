package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

/**
 * Build tool {@link Commandlet} for automatically detecting build configuration files and running the respective tool.
 */
public class BuildCommandlet extends Commandlet {

  /** The explicit build options to use (if empty use defaults). */
  public final StringProperty arguments;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public BuildCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.arguments = add(new StringProperty("", false, true, "args"));
  }

  @Override
  public String getName() {

    return "build";
  }

  @Override
  protected void doRun() {

    Path buildPath = this.context.getCwd();

    if (buildPath == null) {
      throw new CliException("Missing current working directory!");
    }

    LocalToolCommandlet commandlet = this.context.getCommandletManager().findBuildTool(buildPath);
    if (commandlet == null) {
      throw new CliException("Could not find a build descriptor in " + buildPath + " - no supported build tool detected.");
    }
    List<String> args = this.arguments.asList();
    if (args.isEmpty()) {
      String variableName = commandlet.getName().toUpperCase(Locale.ROOT) + "_BUILD_OPTS";
      args = getDefaultToolOptions(variableName);
    }
    commandlet.runTool(args);
  }

  private List<String> getDefaultToolOptions(String buildOptionName) {

    String[] defaultToolOptions = this.context.getVariables().get(buildOptionName).split(" ");
    return List.of(defaultToolOptions);
  }
}
