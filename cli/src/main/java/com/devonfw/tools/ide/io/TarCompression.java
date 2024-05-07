package com.devonfw.tools.ide.io;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * {@link Enum} with the available compression modes of a TAR archive file. A GNU Tape ARchive is the standard archive
 * format on Linux systems. It is similar to ZIP but it allows to represent advanced metadata such as file permissions
 * (e.g. executable flags). Further, it has no compression and is therefore typically combined with generic file
 * compressions like {@link #GZ GNU zip} (not to be confused with Windows ZIP) or {@link #BZIP2}.
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
   * file.
   */
  public String getExtension() {

    return this.extension;
  }

  /**
   * @return the compact file extension of this compression combined with the tar archive information. E.g. "tgz" or
   * "tbz2".
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
   * @param filename the filename or extension (e.g. "archive.tar.bzip2", "tgz", ".tar.gz", etc.)
   * @return the {@link TarCompression} detected from the given {@code filename} or {@code null} if none was detected.
   */
  public static TarCompression of(String filename) {

    if ((filename == null) || filename.isEmpty()) {
      return null;
    }
    String ext = filename.toLowerCase(Locale.ROOT);
    int tarIndex = ext.lastIndexOf("tar");
    if (tarIndex >= 0) {
      if ((tarIndex == 0) || (ext.charAt(tarIndex - 1) == '.')) {
        int tarEnd = tarIndex + 3;
        int rest = ext.length() - tarEnd;
        if (rest == 0) {
          return NONE;
        }
        if (ext.charAt(tarEnd) == '.') {
          String compression = ext.substring(tarEnd + 1);
          for (TarCompression cmp : values()) {
            if (compression.equals(cmp.extension) || compression.equals(cmp.altExtension)) {
              return cmp;
            }
          }
        }
      }
      return null;
    }
    int lastDot = ext.lastIndexOf('.');
    if (lastDot > 0) {
      ext = ext.substring(lastDot + 1);
    }
    for (TarCompression cmp : values()) {
      if (ext.equals(cmp.combinedExtension) || (ext.endsWith(cmp.combinedExtension)
          && ext.charAt(ext.length() - cmp.combinedExtension.length() - 1) == '.')) {
        return cmp;
      }
    }
    return null;
  }

}
