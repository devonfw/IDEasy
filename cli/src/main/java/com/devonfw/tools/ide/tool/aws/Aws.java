package com.devonfw.tools.ide.tool.aws;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;

/**
 * {@link LocalToolCommandlet} for <a href="https://docs.aws.amazon.com/cli/">AWS CLI</a> (Amazon Web Services Command Line Interface).
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
    Path awsConfigDir = this.context.getConfPath().resolve("aws");
    this.context.getFileAccess().mkdirs(awsConfigDir);
  }

  @Override
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
  public void printHelp(NlsBundle bundle) {

    this.context.info("To get detailed help about the usage of the AWS CLI, use \"aws help\"");
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean extraInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, extraInstallation);
    Path awsConfigDir = this.context.getConfPath().resolve("aws");
    environmentContext.withEnvVar("AWS_CONFIG_FILE", awsConfigDir.resolve("config").toString());
    environmentContext.withEnvVar("AWS_SHARED_CREDENTIALS_FILE", awsConfigDir.resolve("credentials").toString());
  }

}
