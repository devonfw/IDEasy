package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.gradle.Gradle;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.npm.Npm;

import java.nio.file.Files;
import java.nio.file.Path;

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

    if (buildPath != null) {
      ToolCommandlet commandlet = null;
      if (Files.exists(buildPath.resolve("pom.xml"))) {
        commandlet = this.context.getCommandletManager().getCommandlet(Mvn.class);
      } else if (Files.exists(buildPath.resolve("build.gradle"))) {
        commandlet = this.context.getCommandletManager().getCommandlet(Gradle.class);
      } else if (Files.exists(buildPath.resolve("package.json"))) {
        if (Files.exists(buildPath.resolve("yarn.lock"))) {
          // TODO: add yarn here
        } else {
          commandlet = this.context.getCommandletManager().getCommandlet(Npm.class);
        }
      }

      if (commandlet != null) {
        commandlet.runTool(null, this.arguments.asArray());
      }
    }

  }
}
