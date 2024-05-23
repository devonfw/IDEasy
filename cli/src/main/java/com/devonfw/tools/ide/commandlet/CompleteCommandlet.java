package com.devonfw.tools.ide.commandlet;

import java.util.List;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.StringListProperty;

/**
 * {@link Commandlet} for auto-completion.
 */
public final class CompleteCommandlet extends Commandlet {

  /** {@link StringListProperty} with the current CLI arguments to complete. */
  public final StringListProperty args;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CompleteCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.args = add(new StringListProperty("", false, "args"));
  }

  @Override
  public String getName() {

    return "complete";
  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }

  @Override
  public void run() {

    CliArguments arguments = CliArguments.ofCompletion(this.args.asArray());
    List<CompletionCandidate> candidates = ((AbstractIdeContext) this.context).complete(arguments, true);
    for (CompletionCandidate candidate : candidates) {
      System.out.println(candidate.text()); // enforce output via System.out even if logging is disabled
    }
  }
}
