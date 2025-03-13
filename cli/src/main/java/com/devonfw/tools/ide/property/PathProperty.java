package com.devonfw.tools.ide.property;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.validation.PropertyValidator;
import com.devonfw.tools.ide.validation.ValidationResult;
import com.devonfw.tools.ide.validation.ValidationState;

/**
 * {@link Property} with {@link Path} as {@link #getValueType() value type}.
 */
public class PathProperty extends Property<Path> {

  private final boolean mustExist;

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param mustExist the {@link #isPathRequiredToExist() required to exist flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public PathProperty(String name, boolean required, String alias, boolean mustExist) {

    this(name, required, alias, mustExist, null);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param mustExist the {@link #isPathRequiredToExist() required to exist flag}.
   * @param validator the {@link PropertyValidator} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public PathProperty(String name, boolean required, String alias, boolean mustExist, PropertyValidator<Path> validator) {

    super(name, required, alias, false, validator);
    this.mustExist = mustExist;
  }

  @Override
  public Class<Path> getValueType() {

    return Path.class;
  }

  @Override
  public Path parse(String valueAsString, IdeContext context) {

    return Path.of(valueAsString);
  }

  @Override
  public ValidationResult validate() {
    ValidationState state = new ValidationState(this.getNameOrAlias());
    for (Path path : this.value) {
      if (path != null && Files.exists(path)) {
        if (isPathRequiredToBeFile() && !Files.isRegularFile(path)) {
          state.addErrorMessage("Path " + path + " is not a file.");
        } else if (isPathRequiredToBeFolder() && !Files.isDirectory(path)) {
          state.addErrorMessage("Path " + path + " is not a folder.");
        }
      } else if (isPathRequiredToExist()) {
        state.addErrorMessage("Path " + path + " does not exist.");
      }
    }
    state.add(super.validate());
    return state;
  }

  /**
   * @return {@code true} if the {@link Path} {@link #getValue() value} must {@link Files#exists(Path, java.nio.file.LinkOption...) exist} if set, {@code false}
   *     otherwise.
   */
  protected boolean isPathRequiredToExist() {

    return this.mustExist;
  }

  /**
   * @return {@code true} if the {@link Path} {@link #getValue() value} must be a {@link Files#isDirectory(Path, java.nio.file.LinkOption...) folder} if it
   *     exists, {@code false} otherwise.
   */
  protected boolean isPathRequiredToBeFolder() {

    return false;
  }

  /**
   * @return {@code true} if the {@link Path} {@link #getValue() value} must be a {@link Files#isRegularFile(Path, java.nio.file.LinkOption...) file} if it
   *     exists, {@code false} otherwise.
   */
  protected boolean isPathRequiredToBeFile() {

    return false;
  }

  @Override
  protected void completeValue(String arg, IdeContext context, Commandlet commandlet, CompletionCandidateCollector collector) {

    Path path = Path.of(arg);
    Path parent = path.getParent();
    //set a default parent directory when unable to obtain the parent directory
    if (parent == null) {
      parent = Path.of(".");
    }
    String filename = path.getFileName().toString();
    completeValuesFromFolder(parent, filename, context, commandlet, collector);
  }

  /**
   * @param folder the {@link Path} to the directory where to search for the file.
   * @param filename the filename (prefix) to complete.
   * @param context the {@link IdeContext}.
   * @param commandlet the owning {@link Commandlet}.
   * @param collector the {@link CompletionCandidateCollector}.
   */
  protected void completeValuesFromFolder(Path folder, String filename, IdeContext context, Commandlet commandlet, CompletionCandidateCollector collector) {

    if (Files.isDirectory(folder)) {
      try (Stream<Path> children = Files.list(folder)) {
        children.filter(child -> isValidPath(child, filename))
            .forEach(child -> collector.add(getPathForCompletion(child, context, commandlet), null, this, commandlet));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  /**
   * @param path the {@link Path} that has been found via completion.
   * @param context the {@link IdeContext}.
   * @param commandlet the owning {@link Commandlet}.
   * @return the {@link String} to {@link CompletionCandidateCollector#add(String, String, Property, Commandlet) add} as completion candidate.
   */
  protected String getPathForCompletion(Path path, IdeContext context, Commandlet commandlet) {

    return path.toString();
  }

  private boolean isValidPath(Path path, String filename) {

    if (isPathRequiredToBeFile() && !Files.isRegularFile(path)) {
      return false; // isnt this wrong? How can I use completion to complete a file in a sub-folder?
    } else if (isPathRequiredToBeFolder() && !Files.isDirectory(path)) {
      return false;
    }
    return path.getFileName().toString().startsWith(filename);
  }

}
