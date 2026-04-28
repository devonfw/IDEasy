package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.StringProperty;

/**
 * // * Link creation {@link Commandlet} similar to {@code ln -s}.
 * <p>
 * It tries to create a true symbolic link first. On Windows, symlink creation may be restricted due to missing privileges. In that case, IDEasy will create a
 * hard link as an alternative (file-only, same volume) to avoid the Git-Bash behavior of silently copying files.
 */
public final class LnCommandlet extends Commandlet {

  /** Grammar token {@code -s} (optional). */
  public final FlagProperty symbolic;

  /** The source path to link to. */
  public final StringProperty target;

  /** The target path (the created link). */
  public final StringProperty link;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public LnCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());

    // Treat -s as grammar token
    this.symbolic = add(new FlagProperty("--symbolic", false, "-s"));
    this.target = add(new StringProperty("", true, "source"));
    this.link = add(new StringProperty("", true, "link"));
  }

  @Override
  public String getName() {

    return "ln";
  }

  @Override
  public boolean isIdeRootRequired() {

    return false;
  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }

  @Override
  public boolean isWriteLogFile() {

    return false;
  }

  @Override
  protected void doRun() {

    Path cwd = this.context.getCwd();
    if (cwd == null) {
      throw new CliException("Missing current working directory!");
    }

    Path targetPath = cwd.resolve(this.target.getValue()).normalize().toAbsolutePath();
    Path linkPath = cwd.resolve(this.link.getValue()).normalize().toAbsolutePath();

    if (!Files.exists(targetPath)) {
      throw new CliException("Source does not exist: " + targetPath);
    }
    if (this.symbolic != null && this.symbolic.getValue() != null) {
      this.context.getFileAccess().symlink(targetPath, linkPath, false);
    } else {
      this.context.getFileAccess().hardlink(targetPath, linkPath);
    }
  }
}
