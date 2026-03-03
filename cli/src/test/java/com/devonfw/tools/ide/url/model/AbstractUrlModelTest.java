package com.devonfw.tools.ide.url.model;

import java.nio.file.Path;

import org.assertj.core.api.Assertions;

import com.devonfw.tools.ide.context.AbstractIdeTestContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;

/**
 * Abstract class for all tests of {@link UrlArtifact}s and {@link UrlRepository}.
 */
public abstract class AbstractUrlModelTest extends Assertions {

  /** {@link Path} to {@link com.devonfw.tools.ide.url.model.folder.UrlRepository} with test-data. */
  protected static final Path URLS_PATH = Path.of("src/test/resources/urls");

  /**
   * @return a new instance of {@link UrlRepository} for testing.
   */
  protected UrlRepository newRepo() {

    return UrlRepository.load(URLS_PATH);
  }

  /**
   * @return a new instance of {@link IdeContext} for testing with urls test-data.
   */
  protected IdeContext newContext() {

    AbstractIdeTestContext context = new IdeTestContext();
    context.setUrlsPath(URLS_PATH);
    return context;
  }

}
