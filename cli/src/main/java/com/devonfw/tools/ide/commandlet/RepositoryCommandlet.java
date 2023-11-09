package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.FolderProperty;
import com.devonfw.tools.ide.property.PathProperty;
import com.devonfw.tools.ide.property.StringProperty;

import java.nio.file.Files;
import java.nio.file.Path;

public class RepositoryCommandlet extends Commandlet {


  private StringProperty setup;

  private PathProperty project;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public RepositoryCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.setup = add(new StringProperty("setup", true, null));
    this.project = add(new PathProperty("", false, "project"));
  }

  @Override
  public String getName() {

    return "repository";
  }

  @Override
  public void run() {

    Path repositoriesPath = this.context.getSettingsPath().resolve("repositories");
    Path legacyRepositoriesPath = this.context.getSettingsPath().resolve("projects");

    if (project != null) {
      Path projectFile = project.getValue();
      if (!Files.exists(projectFile)) {
        projectFile = repositoriesPath.resolve(projectFile);
      }
      if (!Files.exists(projectFile)) {
        Path legacyProjectFile = legacyRepositoriesPath.resolve(projectFile);
        if (Files.exists(legacyProjectFile)) {
          projectFile = legacyProjectFile;
        } else {
          this.context.warning("Could not find " + projectFile);
          return;
        }
      }
      doImportProject(projectFile, true);
    } else {
      //if no project was given, check whether repositoriesPath exists, if not check the legacy repositoriesPath, if not return.
      Path repositories = Files.exists(repositoriesPath) ? repositoriesPath : Files.exists(legacyRepositoriesPath) ? legacyRepositoriesPath : null;
      if (repositories == null) return;

      //iterate through repositories and import all active projects
    }
  }

  private void doImportProject(Path projectFile, boolean force) {



  }
}
