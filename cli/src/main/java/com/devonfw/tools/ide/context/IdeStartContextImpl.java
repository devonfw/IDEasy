package com.devonfw.tools.ide.context;

import java.util.Locale;

import com.devonfw.tools.ide.log.IdeLogArgFormatter;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListener;
import com.devonfw.tools.ide.log.IdeLogListenerBuffer;

/**
 * Implementation of {@link IdeStartContext}.
 */
public class IdeStartContextImpl implements IdeStartContext {

  private static IdeStartContextImpl instance;

  protected final IdeLogListener logListener;

  protected final IdeLogListenerBuffer logListenerBuffer;

  private IdeLogLevel logLevelConsole;

  private IdeLogLevel logLevelLogger;

  private IdeLogArgFormatter argFormatter;

  private boolean skipUpdatesMode;

  private boolean offlineMode;

  private boolean forceMode;

  private boolean forcePull;

  private boolean forcePlugins;

  private boolean forceRepositories;

  private boolean batchMode;

  private boolean quietMode;

  private boolean privacyMode;

  private boolean noColorsMode;

  private boolean writeLogfile;

  private Locale locale;

  /**
   * @param logLevelConsole the minimum enabled {@link #getLogLevelConsole() log level}.
   * @param logListener the {@link #getLogListener() logListener}.
   */
  public IdeStartContextImpl(IdeLogLevel logLevelConsole, IdeLogListener logListener) {

    super();
    this.logLevelConsole = logLevelConsole;
    this.logListener = logListener;
    this.argFormatter = IdeLogArgFormatter.DEFAULT;
    IdeStartContextImpl.instance = this;
    if (logListener instanceof IdeLogListenerBuffer buffer) {
      this.logListenerBuffer = buffer;
    } else {
      this.logListenerBuffer = null;
    }
  }

  @Override
  public IdeLogListener getLogListener() {

    return this.logListener;
  }

  @Override
  public IdeLogLevel getLogLevelConsole() {

    return this.logLevelConsole;
  }

  /**
   * @param logLevelConsole the new {@link IdeLogLevel} for the console.
   * @return the previous set logLevel {@link IdeLogLevel}
   */
  public IdeLogLevel setLogLevelConsole(IdeLogLevel logLevelConsole) {

    IdeLogLevel previousLogLevel = this.logLevelConsole;
    if ((previousLogLevel == null) || (previousLogLevel.ordinal() > IdeLogLevel.INFO.ordinal())) {
      previousLogLevel = IdeLogLevel.INFO;
    }
    this.logLevelConsole = logLevelConsole;
    return previousLogLevel;
  }

  @Override
  public IdeLogLevel getLogLevelLogger() {

    if (this.logLevelLogger == null) {
      if ((this.logListenerBuffer != null) && (this.logListenerBuffer.isBuffering())) {
        return IdeLogLevel.TRACE;
      }
      return this.logLevelConsole;
    }
    return this.logLevelLogger;
  }

  /**
   * @param logLevelLogger the new {@link #getLogLevelLogger() loglevel for the logger}.
   */
  public void setLogLevelLogger(IdeLogLevel logLevelLogger) {

    this.logLevelLogger = logLevelLogger;
  }

  /**
   * @return the {@link IdeLogArgFormatter}.
   */
  public IdeLogArgFormatter getArgFormatter() {

    return this.argFormatter;
  }

  /**
   * Internal method to set the {@link IdeLogArgFormatter}.
   *
   * @param argFormatter the new {@link IdeLogArgFormatter}.
   */
  public void setArgFormatter(IdeLogArgFormatter argFormatter) {

    this.argFormatter = argFormatter;
  }

  /**
   * Ensure the logging system is initialized.
   */
  public void activateLogging() {

    if (this.logListener instanceof IdeLogListenerBuffer buffer) {
      // https://github.com/devonfw/IDEasy/issues/754
      buffer.flushAndEndBuffering();
    }
  }

