package com.devonfw.ide.gui.context;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArrayList;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

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

  public static final String DEFAULT_WORKSPACE = "main";

  private final Path ideRootDir;
  private final ProjectManager projectManager;

  private final CopyOnWriteArrayList<GuiContextChangeListener> listeners = new CopyOnWriteArrayList<>();

  private final StringProperty selectedProject = new SimpleStringProperty();

  private final StringProperty selectedWorkspace = new SimpleStringProperty();

  private final BooleanProperty workspaceSelected = new SimpleBooleanProperty(false);

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

    this.selectedProject.set(projectName);
    this.selectedWorkspace.set(workspaceName);
    this.workspaceSelected.set(true);

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
   * @return the currently selected project name (app-wide, single selection).
   */
  public String getSelectedProject() {

    return this.selectedProject.get();
  }

  /**
   * @return the currently selected workspace name (app-wide, single selection).
   */
  public String getSelectedWorkspace() {

    return this.selectedWorkspace.get();
  }

  /**
   * @return the observable {@link StringProperty} holding the selected project (app-wide, single selection).
   */
  public StringProperty selectedProjectProperty() {

    return this.selectedProject;
  }

  /**
   * @return the observable {@link StringProperty} holding the selected workspace (app-wide, single selection).
   */
  public StringProperty selectedWorkspaceProperty() {

    return this.selectedWorkspace;
  }

  /**
   * @return whether a valid project/workspace selection is active.
   */
  public boolean isWorkspaceSelected() {

    return this.workspaceSelected.get();
  }

  /**
   * @return the observable {@link BooleanProperty} flagging whether a valid project/workspace selection is active.
   */
  public BooleanProperty workspaceSelectedProperty() {

    return this.workspaceSelected;
  }

  /**
   * @return the root directory of IDEasy used by the GUI.
   */
  public Path getIdeRootDir() {

    return this.ideRootDir;
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
