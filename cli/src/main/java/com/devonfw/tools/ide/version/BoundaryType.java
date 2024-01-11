package com.devonfw.tools.ide.version;

/**
 * Enum representing the type of interval regarding its boundaries.
 */
public enum BoundaryType {

  /** Closed interval - includes the specified values at the boundaries. */
  CLOSED,

  /** Open interval - excludes the specified values at the boundaries. */
  OPEN,

  /** Left open interval - excludes the lower bound but includes the upper bound. */
  LEFT_OPEN,

  /** Right open interval - includes the lower bound but excludes the upper bound. */
  RIGHT_OPEN
}
