package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.property.FlagProperty;
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

  public final FlagProperty conf;

  public final FlagProperty home;

  public final FlagProperty workspace;

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
    this.conf = add(new FlagProperty("--conf", false, null));
    this.home = add(new FlagProperty("--home", false, null));
    this.workspace = add(new FlagProperty("--workspace", false, null));
  }

  @Override
  public String getName() {

    return "set-version";
  }

  @Override
  public void run() {

    ToolCommandlet commandlet = this.tool.getValue();
    VersionIdentifier versionIdentifier = this.version.getValue();
    if (this.conf.isTrue()) {
      commandlet.setVersion(versionIdentifier, true, EnvironmentVariablesType.CONF);
    } else if (this.home.isTrue()) {
      commandlet.setVersion(versionIdentifier, true, EnvironmentVariablesType.CONF);
    } else if (this.workspace.isTrue()) {
      commandlet.setVersion(versionIdentifier, true, EnvironmentVariablesType.WORKSPACE);
    } else {
      commandlet.setVersion(versionIdentifier, true);
    }
  }

  @Override
  public ToolCommandlet getToolForVersionCompletion() {

    return this.tool.getValue();
  }

}
