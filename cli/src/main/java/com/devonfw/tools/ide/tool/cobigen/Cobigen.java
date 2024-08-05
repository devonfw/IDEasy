package com.devonfw.tools.ide.tool.cobigen;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.mvn.Mvn;

import java.util.Set;

/**
 * {@link ToolCommandlet} for cobigen CLI (cobigen).
 */
public class Cobigen extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Cobigen(IdeContext context) {

    super(context, "cobigen", Set.of(Tag.GENERATOR));
  }

  @Override
  public boolean install(EnvironmentContext environmentContext, boolean silent) {

    getCommandlet(Mvn.class).install(environmentContext);
    return super.install(environmentContext, silent);
  }

}
