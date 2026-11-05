package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.commandlet.AbstractCommandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractToolCommandlet;

/**
 * {@link StringProperty} for tool arguments with support for tool-specific auto-completion.
 */
public class ToolArgumentsProperty extends StringProperty {


  /**
   * The constructor.
   *
   * @param name the name of this property.
   * @param required {@code true} if this property is required.
   * @param multivalued {@code true} if multiple values are allowed.
   * @param alias alias
   */
  public ToolArgumentsProperty(String name, boolean required, boolean multivalued, String alias) {
    super(name, required, multivalued, alias);
  }


  /**
   * Completes a tool argument by delegating to the owning {@link AbstractToolCommandlet}.
   *
   * @param arg the current argument to complete.
   * @param context the {@link IdeContext}.
   * @param commandlet the owning {@link AbstractCommandlet}.
   * @param collector the {@link CompletionCandidateCollector}.
   */
  @Override
  protected void completeValue(String arg, IdeContext context, AbstractCommandlet commandlet, CompletionCandidateCollector collector) {

    if (commandlet instanceof AbstractToolCommandlet toolCommandlet) {
      toolCommandlet.completeToolArguments(arg, collector, this);
    }

  }
}
