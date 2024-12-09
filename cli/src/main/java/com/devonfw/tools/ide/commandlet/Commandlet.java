package com.devonfw.tools.ide.commandlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.validation.ValidationResult;
import com.devonfw.tools.ide.validation.ValidationState;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * A {@link Commandlet} is a sub-command of the IDE CLI.
 */
public abstract class Commandlet {

  /** The {@link IdeContext} instance. */
  protected final IdeContext context;

  private final List<Property<?>> propertiesList;

  private final List<Property<?>> properties;

  private final List<Property<?>> valuesList;

  private final List<Property<?>> values;

  private final Map<String, Property<?>> optionMap;

  private Property<?> multiValued;

  private KeywordProperty firstKeyword;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Commandlet(IdeContext context) {

    super();
    this.context = context;
    this.propertiesList = new ArrayList<>();
    this.properties = Collections.unmodifiableList(this.propertiesList);
    this.valuesList = new ArrayList<>();
    this.values = Collections.unmodifiableList(this.valuesList);
    this.optionMap = new HashMap<>();
  }

  /**
   * @return the {@link List} with all {@link Property properties} of this {@link Commandlet}.
   */
  public List<Property<?>> getProperties() {

    return this.properties;
  }

  /**
   * @return the {@link List} of {@link Property properties} that are {@link Property#isValue() values}.
   */
  public List<Property<?>> getValues() {

    return this.values;
  }

  /**
   * Clear the set values on all properties of the {@link Commandlet#propertiesList}
   */
  public void reset() {

    for (Property<?> property : this.propertiesList) {
      property.clearValue();
    }
  }

  /**
   * @param nameOrAlias the potential {@link Property#getName() name} or {@link Property#getAlias() alias} of the requested {@link Property}.
   * @return the requested {@link Property property} or {@code null} if not found.
   */
  public Property<?> getOption(String nameOrAlias) {

    return this.optionMap.get(nameOrAlias);
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
      if (!this.properties.isEmpty()) {
        throw new IllegalStateException(property + " must be first property in " + getClass().getSimpleName());
      }
      this.firstKeyword = property;
    }
    add(property);
  }

  /**
   * @param <P> type of the {@link Property}.
   * @param property the {@link Property} to register.
   * @return the given {@link Property}.
   */
  protected <P extends Property<?>> P add(P property) {

    if (this.multiValued != null) {
      throw new IllegalStateException("The multi-valued property " + this.multiValued + " can not be followed by " + property);
    }
    this.propertiesList.add(property);
    if (property.isOption()) {
      add(property.getName(), property, false);
      add(property.getAlias(), property, true);
    }
    if (property.isValue()) {
      this.valuesList.add(property);
    }
    if (property.isMultiValued()) {
      this.multiValued = property;
    }
    return property;
  }

  private void add(String name, Property<?> property, boolean alias) {

    if (alias && (name == null)) {
      return;
    }
    Objects.requireNonNull(name);
    assert (name.equals(name.trim()));
    if (name.isEmpty() && !alias) {
      return;
    }
    Property<?> duplicate = this.optionMap.put(name, property);
    if (duplicate != null) {
      throw new IllegalStateException("Duplicate name or alias " + name + " for " + property + " and " + duplicate);
    }
  }

  /**
   * @return the name of this {@link Commandlet} (e.g. "help").
   */
  public abstract String getName();

  /**
   * @return the first keyword of this {@link Commandlet}. Typically the same as {@link #getName() name} but may also differ (e.g. "set" vs. "set-version").
   */
  public KeywordProperty getFirstKeyword() {

    return this.firstKeyword;
  }

  /**
   * @param <C> type of the {@link Commandlet}.
   * @param commandletType the {@link Class} reflecting the requested {@link Commandlet}.
   * @return the requested {@link Commandlet}.
   * @see CommandletManager#getCommandlet(Class)
   */
  protected <C extends Commandlet> C getCommandlet(Class<C> commandletType) {

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
   *     {@link CompleteCommandlet} logs the suggestions for auto-completion to a bash script. Also the {@link EnvironmentCommandlet} logs the environment
   *     variables for the {@code ide} wrapper script. In such scenarios these logs shall not be spammed with warnings like "IDE_ROOT is not set" that would
   *     break the processing of the output.
   */
  public boolean isProcessableOutput() {

    return false;
  }

  /**
   * Runs this {@link Commandlet}.
   */
  public abstract void run();

  /**
   * @return {@code true} if this {@link Commandlet} is the valid candidate to be {@link #run()}, {@code false} otherwise.
   * @see Property#validate()
   */
  public ValidationResult validate() {
    ValidationState state = new ValidationState(null);
    // avoid validation exception if not a candidate to be run.
    for (Property<?> property : this.propertiesList) {
      state.add(property.validate());
    }
    return state;
  }

  /**
   * Provide additional usage help of this {@link Commandlet} to the user.
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
   * @return the {@link ToolCommandlet} set in a {@link Property} of this commandlet used for auto-completion of a {@link VersionIdentifier} or
   *     {@link com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor}, otherwise {@code null} if not exists or not configured.
   */
  public ToolCommandlet getToolForCompletion() {
    return null;
  }
}
