package com.devonfw.tools.ide.url.model.file;

import java.util.Collections;
import java.util.Iterator;

/**
 * Empty implementation of {@link UrlChecksums}.
 */
public class UrlChecksumsEmpty implements UrlChecksums {

  private static final UrlChecksumsEmpty INSTANCE = new UrlChecksumsEmpty();

  @Override
  public Iterator<UrlGenericChecksum> iterator() {

    return Collections.emptyIterator();
  }

  /**
   * @return the empty instance.
   */
  public static UrlChecksumsEmpty of() {
    return INSTANCE;
  }
}
