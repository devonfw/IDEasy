package com.devonfw.tools.ide.url.model;

import java.nio.file.Path;

import com.devonfw.tools.ide.url.model.file.UrlDownloadFile;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;

/**
 * An {@link UrlArtifact} represents a file or folder in the directory structure of a devonfw-ide urls repository.
 *
 * @see UrlRepository
 * @see UrlTool
 * @see UrlEdition
 * @see UrlVersion
 * @see UrlDownloadFile
 */
public interface UrlArtifact {

  /**
   * @return the {@link Path} to this {@link AbstractUrlArtifact} as folder or file on the disc.
   */
  Path getPath();

  /**
   * @return name the file-name of this {@link AbstractUrlArtifact}.
   */
  String getName();

  /**
   * Loads this artifact from the disc.
   *
   * @param recursive - {@code true} to recursively load the children, {@code false} otherwise (just load the file ).
   */
  void load(boolean recursive);

  /**
   * Saves this artifact to the disc. Will recursively save its children. Unchanged files remain untouched.
   */
  void save();

}
