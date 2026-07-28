package com.devonfw.tools.ide.tool.python;

/**
 * Represents a single entry of the JSON output of {@code uv python list --output-format json}. Only the fields required for version resolution are mapped.
 *
 * @param version the full Python version (e.g. {@code 3.11.8}) or {@code null} if absent.
 * @param implementation the Python implementation (e.g. {@code cpython}, {@code pypy}) or {@code null} if absent.
 */
public record PythonUvListEntry(String version, String implementation) {

  /**
   * @return {@code true} if this entry is a CPython distribution, {@code false} otherwise.
   */
  public boolean isCpython() {

    return "cpython".equalsIgnoreCase(this.implementation);
  }
}
