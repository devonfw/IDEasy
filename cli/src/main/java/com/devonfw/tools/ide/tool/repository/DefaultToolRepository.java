package com.devonfw.tools.ide.tool.repository;

import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.url.model.UrlMetadata;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Default implementation of {@link ToolRepository} based on "ide-urls" using {@link UrlMetadata}.
 */
public class DefaultToolRepository extends AbstractToolRepository {

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   */
  public DefaultToolRepository(IdeContext context) {

    super(context);
  }

  @Override
  public String getId() {

    return ID_DEFAULT;
  }

  @Override
  protected UrlDownloadFileMetadata getMetadata(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet) {

    UrlMetadata metadata = this.context.getUrls();
    UrlVersion urlVersion = metadata.getVersionFolder(tool, edition, version, toolCommandlet);
    SystemInfo sys = this.context.getSystemInfo();
    return urlVersion.getMatchingUrls(sys.getOs(), sys.getArchitecture());
  }

  @Override
  public List<String> getSortedEditions(String tool) {

    return this.context.getUrls().getSortedEditions(tool);
  }

  @Override
  public List<VersionIdentifier> getSortedVersions(String tool, String edition, ToolCommandlet toolCommandlet) {

    return this.context.getUrls().getSortedVersions(tool, edition, toolCommandlet);
  }
}
