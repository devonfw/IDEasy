package com.devonfw.tools.ide.commandlet;

import java.util.List;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.StringProperty;

/**
 * {@link Commandlet} for auto-completion.
 */
public final class CompleteCommandlet extends Commandlet {

  /** The name of the complete commandlet. */
  public static final String NAME = "complete";

  /** {@link StringProperty} with the current CLI arguments to complete. */
  public final StringProperty args;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CompleteCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.args = add(new StringProperty("", false, true, "args"));
  }

  @Override
  public String getName() {

    return NAME;
  }

  @Override
  public boolean isIdeRootRequired() {

    return false;
  }

  @Override
  public boolean isProcessableOutput() {

    return true;
  }

  @Override
  protected void doRun() {

    CliArguments arguments = CliArguments.ofCompletion(this.args.asArray());
    List<CompletionCandidate> candidates = ((AbstractIdeContext) this.context).complete(arguments, true);
    for (CompletionCandidate candidate : candidates) {
      System.out.print(candidate.text()); // checkstyle:ignore SystemOut - completion output must reach stdout even when logging is disabled
      System.out.print('\n'); // checkstyle:ignore SystemOut - raw newline (not println) avoids a CR char in bash completion on Windows
    }
  }
}
