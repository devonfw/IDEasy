package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.property.VersionProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * An internal {@link Commandlet} to set a tool version.
 *
 * @see ToolCommandlet#setVersion(VersionIdentifier, boolean)
 */
public class VersionSetCommandlet extends Commandlet {

  /** The tool to set the version of. */
  public final ToolProperty tool;

  /** The version to set. */
  public final VersionProperty version;

  public final EnumProperty<EnvironmentVariablesType> cfg;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public VersionSetCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.tool = add(new ToolProperty("", true, "tool"));
    this.version = add(new VersionProperty("", true, "version"));
    this.cfg = add(new EnumProperty("--cfg", false, null, EnvironmentVariablesType.class));
  }

  @Override
  public String getName() {

    return "set-version";
  }

  @Override
  public void run() {

    ToolCommandlet commandlet = this.tool.getValue();
    VersionIdentifier versionIdentifier = this.version.getValue();
    EnvironmentVariablesType env = this.cfg.getValue();
    if (env == EnvironmentVariablesType.SETTINGS || env == EnvironmentVariablesType.CONF || env == EnvironmentVariablesType.USER
        || env == EnvironmentVariablesType.WORKSPACE) {
      commandlet.setVersion(versionIdentifier, true, env);
    } else if (env == EnvironmentVariablesType.RESOLVED || env == EnvironmentVariablesType.SYSTEM) {
      context.error("Invalid option for --cfg: " + env);
    } else {
      //use default location
      commandlet.setVersion(versionIdentifier, true);
    }
  }

  @Override
  public ToolCommandlet getToolForVersionCompletion() {

    return this.tool.getValue();
  }

}
