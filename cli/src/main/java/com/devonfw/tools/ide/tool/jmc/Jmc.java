package com.devonfw.tools.ide.tool.jmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://www.oracle.com/java/technologies/jdk-mission-control.html">JDK Mission Control</a>, An advanced set of tools for
 * managing, monitoring, profiling, and troubleshooting Java applications.
 */
public class Jmc extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}. method.
   */
  public Jmc(IdeContext context) {

    super(context, "jmc", Set.of(Tag.JAVA, Tag.ANALYSE));
  }

  @Override
  public void run() {

    runTool(ProcessMode.BACKGROUND, null, this.arguments.asArray());
  }

  @Override
  protected void postExtract(Path extractedDir) {

    super.postExtract(extractedDir);
    if (this.context.getSystemInfo().isWindows() || this.context.getSystemInfo().isLinux()) {
      Path oldBinaryPath = extractedDir.resolve("JDK Mission Control");
      if (Files.isDirectory(oldBinaryPath)) {
        FileAccess fileAccess = this.context.getFileAccess();
        moveFilesAndDirs(oldBinaryPath, extractedDir);
        fileAccess.delete(oldBinaryPath);
      } else {
        this.context.debug(
            "JMC binary folder not found at {} - ignoring as this legacy problem may be resolved in newer versions.",
            oldBinaryPath);
      }
    }
  }

  private void moveFilesAndDirs(Path sourceFolder, Path targetFolder) {

    FileAccess fileAccess = this.context.getFileAccess();
    try (Stream<Path> childStream = Files.list(sourceFolder)) {
      Iterator<Path> iterator = childStream.iterator();
      while (iterator.hasNext()) {
        Path child = iterator.next();
        fileAccess.move(child, targetFolder.resolve(child.getFileName()));
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to list files to move in " + sourceFolder, e);
    }
  }

}
