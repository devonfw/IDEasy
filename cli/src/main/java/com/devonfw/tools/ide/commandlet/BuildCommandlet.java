package com.devonfw.tools.ide.commandlet;

import static com.devonfw.tools.ide.variable.IdeVariables.GRADLE_BUILD_OPTS;
import static com.devonfw.tools.ide.variable.IdeVariables.MVN_BUILD_OPTS;
import static com.devonfw.tools.ide.variable.IdeVariables.NPM_BUILD_OPTS;

import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.gradle.Gradle;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.npm.Npm;

/**
 * Build tool {@link Commandlet} for automatically detecting build configuration files and running the respective tool.
 */
public class BuildCommandlet extends Commandlet {

  /**
   * The arguments to build with.
   */
  public StringProperty arguments;

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
  public void run() {

    Path buildPath = this.context.getCwd();
    String[] defaultToolOptions = new String[0];

    if (buildPath == null) {
      throw new CliException("Missing current working directory!");
    }

    ToolCommandlet commandlet = null;
    if (Files.exists(buildPath.resolve("pom.xml"))) {
      commandlet = this.context.getCommandletManager().getCommandlet(Mvn.class);
      defaultToolOptions = getDefaultToolOptions(MVN_BUILD_OPTS.getName());
    } else if (Files.exists(buildPath.resolve("build.gradle"))) {
      commandlet = this.context.getCommandletManager().getCommandlet(Gradle.class);
      defaultToolOptions = getDefaultToolOptions(GRADLE_BUILD_OPTS.getName());
    } else if (Files.exists(buildPath.resolve("package.json"))) {
      if (Files.exists(buildPath.resolve("yarn.lock"))) {
        // TODO: add yarn here
      } else {
        commandlet = this.context.getCommandletManager().getCommandlet(Npm.class);

        defaultToolOptions = getDefaultToolOptions(NPM_BUILD_OPTS.getName());
      }
    } else {
      throw new CliException("Could not find build descriptor - no pom.xml, build.gradle, or package.json found!");
    }

    if (this.arguments.asArray().length != 0) {
      defaultToolOptions = this.arguments.asArray();
    }

    if (commandlet != null) {
      commandlet.runTool(defaultToolOptions);
    }

  }

  private String[] getDefaultToolOptions(String buildOptionName) {

    String[] defaultToolOptions;
    defaultToolOptions = this.context.getVariables().get(buildOptionName).split(" ");
    return defaultToolOptions;
  }
}
