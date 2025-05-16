package com.devonfw.tools.security;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.Vulnerability;
import org.owasp.dependencycheck.utils.Pair;

import com.devonfw.tools.IDEasy.dev.BuildSecurityJsonFiles;
import com.devonfw.tools.ide.url.model.file.UrlSecurityFile;
import com.devonfw.tools.ide.url.model.file.json.UrlSecurityWarning;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.version.VersionRange;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import io.github.jeremylong.openvulnerability.client.nvd.CvssV3Data;

@WireMockTest
public class UrlSecurityTest extends Assertions {

  @Test
  public void testSecurityWrite(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    //arrange

    //act
    Vulnerability vulnerability = new Vulnerability("tomcat");
    CvssV3Data cvssV3Data = new CvssV3Data();
    //CvssV3 cvssv3 = new CvssV3("test", new Type("test"), cvssV3Data, 1.2, 1.3);
    //vulnerability.setCvssV3(cvssv3);
    VersionRange versionRange = VersionRange.of("latest");
    BigDecimal severity = new BigDecimal(1);
    UrlSecurityWarning urlSecurityWarning = new UrlSecurityWarning(versionRange, severity, "testCVE");
    UrlSecurityWarning urlSecurityWarning2 = new UrlSecurityWarning(versionRange, severity, "testCVE");
    List<UrlSecurityWarning> urlSecurityWarnings = List.of(urlSecurityWarning, urlSecurityWarning2);
    UrlRepository urlRepository = UrlRepository.load(tempDir);
    SecurityUrlUpdaterMock updater = new SecurityUrlUpdaterMock(wmRuntimeInfo);
    Set<Pair<String, String>> foundToolsAndEditions = new HashSet<>();
    UrlTool urlTool = new UrlTool(urlRepository, "tomcat");
    UrlEdition urlEdition = new UrlEdition(urlTool, "tomcat");
    urlEdition.getSecurityFile();
    Path path = Files.createDirectories(tempDir.resolve("tomcat").resolve("tomcat"));
    Files.createFile(path.resolve("status.json"));
    Map<String, String> cpeToUrlVersion = Map.of("1", "2");

    UrlSecurityFile securityFile = urlEdition.getSecurityFile();

    List<Dependency> dependencies = new ArrayList<>();

    Dependency dep1 = new Dependency();
    dep1.setFileName("status.json");
    dep1.setActualFilePath(tempDir.toString());
    dep1.setFilePath(tempDir.toString());
    dep1.setPackagePath(tempDir.toString());

    Dependency dep2 = new Dependency();
    dep2.setFileName("status.json");
    dep2.setActualFilePath("C:\\projects\\_ide\\urls\\docker\\rancher\\0.2.0\\status.json");
    dep2.setFilePath("C:\\projects\\_ide\\urls\\docker\\rancher\\0.2.0\\status.json");
    dep2.setPackagePath("C:\\projects\\_ide\\urls\\docker\\rancher\\0.2.0\\status.json");

    dependencies.add(dep1);
    dependencies.add(dep2);

    BuildSecurityJsonFiles.addVulnerabilityToSecurityFile(vulnerability, securityFile, cpeToUrlVersion, updater);
    //BuildSecurityJsonFiles.saveSecurityFile(dep1, foundToolsAndEditions, "tomcat", "3.4", securityFile, updater);
  }
}
