package com.devonfw.tools.ide.tool.ng;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.npm.Npm;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} for <a href="https://angular.dev/">angular</a>.
 */
public class Ng extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Ng(IdeContext context) {

    super(context, "ng", Set.of(Tag.JAVA_SCRIPT, Tag.TYPE_SCRIPT, Tag.BUILD));
  }

  @Override
  protected boolean isIgnoreSoftwareRepo() {

    return true;
  }

  @Override
  public Path getToolPath() {

    return this.context.getSoftwarePath().resolve("node");
  }

  @Override
  public Path getToolBinPath() {

    return getToolPath();
  }

  private boolean hasNodeBinary(String binary) {

    Path toolPath = getToolPath();
    Path ngPath = toolPath.resolve(binary);
    return Files.exists(ngPath);
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    if (hasNodeBinary("ng")) {
      try {
        List<String> versions = this.context.newProcess()
            .runAndGetOutput("npm", "list", "-g", "--depth 0");
        String parsedVersion = "";
        for (String version : versions) {
          if (version.contains("@angular/cli")) {
            parsedVersion = version.replaceAll(".*@angular/cli@", "");
            break;
          }
        }
        return VersionIdentifier.of(parsedVersion);
      } catch (IllegalStateException e) {
        this.context.debug("An error occurred while parsing the Ng versions in {}", getToolPath(), e);
        return null;
      }
    }
    this.context.debug("Ng is not installed in {}", getToolPath());
    return null;
  }

  @Override
  public String getInstalledEdition() {

    if (hasNodeBinary("ng")) {
      return "ng";
    }
    return null;
  }

  @Override
  public ToolInstallation installTool(GenericVersionRange version, ProcessContext pc, String edition) {

    ToolRepository toolRepository = getToolRepository();
    VersionIdentifier configuredVersion = getConfiguredVersion();
    VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, version, this);
    installToolDependencies(resolvedVersion, edition, pc);
    if (hasNodeBinary("ng")) {
      pc.run("npm", "uninstall", "-g", "@angular/cli");
    }
    String ngPackage = "@angular/cli";
    if (resolvedVersion.isPattern()) {
      this.context.warning("Ng currently does not support version pattern: {}", resolvedVersion);
    } else {
      ngPackage += "@" + resolvedVersion;
    }
    pc.run("npm", "install", "-g", ngPackage);
    return new ToolInstallation(null, null, null, configuredVersion, true);
  }

  private void runNpmInstall(String npmPackage) {

    runNpm("install", "-g", npmPackage);
  }

  private void runNpm(String... args) {

    Npm npm = this.context.getCommandletManager().getCommandlet(Npm.class);
    npm.runTool(args);
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
