package com.devonfw.tools.ide.util;

/**
 * Untility for hexadecimal number conversion.
 */
public final class HexUtil {
  // construction forbidden
  private HexUtil() {

  }

  /**
   * @param bytes the byte array to convert in hex String
   * @return converted string in hexadecimal format
   */
  public static String toHexString(byte[] bytes) {

    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      int i = Byte.toUnsignedInt(b);
      String hex = Integer.toHexString(i);
      if (hex.length() == 1) {
        sb.append('0');
      }
      sb.append(hex);
    }
    return sb.toString();
  }

}
