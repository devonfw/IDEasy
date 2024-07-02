package com.devonfw.tools.ide.tool.helm;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

import java.util.Set;

/**
 * {@link ToolCommandlet} for <a href="https://helm.sh/">Helm</a>, the package manager for Kubernetes.
 */
public class Helm extends LocalToolCommandlet {
  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Helm(IdeContext context) {

    super(context, "helm", Set.of(Tag.KUBERNETES));
  }

  @Override
  public String getHelpCommand() {

    return "--help";
  }
}
