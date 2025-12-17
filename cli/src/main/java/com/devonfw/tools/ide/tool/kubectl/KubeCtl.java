package com.devonfw.tools.ide.tool.kubectl;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.DelegatingToolCommandlet;
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
  public VersionIdentifier getInstalledVersion() {

    if (!isCommandAvailable(this.tool)) {
      return super.getInstalledVersion();
    }

    List<String> outputs = this.context.newProcess().runAndGetOutput(this.tool, "version", "--client");
    String singleLineOutput = String.join("\n", outputs);

    Matcher matcher = KUBECTL_VERSION_PATTERN.matcher(singleLineOutput);
    if (matcher.find()) {
      String version = matcher.group(1);
      this.context.info("Installation found: kubectl v" + version);
      return VersionIdentifier.of(version);
    } else {
      return super.getInstalledVersion();
    }

  }

}
