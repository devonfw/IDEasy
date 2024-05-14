package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.property.StringProperty;

import java.nio.file.Path;

/**
 * {@link Commandlet} to create a new IDEasy instance
 */
public class CreateCommandlet extends AbstractUpdateCommandlet {

  public final StringProperty newProject;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CreateCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    newProject = add(new StringProperty("", true, "project"));
    add(this.settingsRepo);
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

    String newProjectName = newProject.getValue();
    Path newProjectPath = this.context.getIdeRoot().resolve(newProjectName);

    this.context.info("Creating new IDEasy project in {}", newProjectPath);
    if (!this.context.getFileAccess().isEmptyDir(newProjectPath)) {
      this.context.askToContinue("Directory " + newProjectPath + " already exists. Do you want to continue?");
    } else {
      this.context.getFileAccess().mkdirs(newProjectPath);
    }

    initializeProject(newProjectPath);
    this.context.setIdeHome(newProjectPath);
    super.run();
    updateRepositories();
    this.context.success("Successfully created new project '{}'.", newProjectName);
  }

  private void initializeProject(Path newInstancePath) {

    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_SOFTWARE));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_PLUGINS));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN));
  }

  private void updateRepositories() {

    this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class).run();
  }
}
