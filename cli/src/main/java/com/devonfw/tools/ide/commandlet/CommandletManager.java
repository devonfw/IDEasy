package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;
import com.devonfw.tools.ide.tool.AbstractToolCommandlet;

/**
 * Interface to {@link #getCommandlet(Class) get} a {@link AbstractCommandlet} instance that is properly initialized.
 */
public interface CommandletManager {

  /**
   * @param <C> type of the {@link AbstractCommandlet}.
   * @param commandletType the {@link Class} reflecting the requested {@link AbstractCommandlet}.
   * @return the requested {@link AbstractCommandlet}.
   */
  <C extends AbstractCommandlet> C getCommandlet(Class<C> commandletType);

  /**
   * @param name the {@link AbstractCommandlet#getName() name} of the requested {@link AbstractCommandlet}.
   * @return the requested {@link AbstractCommandlet} or {@code null} if not found.
   */
  AbstractCommandlet getCommandlet(String name);

  /**
   * @param keyword the first keyword argument.
   * @return a {@link AbstractCommandlet} having the first {@link Property} {@link Property#isRequired() required} and a {@link KeywordProperty} with the given
   *     {@link Property#getName() name} or {@code null} if no such {@link AbstractCommandlet} is registered.
   */
  AbstractCommandlet getCommandletByFirstKeyword(String keyword);

  /**
   * @return the {@link Collection} of all registered {@link AbstractCommandlet}s.
   */
  Collection<AbstractCommandlet> getCommandlets();

  /**
   * @param name the {@link AbstractCommandlet#getName() name} of the requested {@link AbstractCommandlet}.
   * @return the requested {@link AbstractCommandlet}.
   * @throws IllegalArgumentException if not found.
   */
  default AbstractCommandlet getRequiredCommandlet(String name) {

    AbstractCommandlet commandlet = getCommandlet(name);
    if (commandlet == null) {
      throw new IllegalArgumentException("The commandlet " + name + " could not be found!");
    }
    return commandlet;
  }

  /**
   * @param name the {@link AbstractCommandlet#getName() name} of the requested {@link AbstractToolCommandlet}.
   * @return the requested {@link AbstractToolCommandlet} or {@code null} if not found.
   */
  default AbstractToolCommandlet getToolCommandlet(String name) {

    AbstractCommandlet commandlet = getCommandlet(name);
    if (commandlet instanceof AbstractToolCommandlet tc) {
      return tc;
    }
    return null;
  }

  /**
   * @param name the {@link AbstractCommandlet#getName() name} of the requested {@link AbstractToolCommandlet}.
   * @return the requested {@link AbstractToolCommandlet}.
   * @throws IllegalArgumentException if no {@link AbstractToolCommandlet} exists with the given {@code name}.
   */
  default AbstractToolCommandlet getRequiredToolCommandlet(String name) {

    AbstractCommandlet commandlet = getRequiredCommandlet(name);
    if (commandlet instanceof AbstractToolCommandlet tc) {
      return tc;
    }
    throw new IllegalArgumentException("The commandlet " + name + " is not a ToolCommandlet!");
  }

  /**
   * @param name the {@link AbstractCommandlet#getName() name} of the requested {@link AbstractLocalToolCommandlet}.
   * @return the requested {@link AbstractLocalToolCommandlet}.
   * @throws IllegalArgumentException if no {@link AbstractLocalToolCommandlet} exists with the given {@code name}.
   */
  default AbstractLocalToolCommandlet getRequiredLocalToolCommandlet(String name) {

    AbstractCommandlet commandlet = getRequiredCommandlet(name);
    if (commandlet instanceof AbstractLocalToolCommandlet ltc) {
      return ltc;
    }
    throw new IllegalArgumentException("The commandlet " + name + " is not a LocalToolCommandlet!");
  }

  /**
   * @param arguments the {@link CliArguments}.
   * @param collector the optional {@link CompletionCandidateCollector}. Will be {@code null} if no argument {@link CliArguments#isCompletion() completion}
   *     shall be performed.
   * @return an {@link Iterator} of the matching {@link AbstractCommandlet}(s). Typically empty or containing a single {@link AbstractCommandlet}. Only in
   *     edge-cases multiple {@link AbstractCommandlet}s could be found (e.g. if two {@link AbstractCommandlet}s exist with the same keyword but with different
   *     mandatory properties such as in our legacy devonfw-ide "ide get version ..." and "ide get edition ..." - however, we redesigned our CLI to "ide
   *     get-version ..." and "ide get-edition ..." to simplify this).
   */
  Iterator<AbstractCommandlet> findCommandlet(CliArguments arguments, CompletionCandidateCollector collector);

  /**
   * Detects the applicable build tool for the given {@code buildPath} by {@link AbstractLocalToolCommandlet#findBuildDescriptor(Path) querying} the registered
   * build commandlets (in order of priority) for a matching build descriptor (e.g. {@code pom.xml}, {@code build.gradle} or {@code package.json}).
   *
   * @param buildPath the {@link Path} to the directory to build.
   * @return the applicable build {@link AbstractLocalToolCommandlet} or {@code null} if no build descriptor was found or {@code buildPath} was {@code null}.
   */
  AbstractLocalToolCommandlet findBuildTool(Path buildPath);

}
