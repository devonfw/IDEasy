package com.devonfw.tools.ide.tool.kubectl;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.docker.Docker;

/**
 * {@link ToolCommandlet} for <a href="https://kubernetes.io/">kubernetes</a>.
 */
public class KubeCtl extends GlobalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public KubeCtl(IdeContext context) {

    super(context, "kubectl", Set.of(Tag.DOCKER));
  }

  @Override
  public boolean install() {
    // TODO create kubectl/kubectl/dependencies.json file in ide-urls and delete this method
    getCommandlet(Docker.class).install();
    return super.install();
  }
}
