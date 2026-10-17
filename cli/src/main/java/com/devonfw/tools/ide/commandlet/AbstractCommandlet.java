package com.devonfw.tools.ide.commandlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;

/**
 * A {@link AbstractCommandlet} is a sub-command of the IDE CLI.
 */
public abstract class AbstractCommandlet implements Commandlet {

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
  public AbstractCommandlet(IdeContext context) {

    super();
    this.context = context;
    this.propertiesList = new ArrayList<>();
    this.properties = Collections.unmodifiableList(this.propertiesList);
    this.valuesList = new ArrayList<>();
    this.values = Collections.unmodifiableList(this.valuesList);
    this.optionMap = new HashMap<>();
  }

  /**
   * @return the {@link List} with all {@link Property properties} of this {@link AbstractCommandlet}.
   */
  @Override
  public List<Property<?>> getProperties() {

    return this.properties;
  }

  /**
   * @return the {@link List} of {@link Property properties} that are {@link Property#isValue() values}.
   */
  @Override
  public List<Property<?>> getValues() {

    return this.values;
  }

  /**
   * @param nameOrAlias the potential {@link Property#getName() name} or {@link Property#getAlias() alias} of the requested {@link Property}.
   * @return the requested {@link Property property} or {@code null} if not found.
   */
  @Override
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

  @Override
  public IdeContext getContext() {

    return this.context;
  }

  @Override
  public abstract String getName();

  /**
   * @return the first keyword of this {@link AbstractCommandlet}. Typically, the same as {@link #getName() name} but may also differ (e.g. "set" vs.
   *     "set-version").
   */
  @Override
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
   * Runs this {@link AbstractCommandlet}.
   */
  public final void run() {

    if (this.context != null) { // for ContextCommandlet we do not have a context yet
      ((AbstractIdeContext) this.context).configureJavaUtilLogging(this);
    }
    doRun();
  }

  protected abstract void doRun();

  @Override
  public String toString() {

    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}