  /**
   * Disables the logging system (temporary).
   *
   * @param threshold the {@link IdeLogLevel} acting as threshold.
   * @see com.devonfw.tools.ide.context.IdeContext#runWithoutLogging(Runnable, IdeLogLevel)
   */
  public void deactivateLogging(IdeLogLevel threshold) {

    if (this.logListener instanceof IdeLogListenerBuffer buffer) {
      buffer.startBuffering(threshold);
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public boolean isQuietMode() {

    return this.quietMode;
  }

  /**
   * @param quietMode new value of {@link #isQuietMode()}.
   */
  public void setQuietMode(boolean quietMode) {

    this.quietMode = quietMode;
  }

  @Override
  public boolean isBatchMode() {

    return this.batchMode;
  }

  /**
   * @param batchMode new value of {@link #isBatchMode()}.
   */
  public void setBatchMode(boolean batchMode) {

    this.batchMode = batchMode;
  }

  @Override
  public boolean isForceMode() {

    return this.forceMode;
  }

  /**
   * @param forceMode new value of {@link #isForceMode()}.
   */
  public void setForceMode(boolean forceMode) {

    this.forceMode = forceMode;
  }

  @Override
  public boolean isPrivacyMode() {
    return this.privacyMode;
  }

  /**
   * @param privacyMode new value of {@link #isPrivacyMode()}.
   */
  public void setPrivacyMode(boolean privacyMode) {
    this.privacyMode = privacyMode;
  }

  @Override
  public boolean isForcePull() {

    return this.forcePull;
  }

  /**
   * @param forcePull new value of {@link #isForcePull()}.
   */
  public void setForcePull(boolean forcePull) {

    this.forcePull = forcePull;
  }

  @Override
  public boolean isForcePlugins() {

    return this.forcePlugins;
  }

  /**
   * @param forcePlugins new value of {@link #isForcePlugins()}.
   */
  public void setForcePlugins(boolean forcePlugins) {

    this.forcePlugins = forcePlugins;
  }

  @Override
  public boolean isForceRepositories() {

    return this.forceRepositories;
  }

  /**
   * @param forceRepositories new value of {@link #isForceRepositories()}.
   */
  public void setForceRepositories(boolean forceRepositories) {

    this.forceRepositories = forceRepositories;
  }

  @Override
  public boolean isOfflineMode() {

    return this.offlineMode;
  }

  /**
   * @param offlineMode new value of {@link #isOfflineMode()}.
   */
  public void setOfflineMode(boolean offlineMode) {

    this.offlineMode = offlineMode;
  }

  @Override
  public boolean isSkipUpdatesMode() {

    return this.skipUpdatesMode;
  }

  /**
   * @param skipUpdatesMode new value of {@link #isSkipUpdatesMode()}.
   */
  public void setSkipUpdatesMode(boolean skipUpdatesMode) {

    this.skipUpdatesMode = skipUpdatesMode;
  }

  @Override
  public Locale getLocale() {

    return this.locale;
  }

  /**
   * @param locale new value of {@link #getLocale()}.
   */
  public void setLocale(Locale locale) {

    this.locale = locale;
  }

  @Override
  public boolean isNoColorsMode() {

    return this.noColorsMode;
  }

  /**
   * @param noColoursMode new value of {@link #isNoColorsMode()}.
   */
  public void setNoColorsMode(boolean noColoursMode) {

    this.noColorsMode = noColoursMode;
  }

  /**
   * @return {@code true} to write a logfile to disc, {@code false} otherwise.
   */
  public boolean isWriteLogfile() {

    return this.writeLogfile;
  }

  /**
   * @param writeLogfile new value of {@link #isWriteLogfile()}.
   */
  public void setWriteLogfile(boolean writeLogfile) {
    this.writeLogfile = writeLogfile;
  }

  /**
   * @return the current {@link IdeStartContextImpl} instance.
   */
  public static IdeStartContextImpl get() {

    return instance;
  }

}
