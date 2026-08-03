package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesFiles;
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

  public final EnumProperty<EnvironmentVariablesFiles> cfg;

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
    this.cfg = add(new EnumProperty("--cfg", false, null, EnvironmentVariablesFiles.class));
  }

  @Override
  public String getName() {

    return "set-version";
  }

  @Override
  protected void doRun() {

    ToolCommandlet commandlet = this.tool.getValue();
    VersionIdentifier versionIdentifier = this.version.getValue();
    EnvironmentVariablesFiles env = this.cfg.getValue();
    commandlet.setVersion(versionIdentifier, true, env);
  }

  @Override
  public ToolCommandlet getToolForCompletion() {

    return this.tool.getValue();
  }

}
