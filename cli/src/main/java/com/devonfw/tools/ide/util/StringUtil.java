package com.devonfw.tools.ide.util;

/**
 * Utility for String manipulations.
 */
public class StringUtil {

  /**
   * Returns a new array containing either: - [extraArgs..., args...] if prepend == true, or - [args..., extraArgs...] if prepend == false.
   * <p>
   * Null inputs are treated as empty arrays. The method never returns null.
   *
   * @param args array of strings
   * @param prepend boolean {@code true} if it should prepend, {@code false} if it should append
   * @param extraArgs array of extra args
   * @return the resulting string array.
   */
  public static String[] extendArray(String[] args, boolean prepend, String... extraArgs) {
    int aLen = (args == null) ? 0 : args.length;
    int eLen = (extraArgs == null) ? 0 : extraArgs.length;

    if (aLen == 0) {
      return extraArgs;
    } else if (eLen == 0) {
      return args;
    }

    String[] out = new String[aLen + eLen];

    if (prepend) {
      // [extraArgs..., args...]
      if (eLen > 0) {
        System.arraycopy(extraArgs, 0, out, 0, eLen);
      }
      if (aLen > 0) {
        System.arraycopy(args, 0, out, eLen, aLen);
      }
    } else {
      // [args..., extraArgs...]
      if (aLen > 0) {
        System.arraycopy(args, 0, out, 0, aLen);
      }
      if (eLen > 0) {
        System.arraycopy(extraArgs, 0, out, aLen, eLen);
      }
    }

    return out;
  }

}
