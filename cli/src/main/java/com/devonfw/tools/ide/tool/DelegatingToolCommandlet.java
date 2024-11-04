package com.devonfw.tools.ide.tool;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;

/**
 * {@link ToolCommandlet} that delegates to another ToolCommandlet.
 */
public abstract class DelegatingToolCommandlet<D extends ToolCommandlet> extends ToolCommandlet {

  private Class<D> delegateClass;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   * @param delegateClass the {@link ToolCommandlet}.
   */
  public DelegatingToolCommandlet(IdeContext context, String tool, Set<Tag> tags, Class<D> delegateClass) {

    super(context, tool, tags);
    this.delegateClass = delegateClass;
  }

  @Override
  public final boolean install(boolean silent, EnvironmentContext environmentContext) {
    return getCommandlet(delegateClass).install(silent, environmentContext);
  }

}
