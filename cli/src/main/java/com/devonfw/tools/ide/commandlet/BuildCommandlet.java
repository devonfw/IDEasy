package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.gradle.Gradle;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.npm.Npm;
import com.devonfw.tools.ide.tool.yarn.Yarn;

/**
 * Build tool {@link Commandlet} for automatically detecting build configuration files and running the respective tool.
 */
public class BuildCommandlet extends Commandlet {

  private static final List<Class<? extends LocalToolCommandlet>> BUILD_TOOLS = List.of(Mvn.class, Gradle.class, Yarn.class, Npm.class);

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

    LocalToolCommandlet commandlet = findBuildCommandlet(this.context, buildPath);
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

  /**
   * Detects the applicable build tool for the given {@code buildPath} by {@link LocalToolCommandlet#findBuildDescriptor(Path) querying} the available build
   * commandlets (in order of priority) for a matching build descriptor (e.g. {@code pom.xml}, {@code build.gradle} or {@code package.json}).
   *
   * @param context the {@link IdeContext}.
   * @param buildPath the {@link Path} to the directory to build.
   * @return the applicable build {@link LocalToolCommandlet} or {@code null} if no build descriptor was found.
   */
  static LocalToolCommandlet findBuildCommandlet(IdeContext context, Path buildPath) {

    for (Class<? extends LocalToolCommandlet> toolClass : BUILD_TOOLS) {
      LocalToolCommandlet toolCommandlet = context.getCommandletManager().getCommandlet(toolClass);
      if (toolCommandlet.findBuildDescriptor(buildPath) != null) {
        return toolCommandlet;
      }
    }
    return null;
  }

  private List<String> getDefaultToolOptions(String buildOptionName) {

    String[] defaultToolOptions = this.context.getVariables().get(buildOptionName).split(" ");
    return List.of(defaultToolOptions);
  }
}
