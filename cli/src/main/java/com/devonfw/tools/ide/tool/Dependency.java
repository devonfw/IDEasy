package com.devonfw.tools.ide.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.url.model.file.UrlDependencyFile;
import com.devonfw.tools.ide.url.model.file.dependencyJson.DependencyInfo;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * Class to represent the functionality of installing the dependencies when a tool is being installed.
 */
public class Dependency {

  private final IdeContext context;

  private final String tool;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the tool of the context
   */
  public Dependency(IdeContext context, String tool) {

    this.context = context;
    this.tool = tool;
  }

  public boolean existsDependencyJsonPath(String toolEdition) {

    return Files.exists(getDependencyUrlEdition(toolEdition).getDependencyFile().getPath());
  }

  private UrlEdition getDependencyUrlEdition(String toolEdition) {

    return this.context.getUrls().getEdition(this.tool, toolEdition);
  }

  /**
   * Method to read the Json file
   *
   * @param version of the main tool to be installed.
   * @param toolEdition of the main tool, so that the correct folder can be checked to find the dependency json file
   * @return the {@link List} of {@link DependencyInfo} to be installed
   */
  public List<DependencyInfo> readJson(VersionIdentifier version, String toolEdition) {

    UrlDependencyFile urlDependencyFile = getDependencyUrlEdition(toolEdition).getDependencyFile();
    if (!urlDependencyFile.isDependencyMapNull()) {
      Map<VersionRange, List<DependencyInfo>> dependencyMap = urlDependencyFile.getDependencyMap();
      return urlDependencyFile.findDependenciesFromJson(dependencyMap, version);
    }
    return null;
  }

  /**
   * Method to search the List of versions available in the ide and find the right version to install
   *
   * @param dependencyFound the {@link DependencyInfo} of the dependency that was found that needs to be installed
   * @return {@link VersionIdentifier} of the dependency that is to be installed
   */
  public VersionIdentifier findDependencyVersionToInstall(DependencyInfo dependencyFound) {

    String dependencyEdition = this.context.getVariables().getToolEdition(dependencyFound.getTool());

    List<VersionIdentifier> versions = this.context.getUrls().getSortedVersions(dependencyFound.getTool(), dependencyEdition);

    for (VersionIdentifier vi : versions) {
      if (dependencyFound.getVersionRange().contains(vi)) {
        return vi;
      }
    }
    return null;
  }

  /**
   * Method to check if in the repository of the dependency there is a Version greater or equal to the version range to be installed, and to return the path of
   * this version if it exists
   *
   * @param dependencyRepositoryPath the {@link Path} of the dependency repository
   * @param dependencyVersionRangeFound the {@link VersionRange} of the dependency version to be installed
   * @return the {@link Path} of such version if it exists in repository already, an empty Path
   */
  public Path versionExistsInRepository(Path dependencyRepositoryPath, VersionRange dependencyVersionRangeFound) {

    try (Stream<Path> versions = Files.list(dependencyRepositoryPath)) {
      Iterator<Path> versionsIterator = versions.iterator();
      while (versionsIterator.hasNext()) {
        VersionIdentifier versionFound = VersionIdentifier.of(versionsIterator.next().getFileName().toString());
        if (dependencyVersionRangeFound.contains(versionFound)) {
          assert versionFound != null;
          return dependencyRepositoryPath.resolve(versionFound.toString());
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to iterate through " + dependencyRepositoryPath, e);
    }
    return Path.of("");
  }


}
