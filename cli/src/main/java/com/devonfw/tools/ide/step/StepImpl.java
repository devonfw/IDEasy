package com.devonfw.tools.ide.step;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * Regular implementation of {@link Step}.
 */
public class StepImpl implements Step {

  private final AbstractIdeContext context;

  private final StepImpl parent;

  private final String name;

  private final Object[] params;

  private final List<StepImpl> children;

  private final long start;

  private Boolean success;

  private long duration;

  /**
   * Creates and starts a new {@link StepImpl}.
   *
   * @param context the {@link IdeContext}.
   * @param parent the {@link #getParent() parent step}.
   * @param name the {@link #getName() step name}.
   */
  public StepImpl(AbstractIdeContext context, StepImpl parent, String name) {

    this(context, parent, name, NO_PARAMS);
  }

  /**
   * Creates and starts a new {@link StepImpl}.
   *
   * @param context the {@link IdeContext}.
   * @param parent the {@link #getParent() parent step}.
   * @param name the {@link #getName() step name}.
   * @param params the parameters. Should have reasonable {@link Object#toString() string representations}.
   */
  public StepImpl(AbstractIdeContext context, StepImpl parent, String name, Object... params) {

    super();
    this.context = context;
    this.parent = parent;
    this.name = name;
    this.params = params;
    this.children = new ArrayList<>();
    this.start = System.currentTimeMillis();
    if (parent != null) {
      parent.children.add(this);
    }
    if (params.length == 0) {
      this.context.trace("Starting step {}...", name);
    } else {
      this.context.trace("Starting step {} with params {}...", name, params);
    }
    if (parent != null) {
      this.context.step("Start: {}", name);
    }
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
  public long getDuration() {

    return this.duration;
  }

  @Override
  public Boolean getSuccess() {

    return this.success;
  }

  @Override
  public void success() {

    end(Boolean.TRUE);
  }

  @Override
  public void end() {

    end(Boolean.FALSE);
  }

  private void end(Boolean newSuccess) {

    if (this.success != null) {
      assert (this.duration > 0);
      // if success() was already called and then end() is called, this is normal and OK
      assert (this.success.booleanValue() && !newSuccess.booleanValue());
      return;
    }
    assert (this.duration == 0);
    this.duration = System.currentTimeMillis() - this.start;
    this.success = newSuccess;
    if (newSuccess.booleanValue()) {
      this.context.success("Success: " + this.name);
    } else {
      this.context.error("Failed: " + this.name);
    }
    this.context.endStep(this);
  }

  @Override
  public StepImpl getParent() {

    return this.parent;
  }

  private void append(int depth, long totalDuration, long parentDuration, StringBuilder sb) {

    // indent
    sb.append(" ".repeat(depth));
    sb.append(this.name);
    sb.append('(');
    String seperator = "";
    if (this.params != null) {
      for (Object param : this.params) {
        sb.append(seperator);
        sb.append(param);
        seperator = ",";
      }
    }
    sb.append(") ");
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

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder(4096);
    append(0, this.duration, this.duration, sb);
    return sb.toString();
  }
}
