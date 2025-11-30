package com.devonfw.tools.ide.version;

class VersionRangeCombination {

  private final VersionComparisonResult minComparison;

  private final VersionIdentifier minMin;

  private final boolean minMinExclusive;

  private final VersionIdentifier maxMin;

  private final boolean maxMinExclusive;

  private final VersionComparisonResult maxComparison;

  private final VersionIdentifier minMax;

  private final boolean minMaxExclusive;

  private final VersionIdentifier maxMax;

  private final boolean maxMaxExclusive;

  private final VersionRangeRelation relation;

  VersionRangeCombination(VersionRange range1, VersionRange range2) {
    super();
    // minimums
    VersionIdentifier myMinMin = null;
    VersionIdentifier myMaxMin = null;
    boolean myMinMinExclusive = true;
    boolean myMaxMinExclusive = true;
    if ((range1.min == null) && (range2.min == null)) {
      this.minComparison = VersionComparisonResult.EQUAL;
    } else {
      this.minComparison = VersionIdentifier.compareVersion(range1.min, range2.min, true);
      if (this.minComparison.isLess()) {
        myMinMin = range1.min;
        myMinMinExclusive = range1.boundaryType.isLeftExclusive();
        myMaxMin = range2.min;
        myMaxMinExclusive = range2.boundaryType.isLeftExclusive();
      } else {
        myMinMin = range2.min;
        myMinMinExclusive = range2.boundaryType.isLeftExclusive();
        myMaxMin = range1.min;
        myMaxMinExclusive = range1.boundaryType.isLeftExclusive();
      }
    }
    this.minMin = myMinMin;
    this.minMinExclusive = myMinMinExclusive;
    this.maxMin = myMaxMin;
    this.maxMinExclusive = myMaxMinExclusive;
    // maximums
    VersionIdentifier myMaxMax = null;
    VersionIdentifier myMinMax = null;
    boolean myMaxMaxExclusive = true;
    boolean myMinMaxExclusive = true;
    if ((range1.max == null) && (range2.max == null)) {
      this.maxComparison = VersionComparisonResult.EQUAL;
    } else {
      this.maxComparison = VersionIdentifier.compareVersion(range1.max, range2.max, false);
      if (maxComparison.isGreater()) {
        myMaxMax = range1.max;
        myMaxMaxExclusive = range1.boundaryType.isRightExclusive();
        myMinMax = range2.max;
        myMinMaxExclusive = range2.boundaryType.isRightExclusive();
      } else {
        myMaxMax = range2.max;
        myMaxMaxExclusive = range2.boundaryType.isRightExclusive();
        myMinMax = range1.max;
        myMinMaxExclusive = range1.boundaryType.isRightExclusive();
      }
    }
    this.maxMax = myMaxMax;
    this.maxMaxExclusive = myMaxMaxExclusive;
    this.minMax = myMinMax;
    this.minMaxExclusive = myMinMaxExclusive;
    this.relation = VersionRangeRelation.of(this.maxMin, this.maxMinExclusive, this.minMax, this.minMaxExclusive);
  }

  VersionRange union(VersionRangeRelation minRelation) {

    if (minRelation.ordinal() <= relation.ordinal()) {
      boolean leftExclusive = this.minMinExclusive;
      boolean rightExclusive = this.maxMaxExclusive;
      if (this.minComparison.isEqual()) {
        leftExclusive = this.minMinExclusive && this.maxMinExclusive;
      }
      if (this.maxComparison.isEqual()) {
        rightExclusive = this.maxMaxExclusive && this.minMaxExclusive;
      }
      return VersionRange.of(this.minMin, this.maxMax, BoundaryType.of(leftExclusive, rightExclusive));
    }
    return null;
  }

  VersionRange intersection() {

    VersionComparisonResult comparison;
    if (this.minMax == null) {
      if (this.maxMin == null) {
        assert this.minMaxExclusive && this.maxMinExclusive;
        return VersionRange.UNBOUNDED;
      } else {
        comparison = VersionComparisonResult.LESS;
      }
    } else {
      comparison = this.minMax.compareVersion(this.maxMin);
    }
    boolean leftInclusive = this.maxMinExclusive && this.minMinExclusive;
    boolean rightInclusive = this.minMaxExclusive && this.maxMaxExclusive;
    if (comparison.isLess()) {
      return null;
    } else if (comparison.isEqual()) {
      if (this.minMaxExclusive || this.maxMinExclusive) {
        return null;
      }
      //leftInclusive = !this.minMinExclusive;
      //rightInclusive = !this.maxMaxExclusive;
    }
    if (this.minComparison.isEqual()) {
      leftInclusive = this.minMinExclusive || this.maxMinExclusive;
    }
    if (this.maxComparison.isEqual()) {
      rightInclusive = this.maxMinExclusive || this.minMaxExclusive;
    }
    return VersionRange.of(this.maxMin, this.minMax, BoundaryType.of(leftInclusive, rightInclusive));
  }

}
