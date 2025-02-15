package com.devonfw.tools.ide.url.model.file;

/**
 * Interface for a generic checksum.
 */
public interface UrlGenericChecksum {

  /**
   * @return the checksum as {@link String} (hex-representation).
   */
  String getChecksum();

  /**
   * @return the name of the hash algorithm used to compute the checksum.
   */
  String getHashAlgorithm();
  
}
