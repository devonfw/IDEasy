package com.devonfw.tools.security;

import org.owasp.dependencycheck.Engine;
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
  public static void main(String[] args) throws ReportException {

    //TODO, wenn eine cve gefunden wird. dann in ide cli prompen und auch die cve sagen, damit der user selbst entschienden kann ob es vielleicht doch nicht eine false positive is. weil zb der vendor nicht so richtig gemached worden ist

    // TODO  ~/.m2/repository/org/owasp/dependency-check-utils/8.4.2/data/7.0/odc.update.lock
    // why is this not in projects dir but in user dir?

    Settings settings = new Settings();
    Engine engine = new Engine(settings);

    // das brauche ich um die file endung zu akzeptieren
    FileTypeAnalyzer myAnalyzer = new UrlAnalyzer();
    // engine.getAnalyzers().add(myAnalyzer);
    engine.getFileTypeAnalyzers().add(myAnalyzer);
    List<Dependency> dependencyList = engine.scan("C:\\projects\\_ide\\myUrls");
    System.out.println("size of dependencylist is " + dependencyList.size());

    for (Dependency dependency : dependencyList) {
      // TODO soll ich auch noch die ulr splitten und die zu evidence machen?
      String filePath = dependency.getFilePath();
      Path parent = Paths.get(filePath).getParent();
      String tool = parent.getParent().getParent().getFileName().toString();
      String edition = parent.getParent().getFileName().toString();
      String version = parent.getFileName().toString();

      String vendor = ""; // maybe split url and take domain or second and third after /

      // TODO is versions od dependency updated when adding evidence?

      Evidence productEvidence = new Evidence("mysoure", "myname", tool, Confidence.HIGH);
      dependency.addEvidence(EvidenceType.PRODUCT, productEvidence);

      Evidence editionEvidence = new Evidence("mysoure", "myname", edition, Confidence.HIGH);
      dependency.addEvidence(EvidenceType.PRODUCT, editionEvidence);

      Evidence versionEvidence = new Evidence("mysoure", "myname", version, Confidence.HIGH);
      dependency.addEvidence(EvidenceType.VERSION, versionEvidence);

      Evidence vendorEvidence = new Evidence("mysoure", "myname", "oracle", Confidence.HIGH);
      dependency.addEvidence(EvidenceType.VENDOR, vendorEvidence);

      // Evidence vendorEvidence = new Evidence("mysoure", "myname", "oracle", Confidence.HIGH);
      // dependency.addEvidence(EvidenceType.VENDOR, vendorEvidence);
      // dependency.getAvailableVersions();
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

    File dir = new File("C:\\projects\\devonfw\\report");
    engine.writeReports("applicationName", "groupId", "artifactId", "version", dir, "JSON", exceptions);


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