package com.devonfw.ide.gui;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeStartContextImpl;

/**
 * This class has the purpose of enabling the context state management for the IDEasy GUI. It is a thread-safe singleton implementation (Bill Pugh Singleton).
 */
public class IdeGuiContextManager {

  private String projectDirectory;

  /**
   * Project context based on which project the user works in.
   */
  private IdeGuiContext currentContext;

  private IdeGuiContextManager() {
  }

  /**
   * @return the singleton instance of the {@link IdeGuiContextManager}.
   */
  public static IdeGuiContextManager getInstance() {

    return Holder.INSTANCE;
  }

  /**
   * @param projectName name of the project folder
   * @param workspaceName name of the workspace folder
   * @return the new {@link IdeGuiContext} for the selected project and workspace.
   * @throws FileNotFoundException if workspace or project does not exist
   */
  public IdeGuiContext switchContext(String projectName, String workspaceName) throws FileNotFoundException {

    Path workspacePath = Path.of(projectDirectory, projectName, workspaceName);

    if (!workspacePath.toFile().exists()) {
      throw new FileNotFoundException("Workspace " + workspacePath + " does not exist!");
    }

    IdeGuiContext newContext = new IdeGuiContext(IdeStartContextImpl.get(), workspacePath);
    this.currentContext = newContext;
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

    private static final IdeGuiContextManager INSTANCE = new IdeGuiContextManager();
  }
}
