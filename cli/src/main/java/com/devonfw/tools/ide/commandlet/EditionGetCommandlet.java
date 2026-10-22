package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractToolCommandlet;

/**
 * An internal {@link AbstractCommandlet} to get the installed edition for a tool.
 *
 * @see AbstractToolCommandlet#getInstalledEdition()
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
  protected Object getConfiguredValue(AbstractToolCommandlet commandlet) {

    return commandlet.getConfiguredEdition();
  }

  @Override
  protected Object getInstalledValue(AbstractToolCommandlet commandlet) {

    return commandlet.getInstalledEdition();
  }
}
