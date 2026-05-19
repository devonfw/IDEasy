package com.devonfw.tools.ide.property;

import java.nio.file.Path;

import com.devonfw.tools.ide.validation.PropertyValidator;

/**
 * {@link PathProperty} for a file.
 */
public class FileProperty extends PathProperty {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param mustExist the {@link #isPathRequiredToExist() required to exist flag}.
   * @param placeholder whether this property is substituted by some value or literal
   * @param alias the {@link #getAlias() property alias}.
   */
  public FileProperty(String name, boolean required, String alias, boolean mustExist, boolean placeholder) {

    this(name, required, alias, mustExist, placeholder, null);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param mustExist the {@link #isPathRequiredToExist() required to exist flag}.
   * @param placeholder whether this property is substituted by some value or literal
   * @param validator the {@link PropertyValidator} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public FileProperty(String name, boolean required, String alias, boolean mustExist, boolean placeholder, PropertyValidator<Path> validator) {

    super(name, required, alias, mustExist, placeholder, validator);
  }

  @Override
  protected boolean isPathRequiredToBeFile() {

    return true;
  }

}
