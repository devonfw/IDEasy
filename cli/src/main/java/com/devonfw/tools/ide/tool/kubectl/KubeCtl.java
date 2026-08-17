package com.devonfw.tools.ide.tool.kubectl;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.DelegatingToolCommandlet;
import com.devonfw.tools.ide.tool.EditionAndVersion;
import com.devonfw.tools.ide.tool.docker.Docker;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link DelegatingToolCommandlet} for <a href="https://kubernetes.io/de/docs/tasks/tools/install-kubectl/">Kubectl</a>.
 */
public class KubeCtl extends DelegatingToolCommandlet {

  private static final Pattern KUBECTL_VERSION_PATTERN = Pattern.compile("Client Version: \\s*v([\\d.]+)");

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public KubeCtl(IdeContext context) {

    super(context, "kubectl", Set.of(Tag.KUBERNETES), Docker.class);
  }

  @Override
  public EditionAndVersion getInstalledEditionAndVersion() {

    if (!isCommandAvailable(this.tool)) {
      return super.getInstalledEditionAndVersion();
    }

    List<String> outputs = this.context.newProcess().runAndGetOutput(this.tool, "version", "--client");
    String singleLineOutput = String.join("\n", outputs);
    VersionIdentifier version = resolveVersionWithPattern(singleLineOutput, KUBECTL_VERSION_PATTERN);
    return new EditionAndVersion(this.tool, version);
  }

}
