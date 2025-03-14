package com.devonfw.tools.ide.url.updater;

import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;

/**
 * Interface for an updater of an {@link UrlEdition edition} of a {@link UrlTool tool}.
 */
public interface UrlUpdater {

  /**
   * Performs the update by scanning for new versions to add and potentially validating existing versions.
   *
   * @param urlRepository the {@link UrlRepository} where to perform the update.
   */
  void update(UrlRepository urlRepository);
}
