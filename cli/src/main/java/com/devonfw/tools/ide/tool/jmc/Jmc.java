package com.devonfw.tools.ide.tool.jmc;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;

/**
 * {@link ToolCommandlet} for <a href="https://www.oracle.com/java/technologies/jdk-mission-control.html">JDK Mission
 * Control</a>, An advanced set of tools for managing, monitoring, profiling, and troubleshooting Java applications.
 */
public class Jmc extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}. method.
   */
  public Jmc(IdeContext context) {

    super(context, "jmc", Set.of(Tag.JAVA, Tag.QA, Tag.ANALYSE, Tag.JVM));
  }

  @Override
  public boolean doInstall(boolean silent) {

    getCommandlet(Java.class).install();
    return super.doInstall(silent);
  }

  @Override
  public void run() {

    runTool(true, null, this.arguments.asArray());
  }

  @Override
  public void postInstall() {

    super.postInstall();

    if (context.getSystemInfo().isWindows() || context.getSystemInfo().isLinux()) {
      Path toolPath = getToolPath();
      Path oldBinaryPath = toolPath.resolve("JDK Mission Control");
      FileAccess fileAccess = context.getFileAccess();

      moveFilesAndDirs(oldBinaryPath.toFile(), toolPath.toFile());
      fileAccess.delete(oldBinaryPath);
    }

  }

  private void moveFilesAndDirs(File oldBinaryDir, File toolPathDir) {

    FileAccess fileAccess = context.getFileAccess();
    for (File fileOrDir : oldBinaryDir.listFiles()) {
      fileAccess.move(fileOrDir.toPath(), new File(toolPathDir, fileOrDir.getName()).toPath());
    }
  }

}