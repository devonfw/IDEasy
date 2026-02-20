package com.devonfw.tools.ide.tool.node;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.PackageManagerRequest;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.npm.Npm;

/**
 * {@link ToolCommandlet} for <a href="https://nodejs.org/">node</a>.
 */
public class Node extends LocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Node.class);

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Node(IdeContext context) {

    super(context, "node", Set.of(Tag.JAVA_SCRIPT, Tag.RUNTIME));
  }

  @Override
  protected void postInstallOnNewInstallation(ToolInstallRequest request) {

    super.postInstallOnNewInstallation(request);
    // this code is slightly dangerous: npm has a dependency to node, while here in node we call npm causing a cyclic dependency
    // the problem is that node package already comes with npm so we already have to configure npm properly here
    // inside npm we have to guarantee that we will not trigger an installation (again) causing an infinity loop
    // we would love to get this clean but node and npm are already flawed forcing us to do such hacks...
    Npm npm = this.context.getCommandletManager().getCommandlet(Npm.class);
    PackageManagerRequest packageManagerRequest = new PackageManagerRequest("config", this.tool).addArg("config").addArg("set").addArg("prefix")
        .addArg(getToolPath().toString());
    ProcessResult result = npm.runPackageManager(packageManagerRequest, true);
    if (result.isSuccessful()) {
      LOG.info(IdeLogLevel.SUCCESS.getSlf4jMarker(), "Setting npm config prefix to: {} was successful", getToolPath());
    }
  }

  @Override
  public void printHelp(NlsBundle bundle) {

    LOG.info("For a list of supported options and arguments, use \"node --help\"");
  }

  @Override
  protected boolean isIgnoreSoftwareRepo() {

    return true;
  }
}
