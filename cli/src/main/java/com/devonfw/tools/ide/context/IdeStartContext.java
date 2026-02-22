package com.devonfw.tools.ide.context;

import java.util.Locale;

import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListener;
import com.devonfw.tools.ide.network.ReadOfflineMode;

/**
 * Contains the options configurable via {@link com.devonfw.tools.ide.cli.Ideasy} CLI. The {@link IdeStartContext} is therefore the object configured at
 * bootstrapping and then used to create the actual {@link IdeContext} from it.
 *
 * @see com.devonfw.tools.ide.commandlet.ContextCommandlet
 */
public interface IdeStartContext extends ReadOfflineMode {

  /**
   * @return the {@link IdeLogListener}.
   */
  IdeLogListener getLogListener();

  /**
   * @return the minimum allowed {@link IdeLogLevel} (threshold).
   */
  IdeLogLevel getLogLevel();

  /**
   * @return {@code true} in case of quiet mode (reduced output), {@code false} otherwise.
   */
  boolean isQuietMode();

  /**
   * @return {@code true} in case of batch mode (no {@link IdeContext#askForInput(String) user-interaction}), {@code false} otherwise.
   */
  boolean isBatchMode();

  /**
   * @return {@code true} in case of force mode, {@code false} otherwise.
   */
  boolean isForceMode();

  /**
   * @return {@code true} in case of privacy mode, {@code false} otherwise.
   */
  boolean isPrivacyMode();

  /**
   * @return {@code true} to force pulling repositories (e.g. settings from code repository), {@code false} otherwise.
   */
  boolean isForcePull();

  /**
   * @return {@code true} to force plugin installation (e.g. if already installed according to marker file), {@code false} otherwise.
   */
  boolean isForcePlugins();

  /**
   * @return {@code true} to force repositories (including cloning inactive and pull existing ones), {@code false} otherwise.
   */
  boolean isForceRepositories();

  /**
   * @return {@code true} if updates should be skipped, {@code false} otherwise. If updates are skipped and the configured version is a
   *     {@link com.devonfw.tools.ide.version.VersionIdentifier#isPattern() pattern} (e.g. "*" or "21*") and a matching version is already installed (e.g.
   *     "21.0.3_9"), then updates will be skipped even if they are available (e.g. "21.0.9_10").
   */
  boolean isSkipUpdatesMode();

  /**
   * @return {@code true} if no-colours mode is activated (--no-colours), {@code false} otherwise.
   */
  boolean isNoColorsMode();

  /**
   * @return the current {@link Locale}. Either configured via command-line option or {@link Locale#getDefault() default}.
   */
  Locale getLocale();

}
