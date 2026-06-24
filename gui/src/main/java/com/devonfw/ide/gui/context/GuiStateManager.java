package com.devonfw.ide.gui.context;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListenerBuffer;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * This class has the purpose of enabling the context state management for the IDEasy GUI. It is a thread-safe singleton implementation (Bill Pugh Singleton).
 */
public class GuiStateManager {

  private static final Logger LOG = LoggerFactory.getLogger(GuiStateManager.class);

  private final Path ideRootDir;
  private final ProjectManager projectManager;

  private final CopyOnWriteArrayList<GuiContextChangeListener> listeners = new CopyOnWriteArrayList<>();

  /**
   * Project context based on which project the user works in.
   */
  private volatile IdeGuiContext currentContext;

  /**
   * The {@link IdeStartContextImpl} for the GUI, this stays the same for the whole GUI session, only the {@link IdeGuiContext} changes.
   */
  private final IdeStartContextImpl startContext;

  private final TaskManager taskManager;

  /**
   * @param taskManager the {@link TaskManager} that manages any running tasks in the GUI.
   * @param ideRoot the root directory of IDEasy. If <code>null</code>, the IDE_ROOT environment variable is used.
   */
  public GuiStateManager(TaskManager taskManager, String ideRoot) {

    this.taskManager = taskManager;
    this.ideRootDir = Path.of(ideRoot != null ? ideRoot : System.getenv(IdeVariables.IDE_ROOT.getName()));
    this.projectManager = new ProjectManager(ideRootDir);

    final IdeLogListenerBuffer buffer = new IdeLogListenerBuffer();
    IdeLogLevel logLevel = IdeLogLevel.DEBUG;
    startContext = new IdeStartContextImpl(logLevel, buffer);
  }

  /**
   * @param projectName name of the project folder
   * @param workspaceName name of the workspace folder
   * @return the new {@link IdeGuiContext} for the selected project and workspace.
   * @throws FileNotFoundException if workspace or project does not exist
   */
  public synchronized IdeGuiContext switchContext(String projectName, String workspaceName) throws FileNotFoundException {

    LOG.debug("Trying to switch context to project {} and workspace {}", projectName, workspaceName);

    Path projectPath = ideRootDir.resolve(projectName);
    Path workspacePath = projectPath.resolve("workspaces").resolve(workspaceName);

    if (!Files.exists(projectPath)) {
      throw new FileNotFoundException("Project " + projectPath + " does not exist!");
    } else if (!Files.exists(workspacePath)) {
      throw new FileNotFoundException("Workspace " + workspacePath + " does not exist!");
    }

    this.currentContext = new IdeGuiContext(startContext, workspacePath, taskManager);
    listeners.forEach(listener -> listener.onContextChange(this.currentContext));

    return this.currentContext;
  }

  /**
   * @return the current {@link IdeGuiContext} based on the selected project. is <code>null</code>, if no context has been set via switchContext.
   */
  public IdeGuiContext getCurrentContext() {

    return this.currentContext;
  }

  /**
   * @return instance of {@link ProjectManager}
   */
  public ProjectManager getProjectManager() {

    return projectManager;
  }

  /**
   * @return instance of {@link TaskManager}
   */
  public TaskManager getTaskManager() {

    return taskManager;
  }

  /**
   * Add a listener to the context change events.
   *
   * @param listener the {@link GuiContextChangeListener} to attach to context updates.
   */
  public void addGuiContextChangeListener(GuiContextChangeListener listener) {
    listeners.add(listener);
  }

  /**
   * Remove a listener from the context change events.
   *
   * @param listener the {@link GuiContextChangeListener} to remove from context updates.
   */
  public void removeGuiContextChangeListener(GuiContextChangeListener listener) {
    listeners.remove(listener);
  }
}
