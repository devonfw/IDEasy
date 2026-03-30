package com.devonfw.ide.gui;

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

  private final String projectDirectory;

  /**
   * Project context based on which project the user works in.
   */
  private IdeGuiContext currentContext;

  private IdeGuiStateManager() {
    this.projectDirectory = System.getenv("IDE_ROOT");
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
   * @return the current {@link IdeGuiContext} based on the selected project.
   */
  public IdeGuiContext getCurrentContext() {

    return this.currentContext;
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
