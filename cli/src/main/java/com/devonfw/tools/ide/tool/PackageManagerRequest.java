package com.devonfw.tools.ide.tool;

import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Container for the data related to a package manager request.
 */
public final class PackageManagerRequest {

  /** {@link #getType() Type} of request to install a tool. */
  public static final String TYPE_INSTALL = "install";

  /** {@link #getType() Type} of request to uninstall a tool. */
  public static final String TYPE_UNINSTALL = "uninstall";

  private final String type;

  private final String tool;

  private final List<String> args;

  private VersionIdentifier version;

  private ToolCommandlet packageManager;

  private ProcessContext processContext;

  private ProcessMode processMode;

  /**
   * The constructor.
   *
   * @param type the {@link #getType() type}.
   * @param tool the {@link #getTool() tool}.
   */
  public PackageManagerRequest(String type, String tool) {
    super();
    this.type = type;
    this.tool = tool;
    this.args = new ArrayList<>();
  }

  /**
   * @return the type of this request (the sub-command of the package manager).
   * @see #TYPE_INSTALL
   * @see #TYPE_UNINSTALL
   */
  public String getType() {

    return this.type;
  }

  /**
   * @return the CLI args used to {@link ToolCommandlet#runTool(List) run} the {@link #getPackageManager() package manager}. E.g.
   *     <code>List.of("install", "-gf", "@angular/cli")</code>.
   */
  public List<String> getArgs() {

    return this.args;
  }

  /**
   * @param arg the argument to append to the {@link #getArgs() args}.
   * @return this {@link PackageManagerRequest} for fluent API calls.
   */
  public PackageManagerRequest addArg(String arg) {
    this.args.add(arg);
    return this;
  }

  /**
   * @return the tool to manage (e.g. install) via this request. Will be in the syntax and terminology of the package-manager that can differ from
   *     {@link ToolCommandlet#getName() tool names} in IDEasy.
   * @see PackageManagerBasedLocalToolCommandlet#getPackageName()
   */
  public String getTool() {

    return this.tool;
  }

  /**
   * @return the optional {@link VersionIdentifier} of the {@link #getTool() tool} (e.g. to install exactly this version).
   */
  public VersionIdentifier getVersion() {

    return this.version;
  }

  /**
   * @param version new value of {@link #getVersion()}.
   * @return this {@link PackageManagerRequest} for fluent API calls.
   */
  public PackageManagerRequest setVersion(VersionIdentifier version) {
    if (this.version != null) {
      throw new IllegalStateException();
    }
    this.version = version;
    return this;
  }

  /**
   * @return the {@link ToolCommandlet} acting as package manager.
   */
  public ToolCommandlet getPackageManager() {

    return packageManager;
  }

  /**
   * @param packageManager new value of {@link #getPackageManager()}.
   * @return this {@link PackageManagerRequest} for fluent API calls.
   */
  public PackageManagerRequest setPackageManager(ToolCommandlet packageManager) {

    if (this.packageManager != null) {
      throw new IllegalStateException();
    }
    this.packageManager = packageManager;
    return this;
  }

  /**
   * @return the {@link ProcessContext}.
   */
  public ProcessContext getProcessContext() {

    return this.processContext;
  }

  /**
   * @param processContext new value of {@link #getProcessContext()}.
   * @return this {@link PackageManagerRequest} for fluent API calls.
   */
  public PackageManagerRequest setProcessContext(ProcessContext processContext) {

    if (this.processContext != null) {
      throw new IllegalStateException();
    }
    this.processContext = processContext;
    return this;
  }

  /**
   * @return the {@link ProcessMode} used to invoke the package manager.
   */
  public ProcessMode getProcessMode() {

    return this.processMode;
  }

  /**
   * @param processMode new value of {@link #getProcessMode()}.
   * @return this {@link PackageManagerRequest} for fluent API calls.
   */
  public PackageManagerRequest setProcessMode(ProcessMode processMode) {

    if (this.processMode != null) {
      throw new IllegalStateException();
    }
    this.processMode = processMode;
    return this;
  }

}
