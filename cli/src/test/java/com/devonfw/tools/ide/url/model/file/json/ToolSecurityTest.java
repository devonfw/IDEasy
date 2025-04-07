package com.devonfw.tools.ide.url.model.file.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.url.model.AbstractUrlModelTest;
import com.devonfw.tools.ide.url.model.folder.AbstractUrlToolOrEdition;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * Test of {@link ToolSecurity} and {@link AbstractUrlToolOrEdition#getSecurityFile()}.
 */
public class ToolSecurityTest extends AbstractUrlModelTest {

  @Test
  public void testSecurity() {

    // arrange
    IdeContext context = newContext();

    // act
    Collection<CVE> security = context.getDefaultToolRepository()
        .findSecurity("tomcat", "tomcat").findCVEs(VersionIdentifier.of("(2.40.1)"));
    List<VersionRange> versionRanges = new ArrayList<>();
    versionRanges.add(VersionRange.of("(0,2.39.4)"));
    versionRanges.add(VersionRange.of("[2.40.0,2.40.2)"));

    // assert
    assertThat(security).containsExactly(new CVE("CVE-2024-32002", 9.0f, versionRanges));
  }
}
