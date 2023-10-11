package com.devonfw.tools.ide.tool.helm;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.nio.file.Path;
import java.util.Set;

/**
 * {@link ToolCommandlet} for <a href="https://helm.sh/">Helm</a>, the package manager for Kubernetes.
 */
public class Helm extends ToolCommandlet {
  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Helm(IdeContext context) {

    super(context, "helm", Set.of(TAG_CLOUD));
  }

  @Override
  public void runTool(VersionIdentifier toolVersion, String... args) {
    Path path = Path.of("C:\\Users\\saboucha\\Projects\\TestPrj\\testScript");
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.WARNING).executable(path)
        .addArgs(args);
    pc.run();
  }

}
