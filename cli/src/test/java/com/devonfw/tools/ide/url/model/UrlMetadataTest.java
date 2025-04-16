package com.devonfw.tools.ide.url.model;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

import com.devonfw.tools.ide.context.AbstractIdeTestContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.tool.python.Python;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test suite for {@link UrlMetadata}
 */
public class UrlMetadataTest extends AbstractUrlModelTest {

  /**
   * Tests if versions are excluded from version list in case, the version does not come with any download url for the needed OS
   */
  @Test
  public void testGetSortedVersions_versionNotAvailableForOs() {

    IdeContext context = newContext();
    ((AbstractIdeTestContext) context).setSystemInfo(new SystemInfoImpl(OS.WINDOWS.toString(), "11", SystemArchitecture.ARM64.toString()));
    UrlMetadata urlMetadata = new UrlMetadata(context);
    List<VersionIdentifier> sortedVersions = urlMetadata.getSortedVersions("python", "python", new Python(context));
    assertThat(sortedVersions).extracting(e -> e.toString()).containsExactly("3.11.9");
  }

  /**
   * Tests if versions are retrieved in correct order
   */
  @Test
  public void testGetSortedVersions_versionAvailableForOs() {

    IdeContext context = newContext();
    ((AbstractIdeTestContext) context).setSystemInfo(new SystemInfoImpl(OS.LINUX.toString(), "12", SystemArchitecture.ARM64.toString()));
    UrlMetadata urlMetadata = new UrlMetadata(context);
    List<VersionIdentifier> sortedVersions = urlMetadata.getSortedVersions("python", "python", new Python(context));
    assertThat(sortedVersions).extracting(e -> e.toString()).containsExactly("3.11.10", "3.11.9");
  }
}
