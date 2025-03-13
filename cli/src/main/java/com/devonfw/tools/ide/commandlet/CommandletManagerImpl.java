package com.devonfw.tools.ide.commandlet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.tool.androidstudio.AndroidStudio;
import com.devonfw.tools.ide.tool.aws.Aws;
import com.devonfw.tools.ide.tool.az.Azure;
import com.devonfw.tools.ide.tool.docker.Docker;
import com.devonfw.tools.ide.tool.dotnet.DotNet;
import com.devonfw.tools.ide.tool.eclipse.Eclipse;
import com.devonfw.tools.ide.tool.gcviewer.GcViewer;
import com.devonfw.tools.ide.tool.gh.Gh;
import com.devonfw.tools.ide.tool.graalvm.GraalVm;
import com.devonfw.tools.ide.tool.gradle.Gradle;
import com.devonfw.tools.ide.tool.helm.Helm;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.tool.jasypt.Jasypt;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.jmc.Jmc;
import com.devonfw.tools.ide.tool.kotlinc.Kotlinc;
import com.devonfw.tools.ide.tool.kotlinc.KotlincNative;
import com.devonfw.tools.ide.tool.kubectl.KubeCtl;
import com.devonfw.tools.ide.tool.lazydocker.LazyDocker;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.node.Node;
import com.devonfw.tools.ide.tool.npm.Npm;
import com.devonfw.tools.ide.tool.oc.Oc;
import com.devonfw.tools.ide.tool.pgadmin.PgAdmin;
import com.devonfw.tools.ide.tool.quarkus.Quarkus;
import com.devonfw.tools.ide.tool.sonar.Sonar;
import com.devonfw.tools.ide.tool.squirrelsql.SquirrelSql;
import com.devonfw.tools.ide.tool.terraform.Terraform;
import com.devonfw.tools.ide.tool.tomcat.Tomcat;
import com.devonfw.tools.ide.tool.vscode.Vscode;

/**
 * Implementation of {@link CommandletManager}.
 */
public class CommandletManagerImpl implements CommandletManager {

  private final Map<Class<? extends Commandlet>, Commandlet> commandletTypeMap;

  private final Map<String, Commandlet> commandletNameMap;

  private final Map<String, Commandlet> firstKeywordMap;

  private final Collection<Commandlet> commandlets;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CommandletManagerImpl(IdeContext context) {

    super();
    this.commandletTypeMap = new HashMap<>();
    this.commandletNameMap = new HashMap<>();
    this.firstKeywordMap = new HashMap<>();
    this.commandlets = Collections.unmodifiableCollection(this.commandletTypeMap.values());
    add(new HelpCommandlet(context));
    add(new EnvironmentCommandlet(context));
    add(new CompleteCommandlet(context));
    add(new ShellCommandlet(context));
    add(new InstallCommandlet(context));
    add(new VersionSetCommandlet(context));
    add(new VersionGetCommandlet(context));
    add(new VersionListCommandlet(context));
    add(new EditionGetCommandlet(context));
    add(new EditionSetCommandlet(context));
    add(new EditionListCommandlet(context));
    add(new VersionCommandlet(context));
    add(new StatusCommandlet(context));
    add(new RepositoryCommandlet(context));
    add(new UninstallCommandlet(context));
    add(new UpdateCommandlet(context));
    add(new CreateCommandlet(context));
    add(new BuildCommandlet(context));
    add(new InstallPluginCommandlet(context));
    add(new UninstallPluginCommandlet(context));
    add(new Gh(context));
    add(new Helm(context));
    add(new Java(context));
    add(new Node(context));
    add(new Npm(context));
    add(new Mvn(context));
    add(new GcViewer(context));
    add(new Gradle(context));
    add(new Eclipse(context));
    add(new Terraform(context));
    add(new Oc(context));
    add(new Quarkus(context));
    add(new SquirrelSql(context));
    add(new Kotlinc(context));
    add(new KotlincNative(context));
    add(new KubeCtl(context));
    add(new Tomcat(context));
    add(new Vscode(context));
    add(new Azure(context));
    add(new Aws(context));
    add(new Jmc(context));
    add(new DotNet(context));
    add(new Intellij(context));
    add(new Jasypt(context));
    add(new Docker(context));
    add(new Sonar(context));
    add(new AndroidStudio(context));
    add(new GraalVm(context));
    add(new PgAdmin(context));
    add(new LazyDocker(context));
  }

  /**
   * @param commandlet the {@link Commandlet} to add.
   */
  protected void add(Commandlet commandlet) {

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

}
