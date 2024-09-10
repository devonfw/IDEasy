package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.property.VersionProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.validation.ValidationState;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link Commandlet} to install a tool.
 *
 * @see ToolCommandlet#install()
 */
public class InstallCommandlet extends Commandlet {

  /** The tool to install. */
  public final ToolProperty tool;

  /** The optional version to set and install. */
  public final VersionProperty version;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public InstallCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.tool = add(new ToolProperty("", true, "tool"));
    this.version = add(new VersionProperty("", false, "version",
        (v, state) -> {
          if (!v.isValid()) {
            state.addErrorMessage("Given version " + v + " is not a valid version");
          }
        }));
  }

  @Override
  public String getName() {

    return "install";
  }

  @Override
  public void run() {

    ToolCommandlet commandlet = this.tool.getValue();
    VersionIdentifier versionIdentifier = this.version.getValue();
    if (versionIdentifier != null) {
      commandlet.setVersion(versionIdentifier, false);
    }
    commandlet.install(false);
  }

  @Override
  public ToolCommandlet getToolForCompletion() {

    return this.tool.getValue();
  }
}
