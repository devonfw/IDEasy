package com.devonfw.tools.ide.tool.aws;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * {@link LocalToolCommandlet} for <a href="https://docs.aws.amazon.com/cli/">AWS CLI</a> (Amazon Web Services Command
 * Line Interface).
 */
public class Aws extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Aws(IdeContext context) {

    super(context, "aws", Set.of(Tag.CLOUD));
  }

  @Override
  public void postInstall() {

    super.postInstall();
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

  protected void postExtract(Path extractedDir) {

    if (this.context.getSystemInfo().isLinux()) {
      // running the install-script that aws shipped
      ProcessContext pc = this.context.newProcess();
      Path linuxInstallScript = extractedDir.resolve("install");
      pc.executable(linuxInstallScript);
      pc.addArgs("-i", extractedDir.toString(), "-b", extractedDir.toString());
      pc.run();

      // The install-script that aws ships creates symbolic links to binaries but using absolute paths.
      // Since the current process happens in a temporary dir, these links wouldn't be valid after moving the
      // installation files to the target dir. So the absolute paths are replaced by relative ones.
      FileAccess fileAccess = this.context.getFileAccess();
      for (String file : new String[] { "aws", "aws_completer", "v2/current" }) {
        Path link = extractedDir.resolve(file);
        try {
          fileAccess.symlink(link.toRealPath(), link, true);
        } catch (IOException e) {
          throw new RuntimeException(
              "Failed to replace absolute link (" + link + ") provided by AWS install script with relative link.", e);
        }
      }
      fileAccess.delete(linuxInstallScript);
      fileAccess.delete(extractedDir.resolve("dist"));
    }
  }

  @Override
  public void printToolHelp(String helpcommand) {

    this.context.info("To get detailed help about the usage of the AWS CLI, use \"aws help\"");
  }

}
