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
    newInstance = add(new StringProperty("", true, "project"));
  }

  @Override
  public String getName() {

    return "create";
  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }

  @Override
  public void run() {

    String newProjectName = newInstance.getValue();
    Path newProjectPath = this.context.getIdeRoot().resolve(newProjectName);
    this.context.getFileAccess().mkdirs(newProjectPath);

    this.context.info("Creating new IDEasy project in {}", newProjectPath);
    if (this.context.getFileAccess().isEmptyDir(newProjectPath)) {
      this.context.askToContinue("Directory " + newProjectPath + " already exists. Do you want to continue?");
    } else {
      this.context.getFileAccess().mkdirs(newProjectPath);
    }

    initializeInstance(newProjectPath);
  }

  private void initializeInstance(Path newInstancePath) {

    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_SOFTWARE));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_PLUGINS));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN));
  }
}
