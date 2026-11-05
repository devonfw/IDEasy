package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * A {@link Commandlet} is a sub-command interface of the IDE CLI.
 */
public interface Commandlet {

  /**
   * @return the name of this {@link AbstractCommandlet} (e.g. "help").
   */
  String getName();

  /**
   * @return the {@link IdeContext} of this {@link Commandlet} or {@code null} for commandlets that are executed before the {@link IdeContext} has been
   *     initialized (e.g. {@code ContextCommandlet}).
   */
  default IdeContext getContext() {

    return null;
  }

}
