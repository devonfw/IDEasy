package com.devonfw.tools.ide.url.model.file;

import com.devonfw.tools.ide.url.model.UrlArtifactWithParent;
import com.devonfw.tools.ide.url.model.folder.UrlFolder;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;

/**
 * Interface for a file of an {@link UrlVersion}.
 *
 * @see UrlDownloadFile
 * @param <P> type of the {@link #getParent() parent} {@link UrlFolder folder}.
 */
public interface UrlFile<P extends UrlFolder<?>> extends UrlArtifactWithParent<P> {

}
