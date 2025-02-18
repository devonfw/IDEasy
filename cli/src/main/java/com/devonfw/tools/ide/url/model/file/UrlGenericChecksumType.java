package com.devonfw.tools.ide.url.model.file;

import java.util.Objects;

/**
 * Implementation of {@link UrlGenericChecksum} as immutable datatype.
 */
public final class UrlGenericChecksumType implements UrlGenericChecksum {

  private final String checksum;

  private final String hashAlgorithm;

  private final Object source;

  /**
   * The constructor.
   *
   * @param checksum the {@link #getChecksum() checksum}.
   * @param hashAlgorithm the {@link #getHashAlgorithm() hash algorithm}.
   * @param source the source of this checksum (e.g. {@link java.nio.file.Path}, URL, String, etc.).
   */
  public UrlGenericChecksumType(String checksum, String hashAlgorithm, Object source) {

    super();
    Objects.requireNonNull(checksum);
    Objects.requireNonNull(hashAlgorithm);
    this.checksum = checksum;
    this.hashAlgorithm = hashAlgorithm;
    this.source = source;
  }

  @Override
  public String getChecksum() {

    return this.checksum;
  }

  @Override
  public String getHashAlgorithm() {

    return this.hashAlgorithm;
  }

  @Override
  public int hashCode() {

    return Objects.hash(checksum, hashAlgorithm);
  }

  @Override
  public boolean equals(Object o) {

    if (o == this) {
      return true;
    } else if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UrlGenericChecksumType other = (UrlGenericChecksumType) o;
    return this.checksum.equals(other.checksum) && this.hashAlgorithm.equals(other.hashAlgorithm);
  }

  @Override
  public String toString() {

    return this.checksum + "[" + this.hashAlgorithm + "]@" + this.source;
  }
}
