package com.devonfw.tools.ide.commandlet;

import java.nio.file.AccessDeniedException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.StringProperty;

/**
 * // * Link creation {@link Commandlet} similar to {@code ln -s}.
 * <p>
 * It tries to create a true symbolic link first. On Windows, symlink creation may be restricted due to missing privileges. In that case, IDEasy will create a
 * hard link as an alternative (file-only, same volume) to avoid the Git-Bash behavior of silently copying files.
 */
public final class LnCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(LnCommandlet.class);

  /** Grammar token {@code -s} (required). */
  public final KeywordProperty symbolic;

  /** The source path to link to. */
  public final StringProperty source;

  /** The target path (the created link). */
  public final StringProperty target;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public LnCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());

    // Treat -s as grammar token
    this.symbolic = add(new KeywordProperty("-s", true, null));

    this.source = add(new StringProperty("", true, "source"));
    this.target = add(new StringProperty("", true, "target"));
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

    Path sourcePath = cwd.resolve(this.source.getValue()).normalize().toAbsolutePath();
    Path targetPath = cwd.resolve(this.target.getValue()).normalize().toAbsolutePath();

    if (!Files.exists(sourcePath)) {
      throw new CliException("Source does not exist: " + sourcePath);
    }
    if (Files.exists(targetPath)) {
      throw new CliException("Target already exists: " + targetPath);
    }

    // 1) Try true symlink first (desired behavior).
    try {
      Files.createSymbolicLink(targetPath, sourcePath);
      LOG.info("Created symbolic link {} -> {}", targetPath, sourcePath);
      return;
    } catch (Exception symlinkError) {

      // 2) If Windows blocks symlink creation due to missing privileges:
      //    create hard link as an alternative (never copy).
      if (this.context.getSystemInfo().isWindows() && isWindowsSymlinkPrivilegeProblem(symlinkError)) {
        createHardLink(sourcePath, targetPath, symlinkError);
        return;
      }

      // Otherwise: real failure
      throw new CliException("Failed to create symbolic link " + targetPath + " -> " + sourcePath, symlinkError);
    }
  }

  /**
   * Detects common Windows privilege failures for symlink creation.
   */
  private boolean isWindowsSymlinkPrivilegeProblem(Exception e) {

    if (e instanceof AccessDeniedException) {
      return true;
    }
    if (e instanceof FileSystemException fse) {
      String msg = fse.getMessage();
      if (msg != null) {
        String m = msg.toLowerCase();
        return m.contains("required privilege")
            || m.contains("privilege is not held")
            || m.contains("access is denied");
      }
    }
    Throwable c = e.getCause();
    while (c != null) {
      if (c instanceof AccessDeniedException) {
        return true;
      }
      c = c.getCause();
    }
    return false;
  }

  /**
   * Creates a hard link as an alternative when symbolic link creation is blocked on Windows.
   * <p>
   * Hard links work only for files and only within the same volume.
   */
  private void createHardLink(Path sourcePath, Path targetPath, Exception originalSymlinkError) {

    if (Files.isDirectory(sourcePath)) {
      throw new CliException(
          "Windows blocked symbolic link creation (missing privileges).\n"
              + "Hard link alternative is not possible because the source is a directory.",
          originalSymlinkError);
    }

    ensureSameVolume(sourcePath, targetPath);

    try {
      Files.createLink(targetPath, sourcePath);
      LOG.info("Created hard link {} => {}", targetPath, sourcePath);
      LOG.warn("NOTE: Created hard link as an alternative because Windows blocked symbolic link creation.");
    } catch (Exception e) {
      throw new CliException(
          "Hard link creation failed. Source and target must be on the same volume and filesystem must support hard links.\n"
              + "Source: " + sourcePath + "\nTarget: " + targetPath,
          e);
    }
  }

  private void ensureSameVolume(Path sourcePath, Path targetPath) {

    try {
      FileStore src = Files.getFileStore(sourcePath);
      Path parent = (targetPath.getParent() != null) ? targetPath.getParent() : targetPath;
      FileStore tgt = Files.getFileStore(parent);
      if (!src.equals(tgt)) {
        throw new CliException(
            "Hard link alternative not possible: source and target are on different volumes.\n"
                + "Source: " + sourcePath + "\nTarget: " + targetPath);
      }
    } catch (CliException e) {
      throw e;
    } catch (Exception e) {
      throw new CliException("Failed to check volume for hard link alternative.", e);
    }
  }

}
