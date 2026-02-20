package com.devonfw.tools.ide.commandlet;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * An internal {@link Commandlet} to get the installed version for a tool.
 *
 * @see ToolCommandlet#getInstalledVersion()
 */
public abstract class AbstractVersionOrEditionGetCommandlet extends Commandlet {

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
    Marker marker = IdeLogLevel.PROCESSABLE.getSlf4jMarker();
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
      } else if (this.context.debug().isEnabled()) {
        logToolInfo(commandlet, configuredValue, installedValue);
      } else {
        if (installedValue == null) {
          LOG.info(marker, configuredValue.toString());
        } else {
          LOG.info(marker, installedValue.toString());
        }
      }
    } else {
      if (getInstalledValue) {
        if (installedValue == null) {
          logToolInfo(commandlet, configuredValue, null);
        } else {
          LOG.info(marker, installedValue.toString());
        }
      } else {
        LOG.info(marker, configuredValue.toString());
      }
    }
  }

  private void logToolInfo(ToolCommandlet commandlet, Object configuredValue, Object installedValue) {

    String property = getPropertyToGet();
    String toolName = commandlet.getName();
    Marker marker = IdeLogLevel.PROCESSABLE.getSlf4jMarker();
    if (installedValue == null) {
      LOG.info(marker, "No installation of tool {} was found.", toolName);
    } else {
      LOG.info(marker, "The installed {} for tool {} is {}", property, toolName, installedValue);
    }
    LOG.info(marker, "The configured {} for tool {} is {}", property, toolName, configuredValue);
    if (!Objects.equals(configuredValue, installedValue)) {
      LOG.info(marker, "To install the configured {} call the following command:", property);
      LOG.info(marker, "ide install {}", toolName);
    }
  }

}
