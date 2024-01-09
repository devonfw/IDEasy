package com.devonfw.tools.ide.commandlet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.tool.az.Azure;
import com.devonfw.tools.ide.tool.eclipse.Eclipse;
import com.devonfw.tools.ide.tool.gh.Gh;
import com.devonfw.tools.ide.tool.gradle.Gradle;
import com.devonfw.tools.ide.tool.helm.Helm;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.kotlinc.Kotlinc;
import com.devonfw.tools.ide.tool.kotlinc.KotlincNative;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.node.Node;
import com.devonfw.tools.ide.tool.oc.Oc;
import com.devonfw.tools.ide.tool.quarkus.Quarkus;
import com.devonfw.tools.ide.tool.terraform.Terraform;
import com.devonfw.tools.ide.tool.vscode.Vscode;

/**
 * Implementation of {@link CommandletManager}.
 */
public final class CommandletManagerImpl implements CommandletManager {

  private static CommandletManagerImpl INSTANCE;

  private final Map<Class<? extends Commandlet>, Commandlet> commandletTypeMap;

  private final Map<String, Commandlet> commandletNameMap;

  private final Map<String, Commandlet> firstKeywordMap;

  private final Collection<Commandlet> commandlets;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  private CommandletManagerImpl(IdeContext context) {

    super();
    this.commandletTypeMap = new HashMap<>();
    this.commandletNameMap = new HashMap<>();
    this.firstKeywordMap = new HashMap<>();
    this.commandlets = Collections.unmodifiableCollection(this.commandletTypeMap.values());
    add(new HelpCommandlet(context));
    add(new EnvironmentCommandlet(context));
    add(new InstallCommandlet(context));
    add(new VersionSetCommandlet(context));
    add(new VersionGetCommandlet(context));
    add(new VersionListCommandlet(context));
    add(new VersionCommandlet(context));
    add(new Gh(context));
    add(new Helm(context));
    add(new Java(context));
    add(new Node(context));
    add(new Mvn(context));
    add(new Gradle(context));
    add(new Eclipse(context));
    add(new Terraform(context));
    add(new Oc(context));
    add(new Quarkus(context));
    add(new Kotlinc(context));
    add(new KotlincNative(context));
    add(new Vscode(context));
    add(new Azure(context));
  }

  private void add(Commandlet commandlet) {

    boolean hasRequiredProperty = false;
    List<Property<?>> properties = commandlet.getProperties();
    int propertyCount = properties.size();
    for (int i = 0; i < propertyCount; i++) {
      Property<?> property = properties.get(i);
      if (property.isRequired()) {
        hasRequiredProperty = true;
        if ((i == 0) && (property instanceof KeywordProperty)) {
          String keyword = property.getName();
          this.firstKeywordMap.putIfAbsent(keyword, commandlet);
        }
        break;
      }
    }
    if (!hasRequiredProperty) {
      throw new IllegalStateException("Commandlet " + commandlet + " must have at least one mandatory property!");
    }
    this.commandletTypeMap.put(commandlet.getClass(), commandlet);
    Commandlet duplicate = this.commandletNameMap.put(commandlet.getName(), commandlet);
    if (duplicate != null) {
      throw new IllegalStateException("Commandlet " + commandlet + " has the same name as " + duplicate);
    }
  }

  @Override
  public Collection<Commandlet> getCommandlets() {

    return this.commandlets;
  }

  @Override
  public <C extends Commandlet> C getCommandlet(Class<C> commandletType) {

    Commandlet commandlet = this.commandletTypeMap.get(commandletType);
    if (commandlet == null) {
      throw new IllegalStateException("Commandlet for type " + commandletType + " is not registered!");
    }
    return commandletType.cast(commandlet);
  }

  @Override
  public Commandlet getCommandlet(String name) {

    Commandlet commandlet = this.commandletNameMap.get(name);
    return commandlet;
  }

  @Override
  public Commandlet getCommandletByFirstKeyword(String keyword) {

    return this.firstKeywordMap.get(keyword);
  }

  /**
   * This method gives global access to the {@link CommandletManager} instance. Typically you should have access to
   * {@link IdeContext} and use {@link IdeContext#getCommandletManager()} to access the proper instance of
   * {@link CommandletManager}. Only in very specific cases where there is no {@link IdeContext} available, you may use
   * this method to access it (e.g. from {@link com.devonfw.tools.ide.property.CommandletProperty})
   *
   * @return the static instance of this {@link CommandletManager} implementation that has already been initialized.
   * @throws IllegalStateException if the instance has not been previously initialized via
   *         {@link #getOrCreate(IdeContext)}.
   */
  public static CommandletManager get() {

    return getOrCreate(null);
  }

  /**
   * This method has to be called initially from {@link IdeContext} to create the instance of this
   * {@link CommandletManager} implementation. It will store that instance internally in a static variable so it can
   * later be retrieved with {@link #get()}.
   *
   * @param context the {@link IdeContext}.
   * @return the {@link CommandletManager}.
   */
  public static CommandletManager getOrCreate(IdeContext context) {

    if (context == null) {
      if (INSTANCE == null) {
        throw new IllegalStateException("Not initialized!");
      }
    } else {
      if (context instanceof AbstractIdeContext c) {
        if (c.isMock()) {
          return new CommandletManagerImpl(context);
        }
      }
      if (INSTANCE != null) {
        System.out.println("Multiple initializations!");
      }
      INSTANCE = new CommandletManagerImpl(context);
    }
    return INSTANCE;
  }

  /**
   * Internal method to reset internal instance. May only be used for testing!
   */
  static void reset() {

    INSTANCE = null;
  }

}
