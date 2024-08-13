package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link BooleanProperty} for a keyword (e.g. "install" in "ide install mvn 3.9.4").
 */
public class KeywordProperty extends BooleanProperty {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public KeywordProperty(String name, boolean required, String alias) {

    super(name, required, alias);
    assert (!name.isEmpty() && isValue());
  }

  @Override
  public boolean isExpectValue() {

    return false;
  }

  @Override
  protected boolean applyValue(String argValue, boolean lookahead, CliArguments args, IdeContext context, Commandlet commandlet,
      CompletionCandidateCollector collector) {

    setValue(true);
    return true;
  }
}
