package com.devonfw.ide.gui.context;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListenerBuffer;

/**
 * This class has the purpose of enabling the context state management for the IDEasy GUI. It is a thread-safe singleton implementation (Bill Pugh Singleton).
 */
public class IdeGuiStateManager {

  private static final Logger LOG = LoggerFactory.getLogger(IdeGuiStateManager.class);

  private String projectDirectory;

  private ProjectManager projectManager;

  /**
   * Project context based on which project the user works in.
   */
  private IdeGuiContext currentContext;

  private IdeGuiStateManager() {

    this.projectDirectory = System.getenv("IDE_ROOT");

    if (this.projectDirectory == null) {
      LOG.warn("IDE_ROOT environment variable is not set! Any dependent operation will fail before switchContext is called");
    }

    this.projectManager = new ProjectManager(Path.of(projectDirectory));
  }

  /**
   * @return the singleton instance of the {@link IdeGuiStateManager}.
   */
  public static IdeGuiStateManager getInstance() {
    return Holder.INSTANCE;
  }

  /**
   * @param projectName name of the project folder
   * @param workspaceName name of the workspace folder
   * @return the new {@link IdeGuiContext} for the selected project and workspace.
   * @throws FileNotFoundException if workspace or project does not exist
   */
  public IdeGuiContext switchContext(String projectName, String workspaceName) throws FileNotFoundException {

    LOG.debug("Switching context to project {} and workspace {}", projectName, workspaceName);

    Path workspacePath = Path.of(projectDirectory, projectName, "workspaces", workspaceName);

    if (!workspacePath.toFile().exists()) {
      throw new FileNotFoundException("Workspace " + workspacePath + " does not exist!");
    }

    final IdeLogListenerBuffer buffer = new IdeLogListenerBuffer();
    IdeLogLevel logLevel = IdeLogLevel.DEBUG;
    IdeStartContextImpl startContext = new IdeStartContextImpl(logLevel, buffer);
    this.currentContext = new IdeGuiContext(startContext, workspacePath);
    return this.currentContext;
  }

  /**
   * This variant of the {@link #switchContext(String, String)} method is used when the IDE_ROOT environment variable has to be set manually. USE WITH CARE.
   * (e.g. in tests)
   *
   * @param rootDirectory root directory for the ide projects.
   * @param projectName 1st level folder of the project
   * @param workspaceName used workspace
   * @return the new {@link IdeGuiContext} for the selected project and workspace.
   * @throws FileNotFoundException id either the specified project folder or workspace does not exist.
   */
  public IdeGuiContext switchContext(Path rootDirectory, String projectName, String workspaceName) throws FileNotFoundException {
    
    this.projectDirectory = rootDirectory.toString();
    this.projectManager = new ProjectManager(rootDirectory);

    return switchContext(projectName, workspaceName);
  }

  /**
   * @return the current {@link IdeGuiContext} based on the selected project.
   */
  public IdeGuiContext getCurrentContext() {

    return this.currentContext;
  }

  public ProjectManager getProjectManager() {
    return projectManager;
  }

  /**
   * Holder class for the singleton instance. The static keyword ensures the thread-safety of the singleton.
   *
   * @see <a href="https://www.baeldung.com/java-implement-thread-safe-singleton">More info</a>
   */
  private static class Holder {

    private static final IdeGuiStateManager INSTANCE = new IdeGuiStateManager();
  }
}
