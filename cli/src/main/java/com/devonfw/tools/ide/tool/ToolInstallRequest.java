package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * Container for data related to a tool installation.
 */
public final class ToolInstallRequest {

  private final boolean silent;

  private final boolean direct;

  private boolean dependency;

  private ToolEditionAndVersion requested;

  private ToolEditionAndVersion installed;

  private ProcessContext processContext;

  private Step step;

  /**
   * The constructor.
   *
   * @param silent the {@link #isSilent() silent} flag.
   */
  public ToolInstallRequest(boolean silent) {
    this(silent, false);
  }

  /**
   * The constructor.
   *
   * @param parent the parent {@link ToolInstallRequest} (in case of a dependency).
   */
  public ToolInstallRequest(ToolInstallRequest parent) {
    this(parent.silent, false);
    this.processContext = parent.processContext;
  }

  /**
   * The constructor.
   *
   * @param silent the {@link #isSilent() silent} flag.
   * @param direct the {@link #isDirect() direct} flag.
   */
  private ToolInstallRequest(boolean silent, boolean direct) {
    super();
    this.silent = silent;
    this.direct = direct;
  }

  /**
   * @return {@code true} if this installation should be silent and log information like "tool already installed" only on debug level to avoid spam,
   *     {@code false} otherwise. A {@link #isDirect() direct} installation should never be silent.
   */
  public boolean isSilent() {

    return this.silent;
  }

  /**
   * @return {@code true} if the user directly triggered this tool installation (via "ide install tool"), {@code false} otherwise (indirect installation e.g. as
   *     dependency or via "ide create" or "ide update").
   */
  public boolean isDirect() {

    return this.direct;
  }

  /**
   * @return the {@link ToolEditionAndVersion} that is requested to be installed.
   */
  public ToolEditionAndVersion getRequested() {

    return this.requested;
  }

  /**
   * @param requested new value of {@link #getRequested()}.
   */
  public void setRequested(ToolEditionAndVersion requested) {
    if (this.requested != null) {
      throw new IllegalStateException();
    }
    this.requested = requested;
  }

  /**
   * @return the {@link ToolEditionAndVersion} that is currently installed or {@code null} if the tool is not installed yet.
   */
  public ToolEditionAndVersion getInstalled() {

    return this.installed;
  }

  /**
   * @param installed new value of {@link #getInstalled()}.
   */
  public void setInstalled(ToolEditionAndVersion installed) {

    if (this.installed != null) {
      throw new IllegalStateException();
    }
    this.installed = installed;
  }

  /**
   * @return the {@link ProcessContext} to use for executing the tool. Will also be configured during the installation (variables set, PATH extended).
   */
  public ProcessContext getProcessContext() {
    return this.processContext;
  }

  /**
   * @param processContext new value of {@link #getProcessContext()}.
   */
  public void setProcessContext(ProcessContext processContext) {

    if (this.processContext != null) {
      throw new IllegalStateException();
    }
    this.processContext = processContext;
  }

  /**
   * @return the {@link Step} for the installation.
   */
  public Step getStep() {

    return this.step;
  }

  /**
   * @param step new value of {@link #getStep()}.
   */
  public void setStep(Step step) {

    if (this.step != null) {
      throw new IllegalStateException();
    }
    this.step = step;
  }

  /**
   * @return {@code true} if an additional installation is required not linked to the project's software folder (e.g. because of a transitive installation from
   *     a dependency that is incompatible with the project version), {@code false} otherwise.
   */
  public boolean isAdditionalInstallation() {
    if (this.requested != null) {
      GenericVersionRange versionToInstall = this.requested.getVersion();
      return (versionToInstall instanceof VersionRange);
    }
    return false;
  }

  /**
   * @param skipUpdate {@code true} for {@link com.devonfw.tools.ide.context.IdeStartContext#isSkipUpdatesMode() skip update mode}, {@code false}
   *     otherwise.
   * @return {@code true} if the {@link #getRequested() requested edition and version} matches the {@link #getInstalled() installed edition and version}
   */
  public boolean isAlreadyInstalled(boolean skipUpdate) {

    if (this.installed == null) {
      return false;
    }
    VersionIdentifier installedVersion = this.installed.getResolvedVersion();
    if (installedVersion == null) {
      return false;
    }
    ToolEdition installedEdition = this.installed.getEdition();
    if (installedEdition == null) {
      return false; // should actually never happen
    }
    if (!this.requested.getEdition().equals(installedEdition)) {
      return false;
    }
    VersionIdentifier resolvedVersion = this.requested.getResolvedVersion();
    if ((resolvedVersion == null) || skipUpdate) {
      GenericVersionRange requestedVersion = this.requested.getVersion();
      return requestedVersion.contains(installedVersion);
    }
    return resolvedVersion.equals(installedVersion);
  }

  /**
   * @return a new {@link #isDirect() direct} {@link ToolInstallRequest}.
   */
  public static ToolInstallRequest ofDirect() {

    return new ToolInstallRequest(true, true);
  }
}
