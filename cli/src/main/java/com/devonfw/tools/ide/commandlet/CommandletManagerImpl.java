package com.devonfw.tools.ide.commandlet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.repository.RepositoryCommandlet;
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
import com.devonfw.tools.ide.tool.pycharm.Pycharm;
import com.devonfw.tools.ide.tool.python.Python;
import com.devonfw.tools.ide.tool.quarkus.Quarkus;
import com.devonfw.tools.ide.tool.sonar.Sonar;
import com.devonfw.tools.ide.tool.terraform.Terraform;
import com.devonfw.tools.ide.tool.tomcat.Tomcat;
import com.devonfw.tools.ide.tool.vscode.Vscode;

/**
 * Implementation of {@link CommandletManager}.
 */
public class CommandletManagerImpl implements CommandletManager {

  private final IdeContext context;

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
    this.context = context;
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
    add(new UpgradeSettingsCommandlet(context));
    add(new CreateCommandlet(context));
    add(new BuildCommandlet(context));
    add(new InstallPluginCommandlet(context));
    add(new UninstallPluginCommandlet(context));
    add(new UpgradeCommandlet(context));
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
    add(new Python(context));
    add(new Pycharm(context));
  }

  /**
   * @param commandlet the {@link Commandlet} to add.
   */
  protected void add(Commandlet commandlet) {

    boolean hasRequiredProperty = false;
    List<Property<?>> properties = commandlet.getProperties();
    int propertyCount = properties.size();
    KeywordProperty keyword = commandlet.getFirstKeyword();
    if (keyword != null) {
      String name = keyword.getName();
      registerKeyword(name, commandlet);
      String optionName = keyword.getOptionName();
      if (!optionName.equals(name)) {
        registerKeyword(optionName, commandlet);
      }
      String alias = keyword.getAlias();
      if (alias != null) {
        registerKeyword(alias, commandlet);
      }
    }
    for (int i = 0; i < propertyCount; i++) {
      Property<?> property = properties.get(i);
      if (property.isRequired()) {
        hasRequiredProperty = true;
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

  private void registerKeyword(String keyword, Commandlet commandlet) {

    Commandlet duplicate = this.firstKeywordMap.putIfAbsent(keyword, commandlet);
    if (duplicate != null) {
      this.context.debug("Duplicate keyword {} already used by {} so it cannot be associated also with {}", keyword, duplicate, commandlet);
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

    return this.commandletNameMap.get(name);
  }

  @Override
  public Commandlet getCommandletByFirstKeyword(String keyword) {

    return this.firstKeywordMap.get(keyword);
  }

  @Override
  public Iterator<Commandlet> findCommandlet(CliArguments arguments, CompletionCandidateCollector collector) {

    CliArgument current = arguments.current();
    if (current.isEnd()) {
      return Collections.emptyIterator();
    }
    String keyword = current.get();
    Commandlet commandlet = getCommandletByFirstKeyword(keyword);
    if ((commandlet == null) && (collector == null)) {
      return Collections.emptyIterator();
    }
    return new CommandletFinder(commandlet, arguments.copy(), collector);
  }

  private final class CommandletFinder implements Iterator<Commandlet> {

    private final Commandlet firstCandidate;

    private final Iterator<Commandlet> commandletIterator;

    private final CliArguments arguments;

    private final CompletionCandidateCollector collector;

    private Commandlet next;

    private CommandletFinder(Commandlet firstCandidate, CliArguments arguments, CompletionCandidateCollector collector) {

      this.firstCandidate = firstCandidate;
      this.commandletIterator = getCommandlets().iterator();
      this.arguments = arguments;
      this.collector = collector;
      if (isSuitable(firstCandidate)) {
        this.next = firstCandidate;
      } else {
        this.next = findNext();
      }
    }

    @Override
    public boolean hasNext() {

      return this.next != null;
    }

    @Override
    public Commandlet next() {

      if (this.next == null) {
        throw new NoSuchElementException();
      }
      Commandlet result = this.next;
      this.next = findNext();
      return result;
    }

    private boolean isSuitable(Commandlet commandlet) {

      return (commandlet != null) && (!commandlet.isIdeHomeRequired() || (context.getIdeHome() != null));
    }

    private Commandlet findNext() {
      while (this.commandletIterator.hasNext()) {
        Commandlet cmd = this.commandletIterator.next();
        if ((cmd != this.firstCandidate) && isSuitable(cmd)) {
          List<Property<?>> properties = cmd.getProperties();
          // validation should already be done in add method and could be removed here...
          if (properties.isEmpty()) {
            assert false : cmd.getClass().getSimpleName() + " has no properties!";
          } else {
            Property<?> property = properties.get(0);
            if (property instanceof KeywordProperty) {
              boolean matches = property.apply(arguments.copy(), context, cmd, this.collector);
              if (matches) {
                return cmd;
              }
            } else {
              assert false : cmd.getClass().getSimpleName() + " is invalid as first property must be keyword property but is " + property;
            }
          }
        }
      }
      return null;
    }
  }
}
