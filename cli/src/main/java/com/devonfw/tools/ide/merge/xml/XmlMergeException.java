package com.devonfw.tools.ide.merge.xml;

/**
 * {@link RuntimeException} for errors related to {@link XmlMerger}.
 */
public class XmlMergeException extends RuntimeException {

  /**
   * The constructor.
   *
   * @param message the {@link #getMessage() message}.
   */
  public XmlMergeException(String message) {

    super(message);
  }

  /**
   * The constructor.
   *
   * @param message the {@link #getMessage() message}.
   * @param cause the {@link #getCause() cause}.
   */
  public XmlMergeException(String message, Throwable cause) {

    super(message, cause);
  }
}
