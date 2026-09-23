package com.devonfw.tools.ide.git;

import java.nio.file.Path;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Common base for the {@code GitContext} test fixtures.
 */
public abstract class AbstractGitContextMock extends GitContextImpl {

  /**
   * @param context the {@link IdeContext context}.
   */
  protected AbstractGitContextMock(IdeContext context) {

    super(context);
  }

  @Override
  public Path findGitRequired() {

    return Path.of("git");
  }

  @Override
  public String determineTrackedRemote(Path repository) {

    return DEFAULT_REMOTE;
  }

  @Override
  public List<String> retrieveGitRemotes(Path repository) {

    return List.of();
  }

  @Override
  public void commit(Path repository, String message, boolean addAll) {

  }

  @Override
  public void tag(Path repository, String tagName, String message) {

  }

  @Override
  public void push(Path repository, boolean followTags) {

  }
}
