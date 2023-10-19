package com.devonfw.tools.ide.commandlet;

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
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.utils.AttributedString;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.CommandletProperty;
import com.devonfw.tools.ide.property.Property;

public class CommandletRegistry implements CommandRegistry {

  private final IdeContext context;

  private final Set<String> commandlets;

  private final Map<String, String> aliasCommandlets;

  private class IdeCompleter extends ArgumentCompleter implements Completer {

    public IdeCompleter() {

      super(NullCompleter.INSTANCE);
    }

    @Override
    public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
      assert commandLine != null;
      assert candidates != null;
      String word = commandLine.word();
      List<String> words = commandLine.words();
      // TODO: implement rest of this
    }

    private void addCandidates(List<Candidate> candidates, Iterable<String> cands) {
      addCandidates(candidates, cands, "", "", true);
    }

    private void addCandidates(List<Candidate> candidates, Iterable<String> cands, String preFix, String postFix, boolean complete) {
      for (String s : cands) {
        candidates.add(new Candidate(AttributedString.stripAnsi(preFix + s + postFix), s, null, null, null, null, complete));
      }
    }
  }

  public CommandletRegistry(IdeContext context) {

    this.context = context;

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
    helpCommandlet.add(property);
    helpCommandlet.run();
    String description = "description";
//    out.addAll(Arrays.asList(description.split("\\r?\\n")));
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
    out.add(all, new IdeCompleter());
    return out;
  }

  // For JLine >= 3.16.0
  @Override
  public Object invoke(CommandRegistry.CommandSession session, String command, Object[] args) throws Exception {

    List<String> arguments = new ArrayList<>();
    arguments.add(command);
    arguments.addAll(Arrays.stream(args).map(Object::toString).collect(Collectors.toList()));
    // TODO: run our commandlet here

    context.getCommandletManager().getCommandlet(command).run();
    return null;
  }

  // @Override This method was removed in JLine 3.16.0; keep it in case this component is used with an older version of JLine
  public Object execute(CommandRegistry.CommandSession session, String command, String[] args) throws Exception {
    List<String> arguments = new ArrayList<>();
    arguments.add(command);
    arguments.addAll(Arrays.asList(args));
    // TODO: run our commandlet here
    context.getCommandletManager().getCommandlet(command).run();
    return null;
  }

  // @Override This method was removed in JLine 3.16.0; keep it in case this component is used with an older version of JLine
  public CmdDesc commandDescription(String command) {
    return null;
  }

  @Override
  public CmdDesc commandDescription(List<String> list) {

    List<AttributedString> main = new ArrayList<>();
    Map<String, List<AttributedString>> options = new HashMap<>();
    // TODO: implement this
    return new CmdDesc(main, ArgDesc.doArgNames(Arrays.asList("")), options);
  }
}
