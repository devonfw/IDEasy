package com.devonfw.tools.ide.tool.kubelinter;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;


/**
 * {@link AbstractToolCommandlet} for <a href="https://github.com/stackrox/kube-linter">KubeLinter</a>, a linter for Kubernetes
 * YAML files and Helm charts.
 */
public class KubeLinter extends AbstractLocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public KubeLinter(IdeContext context) {

    super(context, "kube-linter", Set.of(Tag.KUBERNETES, Tag.LINTING));
  }

}
