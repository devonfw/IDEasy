package com.devonfw.tools.ide.commandlet;

import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.tool.AbstractToolCommandlet;
import com.devonfw.tools.ide.validation.ValidationResult;
import com.devonfw.tools.ide.validation.ValidationState;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * A {@link Commandlet} is a sub-command interface of the IDE CLI.
 */
public interface Commandlet {

  /**
   * @return the name of this {@link AbstractCommandlet} (e.g. "help").
   */
  String getName();

  /**
   * Runs this {@link Commandlet}. The {@link AbstractCommandlet} implements this as a final template method that configures logging and delegates to the
   * tool-specific {@code doRun()} hook; a {@link Commandlet} that is <b>not</b> backed by {@link AbstractCommandlet} (e.g. a composed or
   * package-manager based commandlet) provides its own implementation.
   */
  void run();

  /**
   * @return the {@link IdeContext} of this {@link Commandlet} or {@code null} for commandlets that are executed before the {@link IdeContext} has been
   *     initialized (e.g. {@code ContextCommandlet}).
   */
  default IdeContext getContext() {

    return null;
  }

  /**
   * @return the {@link List} with all {@link Property properties} of this {@link AbstractCommandlet}.
   */
  List<Property<?>> getProperties();

  /**
   * @return the {@link List} of {@link Property properties} that are {@link Property#isValue() values}.
   */
  List<Property<?>> getValues();

  /**
   * @param nameOrAlias the potential {@link Property#getName() name} or {@link Property#getAlias() alias} of the requested {@link Property}.
   * @return the requested {@link Property property} or {@code null} if not found.
   */
  Property<?> getOption(String nameOrAlias);

  /**
   * @return the first keyword of this {@link AbstractCommandlet}. Typically, the same as {@link #getName() name} but may also differ (e.g. "set" vs.
   *     "set-version").
   */
  KeywordProperty getFirstKeyword();

  /**
   * @return {@code true} if {@link IdeContext#getIdeHome() IDE_HOME} is required for this commandlet, {@code false} otherwise.
   */
  default boolean isIdeHomeRequired() {

    return isIdeRootRequired();
  }

  /**
   * @return {@code true} if {@link IdeContext#getIdeRoot() IDE_ROOT} is required for this commandlet, {@code false} otherwise.
   */
  default boolean isIdeRootRequired() {

    return true;
  }

  /**
   * @return {@code true} to suppress the {@link com.devonfw.tools.ide.step.StepImpl#logSummary(boolean) step summary success message}.
   */
  default boolean isSuppressStepSuccess() {

    return false;
  }

  /**
   * @return {@code true} if the output of this commandlet is (potentially) processed automatically from outside, {@code false} otherwise. For example
   *     {@link CompleteCommandlet} logs the suggestions for auto-completion to a bash script. Also, the {@link EnvironmentCommandlet} logs the environment
   *     variables for the {@code ide} wrapper script. In such scenarios these logs shall not be spammed with warnings like "IDE_ROOT is not set" that would
   *     break the processing of the output.
   */
  default boolean isProcessableOutput() {

    return false;
  }

  /**
   * @return {@code true} to write a logfile (unless disabled via {@link com.devonfw.tools.ide.variable.IdeVariables#IDE_WRITE_LOGFILE}), {@code false}
   *     otherwise.
   */
  default boolean isWriteLogFile() {
    return !isProcessableOutput();
  }

  /**
   * Provide additional usage help of this {@link AbstractCommandlet} to the user.
   *
   * @param bundle the {@link NlsBundle} to get I18N messages from.
   */
  default void printHelp(NlsBundle bundle) {

  }

  /**
   * @return {@code true} if this {@link AbstractCommandlet} is the valid candidate to be {@link #run()}, {@code false} otherwise.
   * @see Property#validate()
   */
  default ValidationResult validate() {
    ValidationState state = new ValidationState(null);
    // avoid validation exception if not a candidate to be run.
    for (Property<?> property : getProperties()) {
      state.add(property.validate());
    }
    return state;
  }

  /**
   * Clear the set values on all properties of the {@link AbstractCommandlet#getProperties()}
   */
  default void reset() {

    for (Property<?> property : getProperties()) {
      property.clearValue();
    }
  }

  /**
   * @return the {@link AbstractToolCommandlet} set in a {@link Property} of this commandlet used for auto-completion of a {@link VersionIdentifier} or
   *     {@link com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor}, otherwise {@code null} if not exists or not configured.
   */
  default AbstractToolCommandlet getToolForCompletion() {
    return null;
  }

}
