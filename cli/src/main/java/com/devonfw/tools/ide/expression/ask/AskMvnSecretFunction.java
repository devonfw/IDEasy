package com.devonfw.tools.ide.expression.ask;

import com.devonfw.tools.ide.expression.ExpressionContext;
import com.devonfw.tools.ide.tool.mvn.Mvn;

/**
 * {@link AskFunction} {@code @ask-mvn-secret} for a secret that is encrypted via {@link Mvn maven}. It asks like {@link AskSecretFunction} but returns only the
 * encrypted value, never the plain text. This also applies to an already defined variable, its value is encrypted then. Like for {@link AskSecretFunction} the
 * value is not persisted.
 * <p>
 * Example: {@code @ask-mvn-secret('MAVEN_REPO_PASSWORD', 'Please enter your repository password:')}
 */
public class AskMvnSecretFunction extends AskSecretFunction {

  @Override
  public String getName() {

    return "ask-mvn-secret";
  }

  @Override
  protected String convert(String value, ExpressionContext context) {

    if (value.isEmpty()) {
      return value; // nothing to encrypt, so we also do not need to install maven
    }
    Mvn mvn = context.getIdeContext().getCommandletManager().getCommandlet(Mvn.class);
    return mvn.encryptPassword(value);
  }
}
