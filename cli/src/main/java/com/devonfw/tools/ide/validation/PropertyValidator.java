package com.devonfw.tools.ide.validation;

@FunctionalInterface
public interface PropertyValidator<V> {

  void validate(V value, ValidationState validationState);

}
