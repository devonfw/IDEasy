package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
   * @param validator the {@link Consumer} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public PathProperty(String name, boolean required, String alias, boolean mustExist, Consumer<Path> validator) {

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
  public boolean validate() {

    if (this.value != null) {
      if (Files.exists(getValue())) {
        if (isPathRequiredToBeFile() && !Files.isRegularFile(getValue())) {
          throw new IllegalStateException("Path " + this.value + " is not a file.");
        } else if (isPathRequiredToBeFolder() && !Files.isDirectory(getValue())) {
          throw new IllegalStateException("Path " + this.value + " is not a folder.");
        }
      } else if (isPathRequiredToExist()) {
        throw new IllegalStateException("Path " + this.value + " does not exist.");
      }
    }
    return super.validate();

  }

  /**
   * @return {@code true} if the {@link Path} {@link #getValue() value} must {@link Files#exists(Path, java.nio.file.LinkOption...) exist} if set, {@code false}
   * otherwise.
   */
  protected boolean isPathRequiredToExist() {

    return this.mustExist;
  }

  /**
   * @return {@code true} if the {@link Path} {@link #getValue() value} must be a {@link Files#isDirectory(Path, java.nio.file.LinkOption...) folder} if it
   * exists, {@code false} otherwise.
   */
  protected boolean isPathRequiredToBeFolder() {

    return false;
  }

  /**
   * @return {@code true} if the {@link Path} {@link #getValue() value} must be a {@link Files#isRegularFile(Path, java.nio.file.LinkOption...) file} if it
   * exists, {@code false} otherwise.
   */
  protected boolean isPathRequiredToBeFile() {

    return false;
  }

  @Override
  protected void completeValue(String arg, IdeContext context, Commandlet commandlet, CompletionCandidateCollector collector) {

    Path path = Path.of(arg);
    Path parent = path.getParent();
    String filename = path.getFileName().toString();
    if (Files.isDirectory(parent)) {
      try (Stream<Path> children = Files.list(parent)) {
        children.filter(child -> isValidPath(path, filename)).forEach(child -> collector.add(child.toString(), null, this, commandlet));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private boolean isValidPath(Path path, String filename) {

    if (isPathRequiredToBeFile() && !Files.isRegularFile(getValue())) {
      return false;
    } else if (isPathRequiredToBeFolder() && !Files.isDirectory(getValue())) {
      return false;
    }
    return path.getFileName().toString().startsWith(filename);
  }

}
