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
import com.devonfw.tools.ide.tool.cobigen.CobigenUrlUpdater;
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
import com.devonfw.tools.ide.tool.pip.PipUrlUpdater;
import com.devonfw.tools.ide.tool.python.PythonUrlUpdater;
import com.devonfw.tools.ide.tool.quarkus.QuarkusUrlUpdater;
import com.devonfw.tools.ide.tool.sonar.SonarUrlUpdater;
import com.devonfw.tools.ide.tool.terraform.TerraformUrlUpdater;
import com.devonfw.tools.ide.tool.tomcat.TomcatUrlUpdater;
import com.devonfw.tools.ide.tool.vscode.VsCodeUrlUpdater;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.tool.jasypt.JasyptUrlUpdater;

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
      new AzureUrlUpdater(), new CobigenUrlUpdater(), new DockerDesktopUrlUpdater(), new DotNetUrlUpdater(),
      new EclipseCppUrlUpdater(), new EclipseJeeUrlUpdater(), new EclipseJavaUrlUpdater(), new GCloudUrlUpdater(),
      new GcViewerUrlUpdater(), new GhUrlUpdater(), new GraalVmCommunityUpdater(), new GraalVmOracleUrlUpdater(),
      new GradleUrlUpdater(), new HelmUrlUpdater(), new IntellijUrlUpdater(), new JavaUrlUpdater(),
      new JenkinsUrlUpdater(), new JmcUrlUpdater(), new KotlincUrlUpdater(), new KotlincNativeUrlUpdater(),
      new LazyDockerUrlUpdater(), new MvnUrlUpdater(), new Mvn4UrlUpdater(), new NodeUrlUpdater(), new NpmUrlUpdater(), new OcUrlUpdater(),
      new PipUrlUpdater(), new PythonUrlUpdater(), new QuarkusUrlUpdater(), new DockerRancherDesktopUrlUpdater(),
      new SonarUrlUpdater(), new TerraformUrlUpdater(), new TomcatUrlUpdater(), new VsCodeUrlUpdater(),
      new JasyptUrlUpdater());

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
