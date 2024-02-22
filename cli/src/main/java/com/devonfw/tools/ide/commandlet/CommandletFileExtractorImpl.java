package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.TarCompression;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.util.FilenameUtil;

/**
 * Implements common {@link CommandletFileExtractor} for a {@link ToolCommandlet}
 */
public class CommandletFileExtractorImpl implements CommandletFileExtractor {

  protected final IdeContext context;

  protected final ToolCommandlet commandlet;

  public CommandletFileExtractorImpl(IdeContext context, ToolCommandlet commandlet) {

    this.context = context;
    this.commandlet = commandlet;
  }

  @Override
  public void extract(Path file, Path targetDir, boolean isExtract) {

    FileAccess fileAccess = this.context.getFileAccess();
    if (isExtract) {
      Path tmpDir = this.context.getFileAccess().createTempDir("extract-" + file.getFileName());
      this.context.trace("Trying to extract the downloaded file {} to {} and move it to {}.", file, tmpDir, targetDir);
      String extension = FilenameUtil.getExtension(file.getFileName().toString());
      this.context.trace("Determined file extension {}", extension);
      TarCompression tarCompression = TarCompression.of(extension);
      if (tarCompression != null) {
        fileAccess.untar(file, tmpDir, tarCompression);
      } else if ("zip".equals(extension) || "jar".equals(extension)) {
        fileAccess.unzip(file, tmpDir);
      } else if ("dmg".equals(extension)) {
        assert this.context.getSystemInfo().isMac();
        Path mountPath = this.context.getIdeHome().resolve(IdeContext.FOLDER_UPDATES).resolve(IdeContext.FOLDER_VOLUME);
        fileAccess.mkdirs(mountPath);
        ProcessContext pc = this.context.newProcess();
        pc.executable("hdiutil");
        pc.addArgs("attach", "-quiet", "-nobrowse", "-mountpoint", mountPath, file);
        pc.run();
        Path appPath = fileAccess.findFirst(mountPath, p -> p.getFileName().toString().endsWith(".app"), false);
        if (appPath == null) {
          throw new IllegalStateException("Failed to unpack DMG as no MacOS *.app was found in file " + file);
        }
        fileAccess.copy(appPath, tmpDir);
        pc.addArgs("detach", "-force", mountPath);
        pc.run();
      } else if ("msi".equals(extension)) {
        this.context.newProcess().executable("msiexec").addArgs("/a", file, "/qn", "TARGETDIR=" + tmpDir).run();
        // msiexec also creates a copy of the MSI
        Path msiCopy = tmpDir.resolve(file.getFileName());
        fileAccess.delete(msiCopy);
      } else if ("pkg".equals(extension)) {

        Path tmpDirPkg = fileAccess.createTempDir("ide-pkg-");
        ProcessContext pc = this.context.newProcess();
        // we might also be able to use cpio from commons-compression instead of external xar...
        pc.executable("xar").addArgs("-C", tmpDirPkg, "-xf", file).run();
        Path contentPath = fileAccess.findFirst(tmpDirPkg, p -> p.getFileName().toString().equals("Payload"), true);
        fileAccess.untar(contentPath, tmpDir, TarCompression.GZ);
        fileAccess.delete(tmpDirPkg);
      } else {
        throw new IllegalStateException("Unknown archive format " + extension + ". Can not extract " + file);
      }
      moveAndProcessExtraction(getProperInstallationSubDirOf(tmpDir), targetDir);
      fileAccess.delete(tmpDir);
    } else {
      this.context.trace("Extraction is disabled for '{}' hence just moving the downloaded file {}.",
          commandlet.getName(), file);

      if (Files.isDirectory(file)) {
        fileAccess.move(file, targetDir);
      } else {
        try {
          Files.createDirectories(targetDir);
        } catch (IOException e) {
          throw new IllegalStateException("Failed to create folder " + targetDir);
        }
        fileAccess.move(file, targetDir.resolve(file.getFileName()));
      }
    }
  }

  @Override
  public void moveAndProcessExtraction(Path from, Path to) {

    this.context.getFileAccess().move(from, to);
  }

  /**
   * @param path the {@link Path} to start the recursive search from.
   * @return the deepest subdir {@code s} of the passed path such that all directories between {@code s} and the passed
   *         path (including {@code s}) are the sole item in their respective directory and {@code s} is not named
   *         "bin".
   */
  private Path getProperInstallationSubDirOf(Path path) {

    try (Stream<Path> stream = Files.list(path)) {
      Path[] subFiles = stream.toArray(Path[]::new);
      if (subFiles.length == 0) {
        throw new CliException("The downloaded package for the tool " + commandlet.getName()
            + " seems to be empty as you can check in the extracted folder " + path);
      } else if (subFiles.length == 1) {
        String filename = subFiles[0].getFileName().toString();
        if (!filename.equals(IdeContext.FOLDER_BIN) && !filename.equals(IdeContext.FOLDER_CONTENTS)
            && !filename.endsWith(".app") && Files.isDirectory(subFiles[0])) {
          return getProperInstallationSubDirOf(subFiles[0]);
        }
      }
      return path;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to get sub-files of " + path);
    }
  }

}
