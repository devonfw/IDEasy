package com.devonfw.tools.ide.repo;

import java.util.Collection;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.url.model.UrlMetadata;
import com.devonfw.tools.ide.url.model.file.UrlDependencyFile;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFile;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.version.GenericVersionRange;
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
  public VersionIdentifier resolveVersion(String tool, String edition, GenericVersionRange version) {

    UrlMetadata metadata = this.context.getUrls();
    UrlVersion urlVersion = metadata.getVersionFolder(tool, edition, version);
    return urlVersion.getVersionIdentifier();
  }

  @Override
  protected UrlDownloadFileMetadata getMetadata(String tool, String edition, VersionIdentifier version) {

    UrlMetadata metadata = this.context.getUrls();
    UrlVersion urlVersion = metadata.getVersionFolder(tool, edition, version);
    SystemInfo sys = this.context.getSystemInfo();
    UrlDownloadFile urls = urlVersion.getMatchingUrls(sys.getOs(), sys.getArchitecture());
    return urls;
  }

  @Override
  public Collection<ToolDependency> findDependencies(String tool, String edition, VersionIdentifier version) {

    UrlDependencyFile dependencyFile = this.context.getUrls().getEdition(tool, edition).getDependencyFile();
    return dependencyFile.getDependencies().findDependencies(version, this.context);
  }
}
