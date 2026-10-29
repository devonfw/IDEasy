package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.function.ParameterizedFunction;

/**
 * A {@link Commandlet} is a sub-command interface of the IDE CLI.
 */
public interface Commandlet extends ParameterizedFunction {

  /**
   * @return the {@link IdeContext} of this {@link Commandlet} or {@code null} for commandlets that are executed before the {@link IdeContext} has been
   *     initialized (e.g. {@code ContextCommandlet}).
   */
  default IdeContext getContext() {

    return null;
  }

}
