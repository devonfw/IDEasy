package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.LocaleProperty;

/**
 * An internal pseudo-commandlet to create the {@link IdeContext}. It shall not be registered in
 * {@link CommandletManager}.
 */
public class ContextCommandlet extends Commandlet {

  private final FlagProperty batch;

  private final FlagProperty force;

  private final FlagProperty trace;

  private final FlagProperty debug;

  private final FlagProperty quiet;

  private final FlagProperty offline;

  private final LocaleProperty locale;

  private AbstractIdeContext ideContext;

  /**
   * The constructor.
   */
  public ContextCommandlet() {

    super(null);
    this.batch = add(new FlagProperty("--batch", false, "-b"));
    this.force = add(new FlagProperty("--force", false, "-f"));
    this.trace = add(new FlagProperty("--trace", false, "-t"));
    this.debug = add(new FlagProperty("--debug", false, "-d"));
    this.quiet = add(new FlagProperty("--quiet", false, "-q"));
    this.offline = add(new FlagProperty("--offline", false, "-o"));
    this.locale = add(new LocaleProperty("--locale", false, null));
  }

  @Override
  public String getName() {

    return "context";
  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }

  @Override
  public void run() {

    IdeLogLevel logLevel = IdeLogLevel.INFO;
    if (this.trace.isTrue()) {
      logLevel = IdeLogLevel.TRACE;
    } else if (this.debug.isTrue()) {
      logLevel = IdeLogLevel.DEBUG;
    } else if (this.quiet.isTrue()) {
      logLevel = IdeLogLevel.WARNING;
    }
    this.ideContext = new IdeContextConsole(logLevel, null, true);
    this.ideContext.setBatchMode(this.batch.isTrue());
    this.ideContext.setForceMode(this.force.isTrue());
    this.ideContext.setQuietMode(this.quiet.isTrue());
    this.ideContext.setOfflineMode(this.offline.isTrue());
    this.ideContext.setLocale(this.locale.getValue());
  }

  /**
   * @return the {@link IdeContext} that has been created by {@link #run()}.
   */
  public AbstractIdeContext getIdeContext() {

    return this.ideContext;
  }

}
