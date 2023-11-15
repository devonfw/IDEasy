package com.devonfw.tools.security;

import java.util.List;
import java.util.stream.Collectors;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.url.updater.UpdateManager;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.AnalysisPhase;
import org.owasp.dependencycheck.analyzer.FileNameAnalyzer;
import org.owasp.dependencycheck.analyzer.FileTypeAnalyzer;
import org.owasp.dependencycheck.dependency.*;
import org.owasp.dependencycheck.exception.ExceptionCollection;
import org.owasp.dependencycheck.utils.Settings;

public class Main {
  public static void main(String[] args) {
    // TODO edit dependency check properties file to switch off analysers, this file is currently read only
    // TODO maybe this can be done in pom.xml
    // or simply remove it like FileNameAnalyzer was removed

    // TODO: note settings.setBoolean(Settings.KEYS.ANALYZER_NODE_AUDIT_USE_CACHE, false);

    // TODO ~/.m2/repository/org/owasp/dependency-check-utils/8.4.2/data/7.0/odc.update.lock
    // why is this not in projects dir but in user dir?

    Settings settings = new Settings();
    Engine engine = new Engine(settings); // doesn't work with "try with resource"

    IdeContext ideContext = new IdeContextConsole(IdeLogLevel.INFO, null, false);
    UpdateManager updateManager = new UpdateManager(ideContext.getUrlsPath(), null);

    FileTypeAnalyzer myAnalyzer = new UrlAnalyzer(updateManager);
    engine.getFileTypeAnalyzers().add(myAnalyzer);
    engine.getAnalyzers(AnalysisPhase.INFORMATION_COLLECTION).add(myAnalyzer);
    engine.getAnalyzers(AnalysisPhase.INFORMATION_COLLECTION)
        .removeIf(analyzer -> analyzer instanceof FileNameAnalyzer);

    engine.scan("C:\\projects\\_ide\\myUrls");

    try {
      engine.analyzeDependencies();// needed for db stuff which is private
    } catch (ExceptionCollection e) {
      throw new RuntimeException(e);
    }
    float minV2Severity = 0.0f;
    float minV3Severity = 0.0f;

    for (Dependency dependency : engine.getDependencies()) {
      List<Vulnerability> noSeverity = dependency.getVulnerabilities(true).stream()
          .filter(v -> v.getCvssV2() == null && v.getCvssV3() == null).collect(Collectors.toList());
      List<Vulnerability> onlyV2Severity = dependency.getVulnerabilities(true).stream()
          .filter(v -> v.getCvssV2() != null && v.getCvssV3() == null).collect(Collectors.toList());
      List<Vulnerability> hasV3Severity = dependency.getVulnerabilities(true).stream()
          .filter(v -> v.getCvssV3() != null).collect(Collectors.toList());

      if (!noSeverity.isEmpty()) {
        System.out.println("no severity is not empty: " + dependency.getFileName());
        System.exit(1);
      }

      onlyV2Severity.removeIf(v -> v.getCvssV2().getScore() < minV3Severity);
      hasV3Severity.removeIf(v -> v.getCvssV3().getBaseScore() < minV2Severity);

      System.out.println("There were vulnerabilities found in: " + dependency.getFileName());
      onlyV2Severity.forEach(v -> System.out.println("V2: " + v.getName() + " " + v.getCvssV2().getScore()));
      hasV3Severity.forEach(v -> System.out.println("V3: " + v.getName() + " " + v.getCvssV3().getBaseScore()));

      // TODO read min levels from console
      // TODO list all vulnerabilities, so maybe description, all fields of cvssv3 and cvssv2, cve name, source,
      // url of vulnerabilityIds, and vulnerableSoftware
      // TODO take all vulnerabilities, or ask for another min level und update the numbers of vulnerabilities
      // TODO write vulnerabilities to file -> new format? that includes CVE name?

    }
  }
}