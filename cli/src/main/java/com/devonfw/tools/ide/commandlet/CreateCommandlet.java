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

    Path newProjectPath = getNewProjectPath();
    LOG.info("Creating new IDEasy project in {}", newProjectPath);
    FileAccess fileAccess = this.context.getFileAccess();
    if (!fileAccess.isEmptyDir(newProjectPath)) {
      this.context.askToContinue("Directory {} already exists. Do you want to continue?", newProjectPath);
      fileAccess.backup(newProjectPath);
    }
    // point IDE_HOME to the new project before the settings are checked - this only computes the paths and creates nothing on disk so that a failing
    // health check leaves no project behind. As IDE_HOME/settings does not exist yet the settings will be cloned instead of pulled.
    this.context.setIdeHome(newProjectPath);
    super.doRun();
    this.context.getFileAccess().writeFileContent(IdeVersion.getVersionString(), newProjectPath.resolve(IdeContext.FILE_SOFTWARE_VERSION));
    IdeLogLevel.SUCCESS.log(LOG, "Successfully created new project '{}'.", this.newProject.getValue());
    logWelcomeMessage();
  }

  @Override
  protected void prepareProject() {

    // only called after the settings passed the health check
    Path newProjectPath = getNewProjectPath();
    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(newProjectPath);
    fileAccess.mkdirs(newProjectPath.resolve(IdeContext.FOLDER_SOFTWARE));
    fileAccess.mkdirs(newProjectPath.resolve(IdeContext.FOLDER_PLUGINS));
    fileAccess.mkdirs(newProjectPath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN));
  }

  private Path getNewProjectPath() {

    return this.context.getIdeRoot().resolve(this.newProject.getValue());
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
