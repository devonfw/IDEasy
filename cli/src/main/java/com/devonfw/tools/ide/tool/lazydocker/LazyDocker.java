package com.devonfw.tools.ide.tool.lazydocker;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.docker.Docker;

import java.util.Set;

public class LazyDocker extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public LazyDocker(IdeContext context) {

    super(context, "lazydocker", Set.of(Tag.DOCKER));
  }

  @Override
  public boolean install(boolean silent) {

    getCommandlet(Docker.class).install();
    return super.doInstall(silent);
  }
}
