package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Prints the IDEasy version and exits
 */
public class VersionCommandlet extends Commandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public VersionCommandlet(IdeContext context) {

    super(context);
    addKeyword(new FlagProperty(getName(), true, "-v"));
  }

  @Override
  public String getName() {

    return "--version";
  }

  @Override
  public void run() {

    this.context.info(IdeVersion.get());
  }
}
