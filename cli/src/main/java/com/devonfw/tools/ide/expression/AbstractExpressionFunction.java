package com.devonfw.tools.ide.expression;

import java.util.List;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.function.AbstractParameterizedFunction;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.validation.ValidationResult;

/**
 * Abstract base implementation of {@link ExpressionFunction}.
 */
public abstract class AbstractExpressionFunction extends AbstractParameterizedFunction implements ExpressionFunction {

  @Override
  public final String apply(List<String> parameters, ExpressionContext context) {

    int paramCount = parameters.size();
    int paramIndex = 0;
    List<Property<?>> properties = getProperties();
    int propertyCount = properties.size();
    int propertyIndex = 0;
    Property<?> property = null;
    boolean done = false;
    while (!done) {
      boolean hasParam = paramIndex < paramCount;
      String param = null;
      if (hasParam) {
        param = parameters.get(paramIndex);
        paramIndex++;
      }
      boolean hasProperty = propertyIndex < propertyCount;
      if (hasProperty) {
        property = properties.get(propertyIndex);
        propertyIndex++;
      } else if (property != null) {
        if (property.isMultiValued()) {
          hasProperty = true;
        } else {
          property = null;
        }
      }
      if (hasParam && hasProperty) {
        property.setValueAsString(param, context.getIdeContext());
      } else if (!hasParam) {
        if (property == null) {
          done = true;
        } else if (property.isRequired()) {
          if (property.isMultiValued() && (property.getValueCount() > 0)) {
            done = true;
          } else {
            throw throwParameterMismatch(paramCount, "Missing required argument #" + paramIndex + " for parameter " + property.getName());
          }
        }
      } else {
        throw throwParameterMismatch(paramCount, "Unknown argument #" + paramIndex);
      }
    }
    ValidationResult validationResult = validate();
    if (!validationResult.isValid()) {
      throw throwParameterMismatch(paramCount, validationResult.getErrorMessage());
    }
    return execute(paramCount, context);
  }

  protected RuntimeException throwParameterMismatch(int parameterCount, String reason) {
    StringBuilder message = new StringBuilder(64);
    message.append("Cannot call expression function ");
    appendFunctionSignature(message);
    message.append(" with ").append(parameterCount).append(" parameter(s):\n");
    message.append(reason);
    throw new CliException(message.toString());
  }

  private void appendFunctionSignature(StringBuilder sb) {
    sb.append('@').append(getName()).append("(");
    String separator = "";
    for (Property<?> property : getProperties()) {
      sb.append(separator);
      sb.append(property.getValueType().getSimpleName());
      if (property.isMultiValued()) {
        sb.append("...");
      }
      sb.append(' ');
      sb.append(property.getName());
      if (property.isRequired()) {
        sb.append("*");
      }
      separator = ", ";
    }
    sb.append(')');
  }

  protected abstract String execute(int paramCount, ExpressionContext context);
}
