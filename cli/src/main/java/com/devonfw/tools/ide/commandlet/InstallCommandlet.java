package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.property.VersionProperty;
import com.devonfw.tools.ide.tool.IdeasyCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
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
    this.tool = add(new ToolProperty("", false, "tool"));
    this.version = add(new VersionProperty("", false, "version"));
  }

  @Override
  public String getName() {

    return "install";
  }

  @Override
  public boolean isIdeRootRequired() {

    return this.tool.getValueCount() > 0;
  }

  @Override
  public void run() {

    if (this.tool.getValueCount() == 0) {
      if (!this.context.isForceMode()) {
        this.context.askToContinue("Sub-command install without any further arguments will perform the initial installation of IDEasy.\n"
            + "Since this is typically not to be called manually, you may have forgotten to specify the tool to install as extra argument.\n"
            + "The current command will install IDEasy on your computer. Are you sure?");
      }
      IdeasyCommandlet ideasy = new IdeasyCommandlet(this.context);
      Path cwd = Path.of("").toAbsolutePath();
      ideasy.installIdeasy(cwd);
      return;
    }
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
