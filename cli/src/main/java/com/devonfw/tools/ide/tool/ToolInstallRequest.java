package com.devonfw.tools.ide.tool;

import java.nio.file.Path;

import com.devonfw.tools.ide.log.IdeLogger;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * Container for data related to a tool installation.
 */
public final class ToolInstallRequest {

  private final ToolInstallRequest parent;

  private final boolean silent;

  private final boolean direct;

  private boolean cveCheckDone;

  private ToolEditionAndVersion requested;

  private ToolEditionAndVersion installed;

  private ProcessContext processContext;

  private Path toolPath;

  private boolean extraInstallation;

  private Step step;

  /**
   * The constructor.
   *
   * @param silent the {@link #isSilent() silent} flag.
   */
  public ToolInstallRequest(boolean silent) {
    this(null, silent, false);
  }

  /**
   * The constructor.
   *
   * @param parent the parent {@link ToolInstallRequest} (in case of a dependency).
   */
  public ToolInstallRequest(ToolInstallRequest parent) {
    this(parent, parent.silent, false);
  }

  /**
   * The constructor.
   *
   * @param silent the {@link #isSilent() silent} flag.
   * @param direct the {@link #isDirect() direct} flag.
   */
  private ToolInstallRequest(ToolInstallRequest parent, boolean silent, boolean direct) {
    super();
    this.parent = parent;
    this.silent = silent;
    this.direct = direct;
    if (parent != null) {
      this.processContext = parent.processContext;
    }
  }

  /**
   * @param logger the {@link IdeLogger} used to log an installation loop if found.
   * @return {@code true} if an installation loop was found and logged, {@code false} otherwise.
   */
  public boolean isInstallLoop(IdeLogger logger) {

    if ((this.requested == null) || (this.requested.getEdition() == null)) {
      throw new IllegalStateException(); // this method was called too early
    }
    StringBuilder sb = new StringBuilder();
    boolean loopFound = detectInstallLoopRecursively(this.requested, sb);
    if (loopFound) {
      logger.warning("Found installation loop:\n"
              + "{}\n"
              + "This typically indicates an internal bug in IDEasy.\n"
              + "Please report this bug, when you see this and include this entire warning message.\n"
              + "We are now trying to prevent an infinity loop and abort the recursive installation.",
          sb);
    }
    return loopFound;
  }

  private boolean detectInstallLoopRecursively(ToolEditionAndVersion toolEditionAndVersion, StringBuilder sb) {

    if (this.requested != toolEditionAndVersion) {
      if (this.requested.getEdition().equals(toolEditionAndVersion.getEdition())) {
        if (this.requested.getResolvedVersion().equals(toolEditionAndVersion.getResolvedVersion())) {
          sb.append(this.requested);
          return true;
        }
      }
    }
    if (this.parent == null) {
      return false;
    }
    boolean loopFound = this.parent.detectInstallLoopRecursively(toolEditionAndVersion, sb);
    if (loopFound && (sb != null)) {
      sb.append("-->");
      sb.append(this.requested);
    }
    return loopFound;
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
   * @return {@code true} if CVEs have already been checked, {@code false} otherwise.
   */
  public boolean isCveCheckDone() {

    return this.cveCheckDone;
  }

  void setCveCheckDone() {

    assert !this.cveCheckDone;
    this.cveCheckDone = true;
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
   * @return the {@link Path} inside the project where the tool installation should be linked to.
   */
  public Path getToolPath() {

    return this.toolPath;
  }

  /**
   * @param toolPath new value of {@link #getToolPath()}.
   */
  public void setToolPath(Path toolPath) {

    if (this.toolPath != null) {
      throw new IllegalStateException();
    }
    this.toolPath = toolPath;
  }

  /**
   * Called to trigger an extra installation with a custom tool path.
   *
   * @param toolPath new value of {@link #getToolPath()}.
   */
  public void setToolPathForExtraInstallation(Path toolPath) {

    setToolPath(toolPath);
    this.extraInstallation = true;
  }

  /**
   * @return {@code true} if this is an extra installation, {@code false} otherwise (standard installation).
   */
  public boolean isExtraInstallation() {

    return this.extraInstallation;
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
   * @return {@code true} if the {@link #getRequested() requested edition and version} matches the {@link #getInstalled() installed edition and version}
   */
  public boolean isAlreadyInstalled() {

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
    return installedVersion.equals(this.requested.getResolvedVersion());
  }

  /**
   * @return a new {@link #isDirect() direct} {@link ToolInstallRequest}.
   */
  public static ToolInstallRequest ofDirect() {

    return new ToolInstallRequest(null, false, true);
  }
}
