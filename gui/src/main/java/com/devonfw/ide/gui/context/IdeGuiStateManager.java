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

/**
 * This class has the purpose of enabling the context state management for the IDEasy GUI. It is a thread-safe singleton implementation (Bill Pugh Singleton).
 */
public class IdeGuiStateManager {

  private static final Logger LOG = LoggerFactory.getLogger(IdeGuiStateManager.class);

  private Path ideRootDir;
  private ProjectManager projectManager;

  private final CopyOnWriteArrayList<GuiContextChangeListener> listeners = new CopyOnWriteArrayList<>();

  /**
   * Project context based on which project the user works in.
   */
  private volatile IdeGuiContext currentContext;

  /**
   * The {@link IdeStartContextImpl} for the GUI, this stays the same for the whole GUI session, only the {@link IdeGuiContext} changes.
   */
  private final IdeStartContextImpl startContext;

  private IdeGuiStateManager() {

    final IdeLogListenerBuffer buffer = new IdeLogListenerBuffer();
    IdeLogLevel logLevel = IdeLogLevel.DEBUG;
    startContext = new IdeStartContextImpl(logLevel, buffer);
  }

  /**
   * @return the singleton instance of the {@link IdeGuiStateManager}.
   */
  public static IdeGuiStateManager getInstance() {

    IdeGuiStateManager instance = Holder.INSTANCE;
    if (instance.ideRootDir == null) {
      String ideRoot = System.getenv("IDE_ROOT");
      if (ideRoot == null) {
        throw new IllegalStateException("IDE_ROOT environment variable is not set!");
      }
      instance.ideRootDir = Path.of(ideRoot);
      instance.projectManager = new ProjectManager(instance.ideRootDir);
    }
    return Holder.INSTANCE;
  }

  /**
   * <strong><u>USE WITH CARE.</u></strong>
   * This method is used in cases where the IDE_ROOT environment variable is not set, e.g. in test contexts on GitHub actions. This method will retrieve the
   * current instance, set the project directory manually an then return the updated instance.
   *
   * @param ideRoot root directory for the ide projects.
   * @return the singleton instance of the {@link IdeGuiStateManager}.
   */
  //TODO: remove this method once we have a better solution for the test context, after implementing CLi's test jar.
  public static IdeGuiStateManager getInstanceOverrideRootDir(String ideRoot) {
    LOG.warn("Using unsafe getInstanceOverrideRootDir method.");

    IdeGuiStateManager instance = Holder.INSTANCE;
    if (ideRoot == null) {
      throw new IllegalArgumentException("ideRoot must not be null!");
    } else if (instance.ideRootDir != null) {
      LOG.warn("ideRootDir is already set. You are overriding it.");
    }

    instance.ideRootDir = Path.of(ideRoot);
    instance.projectManager = new ProjectManager(instance.ideRootDir);
    return instance;
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

    this.currentContext = new IdeGuiContext(startContext, workspacePath);
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

  /**
   * Holder class for the singleton instance. The static keyword ensures the thread-safety of the singleton.
   *
   * @see <a href="https://www.baeldung.com/java-implement-thread-safe-singleton">More info</a>
   */
  private static class Holder {

    private static final IdeGuiStateManager INSTANCE = new IdeGuiStateManager();
  }
}
