package com.devonfw.tools.ide.tool.kubectl;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.DelegatingToolCommandlet;
import com.devonfw.tools.ide.tool.docker.Docker;

/**
 * {@link DelegatingToolCommandlet} for <a href="https://kubernetes.io/de/docs/tasks/tools/install-kubectl/">Kubectl</a>.
 */
public class KubeCtl extends DelegatingToolCommandlet {


  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public KubeCtl(IdeContext context) {

    super(context, "kubectl", Set.of(Tag.KUBERNETES), Docker.class);
  }
}
