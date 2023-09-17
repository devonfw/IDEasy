package com.devonfw.tools.ide.url.updater;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.androidstudio.AndroidStudioUrlUpdater;
import com.devonfw.tools.ide.url.updater.aws.AwsUrlUpdater;
import com.devonfw.tools.ide.url.updater.az.AzureUrlUpdater;
import com.devonfw.tools.ide.url.updater.cobigen.CobigenUrlUpdater;
import com.devonfw.tools.ide.url.updater.docker.DockerRancherDesktopUrlUpdater;
import com.devonfw.tools.ide.url.updater.dotnet.DotNetUrlUpdater;
import com.devonfw.tools.ide.url.updater.eclipse.EclipseCppUrlUpdater;
import com.devonfw.tools.ide.url.updater.eclipse.EclipseJavaUrlUpdater;
import com.devonfw.tools.ide.url.updater.gcloud.GCloudUrlUpdater;
import com.devonfw.tools.ide.url.updater.gcviewer.GcViewerUrlUpdater;
import com.devonfw.tools.ide.url.updater.gh.GhUrlUpdater;
import com.devonfw.tools.ide.url.updater.graalvm.GraalVmCommunityUpdater;
import com.devonfw.tools.ide.url.updater.graalvm.GraalVmOracleUrlUpdater;
import com.devonfw.tools.ide.url.updater.gradle.GradleUrlUpdater;
import com.devonfw.tools.ide.url.updater.helm.HelmUrlUpdater;
import com.devonfw.tools.ide.url.updater.intellij.IntellijUrlUpdater;
import com.devonfw.tools.ide.url.updater.java.JavaUrlUpdater;
import com.devonfw.tools.ide.url.updater.jenkins.JenkinsUrlUpdater;
import com.devonfw.tools.ide.url.updater.jmc.JmcUrlUpdater;
import com.devonfw.tools.ide.url.updater.kotlinc.KotlincNativeUrlUpdater;
import com.devonfw.tools.ide.url.updater.kotlinc.KotlincUrlUpdater;
import com.devonfw.tools.ide.url.updater.lazydocker.LazyDockerUrlUpdater;
import com.devonfw.tools.ide.url.updater.mvn.MvnUrlUpdater;
import com.devonfw.tools.ide.url.updater.node.NodeUrlUpdater;
import com.devonfw.tools.ide.url.updater.npm.NpmUrlUpdater;
import com.devonfw.tools.ide.url.updater.oc.OcUrlUpdater;
import com.devonfw.tools.ide.url.updater.pip.PipUrlUpdater;
import com.devonfw.tools.ide.url.updater.python.PythonUrlUpdater;
import com.devonfw.tools.ide.url.updater.quarkus.QuarkusUrlUpdater;
import com.devonfw.tools.ide.url.updater.sonar.SonarUrlUpdater;
import com.devonfw.tools.ide.url.updater.terraform.TerraformUrlUpdater;
import com.devonfw.tools.ide.url.updater.tomcat.TomcatUrlUpdater;
import com.devonfw.tools.ide.url.updater.vscode.VsCodeUrlUpdater;

/**
 * The {@code UpdateManager} class manages the update process for various tools by using a list of
 * {@link AbstractUrlUpdater}s to update the {@link UrlRepository}. The list of {@link AbstractUrlUpdater}s contains
 * crawlers for different tools and services, To use the UpdateManager, simply create an instance with the path to the
 * repository as a parameter and call the {@link #updateAll()} method.
 */
public class UpdateManager extends AbstractProcessorWithTimeout {

  private static final Logger logger = LoggerFactory.getLogger(AbstractUrlUpdater.class);

  private final UrlRepository urlRepository;

  private final List<AbstractUrlUpdater> updaters = Arrays.asList(new AndroidStudioUrlUpdater(), new AwsUrlUpdater(),
      new AzureUrlUpdater(), new CobigenUrlUpdater(), new DotNetUrlUpdater(), new EclipseCppUrlUpdater(),
      new EclipseJavaUrlUpdater(), new GCloudUrlUpdater(), new GcViewerUrlUpdater(), new GhUrlUpdater(),
      new GraalVmCommunityUpdater(), new GraalVmOracleUrlUpdater(), new GradleUrlUpdater(), new HelmUrlUpdater(), new IntellijUrlUpdater(),
      new JavaUrlUpdater(), new JenkinsUrlUpdater(), new JmcUrlUpdater(), new KotlincUrlUpdater(), new KotlincNativeUrlUpdater(),
      new LazyDockerUrlUpdater(), new MvnUrlUpdater(), new NodeUrlUpdater(), new NpmUrlUpdater(), new OcUrlUpdater(),
      new PipUrlUpdater(), new PythonUrlUpdater(), new QuarkusUrlUpdater(), new DockerRancherDesktopUrlUpdater(),
      new SonarUrlUpdater(), new TerraformUrlUpdater(), new TomcatUrlUpdater(), new VsCodeUrlUpdater());

  /**
   * The constructor.
   *
   * @param pathToRepository the {@link Path} to the {@code ide-urls} repository to update.
   * @param expirationTime for GitHub actions url-update job
   */
  public UpdateManager(Path pathToRepository, Instant expirationTime) {

    this.urlRepository = UrlRepository.load(pathToRepository);
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
        updater.update(this.urlRepository);
      } catch (Exception e) {
        logger.error("Failed to update {}", updater.getToolWithEdition(), e);
      }
    }
  }

}
