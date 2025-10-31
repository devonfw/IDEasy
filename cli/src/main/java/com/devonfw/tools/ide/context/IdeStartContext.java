package com.devonfw.tools.ide.context;

import java.util.Locale;

import com.devonfw.tools.ide.log.IdeLogger;
import com.devonfw.tools.ide.network.ReadOfflineMode;

/**
 * Extends {@link IdeLogger} with the options configurable via {@link com.devonfw.tools.ide.cli.Ideasy} CLI (see
 * {@link com.devonfw.tools.ide.commandlet.ContextCommandlet}). The {@link IdeStartContext} is therefore the object configured at bootstrapping and then used to
 * create the actual {@link IdeContext} from it.
 */
public interface IdeStartContext extends IdeLogger, ReadOfflineMode {

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
   * @return {@code true} if quickStart mode is activated (-s/--quickStart), {@code false} otherwise.
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
