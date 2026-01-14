package com.devonfw.tools.ide.url.model.file.json;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.version.VersionRange;

/**
 * Test of {@link ToolSecurityMapper}.
 */
class ToolSecurityMapperTest extends Assertions {

  @Test
  void testLoadAndSaveJson(@TempDir Path tmpDir) {
    // arrange
    Path testPath = Path.of("src/test/resources/tool-security");
    ToolSecurityMapper mapper = ToolSecurityMapper.get();
    Cve Cve2024_32002 = new Cve("CVE-2024-32002", 9.0, List.of(VersionRange.of("(0,2.39.4)"), VersionRange.of("[2.40.0,2.40.2)")));
    Cve Cve2024_33001 = new Cve("CVE-2024-33001", 5.0,
        List.of(VersionRange.of("(0,2.39.5)"), VersionRange.of("[2.40.3,2.40.6)"), VersionRange.of("[2023.3.3]")));
    Cve CveTest = new Cve("CVE-Test", 4, List.of(VersionRange.of("[2022.3.1]")));
    // act
    ToolSecurity toolSecurity = mapper.loadJsonFromFolder(testPath);
    // assert
    Collection<Cve> issues = toolSecurity.getIssues();
    assertThat(issues).containsExactly(Cve2024_32002, Cve2024_33001, CveTest);
    // act
    mapper.saveJsonToFolder(toolSecurity, tmpDir);
    // assert
    assertThat(tmpDir.resolve(mapper.getStandardFilename())).hasSameTextualContentAs(testPath.resolve("security-normalized.json"));
  }

}
