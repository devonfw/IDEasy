package com.devonfw.tools.ide.step;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeSubLogger;

/**
 * Regular implementation of {@link Step}.
 */
public final class StepImpl implements Step {

  private final AbstractIdeContext context;

  private final StepImpl parent;

  private final String name;

  private final Object[] params;

  private final List<StepImpl> children;

  private final long start;

  private final boolean silent;

  private Boolean success;

  private String errorMessage;

  private long duration;

  /**
   * Creates and starts a new {@link StepImpl}.
   *
   * @param context the {@link IdeContext}.
   * @param parent the {@link #getParent() parent step}.
   * @param name the {@link #getName() step name}.
   * @param silent the {@link #isSilent() silent flag}.
   * @param params the parameters. Should have reasonable {@link Object#toString() string representations}.
   */
  public StepImpl(AbstractIdeContext context, StepImpl parent, String name, boolean silent, Object... params) {

    super();
    this.context = context;
    this.parent = parent;
    this.name = name;
    this.params = params;
    this.silent = silent;
    this.children = new ArrayList<>();
    this.start = System.currentTimeMillis();
    if (parent != null) {
      parent.children.add(this);
    }
    if (params.length == 0) {
      this.context.trace("Starting step {}...", name);
    } else {
      this.context.trace("Starting step {} with params {}...", name, Arrays.toString(params));
    }
    if (!this.silent) {
      this.context.step("Start: {}", name);
    }
  }

  @Override
  public StepImpl getParent() {

    return this.parent;
  }

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  public Object getParameter(int i) {

    if ((i < 0) || (i >= this.params.length)) {
      return null;
    }
    return this.params[i];
  }

  @Override
  public int getParameterCount() {

    return this.params.length;
  }

  @Override
  public boolean isSilent() {

    return this.silent;
  }

  @Override
  public long getDuration() {

    return this.duration;
  }

  @Override
  public Boolean getSuccess() {

    return this.success;
  }

  @Override
  public void success(String message, Object... args) {

    end(Boolean.TRUE, null, false, message, args);
  }

  @Override
  public void error(Throwable error, boolean suppress, String message, Object... args) {

    end(Boolean.FALSE, error, suppress, message, args);
  }

  @Override
  public void end() {

    end(null, null, false, null, null);
  }

  private void end(Boolean newSuccess, Throwable error, boolean suppress, String message, Object[] args) {

    if (this.success != null) {
      assert (this.duration > 0);
      // success or error may only be called once per Step, while end() will be called again in finally block
      assert (newSuccess == null) : "Step " + this.name + " already ended with " + this.success
          + " and cannot be ended again with " + newSuccess;
      return;
    }
    assert (this.duration == 0);
    long delay = System.currentTimeMillis() - this.start;
    if (delay == 0) {
      delay = 1;
    }
    this.duration = delay;
    if (newSuccess == null) {
      newSuccess = Boolean.FALSE;
    }
    this.success = newSuccess;
    if (newSuccess.booleanValue()) {
      assert (error == null);
      if (message != null) {
        this.context.success(message, args);
      } else if (!this.silent) {
        this.context.success(this.name);
      }
      this.context.debug("Step '{}' ended successfully.", this.name);
    } else {
      IdeSubLogger logger;
      if ((message != null) || (error != null)) {
        if (suppress) {
          if (error != null) {
            this.errorMessage = error.toString();
          } else {
            this.errorMessage = message;
          }
        } else {
          this.errorMessage = this.context.error().log(error, message, args);
        }
        logger = this.context.debug();
      } else {
        logger = this.context.info();
      }
      logger.log("Step '{}' ended with failure.", this.name);
    }
    this.context.endStep(this);
  }

  /**
   * Logs the summary of this {@link Step}. Should typically only be called on the top-level {@link Step}.
   *
   * @param suppressSuccess - {@code true} to suppress the success message, {@code false} otherwise.
   */
  public void logSummary(boolean suppressSuccess) {

    if (this.context.trace().isEnabled()) {
      this.context.trace(toString());
    }
    if (this.context.isQuietMode()) {
      return;
    }
    StepSummary summary = new StepSummary();
    logErrorSummary(0, summary);
    if (summary.getError() == 0) {
      if (!suppressSuccess) {
        this.context.success("Successfully completed {}", getNameWithParams());
      }
    } else {
      this.context.error(summary.toString());
    }
  }

  private void logErrorSummary(int depth, StepSummary summary) {

    boolean failure = isFailure();
    summary.add(failure);
    if (failure) {
      this.context.error("{}Step '{}' failed: {}", getIndent(depth), getNameWithParams(), this.errorMessage);
    }
    depth++;
    for (StepImpl child : this.children) {
      child.logErrorSummary(depth, summary);
    }
  }

  private String getNameWithParams() {

    if ((this.params == null) || (this.params.length == 0)) {
      return this.name;
    }
    StringBuilder sb = new StringBuilder(this.name.length() + 3 + this.params.length * 6);
    getNameWithParams(sb);
    return sb.toString();
  }

  private void getNameWithParams(StringBuilder sb) {

    sb.append(this.name);
    sb.append(" (");
    String seperator = "";
    if (this.params != null) {
      for (Object param : this.params) {
        sb.append(seperator);
        sb.append(param);
        seperator = ",";
      }
    }
    sb.append(')');
  }

  private void append(int depth, long totalDuration, long parentDuration, StringBuilder sb) {

    // indent
    sb.append(getIndent(depth));
    getNameWithParams(sb);
    sb.append(' ');
    if (this.success == null) {
      sb.append("is still running or was not properly ended due to programming error not using finally block ");
    } else {
      if (this.success.booleanValue()) {
        sb.append("succeeded after ");
      } else {
        sb.append("failed after ");
      }
      sb.append(Duration.ofMillis(this.duration));
    }
    if (this.duration < totalDuration) {
      sb.append(" ");
      double percentageBase = this.duration * 100;
      double totalPercentage = percentageBase / totalDuration;
      sb.append(totalPercentage);
      sb.append("% of total ");
      if (parentDuration < totalDuration) {
        double parentPercentage = percentageBase / parentDuration;
        sb.append(parentPercentage);
        sb.append("% of parent");
      }
    }
    sb.append('\n');
    int childDepth = depth + 1;
    for (StepImpl child : this.children) {
      child.append(childDepth, totalDuration, this.duration, sb);
    }
  }

  private String getIndent(int depth) {

    return " ".repeat(depth);
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder(4096);
    append(0, this.duration, this.duration, sb);
    return sb.toString();
  }
}
