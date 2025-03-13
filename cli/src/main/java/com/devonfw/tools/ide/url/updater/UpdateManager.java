package com.devonfw.tools.ide.url.updater;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.tool.androidstudio.AndroidStudioUrlUpdater;
import com.devonfw.tools.ide.tool.aws.AwsUrlUpdater;
import com.devonfw.tools.ide.tool.az.AzureUrlUpdater;
import com.devonfw.tools.ide.tool.docker.DockerDesktopUrlUpdater;
import com.devonfw.tools.ide.tool.docker.DockerRancherDesktopUrlUpdater;
import com.devonfw.tools.ide.tool.dotnet.DotNetUrlUpdater;
import com.devonfw.tools.ide.tool.eclipse.EclipseCppUrlUpdater;
import com.devonfw.tools.ide.tool.eclipse.EclipseJavaUrlUpdater;
import com.devonfw.tools.ide.tool.eclipse.EclipseJeeUrlUpdater;
import com.devonfw.tools.ide.tool.gcloud.GCloudUrlUpdater;
import com.devonfw.tools.ide.tool.gcviewer.GcViewerUrlUpdater;
import com.devonfw.tools.ide.tool.gh.GhUrlUpdater;
import com.devonfw.tools.ide.tool.graalvm.GraalVmCommunityUpdater;
import com.devonfw.tools.ide.tool.graalvm.GraalVmOracleUrlUpdater;
import com.devonfw.tools.ide.tool.gradle.GradleUrlUpdater;
import com.devonfw.tools.ide.tool.helm.HelmUrlUpdater;
import com.devonfw.tools.ide.tool.intellij.IntellijUrlUpdater;
import com.devonfw.tools.ide.tool.jasypt.JasyptUrlUpdater;
import com.devonfw.tools.ide.tool.java.JavaUrlUpdater;
import com.devonfw.tools.ide.tool.jenkins.JenkinsUrlUpdater;
import com.devonfw.tools.ide.tool.jmc.JmcUrlUpdater;
import com.devonfw.tools.ide.tool.kotlinc.KotlincNativeUrlUpdater;
import com.devonfw.tools.ide.tool.kotlinc.KotlincUrlUpdater;
import com.devonfw.tools.ide.tool.lazydocker.LazyDockerUrlUpdater;
import com.devonfw.tools.ide.tool.mvn.Mvn4UrlUpdater;
import com.devonfw.tools.ide.tool.mvn.MvnUrlUpdater;
import com.devonfw.tools.ide.tool.node.NodeUrlUpdater;
import com.devonfw.tools.ide.tool.npm.NpmUrlUpdater;
import com.devonfw.tools.ide.tool.oc.OcUrlUpdater;
import com.devonfw.tools.ide.tool.pgadmin.PgAdminUrlUpdater;
import com.devonfw.tools.ide.tool.pip.PipUrlUpdater;
import com.devonfw.tools.ide.tool.python.PythonUrlUpdater;
import com.devonfw.tools.ide.tool.quarkus.QuarkusUrlUpdater;
import com.devonfw.tools.ide.tool.sonar.SonarUrlUpdater;
import com.devonfw.tools.ide.tool.squirrelsql.SquirrelUrlUpdater;
import com.devonfw.tools.ide.tool.terraform.TerraformUrlUpdater;
import com.devonfw.tools.ide.tool.tomcat.TomcatUrlUpdater;
import com.devonfw.tools.ide.tool.vscode.VsCodeUrlUpdater;
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
      new PythonUrlUpdater(), new QuarkusUrlUpdater(), new DockerRancherDesktopUrlUpdater(), new SonarUrlUpdater(), new SquirrelUrlUpdater(),
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
      update(updater);
    }
  }

  /**
   * Update only a single tool. Mainly used in local development only to test updater only for a tool where changes have been made.
   *
   * @param tool the name of the tool to update.
   */
  public void update(String tool) {

    for (AbstractUrlUpdater updater : this.updaters) {
      if (updater.getTool().equals(tool)) {
        update(updater);
      }
    }
  }

  private void update(AbstractUrlUpdater updater) {
    try {
      updater.setExpirationTime(getExpirationTime());
      updater.setUrlFinalReport(this.urlFinalReport);
      String updaterName = updater.getClass().getSimpleName();
      String toolName = updater.getTool();
      logger.debug("Starting {} for tool {}", updaterName, toolName);
      updater.update(this.urlRepository);
      logger.debug("Ended {} for tool {}", updaterName, updater.getTool());
    } catch (Exception e) {
      logger.error("Failed to update {}", updater.getToolWithEdition(), e);
    }
  }

}
