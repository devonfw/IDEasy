package com.devonfw.tools.ide.url.updater;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.tool.gcviewer.GcViewerUrlUpdater;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.report.UrlFinalReport;

/**
 * The {@code UpdateManager} class manages the update process for various tools by using a list of {@link AbstractUrlUpdater}s to update the
 * {@link UrlRepository}. The list of {@link AbstractUrlUpdater}s contains crawlers for different tools and services, To use the UpdateManager, simply create an
 * instance with the path to the repository as a parameter and call the {@link #updateAll(UrlFinalReport)} method.
 */
public class UpdateManager extends AbstractProcessorWithTimeout {

  private static final Logger logger = LoggerFactory.getLogger(AbstractUrlUpdater.class);

  private final UrlRepository urlRepository;

  private UrlFinalReport urlFinalReport;

  private final List<AbstractUrlUpdater> updaters = Arrays.asList(
      new AndroidStudioUrlUpdater(), new AwsUrlUpdater(), new AzureUrlUpdater(), new DockerDesktopUrlUpdater(), new DotNetUrlUpdater(),
      new EclipseCppUrlUpdater(), new EclipseJeeUrlUpdater(), new EclipseJavaUrlUpdater(), new GCloudUrlUpdater(),
      new GcViewerUrlUpdater(), new GhUrlUpdater(), new GraalVmCommunityUpdater(), new GraalVmOracleUrlUpdater(),
      new GradleUrlUpdater(), new HelmUrlUpdater(), new IntellijUrlUpdater(), new JasyptUrlUpdater(),
      new JavaUrlUpdater(), new JenkinsUrlUpdater(), new JmcUrlUpdater(), new KotlincUrlUpdater(),
      new KotlincNativeUrlUpdater(), new LazyDockerUrlUpdater(), new MvnUrlUpdater(), new Mvn4UrlUpdater(),
      new NodeUrlUpdater(), new NpmUrlUpdater(), new OcUrlUpdater(), new PgAdminUrlUpdater(), new PipUrlUpdater(),
      new PythonUrlUpdater(), new QuarkusUrlUpdater(), new DockerRancherDesktopUrlUpdater(), new SonarUrlUpdater(),
      new TerraformUrlUpdater(), new TomcatUrlUpdater(), new VsCodeUrlUpdater());

  /**
   * The constructor.
   *
   * @param pathToRepository the {@link Path} to the {@code ide-urls} repository to update.
   * @param expirationTime for GitHub actions url-update job
   */
  public UpdateManager(Path pathToRepository, UrlFinalReport urlFinalReport, Instant expirationTime) {

    this.urlRepository = UrlRepository.load(pathToRepository);
    this.urlFinalReport = urlFinalReport;
    setExpirationTime(expirationTime);
  }

  /**
   * Updates {@code ide-urls} for all tools their editions and all found versions.
   */
  public void updateAll() {

    for (AbstractUrlUpdater updater : this.updaters) {
      if (isTimeoutExpired()) {
        break;
      }
      try {
        updater.setExpirationTime(getExpirationTime());
        updater.setUrlFinalReport(this.urlFinalReport);
        updater.update(this.urlRepository);
      } catch (Exception e) {
        logger.error("Failed to update {}", updater.getToolWithEdition(), e);
      }
    }
  }

}
