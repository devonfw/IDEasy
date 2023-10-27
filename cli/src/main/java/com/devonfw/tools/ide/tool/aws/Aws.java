package com.devonfw.tools.ide.tool.aws;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;

import static com.devonfw.tools.ide.os.OperatingSystem.LINUX;

/**
 * {@link ToolCommandlet} for AWS CLI (aws).
 */

public class Aws extends ToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Aws(IdeContext context) {

    super(context, "aws", Set.of(TAG_CLOUD));
  }

  @Override
  public void postInstall() {

    super.postInstall();

    if (this.context.getSystemInfo().getOs() == LINUX) {
      ProcessContext pc = this.context.newProcess();
      Path installToolPath = this.getToolPath();
      Path linuxInstallScript = installToolPath.resolve("install");
      pc.executable(linuxInstallScript);
      pc.addArgs("-i", installToolPath.toString(), "-b", installToolPath.toString());
      pc.run();

      // I (MattesMrzik) used WSL to test this and my aws binary was not marked executable by default.
      pc.executable("chmod");
      Path realPathOfBinary = null;
      try {
        realPathOfBinary = installToolPath.resolve("aws").toRealPath();
      } catch (IOException e) {
        throw new CliException(
            "Failed to get real path of " + installToolPath.resolve("aws") + " for making it executable.");
      }
      pc.addArgs("+x", realPathOfBinary.toString());
      pc.run();

      this.context.getFileAccess().delete(linuxInstallScript);
      this.context.getFileAccess().delete(installToolPath.resolve("dist"));
    }

    EnvironmentVariables variables = this.context.getVariables();
    EnvironmentVariables typeVariables = variables.getByType(EnvironmentVariablesType.CONF);

    Path awsConfigDir = this.context.getConfPath().resolve("aws");
    this.context.getFileAccess().mkdirs(awsConfigDir);
    Path awsConfigFile = awsConfigDir.resolve("config");
    Path awsCredentialsFile = awsConfigDir.resolve("credentials");
    typeVariables.set("AWS_CONFIG_FILE", awsConfigFile.toString(), true);
    typeVariables.set("AWS_SHARED_CREDENTIALS_FILE", awsCredentialsFile.toString(), true);
    typeVariables.save();
  }
}
