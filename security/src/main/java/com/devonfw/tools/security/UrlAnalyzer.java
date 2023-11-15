package com.devonfw.tools.security;

import java.io.FileFilter;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.AbstractFileTypeAnalyzer;
import org.owasp.dependencycheck.analyzer.AnalysisPhase;
import org.owasp.dependencycheck.analyzer.exception.AnalysisException;
import org.owasp.dependencycheck.dependency.Confidence;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.Evidence;
import org.owasp.dependencycheck.dependency.EvidenceType;
import org.owasp.dependencycheck.exception.InitializationException;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.url.updater.UpdateManager;

public class UrlAnalyzer extends AbstractFileTypeAnalyzer {

  // The file filter is used to filter supported files.
  private FileFilter fileFilter = null;

  private static final String ANALYZER_NAME = "UrlAnalyzer";

  private final UpdateManager updateManager;
  public UrlAnalyzer(UpdateManager updateManager) {

    fileFilter = new UrlFileFilter();
    this.updateManager = updateManager;
  }

  @Override
  protected void analyzeDependency(Dependency dependency, Engine engine) throws AnalysisException {



    String filePath = dependency.getFilePath();
    Path parent = Paths.get(filePath).getParent();
    String tool = parent.getParent().getParent().getFileName().toString();
    String edition = parent.getParent().getFileName().toString();

    String source = "UrlAnalyzer";

    // adding vendor evidence
    String vendor = updateManager.getCpeVendor(tool);
    Evidence evidence = new Evidence(source, "CpeVendor", vendor, Confidence.HIGH);
    dependency.addEvidence(EvidenceType.VENDOR, evidence);

    // adding product evidence
    String product = updateManager.getCpeProduct(tool);
    if (product == null) {
      product = tool;
    }
    evidence = new Evidence(source, "CpeProduct", product, Confidence.HIGH);
    dependency.addEvidence(EvidenceType.PRODUCT, evidence);

    // adding version evidence
    String version = updateManager.mapUrlVersionToCpeVersion(tool, parent.getFileName().toString());
    evidence = new Evidence(source, "CpeVersion", version, Confidence.HIGH);
    dependency.addEvidence(EvidenceType.VERSION, evidence);
  }

  @Override
  public boolean isEnabled() {

    return true;
  }

  @Override
  protected String getAnalyzerEnabledSettingKey() {
    // whether this Analyzer is enabled or not is not configurable but fixed by isEnabled()
    return null;
  }

  @Override
  protected FileFilter getFileFilter() {

    return fileFilter;
  }

  @Override
  protected void prepareFileTypeAnalyzer(Engine engine) throws InitializationException {
    // nothing to prepare here
  }

  @Override
  public String getName() {

    return ANALYZER_NAME;
  }

  @Override
  public AnalysisPhase getAnalysisPhase() {

    return AnalysisPhase.INFORMATION_COLLECTION;
  }
}
