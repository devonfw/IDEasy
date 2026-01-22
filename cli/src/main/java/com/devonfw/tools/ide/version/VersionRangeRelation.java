package com.devonfw.tools.ide.version;

/**
 * {@link Enum} with the available types how {@link VersionRange}s can relate to each other. Examples are given by the following table:
 * <table border="1">
 *   <tr>
 *     <th>range1</th>
 *     <th>range2</th>
 *     <th>relation</th>
 *   </tr>
 *   <tr>
 *     <td>[2.0,5.0)</td>
 *     <td>(3.0,5.0]</td>
 *     <td>{@link #OVERLAPPING}</td>
 *   </tr>
 *   <tr>
 *     <td>[2.0,3.0]</td>
 *     <td>[3.0,5.0]</td>
 *     <td>{@link #OVERLAPPING}</td>
 *   </tr>
 *   <tr>
 *     <td>[2.0,3.0)</td>
 *     <td>[3.0,5.0]</td>
 *     <td>{@link #CONNECTED}</td>
 *   </tr>
 *   <tr>
 *     <td>[2.0,3.0]</td>
 *     <td>(3.0,5.0]</td>
 *     <td>{@link #CONNECTED}</td>
 *   </tr>
 *   <tr>
 *     <td>[2.0,2.2]</td>
 *     <td>[2.3,5.0]</td>
 *     <td>{@link #CONNECTED_LOOSELY}</td>
 *   </tr>
 *   <tr>
 *     <td>[2.0,3.0]</td>
 *     <td>[4.0,5.0]</td>
 *     <td>{@link #DISJUNCT}</td>
 *   </tr>
 *   <tr>
 *     <td>[2.0,2.2)</td>
 *     <td>(2.2,5.0]</td>
 *     <td>{@link #DISJUNCT}</td>
 *   </tr>
 * </table>
 * For readability, we sorted the {@link VersionRange}s in the examples from the table above but there is no order in the {@link VersionRange}s to be considered.
 * Further, the last example seems to raise some question: Why is a gap of "(2.2, 2.3)" accepted as {@link #CONNECTED_LOOSELY} while a gap of only exactly "[2.2, 2.2]" is not?
 * From a strictly mathematical point of view this seems confusing. However, in the last example version "2.2" is considered to be known and is explicitly not included in both ranges.
 * In contrast, the example for {@link #CONNECTED_LOOSELY} explicitly includes versions "2.2" and "2.3" and if we assume a strict versioning schema, then
 * "2.3" is the {@link VersionIdentifier#incrementSegment(int, boolean) next version build by incrementing only the last digit}. Even though in math there are infinite numbers in "(2.2, 2.3)" but from a pragmatical view
 * of a product versioning scheme we could assume that "(2.2, 2.3)" is actually empty.
 *
 * @see VersionRange#union(VersionRange, VersionRangeRelation)
 */
public enum VersionRangeRelation {

  /** The {@link VersionRange}s are disjunct so there is no value contained in all of them. */
  DISJUNCT,

  /**
   * The {@link VersionRange}s are connected loosely so the maximum of the {@link VersionRange#getMin() minimums} is equal to the minimum of the *
   * {@link VersionRange#getMax() maximums} and that value is {@link VersionRange#contains(VersionIdentifier) contained} in only one of them.
   */
  CONNECTED_LOOSELY,

  /**
   * The {@link VersionRange}s are connected so the maximum of the {@link VersionRange#getMin() minimums} is equal to the minimum of the
   * {@link VersionRange#getMax() maximums} and that value is {@link VersionRange#contains(VersionIdentifier) contained} in only one of them.
   */
  CONNECTED,

  /**
   * The {@link VersionRange}s overlap so the maximum of the {@link VersionRange#getMin() minimums} is {@link VersionIdentifier#isLessOrEqual less or equal} to
   * the minimum of the {@link VersionRange#getMax() maximums} and if they are equal that value must be
   * {@link VersionRange#contains(VersionIdentifier) contained} in the {@link VersionRange}s.
   */
  OVERLAPPING;

  /**
   * @param maxMin the maximum of the {@link VersionRange#getMin() minimums}.
   * @param maxMinExclusive {@code true} if {@code maxMin} is exclusive, {@code false} otherwise.
   * @param minMax the minimum of the {@link VersionRange#getMax() maximums}.
   * @param minMaxExclusive {@code true} if {@code minMax} is exclusive, {@code false} otherwise.
   * @return the {@link VersionRangeRelation} for the given situation.
   */
  static VersionRangeRelation of(VersionIdentifier maxMin, boolean maxMinExclusive, VersionIdentifier minMax, boolean minMaxExclusive) {

    if ((minMax == null) || (maxMin == null)) {
      // "(,1]" and "(,10)" overlaps also "(1,)" and "(2,)" as well as "(,)" and "(,)" do
      assert (minMax != null) || minMaxExclusive;
      assert (maxMin != null) || maxMinExclusive;
      return OVERLAPPING;
    }
    VersionComparisonResult comparison = minMax.compareVersion(maxMin);
    if (comparison.isGreater()) {
      return OVERLAPPING; // "[1.0,2.1)" and "(2.0,3.0]" overlap
    } else if (comparison.isEqual()) {
      if (!maxMinExclusive && !minMaxExclusive) {
        return OVERLAPPING;
      } else if (maxMinExclusive && minMaxExclusive) {
        return DISJUNCT;
      } else {
        return CONNECTED;
      }
    } else { // less than
      VersionIdentifier minMaxNext = minMax.incrementLastDigit(true);
      if (minMaxNext.isGreaterOrEqual(maxMin)) {
        return CONNECTED_LOOSELY;
      }
    }
    return DISJUNCT;
  }
}
