package com.devonfw.tools.ide.tool.kubectl;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.ProxyToolCommandlet;
import com.devonfw.tools.ide.tool.docker.Docker;
import com.devonfw.tools.ide.version.VersionIdentifier;

public class KubeCtl extends ProxyToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public KubeCtl(IdeContext context) {

    super(context, "kubectl", Set.of(Tag.KUBERNETES));
  }

  @Override
  public boolean callInstaller(boolean silent, EnvironmentContext environmentContext) {
    // TODO create kubectl/kubectl/dependencies.json file in ide-urls and delete this method
    return getCommandlet(Docker.class).install(silent, environmentContext);
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    return null;
  }

  @Override
  public String getInstalledEdition() {

    return null;
  }

  @Override
  public void uninstall() {

  }
}
