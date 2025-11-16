package com.devonfw.tools.ide.commandlet;

import java.util.Collection;
import java.util.Iterator;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
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
   * @return a {@link Commandlet} having the first {@link Property} {@link Property#isRequired() required} and a {@link KeywordProperty} with the given
   *     {@link Property#getName() name} or {@code null} if no such {@link Commandlet} is registered.
   */
  Commandlet getCommandletByFirstKeyword(String keyword);

  /**
   * @return the {@link Collection} of all registered {@link Commandlet}s.
   */
  Collection<Commandlet> getCommandlets();

  /**
   * @param name the {@link Commandlet#getName() name} of the requested {@link Commandlet}.
   * @return the requested {@link Commandlet}.
   * @throws IllegalArgumentException if not found.
   */
  default Commandlet getRequiredCommandlet(String name) {

    Commandlet commandlet = getCommandlet(name);
    if (commandlet == null) {
      throw new IllegalArgumentException("The commandlet " + name + " could not be found!");
    }
    return commandlet;
  }

  /**
   * @param name the {@link Commandlet#getName() name} of the requested {@link ToolCommandlet}.
   * @return the requested {@link ToolCommandlet} or {@code null} if not found.
   */
  default ToolCommandlet getToolCommandlet(String name) {

    Commandlet commandlet = getCommandlet(name);
    if (commandlet instanceof ToolCommandlet tc) {
      return tc;
    }
    return null;
  }

  /**
   * @param name the {@link Commandlet#getName() name} of the requested {@link ToolCommandlet}.
   * @return the requested {@link ToolCommandlet}.
   * @throws IllegalArgumentException if no {@link ToolCommandlet} exists with the given {@code name}.
   */
  default ToolCommandlet getRequiredToolCommandlet(String name) {

    Commandlet commandlet = getRequiredCommandlet(name);
    if (commandlet instanceof ToolCommandlet tc) {
      return tc;
    }
    throw new IllegalArgumentException("The commandlet " + name + " is not a ToolCommandlet!");
  }

  /**
   * @param name the {@link Commandlet#getName() name} of the requested {@link LocalToolCommandlet}.
   * @return the requested {@link LocalToolCommandlet}.
   * @throws IllegalArgumentException if no {@link LocalToolCommandlet} exists with the given {@code name}.
   */
  default LocalToolCommandlet getRequiredLocalToolCommandlet(String name) {

    Commandlet commandlet = getRequiredCommandlet(name);
    if (commandlet instanceof LocalToolCommandlet ltc) {
      return ltc;
    }
    throw new IllegalArgumentException("The commandlet " + name + " is not a LocalToolCommandlet!");
  }

  /**
   * @param arguments the {@link CliArguments}.
   * @param collector the optional {@link CompletionCandidateCollector}. Will be {@code null} if no argument {@link CliArguments#isCompletion() completion}
   *     shall be performed.
   * @return an {@link Iterator} of the matching {@link Commandlet}(s). Typically empty or containing a single {@link Commandlet}. Only in edge-cases multiple
   *     {@link Commandlet}s could be found (e.g. if two {@link Commandlet}s exist with the same keyword but with different mandatory properties such as in our
   *     legacy devonfw-ide "ide get version ..." and "ide get edition ..." - however, we redesigned our CLI to "ide get-version ..." and "ide get-edition ..."
   *     to simplify this).
   */
  Iterator<Commandlet> findCommandlet(CliArguments arguments, CompletionCandidateCollector collector);

}
