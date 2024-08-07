package com.devonfw.tools.ide.commandlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.property.CommandletProperty;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * {@link Commandlet} to print the environment variables.
 */
public final class HelpCommandlet extends Commandlet {

  static final String LOGO = """
      __       ___ ___  ___
      ╲ ╲     |_ _|   ╲| __|__ _ ____ _
       > >     | || |) | _|/ _` (_-< || |
      /_/ ___ |___|___/|___╲__,_/__/╲_, |
         |___|                       |__/
      """.replace('╲', '\\');

  /** The optional commandlet to get help about. */
  public final CommandletProperty commandlet;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public HelpCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.commandlet = add(new CommandletProperty("", false, "commandlet"));
  }

  @Override
  public String getName() {

    return "help";
  }

  @Override
  public boolean isIdeRootRequired() {

    return false;
  }

  private void printLogo() {

    this.context.info(LOGO);
  }

  @Override
  public void run() {

    printLogo();
    NlsBundle bundle = NlsBundle.of(this.context);
    this.context.success(bundle.get("version-banner"), IdeVersion.get());
    Commandlet cmd = this.commandlet.getValue();
    if (cmd == null) {
      this.context.info(bundle.get("usage") + " ide [option]* [[commandlet] [arg]*]");
      this.context.info("");
      printCommandlets(bundle);
    } else {
      printCommandletHelp(bundle, cmd);
    }
    this.context.info("");
    this.context.info(bundle.get("options"));
    Args options = new Args();
    ContextCommandlet cxtCmd = new ContextCommandlet();
    collectOptions(options, cxtCmd, bundle);
    if (cmd != null) {
      collectOptions(options, cmd, bundle);
    }
    options.print();
    if (cmd == null) {
      this.context.info("");
      this.context.info(bundle.getDetail(this.context.getCommandletManager().getCommandlet(HelpCommandlet.class)));
    }
  }

  private void printCommandletHelp(NlsBundle bundle, Commandlet cmd) {

    StringBuilder usage = new StringBuilder();
    Args values = new Args();
    usage.append(bundle.get("usage"));
    usage.append(" ide [option]*");
    for (Property<?> property : cmd.getProperties()) {
      if (property.isValue() || property.isRequired()) {
        usage.append(" ");
        if (!property.isRequired()) {
          usage.append('[');
        }
        String name = property.getName();
        if (name.isEmpty()) {
          assert !(property instanceof KeywordProperty);
          String key = "<" + property.getAlias() + ">";
          usage.append(key);
          values.add(key, bundle.get(cmd, property));
        } else {
          usage.append(name);
        }
        if (property.isMultiValued()) {
          usage.append('*');
        }
        if (!property.isRequired()) {
          usage.append(']');
        }
      }
    }
    this.context.info(usage.toString());
    this.context.info(bundle.get(cmd));
    this.context.info(bundle.getDetail(cmd));
    this.context.info("");
    this.context.info(bundle.get("values"));
    values.print();
    cmd.printHelp(bundle);
  }

  private void printCommandlets(NlsBundle bundle) {

    Args commandlets = new Args();
    Args toolcommandlets = new Args();
    for (Commandlet cmd : this.context.getCommandletManager().getCommandlets()) {
      String key = cmd.getName();
      String keyword = cmd.getKeyword();
      if ((keyword != null) && !keyword.equals(key)) {
        key = key + "(" + keyword + ")";
      }
      if (cmd instanceof ToolCommandlet) {
        toolcommandlets.add(key, bundle.get(cmd));
      } else {
        commandlets.add(key, bundle.get(cmd));
      }
    }

    this.context.info(bundle.get("commandlets"));
    commandlets.print(IdeLogLevel.INTERACTION);
    this.context.info("");
    this.context.info(bundle.get("toolcommandlets"));
    toolcommandlets.print(IdeLogLevel.INTERACTION);
  }

  private void collectOptions(Args options, Commandlet cmd, NlsBundle bundle) {

    for (Property<?> property : cmd.getProperties()) {
      if (property.isOption() && !property.isRequired()) {
        String id = property.getAlias();
        String name = property.getName();
        if (id == null) {
          id = name;
        } else {
          id = id + " | " + name;
        }
        String description = bundle.get(cmd, property);
        options.add(id, description);
      }
    }
  }

  private static class Arg implements Comparable<Arg> {

    private final String key;

    private final String description;

    private Arg(String key, String description) {

      super();
      this.key = key;
      this.description = description;
    }

    @Override
    public int compareTo(Arg arg) {

      if (arg == null) {
        return 1;
      }
      return this.key.compareTo(arg.key);
    }
  }

  private class Args {

    private final List<Arg> args;

    private int maxKeyLength;

    private Args() {

      super();
      this.args = new ArrayList<>();
    }

    private void add(String key, String description) {

      add(new Arg(key, description));
    }

    private void add(Arg arg) {

      this.args.add(arg);
      int keyLength = arg.key.length();
      if (keyLength > this.maxKeyLength) {
        this.maxKeyLength = keyLength;
      }
    }

    String format(Arg arg) {

      StringBuilder sb = new StringBuilder(this.maxKeyLength + 2 + arg.description.length());
      sb.append(arg.key);
      int delta = this.maxKeyLength - arg.key.length();
      while (delta > 0) {
        sb.append(' ');
        delta--;
      }
      sb.append("  ");
      sb.append(arg.description);
      return sb.toString();
    }

    void print() {

      print(IdeLogLevel.INFO);
    }

    void print(IdeLogLevel level) {

      IdeSubLogger logger = HelpCommandlet.this.context.level(level);
      for (Arg arg : get()) {
        logger.log(format(arg));
      }
    }

    public List<Arg> get() {

      Collections.sort(this.args);
      return this.args;
    }
  }
}
