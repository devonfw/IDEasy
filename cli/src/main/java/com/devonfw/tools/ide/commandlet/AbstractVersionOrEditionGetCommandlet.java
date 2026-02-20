package com.devonfw.tools.ide.commandlet;

import java.util.Objects;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * An internal {@link Commandlet} to get the installed version for a tool.
 *
 * @see ToolCommandlet#getInstalledVersion()
 */
public abstract class AbstractVersionOrEditionGetCommandlet extends Commandlet {

  /** The tool to get the version of. */
  public final ToolProperty tool;

  /** Flag to get the configured version. */
  public final FlagProperty configured;

  /** Flag to get the installed version. */
  public final FlagProperty installed;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public AbstractVersionOrEditionGetCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.tool = add(new ToolProperty("", true, "tool"));
    this.configured = add(new FlagProperty("--configured"));
    this.installed = add(new FlagProperty("--installed"));
  }

  @Override
  public boolean isProcessableOutput() {

    return true;
  }

  /**
   * @return the property to get (e.g. "version" or "edition").
   */
  protected abstract String getPropertyToGet();

  /**
   * @param commandlet the {@link ToolCommandlet} to get the value from.
   * @return the configured value.
   * @see ToolCommandlet#getConfiguredVersion()
   * @see ToolCommandlet#getConfiguredEdition()
   */
  protected abstract Object getConfiguredValue(ToolCommandlet commandlet);

  /**
   * @param commandlet the {@link ToolCommandlet} to get the value from.
   * @return the installed value or {@code null} if the tool is not installed.
   * @see ToolCommandlet#getInstalledVersion()
   * @see ToolCommandlet#getInstalledEdition()
   */
  protected abstract Object getInstalledValue(ToolCommandlet commandlet);

  @Override
  protected void doRun() {

    ToolCommandlet commandlet = this.tool.getValue();
    IdeSubLogger logger = this.context.level(IdeLogLevel.PROCESSABLE);
    Object configuredValue = getConfiguredValue(commandlet);
    Object installedValue = getInstalledValue(commandlet);
    boolean getInstalledValue = this.installed.isTrue();
    boolean getConfiguredValue = this.configured.isTrue();
    if (installedValue == null && getInstalledValue && !getConfiguredValue) {
      throw new CliException("Tool " + commandlet.getName() + " is not installed.", 1);
    }
    if (getInstalledValue == getConfiguredValue) {
      if (getInstalledValue) { // both --configured and --installed
        logToolInfo(logger, commandlet, configuredValue, installedValue);
      } else if (this.context.debug().isEnabled()) {
        logToolInfo(logger, commandlet, configuredValue, installedValue);
      } else {
        if (installedValue == null) {
          logger.log(configuredValue.toString());
        } else {
          logger.log(installedValue.toString());
        }
      }
    } else {
      if (getInstalledValue) {
        if (installedValue == null) {
          logToolInfo(logger, commandlet, configuredValue, null);
        } else {
          logger.log(installedValue.toString());
        }
      } else {
        logger.log(configuredValue.toString());
      }
    }
  }

  private void logToolInfo(IdeSubLogger logger, ToolCommandlet commandlet, Object configuredValue, Object installedValue) {

    String property = getPropertyToGet();
    String toolName = commandlet.getName();
    if (installedValue == null) {
      logger.log("No installation of tool {} was found.", toolName);
    } else {
      logger.log("The installed {} for tool {} is {}", property, toolName, installedValue);
    }
    logger.log("The configured {} for tool {} is {}", property, toolName, configuredValue);
    if (!Objects.equals(configuredValue, installedValue)) {
      logger.log("To install the configured {} call the following command:", property);
      logger.log("ide install {}", toolName);
    }
  }

}
