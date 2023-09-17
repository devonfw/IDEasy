package com.devonfw.tools.ide.commandlet;

import java.util.Collection;

import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * Interface to {@link #getCommandlet(Class) get} a {@link Commandlet} instance that is properly initialized.
 */
public interface CommandletManager {

  /**
   * @param <C> type of the {@link Commandlet}.
   * @param commandletType the {@link Class} reflecting the requested {@link Commandlet}.
   * @return the requested {@link Commandlet}.
   */
  <C extends Commandlet> C getCommandlet(Class<C> commandletType);

  /**
   * @param name the {@link Commandlet#getName() name} of the requested {@link Commandlet}.
   * @return the requested {@link Commandlet} or {@code null} if not found.
   */
  Commandlet getCommandlet(String name);

  /**
   * @param keyword the first keyword argument.
   * @return a {@link Commandlet} having the first {@link Property} {@link Property#isRequired() required} and a
   *         {@link KeywordProperty} with the given {@link Property#getName() name} or {@code null} if no such
   *         {@link Commandlet} is registered.
   */
  Commandlet getCommandletByFirstKeyword(String keyword);

  /**
   * @return the {@link Collection} of all registered {@link Commandlet}s.
   */
  Collection<Commandlet> getCommandlets();

  /**
   * @param name the {@link Commandlet#getName() name} of the requested {@link ToolCommandlet}.
   * @return the requested {@link ToolCommandlet} or {@code null} if not found.
   */
  default ToolCommandlet getToolCommandlet(String name) {

    Commandlet commandlet = getCommandlet(name);
    if (commandlet instanceof ToolCommandlet) {
      return (ToolCommandlet) commandlet;
    }
    throw new IllegalArgumentException("The commandlet " + name + " is not a ToolCommandlet!");
  }

}
