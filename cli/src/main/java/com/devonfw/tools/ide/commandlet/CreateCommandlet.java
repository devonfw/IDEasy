package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * {@link Commandlet} to create a new IDEasy instance
 */
public class CreateCommandlet extends AbstractUpdateCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(CreateCommandlet.class);

  /** {@link StringProperty} for the name of the new project */
  public final StringProperty newProject;

  /** {@link FlagProperty} for creating a project with settings inside a code repository */
  public final FlagProperty codeRepositoryFlag;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CreateCommandlet(IdeContext context) {

    super(context);
    this.newProject = add(new StringProperty("", true, "project"));
    this.codeRepositoryFlag = add(new FlagProperty("--code"));
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
    } else {
      this.context.getFileAccess().mkdirs(newProjectPath);
    }

    initializeProject(newProjectPath);
    this.context.setIdeHome(newProjectPath);
    this.context.verifyIdeMinVersion(true);
    super.doRun();
    this.context.verifyIdeMinVersion(true);
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
  protected boolean isCodeRepository() {
    return this.codeRepositoryFlag.isTrue();
  }

  @Override
  protected String getStepMessage() {

    return "Create (clone) " + (isCodeRepository() ? "code" : "settings") + " repository";
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
