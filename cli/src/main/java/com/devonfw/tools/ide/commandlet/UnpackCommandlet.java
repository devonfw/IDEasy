package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.PathProperty;
import com.devonfw.tools.ide.util.FilenameUtil;

/**
 * {@link Commandlet} to extract an archive file to a target directory.
 * <p>
 * Supports ZIP, TAR, TAR.GZ, TAR.BZ2, 7Z, JAR archives (cross-platform), as well as MSI (Windows) and DMG/PKG (Mac).
 * </p>
 */
public final class UnpackCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(UnpackCommandlet.class);

  /** The archive file to extract. */
  public final PathProperty archive;

  /** The target directory to extract into. If not specified, defaults to {@code <cwd>/<archive_name_without_extension>}. */
  public final PathProperty target;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UnpackCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());

    this.archive = add(new PathProperty("", true, "archive", true));
    this.target = add(new PathProperty("target", false, "target", false));
  }

  @Override
  public String getName() {

    return "unpack";
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

    Path archivePath = this.archive.getValue();
    if (!archivePath.isAbsolute()) {
      archivePath = cwd.resolve(archivePath).normalize();
    }

    Path targetDir = this.target.getValue();
    if (targetDir == null) {
      // Derive default target from archive filename without extension
      String targetName = FilenameUtil.getFilenameWithoutExtension(archivePath);
      targetDir = cwd.resolve(targetName);
    }
    if (!targetDir.isAbsolute()) {
      targetDir = cwd.resolve(targetDir).normalize();
    }

    LOG.info("Extracting {} to {}", archivePath, targetDir);
    this.context.getFileAccess().extract(archivePath, targetDir);
    LOG.info("Extraction completed successfully.");
  }
}
