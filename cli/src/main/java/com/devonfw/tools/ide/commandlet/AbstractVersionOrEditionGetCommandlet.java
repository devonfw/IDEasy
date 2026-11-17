package com.devonfw.tools.ide.commandlet;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.AbstractToolCommandlet;

/**
 * An internal {@link AbstractCommandlet} to get the installed version for a tool.
 *
 * @see AbstractToolCommandlet#getInstalledVersion()
 */
public abstract class AbstractVersionOrEditionGetCommandlet extends AbstractCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractVersionOrEditionGetCommandlet.class);

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
   * @param commandlet the {@link AbstractToolCommandlet} to get the value from.
   * @return the configured value.
   * @see AbstractToolCommandlet#getConfiguredVersion()
   * @see AbstractToolCommandlet#getConfiguredEdition()
   */
  protected abstract Object getConfiguredValue(AbstractToolCommandlet commandlet);

  /**
   * @param commandlet the {@link AbstractToolCommandlet} to get the value from.
   * @return the installed value or {@code null} if the tool is not installed.
   * @see AbstractToolCommandlet#getInstalledVersion()
   * @see AbstractToolCommandlet#getInstalledEdition()
   */
  protected abstract Object getInstalledValue(AbstractToolCommandlet commandlet);

  @Override
  protected void doRun() {

    AbstractToolCommandlet commandlet = this.tool.getValue();
    IdeLogLevel level = IdeLogLevel.PROCESSABLE;
    Object configuredValue = getConfiguredValue(commandlet);
    Object installedValue = getInstalledValue(commandlet);
    boolean getInstalledValue = this.installed.isTrue();
    boolean getConfiguredValue = this.configured.isTrue();
    if (installedValue == null && getInstalledValue && !getConfiguredValue) {
      throw new CliException("Tool " + commandlet.getName() + " is not installed.", 1);
    }
    if (getInstalledValue == getConfiguredValue) {
      if (getInstalledValue) { // both --configured and --installed
        logToolInfo(commandlet, configuredValue, installedValue);
      } else if (LOG.isDebugEnabled()) {
        logToolInfo(commandlet, configuredValue, installedValue);
      } else {
        if (installedValue == null) {
          level.log(LOG, configuredValue.toString());
        } else {
          level.log(LOG, installedValue.toString());
        }
      }
    } else {
      if (getInstalledValue) {
        if (installedValue == null) {
          logToolInfo(commandlet, configuredValue, null);
        } else {
          level.log(LOG, installedValue.toString());
        }
      } else {
        level.log(LOG, configuredValue.toString());
      }
    }
  }

  private void logToolInfo(AbstractToolCommandlet commandlet, Object configuredValue, Object installedValue) {

    String property = getPropertyToGet();
    String toolName = commandlet.getName();
    IdeLogLevel level = IdeLogLevel.PROCESSABLE;
    if (installedValue == null) {
      level.log(LOG, "No installation of tool {} was found.", toolName);
    } else {
      level.log(LOG, "The installed {} for tool {} is {}", property, toolName, installedValue);
    }
    level.log(LOG, "The configured {} for tool {} is {}", property, toolName, configuredValue);
    if (!Objects.equals(configuredValue, installedValue)) {
      level.log(LOG, "To install the configured {} call the following command:", property);
      level.log(LOG, "ide install {}", toolName);
    }
  }

}
