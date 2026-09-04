package com.devonfw.tools.ide.expression;

import java.util.HashMap;
import java.util.Map;

import com.devonfw.tools.ide.expression.function.AskFunction;
import com.devonfw.tools.ide.expression.function.IfOsFunction;
import com.devonfw.tools.ide.expression.function.PathFunction;

/**
 * Manager where all {@link ExpressionFunction}s are registered so they can be looked up by
 * {@link #getFunction(String) name} while an expression is resolved.
 * <p>
 * With new IDEasy releases additional functions can simply be registered here.
 */
public class ExpressionFunctionManager {

  private static final ExpressionFunctionManager DEFAULT = createDefault();

  private final Map<String, ExpressionFunction> functions;

  /**
   * The constructor.
   */
  public ExpressionFunctionManager() {

    super();
    this.functions = new HashMap<>();
  }

  /**
   * @param function the {@link ExpressionFunction} to register.
   */
  public void register(ExpressionFunction function) {

    ExpressionFunction duplicate = this.functions.put(function.getName(), function);
    if (duplicate != null) {
      throw new IllegalStateException("Duplicate expression function @" + function.getName());
    }
  }

  /**
   * @param name the {@link ExpressionFunction#getName() name} of the requested function.
   * @return the {@link ExpressionFunction} or {@code null} if no function is registered for the given name. A
   *     {@code null} result is not an error: the expression is then left untouched.
   */
  public ExpressionFunction getFunction(String name) {

    return this.functions.get(name);
  }

  /**
   * @return the default instance with all standard functions registered.
   */
  public static ExpressionFunctionManager get() {

    return DEFAULT;
  }

  private static ExpressionFunctionManager createDefault() {

    ExpressionFunctionManager manager = new ExpressionFunctionManager();
    manager.register(new PathFunction());
    manager.register(AskFunction.ofVariable());
    manager.register(AskFunction.ofSecret());
    for (IfOsFunction function : IfOsFunction.all()) {
      manager.register(function);
    }
    return manager;
  }

}
