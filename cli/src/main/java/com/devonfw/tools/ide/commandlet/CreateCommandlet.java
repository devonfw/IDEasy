package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.property.StringProperty;

import java.nio.file.Path;

/**
 * {@link Commandlet} to create a new IDEasy instance
 */
public class CreateCommandlet extends Commandlet {

  private final StringProperty newInstance;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CreateCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    newInstance = add(new StringProperty("", false, "newInstance"));
  }

  @Override
  public String getName() {

    return "create";
  }

  @Override
  public void run() {

    String newInstanceName = newInstance.getValue();
    Path newInstancePath;

    if (newInstanceName == null) {
      newInstancePath = this.context.getCwd();
    } else {
      newInstancePath = this.context.getIdeRoot().resolve(newInstanceName);
      this.context.getFileAccess().mkdirs(newInstancePath);
    }

    this.context.info("Creating new IDEasy instance in {}", newInstancePath);
    if (!this.context.getFileAccess().isEmptyDir(newInstancePath)) {
      this.context.askToContinue("Directory is not empty, continue?");
    }

    initializeInstance(newInstancePath);
    ProcessContext pc = this.context.newProcess().executable("ideasy");
    pc.addArgs("update");
    pc.directory(newInstancePath);
    if (pc.run() == ProcessResult.SUCCESS) {
      this.context.success("IDEasy Instance successfully created in {}", newInstancePath);
    } else {
      this.context.warning("Could not create IDEasy Instance.");
    }
  }

  private void initializeInstance(Path newInstancePath) {

    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_SOFTWARE));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_UPDATES));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_PLUGINS));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_WORKSPACES));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_SETTINGS));

  }
}
