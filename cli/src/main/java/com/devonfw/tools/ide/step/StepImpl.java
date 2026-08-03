package com.devonfw.tools.ide.step;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Regular implementation of {@link Step}.
 */
public final class StepImpl implements Step {

  private static final Logger LOG = LoggerFactory.getLogger(StepImpl.class);

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
      LOG.trace("Starting step {}...", name);
    } else {
      LOG.trace("Starting step {} with params {}...", name, Arrays.toString(params));
    }
    if (!this.silent) {
      IdeLogLevel.STEP.log(LOG, "Start: {}", name);
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
  public void close() {

    end(null, null, false, null, null);
  }

  private void end(Boolean newSuccess, Throwable error, boolean suppress, String message, Object[] args) {

    boolean firstCallOfEnd = (this.success == null);
    if (!firstCallOfEnd) {
      assert (this.duration > 0);
      if ((newSuccess != null) && (newSuccess != this.success)) {
        LOG.warn("Step '{}' already ended with {} and now ended again with {}.", this.name, this.success, newSuccess);
      } else {
        return;
      }
    }
    long delay = System.currentTimeMillis() - this.start;
    if (delay == 0) {
      delay = 1;
    }
    if (newSuccess == null) {
      newSuccess = Boolean.FALSE;
    }
    if (!Boolean.FALSE.equals(this.success)) { // never allow a failed step to change to success
      this.duration = delay;
      this.success = newSuccess;
    }
    if (newSuccess.booleanValue()) {
      assert (error == null);
      if (message != null) {
        IdeLogLevel.SUCCESS.log(LOG, message, args);
      } else if (!this.silent) {
        IdeLogLevel.SUCCESS.log(LOG, "Successfully ended step '{}'.", this.name);
      }
      LOG.debug("Step '{}' ended successfully.", this.name);
    } else {
      Level level;
      if ((message != null) || (error != null)) {
        if (suppress) {
          if (error != null) {
            this.errorMessage = error.toString();
          } else {
            this.errorMessage = message;
          }
          level = Level.DEBUG;
        } else {
          if (message == null) {
            message = error.getMessage();
          }
          if (args == null) {
            LOG.atError().setCause(error).log(message);
          } else {
            LOG.atError().setCause(error).log(message, args);
          }
          if (error == null) {
            level = Level.DEBUG;
          } else {
            level = Level.ERROR;
          }
        }
      } else {
        level = Level.INFO;
      }
      LOG.atLevel(level).log("Step '{}' ended with failure.", this.name);
    }
    if (firstCallOfEnd) {
      this.context.endStep(this);
    }
  }

  /**
   * Logs the summary of this {@link Step}. Should typically only be called on the top-level {@link Step}.
   *
   * @param suppressSuccess - {@code true} to suppress the success message, {@code false} otherwise.
   */
  public void logSummary(boolean suppressSuccess) {

    if (LOG.isTraceEnabled()) {
      LOG.trace(toString());
    }
    if (this.context.isQuietMode() || (this.children.isEmpty())) {
      return;
    }
    StepSummary summary = new StepSummary();
    logErrorSummary(0, summary);
    if (summary.getError() == 0) {
      if (!suppressSuccess) {
        IdeLogLevel.SUCCESS.log(LOG, "Successfully completed {}", getNameWithParams());
      }
    } else {
      LOG.error(summary.toString());
    }
  }

  private void logErrorSummary(int depth, StepSummary summary) {

    boolean failure = isFailure();
    summary.add(failure);
    if (failure) {
      String error = this.errorMessage;
      if (error == null) {
        error = "unexpected error";
      }
      LOG.error("{}Step '{}' failed: {}", getIndent(depth), getNameWithParams(), error);
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
