package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.tool.IdeasyCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;

/**
 * {@link Commandlet} to upgrade the version of IDEasy
 */
public class UpgradeCommandlet extends Commandlet {

  /** Optional {@link UpgradeMode}. */
  public final EnumProperty<UpgradeMode> mode;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UpgradeCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.mode = add(new EnumProperty<>("--mode", false, null, UpgradeMode.class));
  }

  @Override
  public String getName() {

    return "upgrade";
  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }

  @Override
  public void run() {

    IdeasyCommandlet ideasy = new IdeasyCommandlet(this.context, this.mode.getValue());
    ToolInstallation installation = ideasy.install(false);
    if (installation.newInstallation()) {
      this.context.interaction("It is recommended to run 'ide update' on your IDEasy projects now.");
    }
  }

}
