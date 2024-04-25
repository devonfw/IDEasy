package com.devonfw.tools.ide.tool.graalvm;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.nio.file.Path;
import java.util.Set;

/**
 * {@link LocalToolCommandlet} for <a href="https://www.graalvm.org/">GraalVM</a>, an advanced JDK with ahead-of-time Native Image compilation.
 */
public class GraalVm extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public GraalVm(IdeContext context) {

    super(context, "graalvm", Set.of(Tag.JAVA, Tag.RUNTIME));
  }

  @Override
  public Path getToolPath() {

    return this.context.getSoftwareExtraPath().resolve(getName());
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    return super.getInstalledVersion(getToolPath());
  }

  @Override
  protected String getBinaryName() {

    return getToolPath().resolve(IdeContext.FOLDER_BIN).resolve("java").toString();
  }

  @Override
  public void postInstall() {

    EnvironmentVariables envVars = this.context.getVariables().getByType(EnvironmentVariablesType.CONF);
    envVars.set("GRAALVM_HOME", getToolPath().toString(), true);
    envVars.save();
    super.postInstall();
  }
}