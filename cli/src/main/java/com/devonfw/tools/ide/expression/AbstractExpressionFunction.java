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
  public final String apply(List<Object> parameters, ExpressionContext context) {

    int paramCount = parameters.size();
    int paramIndex = 0;
    List<Property<?>> properties = getProperties();
    int propertyCount = properties.size();
    int propertyIndex = 0;
    Property<?> property = null;
    boolean done = false;
    while (!done) {
      boolean hasParam = paramIndex < paramCount;
      Object param = null;
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
        applyParameter(property, param, context, propertyCount);
      } else if (!hasParam) {
        if (property == null) {
          done = true;
        } else if (property.isRequired()) {
          if (property.isMultiValued() && (property.getValueCount() > 0)) {
            done = true;
          } else {
            throw throwParameterMismatch("Missing required argument #" + paramIndex + " for parameter " + property.getName());
          }
        }
      } else {
        throw throwParameterMismatch("Unknown argument #" + paramIndex);
      }
    }
    ValidationResult validationResult = validate();
    if (!validationResult.isValid()) {
      throw throwParameterMismatch(validationResult.getErrorMessage());
    }
    return execute(context);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void applyParameter(Property property, Object param, ExpressionContext context, int propertyCount) {
    if (param instanceof String) {
      property.setValueAsString(param.toString(), context.getIdeContext());
    } else {
      try {
        property.setValue(param);
      } catch (Exception e) {
        throw throwParameterMismatch("Invalid argument '" + param + "' for parameter " + property.getName(), e);
      }
    }
  }

  protected RuntimeException throwParameterMismatch(String reason) {
    return throwParameterMismatch(reason, null);
  }

  protected RuntimeException throwParameterMismatch(String reason, Throwable error) {
    StringBuilder message = new StringBuilder(64);
    message.append("Cannot call expression function ");
    appendFunctionSignature(message);
    message.append(" with ").append(getProperties().size()).append(" parameter(s):\n");
    message.append(reason);
    throw new CliException(message.toString(), error);
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

  protected abstract String execute(ExpressionContext context);
}
