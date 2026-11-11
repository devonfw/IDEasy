package com.devonfw.tools.ide.commandlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.function.AbstractParameterizedFunction;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.tool.AbstractToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * A {@link AbstractCommandlet} is a sub-command of the IDE CLI.
 */
public abstract class AbstractCommandlet extends AbstractParameterizedFunction implements Commandlet {

  /** The {@link IdeContext} instance. */
  protected final IdeContext context;

  private final List<Property<?>> valuesList;

  private final List<Property<?>> values;

  private final Map<String, Property<?>> optionMap;

  private KeywordProperty firstKeyword;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public AbstractCommandlet(IdeContext context) {

    super();
    this.context = context;
    this.valuesList = new ArrayList<>();
    this.values = Collections.unmodifiableList(this.valuesList);
    this.optionMap = new HashMap<>();
  }

  /**
   * @return the {@link List} of {@link Property properties} that are {@link Property#isValue() values}.
   */
  public List<Property<?>> getValues() {

    return this.values;
  }

  /**
   * @param nameOrAlias the potential {@link Property#getName() name} or {@link Property#getAlias() alias} of the requested {@link Property}.
   * @return the requested {@link Property property} or {@code null} if not found.
   */
  public Property<?> getOption(String nameOrAlias) {

    return this.optionMap.get(nameOrAlias);
  }

  @Override
  protected <P extends Property<?>> P add(P property) {

    super.add(property);
    if (property.isValue()) {
      this.valuesList.add(property);
    }
    return property;
  }

  @Override
  protected Property<?> registerOption(String name, Property<?> property) {

    return this.optionMap.put(name, property);
  }

  /**
   * @param keyword the {@link KeywordProperty keyword} to {@link #add(Property) add}.
   */
  protected void addKeyword(String keyword) {

    addKeyword(keyword, null);
  }

  /**
   * @param keyword the {@link KeywordProperty keyword} to {@link #add(Property) add}.
   * @param alias the optional {@link KeywordProperty#getAlias() alias}.
   */
  protected void addKeyword(String keyword, String alias) {

    KeywordProperty property = new KeywordProperty(keyword, true, alias);
    if (this.firstKeyword == null) {
      if (!getProperties().isEmpty()) {
        throw new IllegalStateException(property + " must be first property in " + getClass().getSimpleName());
      }
      this.firstKeyword = property;
    }
    add(property);
  }

  @Override
  public IdeContext getContext() {

    return this.context;
  }

  /**
   * @return the first keyword of this {@link AbstractCommandlet}. Typically, the same as {@link #getName() name} but may also differ (e.g. "set" vs.
   *     "set-version").
   */
  public KeywordProperty getFirstKeyword() {

    return this.firstKeyword;
  }

  /**
   * @param <C> type of the {@link AbstractCommandlet}.
   * @param commandletType the {@link Class} reflecting the requested {@link AbstractCommandlet}.
   * @return the requested {@link AbstractCommandlet}.
   * @see CommandletManager#getCommandlet(Class)
   */
  protected <C extends AbstractCommandlet> C getCommandlet(Class<C> commandletType) {

    return this.context.getCommandletManager().getCommandlet(commandletType);
  }

  /**
   * @return {@code true} if {@link IdeContext#getIdeHome() IDE_HOME} is required for this commandlet, {@code false} otherwise.
   */
  public boolean isIdeHomeRequired() {

    return isIdeRootRequired();
  }

  /**
   * @return {@code true} if {@link IdeContext#getIdeRoot() IDE_ROOT} is required for this commandlet, {@code false} otherwise.
   */
  public boolean isIdeRootRequired() {

    return true;
  }

  /**
   * @return {@code true} to suppress the {@link com.devonfw.tools.ide.step.StepImpl#logSummary(boolean) step summary success message}.
   */
  public boolean isSuppressStepSuccess() {

    return false;
  }

  /**
   * @return {@code true} if the output of this commandlet is (potentially) processed automatically from outside, {@code false} otherwise. For example
   *     {@link CompleteCommandlet} logs the suggestions for auto-completion to a bash script. Also, the {@link EnvironmentCommandlet} logs the environment
   *     variables for the {@code ide} wrapper script. In such scenarios these logs shall not be spammed with warnings like "IDE_ROOT is not set" that would
   *     break the processing of the output.
   */
  public boolean isProcessableOutput() {

    return false;
  }

  /**
   * @return {@code true} to write a logfile (unless disabled via {@link com.devonfw.tools.ide.variable.IdeVariables#IDE_WRITE_LOGFILE}), {@code false}
   *     otherwise.
   */
  public boolean isWriteLogFile() {
    return !isProcessableOutput();
  }

  /**
   * Runs this {@link AbstractCommandlet}.
   */
  public final void run() {

    if (this.context != null) { // for ContextCommandlet we do not have a context yet
      ((AbstractIdeContext) this.context).configureJavaUtilLogging(this);
    }
    doRun();
  }

  protected abstract void doRun();

  /**
   * Provide additional usage help of this {@link AbstractCommandlet} to the user.
   *
   * @param bundle the {@link NlsBundle} to get I18N messages from.
   */
  public void printHelp(NlsBundle bundle) {

  }

  @Override
  public String toString() {

    return getClass().getSimpleName() + "[" + getName() + "]";
  }

  /**
   * @return the {@link AbstractToolCommandlet} set in a {@link Property} of this commandlet used for auto-completion of a {@link VersionIdentifier} or
   *     {@link com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor}, otherwise {@code null} if not exists or not configured.
   */
  public AbstractToolCommandlet getToolForCompletion() {
    return null;
  }
}
