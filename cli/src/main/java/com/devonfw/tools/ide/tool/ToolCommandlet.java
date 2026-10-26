package com.devonfw.tools.ide.tool;

import java.nio.file.Path;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.common.Tags;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link Commandlet} for a tool managed by IDEasy.
 */
public interface ToolCommandlet extends Commandlet, Tags {

  /**
   * @return the {@link ToolRepository}.
   */
  default ToolRepository getToolRepository() {

    return getContext().getDefaultToolRepository();
  }

  /**
   * @return the {@link EnvironmentVariables#getToolVersion(String) tool version}.
   */
  default VersionIdentifier getConfiguredVersion() {

    return getContext().getVariables().getToolVersion(getName());
  }

  /**
   * @return the {@link EnvironmentVariables#getToolEdition(String) tool edition}.
   */
  default String getConfiguredEdition() {

    return getContext().getVariables().getToolEdition(getName());
  }

  /**
   * @return the {@link Path} where the tool is located (installed). Will be {@code null} for global tools that do not know the {@link Path} since it is
   *     determined by the installer.
   */
  default Path getToolPath() {
    return null;
  }

  /**
   * @return the tool's specific help command. Usually help, --help or -h. Return null if not applicable.
   */
  default String getToolHelpArguments() {

    return null;
  }

  /**
   * @return the installed edition and version of this tool or {@code null} if not installed.
   */
  EditionAndVersion getInstalledEditionAndVersion();

  /**
   * @return the installed version of this tool or {@code null} if not installed.
   */
  default VersionIdentifier getInstalledVersion() {

    EditionAndVersion editionAndVersion = getInstalledEditionAndVersion();
    return (editionAndVersion != null) ? editionAndVersion.version() : null;
  }

  /**
   * @return the installed edition of this tool or {@code null} if not installed.
   */
  default String getInstalledEdition() {

    EditionAndVersion editionAndVersion = getInstalledEditionAndVersion();
    return (editionAndVersion != null) ? editionAndVersion.edition() : null;
  }

  /**
   * @return {@code true} if this tool is installed, {@code false} otherwise.
   */
  default boolean isInstalled() {

    return getInstalledVersion() != null;
  }

  /**
   * @return the execution directory where the tool will be executed. Will be {@code null} by default leading to execution in the users current working
   *     directory where IDEasy was called.
   */
  Path getExecutionDirectory();

  /**
   * @param executionDirectory the new value of {@link #getExecutionDirectory()}.
   */
  void setExecutionDirectory(Path executionDirectory);

  /**
   * Uninstalls the managed {@link #getName() tool}.
   */
  void uninstall();

}
