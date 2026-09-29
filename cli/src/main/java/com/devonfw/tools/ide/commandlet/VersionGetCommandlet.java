package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractToolCommandlet;

/**
 * An internal {@link AbstractCommandlet} to get the installed version for a tool.
 *
 * @see AbstractToolCommandlet#getInstalledVersion()
 */
public class VersionGetCommandlet extends AbstractVersionOrEditionGetCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public VersionGetCommandlet(IdeContext context) {

    super(context);
  }

  @Override
  public String getName() {

    return "get-version";
  }

  @Override
  protected String getPropertyToGet() {

    return "version";
  }

  @Override
  protected Object getConfiguredValue(AbstractToolCommandlet commandlet) {

    return commandlet.getConfiguredVersion();
  }

  @Override
  protected Object getInstalledValue(AbstractToolCommandlet commandlet) {

    return commandlet.getInstalledVersion();
  }
}
