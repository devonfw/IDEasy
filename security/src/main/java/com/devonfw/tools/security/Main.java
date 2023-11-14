package com.devonfw.tools.security;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.url.updater.UpdateManager;
import com.devonfw.tools.ide.url.updater.UrlUpdater;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.AnalysisPhase;
import org.owasp.dependencycheck.analyzer.FileTypeAnalyzer;
import org.owasp.dependencycheck.dependency.*;
import org.owasp.dependencycheck.exception.ExceptionCollection;
import org.owasp.dependencycheck.exception.ReportException;
import org.owasp.dependencycheck.utils.Settings;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {
  // TODO is owasp dependence i main pomxlm correc tor should i move it to security pomxml

  public static void main(String[] args) throws ReportException {

    // TODO edit depedency check properties file to switch off analysers, this file is currently read only
    // TODO maybe this can be done in pom.xml

    //TODO, wenn eine cve gefunden wird. dann in ide cli prompten und auch die cve sagen, damit der user selbst entschienden kann ob es vielleicht doch nicht eine false positive is. weil zb der vendor nicht so richtig gemached worden ist

    // TODO  ~/.m2/repository/org/owasp/dependency-check-utils/8.4.2/data/7.0/odc.update.lock
    // why is this not in projects dir but in user dir?

    Settings settings = new Settings();
    File dir;


    settings.setBoolean(Settings.KEYS.ANALYZER_NODE_AUDIT_USE_CACHE, false);


    try (Engine engine = new Engine(settings)) {

      // das brauche ich um die file endung zu akzeptieren
      FileTypeAnalyzer myAnalyzer = new UrlAnalyzer();
      // engine.getAnalyzers().add(myAnalyzer);
      engine.getFileTypeAnalyzers().add(myAnalyzer);
//      engine.getAnalyzers(AnalysisPhase.INFORMATION_COLLECTION).add(new UrlAnalyzer());
      List<Dependency> dependencyList = engine.scan("C:\\projects\\_ide\\myUrls");
      System.out.println("size of dependencyList is " + dependencyList.size());

      // add my infos to dependency
      for (Dependency dependency : dependencyList) {
        // TODO soll ich auch noch die ulr splitten und die zu evidence machen?
        String filePath = dependency.getFilePath();
        Path parent = Paths.get(filePath).getParent();
        String tool = parent.getParent().getParent().getFileName().toString();
        String edition = parent.getParent().getFileName().toString();
        String version = parent.getFileName().toString();


        // TODO is versions od dependency updated when adding evidence?

        // from the context I want to get the JavaUrlUpdater
        //        UpdateManager updateManager = new UpdateManager(ideContext.getUrlsPath(), null);
        //        String vendor = updateManager.getVendor("java");

        Evidence productEvidence = new Evidence("mysoure", "myname", tool, Confidence.HIGH);
        dependency.addEvidence(EvidenceType.PRODUCT, productEvidence);

        Evidence editionEvidence = new Evidence("mysoure", "myname", edition, Confidence.HIGH);
        dependency.addEvidence(EvidenceType.PRODUCT, editionEvidence);

        Evidence versionEvidence = new Evidence("mysoure", "myname", version, Confidence.HIGH);
        dependency.addEvidence(EvidenceType.VERSION, versionEvidence);

        Evidence vendorEvidence = new Evidence("mysoure", "myname", "oracle", Confidence.HIGH);
        dependency.addEvidence(EvidenceType.VENDOR, vendorEvidence);


      }

      // TODO oder kann ich doch manche analyzer weg machen?
      // welche sollen weg?
      try {
        engine.analyzeDependencies();// needed for db stuff which is private
        for (Dependency dependency : engine.getDependencies()) {
          engine.removeDependency(dependency);
          for (EvidenceType type : EvidenceType.values()) {
            for (Evidence evidence : dependency.getEvidence(type)) {
              if (!evidence.getName().equals("myname")) {
                dependency.removeEvidence(type, evidence);
              }
            }
          }
          engine.addDependency(dependency);
        }

      } catch (ExceptionCollection e) {
        throw new RuntimeException(e);
      }

      // TODO dont do this with this method but try to do it by hand, since i cant seem to add my URL analyzer to the map of engine
      // look at path and them extract name and version and vendor maybe from url
      List<Throwable> exceptionsList = new ArrayList<>();
      ExceptionCollection exceptions = new ExceptionCollection(exceptionsList);

      dir = new File("C:\\projects\\devonfw\\report");
      engine.writeReports("applicationName", "groupId", "artifactId", "version", dir, "JSON", exceptions);
    }


    String filename = dir.toString() + "\\dependency-check-report.json";
    Path filepath = Paths.get(filename);
    // Read all lines from the file into a List
    String formatted = "";
    try {
      List<String> lines = Files.readAllLines(filepath);
      assert (lines.size() == 1);
      formatted = formatJsonString(lines.get(0));

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Path newfilepath = filepath.getParent().resolve("dependency-check-report2.json");
    try {
      Files.delete(filepath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      if (Files.exists(newfilepath)) {
        Files.delete(newfilepath);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      Files.write(newfilepath, formatted.getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String formatJsonString(String jsonString) {
    int level = 0;
    StringBuilder formattedJson = new StringBuilder();
    int stringLength = jsonString.length();

    for (int i = 0; i < stringLength; i++) {
      char ch = jsonString.charAt(i);

      if (ch == '{' || ch == '[') {
        formattedJson.append(ch).append("\n").append(getIndent(++level));
      } else if (ch == '}' || ch == ']') {
        formattedJson.append("\n").append(getIndent(--level)).append(ch);
      } else if (ch == ',') {
        formattedJson.append(ch).append("\n").append(getIndent(level));
      } else {
        formattedJson.append(ch);
      }
    }

    return formattedJson.toString();
  }

  private static String getIndent(int level) {
    StringBuilder indent = new StringBuilder();
    for (int i = 0; i < level; i++) {
      indent.append("\t");
    }
    return indent.toString();
  }
}