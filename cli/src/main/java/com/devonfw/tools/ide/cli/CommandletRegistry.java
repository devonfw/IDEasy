package com.devonfw.tools.ide.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.console.CommandRegistry;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.utils.AttributedString;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.CommandletProperty;
import com.devonfw.tools.ide.property.Property;

/**
 * Implements the {@link CommandRegistry} for jline3.
 */
public class CommandletRegistry implements CommandRegistry {

  private final ContextCommandlet cmd;

  private final IdeContext context;

  private final Ide ide;

  private final IdeCompleter ideCompleter;

  private final Set<String> commandlets;

  private final Map<String, String> aliasCommandlets;

  public CommandletRegistry(ContextCommandlet cmd, Ide ide, IdeContext context) {

    this.ideCompleter = new IdeCompleter(cmd, context);
    this.cmd = cmd;
    this.context = context;
    this.ide = ide;

    Set<String> commandlets = new HashSet<>();
    Collection<Commandlet> commandletCollection = context.getCommandletManager().getCommandlets();

    for (Commandlet commandlet : commandletCollection) {
      commandlets.add(commandlet.getName());
    }

    // TODO: get correct aliases, uses command name keywords instead now
    Map<String, String> aliasCommandlets = new HashMap<>();
    Collection<Commandlet> aliasCommandletCollection = context.getCommandletManager().getCommandlets();

    for (Commandlet commandlet : aliasCommandletCollection) {
      aliasCommandlets.put(commandlet.getName(), commandlet.getKeyword());
    }

    this.commandlets = commandlets;
    this.aliasCommandlets = aliasCommandlets;
  }

  @Override
  public Set<String> commandNames() {

    return commandlets;
  }

  @Override
  public Map<String, String> commandAliases() {

    return aliasCommandlets;
  }

  @Override
  public List<String> commandInfo(String command) {

    List<String> out = new ArrayList<>();
    // TODO: take command and get help from Ide
    Commandlet helpCommandlet = context.getCommandletManager().getCommandlet("help");
    Property<?> property = new CommandletProperty(command, false, "");
    // helpCommandlet.add(property);
    helpCommandlet.run();
    // TODO: add our own description for each commandlet here
    String description = "placeholder description";
    out.addAll(Arrays.asList(description.split("\\r?\\n")));
    return out;
  }

  @Override
  public boolean hasCommand(String command) {

    return commandlets.contains(command) || aliasCommandlets.containsKey(command);
  }

  @Override
  public SystemCompleter compileCompleters() {

    SystemCompleter out = new SystemCompleter();
    List<String> all = new ArrayList<>();
    all.addAll(commandlets);
    all.addAll(aliasCommandlets.keySet());
    out.add(all, new IdeCompleter(cmd, context));
    return out;
  }

  // For JLine >= 3.16.0
  @Override
  public Object invoke(CommandRegistry.CommandSession session, String command, Object[] args) throws Exception {

    List<String> arguments = new ArrayList<>();
    arguments.add(command);
    arguments.addAll(Arrays.stream(args).map(Object::toString).collect(Collectors.toList()));
    // TODO: run our commandlet here
    runCommand(command, arguments);
    return null;
  }

  private void runCommand(String command, List<String> arguments) {

    String[] convertedArgs = arguments.toArray(new String[0]);
    CliArgument first = CliArgument.of(convertedArgs);
    Commandlet firstCandidate = this.context.getCommandletManager().getCommandletByFirstKeyword(command);
    ide.applyAndRun(first, firstCandidate);
  }

  // @Override This method was removed in JLine 3.16.0; keep it in case this component is used with an older version of
  // JLine
  public Object execute(CommandRegistry.CommandSession session, String command, String[] args) throws Exception {

    List<String> arguments = new ArrayList<>();
    arguments.add(command);
    arguments.addAll(Arrays.asList(args));
    // TODO: run our commandlet here
    runCommand(command, arguments);
    return null;
  }

  // @Override This method was removed in JLine 3.16.0; keep it in case this component is used with an older version of
  // JLine
  public CmdDesc commandDescription(String command) {

    return null;
  }

  @Override
  public CmdDesc commandDescription(List<String> list) {

    Commandlet sub = ideCompleter.findSubcommandlet(list, list.size());

    if (sub == null) {
      return null;
    }

    List<AttributedString> main = new ArrayList<>();
    Map<String, List<AttributedString>> options = new HashMap<>();
    // String synopsis =
    // AttributedString.stripAnsi(spec.usageMessage().sectionMap().get("synopsis").render(cmdhelp).toString());
    // main.add(Options.HelpException.highlightSyntax(synopsis.trim(), Options.HelpException.defaultStyle()));

    AttributedString attributedString = new AttributedString("test");
    main.add(attributedString);
    options.put("test", main);
    // for (OptionSpec o : spec.options()) {
    // String key = Arrays.stream(o.names()).collect(Collectors.joining(" "));
    // List<AttributedString> val = new ArrayList<>();
    // for (String d: o.description()) {
    // val.add(new AttributedString(d));
    // }
    // if (o.arity().max() > 0) {
    // key += "=" + o.paramLabel();
    // }
    // options.put(key, val);
    // }
    // return new CmdDesc(main, ArgDesc.doArgNames(Arrays.asList("")), options);
    // TODO: implement this
    return new CmdDesc(main, ArgDesc.doArgNames(Arrays.asList("description")), options);
  }
}
