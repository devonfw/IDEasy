package com.devonfw.tools.ide.url.model.file;

import java.util.Iterator;
import java.util.List;

/**
 * Implementation of {@link UrlChecksums}.
 */
public class UrlGenericChecksums implements UrlChecksums {

  /** An empty instance of {@link UrlGenericChecksums}. */
  public static final UrlGenericChecksums EMPTY = new UrlGenericChecksums(List.of());

  private final List<UrlGenericChecksum> checksums;

  /**
   * The constructor.
   *
   * @param checksums the {@link #iterator() checksums}.
   */
  public UrlGenericChecksums(List<UrlGenericChecksum> checksums) {

    this.checksums = checksums;
  }

  @Override
  public Iterator<UrlGenericChecksum> iterator() {

    return this.checksums.iterator();
  }
}
