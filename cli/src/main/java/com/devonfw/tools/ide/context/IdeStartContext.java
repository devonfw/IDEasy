package com.devonfw.tools.ide.context;

import java.util.Locale;

import com.devonfw.tools.ide.log.IdeLogger;

/**
 * Extends {@link IdeLogger} with the options configurable via {@link com.devonfw.tools.ide.cli.Ideasy} CLI (see
 * {@link com.devonfw.tools.ide.commandlet.ContextCommandlet}). The {@link IdeStartContext} is therefore the object configured at bootstrapping and then used to
 * create the actual {@link IdeContext} from it.
 */
public interface IdeStartContext extends IdeLogger {

  /**
   * @return {@code true} in case of quiet mode (reduced output), {@code false} otherwise.
   */
  boolean isQuietMode();

  /**
   * @return {@code true} in case of batch mode (no {@link IdeContext#question(String) user-interaction}), {@code false} otherwise.
   */
  boolean isBatchMode();

  /**
   * @return {@code true} in case of force mode, {@code false} otherwise.
   */
  boolean isForceMode();

  /**
   * @return {@code true} if offline mode is activated (-o/--offline), {@code false} otherwise.
   */
  boolean isOfflineMode();

  /**
   * @return the current {@link Locale}. Either configured via command-line option or {@link Locale#getDefault() default}.
   */
  Locale getLocale();

}
