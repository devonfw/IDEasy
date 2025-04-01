package com.devonfw.tools.ide.context;

import java.util.Locale;
import java.util.function.Function;

import com.devonfw.tools.ide.log.AbstractIdeSubLogger;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLoggerImpl;
import com.devonfw.tools.ide.log.IdeSubLogger;

/**
 * Implementation of {@link IdeStartContext}.
 */
public class IdeStartContextImpl extends IdeLoggerImpl implements IdeStartContext {

  private boolean skipUpdatesMode;

  private boolean offlineMode;

  private boolean forceMode;

  private boolean forcePull;

  private boolean forcePlugins;

  private boolean forceRepositories;

  private boolean batchMode;

  private boolean quietMode;

  private Locale locale;

  /**
   * @param minLogLevel the minimum enabled {@link IdeLogLevel}.
   * @param factory the factory to create active {@link IdeSubLogger} instances.
   */
  public IdeStartContextImpl(IdeLogLevel minLogLevel, Function<IdeLogLevel, AbstractIdeSubLogger> factory) {

    super(minLogLevel, factory);
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

  @Override
  public boolean isForcePull() {

    return this.forcePull;
  }

  @Override
  public boolean isForcePlugins() {

    return this.forcePlugins;
  }

  @Override
  public boolean isForceRepositories() {

    return this.forceRepositories;
  }


  public void setForceMode(boolean forceMode) {

    this.forceMode = forceMode;
  }

  @Override
  public void setForcePull(boolean forcePull) {

    this.forcePull = forcePull;
  }

  @Override
  public void setForcePlugins(boolean forcePlugins) {

    this.forcePlugins = forcePlugins;
  }

  @Override
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
   * @param skipUpdatesMode new value of {@link #isSkipUpdatesMode()} ()}.
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

}
