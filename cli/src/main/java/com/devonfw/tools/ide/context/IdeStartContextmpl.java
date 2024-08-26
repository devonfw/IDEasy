package com.devonfw.tools.ide.context;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogger;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.log.IdeSubLoggerNone;

/**
 * Implementation of {@link IdeLogger}.
 */
public class IdeStartContextmpl implements IdeStartContext {

  private final Function<IdeLogLevel, IdeSubLogger> loggerFactory;

  private final IdeSubLogger[] loggers;

  private boolean offlineMode;

  private boolean forceMode;

  private boolean batchMode;

  private boolean quietMode;

  private Locale locale;

  /**
   * @param minLogLevel the minimum enabled {@link IdeLogLevel}.
   * @param factory the factory to create active {@link IdeSubLogger} instances.
   */
  public IdeStartContextmpl(IdeLogLevel minLogLevel, Function<IdeLogLevel, IdeSubLogger> factory) {

    super();
    this.loggerFactory = factory;
    this.loggers = new IdeSubLogger[IdeLogLevel.values().length];
    setLogLevel(minLogLevel);
  }

  @Override
  public IdeSubLogger level(IdeLogLevel level) {

    IdeSubLogger logger = this.loggers[level.ordinal()];
    Objects.requireNonNull(logger);
    return logger;
  }

  /**
   * Sets the log level.
   *
   * @param logLevel {@link IdeLogLevel}
   */
  public void setLogLevel(IdeLogLevel logLevel) {

    for (IdeLogLevel level : IdeLogLevel.values()) {
      boolean enabled = level.ordinal() >= logLevel.ordinal();
      setLogLevel(level, enabled);
    }
  }

  /**
   * @param logLevel the {@link IdeLogLevel} to modify.
   * @param enabled - {@code true} to enable, {@code false} to disable.
   */
  public void setLogLevel(IdeLogLevel logLevel, boolean enabled) {

    IdeSubLogger logger;
    if (enabled) {
      logger = this.loggerFactory.apply(logLevel);
    } else {
      logger = IdeSubLoggerNone.of(logLevel);
    }
    this.loggers[logLevel.ordinal()] = logger;
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
  public Locale getLocale() {

    return this.locale;
  }


  /**
   * @param locale new value of {@link #getLocale()}.
   */
  public void setLocale(Locale locale) {

    this.locale = locale;
  }

}
