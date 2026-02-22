package com.devonfw.tools.ide.commandlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
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

  private static final Logger LOG = LoggerFactory.getLogger(HelpCommandlet.class);

  /** The optional commandlet to get help about. */
  public final CommandletProperty commandlet;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public HelpCommandlet(IdeContext context) {

    super(context);
    addKeyword("--help", "-h");
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

  @Override
  public boolean isWriteLogFile() {

    return false;
  }

  @Override
  protected void doRun() {

    this.context.printLogo();
    NlsBundle bundle = NlsBundle.of(this.context);
    LOG.info(IdeLogLevel.SUCCESS.getSlf4jMarker(), bundle.get("version-banner"), IdeVersion.getVersionString());
    Commandlet cmd = this.commandlet.getValue();
    if (cmd == null) {
      String usage = bundle.get("usage") + " ide [option]* [[commandlet] [arg]*]";
      LOG.info(usage);
      LOG.info("");
      printCommandlets(bundle);
    } else {
      printCommandletHelp(bundle, cmd);
    }
    LOG.info("");
    LOG.info(bundle.get("options"));
    Args options = new Args();
    ContextCommandlet cxtCmd = new ContextCommandlet(((AbstractIdeContext) this.context).getStartContext());
    collectOptions(options, cxtCmd, bundle);
    if (cmd != null) {
      collectOptions(options, cmd, bundle);
    }
    options.print();
    if (cmd == null) {
      LOG.info("");
      LOG.info(bundle.getDetail(this.context.getCommandletManager().getCommandlet(HelpCommandlet.class)));
    }

    LOG.info("");
    LOG.info(bundle.get("icd-hint"));
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
    LOG.info(usage.toString());
    LOG.info(bundle.get(cmd));
    LOG.info(bundle.getDetail(cmd));
    LOG.info("");
    LOG.info(bundle.get("values"));
    values.print();
    cmd.printHelp(bundle);
  }

  private void printCommandlets(NlsBundle bundle) {

    Args commandlets = new Args();
    Args toolcommandlets = new Args();
    for (Commandlet cmd : this.context.getCommandletManager().getCommandlets()) {
      String key = cmd.getName();
      KeywordProperty keyword = cmd.getFirstKeyword();
      if (keyword != null) {
        String name = keyword.getName();
        if (!name.equals(key)) {
          key = key + "(" + keyword + ")";
        }
      }
      if (cmd instanceof ToolCommandlet) {
        toolcommandlets.add(key, bundle.get(cmd));
      } else {
        commandlets.add(key, bundle.get(cmd));
      }
    }

    LOG.info(bundle.get("commandlets"));
    commandlets.print(IdeLogLevel.INTERACTION);
    LOG.info("");
    LOG.info(bundle.get("toolcommandlets"));
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

      for (Arg arg : get()) {
        String message = format(arg);
        Level slf4jLevel = level.getSlf4jLevel();
        Marker marker = level.getSlf4jMarker();
        if (marker == null) {
          LOG.atLevel(slf4jLevel).log(message);
        } else {
          assert slf4jLevel == Level.INFO;
          LOG.info(marker, message);
        }
      }
    }

    public List<Arg> get() {

      Collections.sort(this.args);
      return this.args;
    }
  }
}
