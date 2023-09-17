package com.devonfw.tools.ide.io;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/**
 * {@link Enum} with the available compression modes of a TAR archive file.
 */
public enum TarCompression {

  /** No compression (uncompressed TAR). */
  NONE("", "tar", null),

  /** GNU-Zip compression. */
  GZ("gz", "tgz", "-z") {

    @Override
    InputStream unpackRaw(InputStream in) throws IOException {

      return new GzipCompressorInputStream(in);
    }
  },

  /** BZip2 compression. */
  BZIP2("bz2", "tbz2", "-j", "bzip2") {

    @Override
    InputStream unpackRaw(InputStream in) throws IOException {

      return new BZip2CompressorInputStream(in);
    }
  };

  private final String extension;

  private final String combinedExtension;

  private final String tarOption;

  private final String altExtension;

  private TarCompression(String extension, String combinedExtension, String tarOption) {

    this(extension, combinedExtension, tarOption, null);
  }

  private TarCompression(String extension, String combinedExtension, String tarOption, String altExtension) {

    this.extension = extension;
    this.combinedExtension = combinedExtension;
    this.tarOption = tarOption;
    this.altExtension = altExtension;
  }

  /**
   * @return the (default) file extension of this compression (excluding the dot). E.g. "gz" for a "tar.gz" or "tgz"
   *         file.
   */
  public String getExtension() {

    return this.extension;
  }

  /**
   * @return the compact file extension of this compression combined with the tar archive information. E.g. "tgz" or
   *         "tbz2".
   */
  public String getCombinedExtension() {

    return this.combinedExtension;
  }

  /**
   * @return the CLI option to enable this compression in the GNU tar command.
   */
  public String getTarOption() {

    return this.tarOption;
  }

  /**
   * @return altExtension
   */
  public String getAltExtension() {

    return this.altExtension;
  }

  /**
   * @param in the {@link InputStream} to wrap for unpacking.
   * @return an {@link InputStream} to read the unpacked payload of the given {@link InputStream}.
   */
  public final InputStream unpack(InputStream in) {

    try {
      return unpackRaw(in);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to open unpacking stream!", e);
    }
  }

  InputStream unpackRaw(InputStream in) throws IOException {

    return in;
  }

  /**
   * @param extension the file extension (e.g. "tgz", ".tar.gz", "bz2", etc.)
   * @return the {@link TarCompression} detected from the given {@code extension} or {@code null} if none was detected.
   */
  public static TarCompression of(String extension) {

    if ((extension == null) || extension.isEmpty()) {
      return null;
    }
    String ext = extension;
    if (ext.charAt(0) == '.') {
      ext = ext.substring(1);
    }
    boolean isTar = false;
    if (ext.startsWith("tar")) {
      isTar = true;
      if ((ext.length() > 3) && (ext.charAt(3) == '.')) {
        ext = ext.substring(4);
      } else {
        return NONE;
      }
    }
    for (TarCompression cmp : values()) {
      if (cmp.extension.equals(ext)) {
        return cmp;
      } else if (!isTar && cmp.combinedExtension.equals(ext)) {
        return cmp;
      } else if ((cmp.altExtension != null) && cmp.altExtension.equals(ext)) {
        return cmp;
      }
    }
    return null;
  }

}
