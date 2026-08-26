package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.commandlet.update.AbstractUpdateCommandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * {@link Commandlet} to create a new IDEasy instance
 */
public class CreateCommandlet extends AbstractUpdateCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(CreateCommandlet.class);

  /** {@link StringProperty} for the name of the new project */
  public final StringProperty newProject;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CreateCommandlet(IdeContext context) {

    super(context);
    this.newProject = add(new StringProperty("", true, "project"));
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
  protected void doRun() {

    String newProjectName = this.newProject.getValue();
    Path newProjectPath = this.context.getIdeRoot().resolve(newProjectName);

    LOG.info("Creating new IDEasy project in {}", newProjectPath);
    if (!this.context.getFileAccess().isEmptyDir(newProjectPath)) {
      this.context.askToContinue("Directory {} already exists. Do you want to continue?", newProjectPath);
    }

    // First run the settings update (super.doRun()) to validate the settings repository
    // Only if that succeeds, we create the project structure
    try {
      super.doRun();
    } catch (Exception e) {
      // If settings update fails, clean up any temp directories and rethrow
      throw e;
    }

    // Settings update succeeded, now create the project structure
    if (!this.context.getFileAccess().isEmptyDir(newProjectPath)) {
      this.context.getFileAccess().backup(newProjectPath);
    }
    this.context.getFileAccess().mkdirs(newProjectPath);
    initializeProject(newProjectPath);
    this.context.setIdeHome(newProjectPath);
    this.context.getFileAccess().writeFileContent(IdeVersion.getVersionString(), newProjectPath.resolve(IdeContext.FILE_SOFTWARE_VERSION));
    IdeLogLevel.SUCCESS.log(LOG, "Successfully created new project '{}'.", newProjectName);

    logWelcomeMessage();
  }

  private void initializeProject(Path newInstancePath) {

    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_SOFTWARE));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_PLUGINS));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN));
  }

  @Override
  protected String getStepMessage() {

    return "Create (clone) repository";
  }

  private void logWelcomeMessage() {
    Path settingsFolder = this.context.getSettingsPath();
    if (Files.exists(settingsFolder)) {
      Predicate<Path> welcomePredicate = path -> String.valueOf(path.getFileName()).startsWith("welcome.");
      Path welcomeFilePath = this.context.getFileAccess().findFirst(settingsFolder, welcomePredicate, false);
      if (welcomeFilePath != null) {
        LOG.info(this.context.getFileAccess().readFileContent(welcomeFilePath));
      }
    }
  }
}
