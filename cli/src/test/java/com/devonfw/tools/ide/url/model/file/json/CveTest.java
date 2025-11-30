package com.devonfw.tools.ide.url.model.file.json;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.version.VersionRange;

/**
 * Test of {@link Cve}.
 */
public class CveTest extends Assertions {

  @Test
  public void testMerge() {

    // arrange
    Cve cve1 = new Cve("CVE-2012-0845", 5.0,
        List.of(VersionRange.of("[2.0.0,2.0.1]"), VersionRange.of("[2.1.0,2.1.3]"), VersionRange.of("[2.2.0,2.2.3]"), VersionRange.of("[2.3.0,2.3.4]"),
            VersionRange.of("[2.8.0,2.8.1]")));
    Cve cve2 = new Cve("CVE-2012-0845", 5.0,
        List.of(VersionRange.of("(,2.6.7]"), VersionRange.of("[2.0.2,2.0.2]"), VersionRange.of("[2.6.8,2.6.9]"), VersionRange.of("[2.8.2,2.8.3]")));

    // act
    Cve merged = cve1.merge(cve2);

    // assert
    assertThat(merged.id()).isEqualTo("CVE-2012-0845");
    assertThat(merged.severity()).isEqualTo(5.0);
    assertThat(merged.versions()).containsExactly(VersionRange.of("(,2.6.9]"), VersionRange.of("[2.8.0,2.8.3]"));
  }

}
