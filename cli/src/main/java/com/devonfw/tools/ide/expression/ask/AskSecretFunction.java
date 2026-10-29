package com.devonfw.tools.ide.expression.ask;

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
