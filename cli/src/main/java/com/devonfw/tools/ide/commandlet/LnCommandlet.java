package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.PathLinkType;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.PathProperty;

/**
 * // * Link creation {@link Commandlet} similar to {@code ln -s}.
 * <p>
 * It tries to create a true symbolic link first. On Windows, symlink creation may be restricted due to missing privileges. In that case, IDEasy will create a
 * hard link as an alternative (file-only, same volume) to avoid the Git-Bash behavior of silently copying files.
 */
public final class LnCommandlet extends Commandlet {

  /** Grammar token {@code -s} (optional). */
  public final FlagProperty symbolic;

  /** Grammar token {@code -r} (optional). */
  public final FlagProperty relative;

  /** The source path to link to. */
  public final PathProperty source;

  /** The target path (the created link). */
  public final PathProperty link;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public LnCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());

    this.symbolic = add(new FlagProperty("--symbolic", false, "-s"));
    this.relative = add(new FlagProperty("--relative", false, "-r"));
    this.source = add(new PathProperty("", true, "source", true));
    this.link = add(new PathProperty("", true, "link", false));
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

    Path sourcePath = cwd.resolve(this.source.getValue()).normalize();
    Path linkPath = cwd.resolve(this.link.getValue()).normalize();
    boolean relative = this.relative.isTrue();

    if (!Files.exists(sourcePath)) {
      throw new CliException("Source does not exist: " + sourcePath);
    }

    PathLinkType linkType = this.symbolic.isTrue() ? PathLinkType.SYMBOLIC_LINK : PathLinkType.HARD_LINK;
    this.context.getFileAccess().link(sourcePath, linkPath, relative, linkType);
  }
}
