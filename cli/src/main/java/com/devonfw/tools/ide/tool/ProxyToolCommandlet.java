package com.devonfw.tools.ide.tool;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;

public abstract class ProxyToolCommandlet extends ToolCommandlet {

  public ProxyToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  @Override
  public final boolean install(boolean silent, EnvironmentContext environmentContext) {
    return callInstaller(silent, environmentContext);
  }

  protected boolean callInstaller(boolean silent, EnvironmentContext environmentContext) {
    return false;
  }

}
