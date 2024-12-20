package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * An internal {@link Commandlet} to get the installed edition for a tool.
 *
 * @see ToolCommandlet#getInstalledEdition()
 */
public class EditionGetCommandlet extends AbstractVersionOrEditionGetCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public EditionGetCommandlet(IdeContext context) {

    super(context);
  }

  @Override
  public String getName() {

    return "get-edition";
  }

  @Override
  protected String getPropertyToGet() {

    return "edition";
  }

  @Override
  protected Object getConfiguredValue(ToolCommandlet commandlet) {

    return commandlet.getConfiguredEdition();
  }

  @Override
  protected Object getInstalledValue(ToolCommandlet commandlet) {

    return commandlet.getInstalledEdition();
  }
}
