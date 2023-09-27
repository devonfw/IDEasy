package com.devonfw.tools.ide.tool.helm;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;

import java.util.Set;

public class Helm extends ToolCommandlet {
  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * method.
   */
  public Helm(IdeContext context) {

    super(context, "helm", Set.of(TAG_CLOUD));
  }

  public boolean install(boolean silent) {

    return super.install(silent);
  }


}
