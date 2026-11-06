package com.devonfw.tools.ide.expression.ask;

/**
 * {@link AskFunction} for a secret value.
 */
public class AskSecretFunction extends AskFunction {

  @Override
  public String getName() {

    return "ask-secret";
  }

  @Override
  protected boolean isSecret() {

    return true;
  }
}
