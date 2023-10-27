package com.devonfw.tools.ide.tool.aws;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.tool.ToolCommandlet;

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
