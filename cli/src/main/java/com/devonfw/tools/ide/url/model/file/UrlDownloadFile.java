package com.devonfw.tools.ide.url.model.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

import com.devonfw.tools.ide.common.OperatingSystem;
import com.devonfw.tools.ide.common.SystemArchitecture;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link UrlFile} with the download URLs. Its {@link #getName() name} has to follow one of the following conventions:
 * <ul>
 * <li>«os»_«arch».urls</li>
 * <li>«os».urls</li>
 * <li>urls</li>
 * </ul>
 */
public class UrlDownloadFile extends AbstractUrlFile<UrlVersion> implements UrlDownloadFileMetadata {

  /** The name {@value} for OS- and architecture-agnostic URLs. */
  public static final String NAME_URLS = "urls";

  /** The extension {@value}. */
  public static final String EXTENSION_URLS = ".urls";

  private final Set<String> urls;

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   * @param name the {@link #getName() filename}.
   */
  public UrlDownloadFile(UrlVersion parent, String name) {

    super(parent, name);
    this.urls = new HashSet<>();
  }

  /**
   * @return the number of #getUrl
   */
  public int getUrlCount() {

    return this.urls.size();
  }

  /**
   * @param url the download URL to add.
   */
  public void addUrl(String url) {

    boolean added = this.urls.add(url);
    if (added) {
      this.modified = true;
    }
  }

  /**
   * Avoid direct mutation of this {@link Set} and use {@link #addUrl(String)} or {@link #removeUrl(String)} instead.
   */
  @Override
  public Set<String> getUrls() {

    load(false);
    return this.urls;
  }

  /**
   * @param url the download URL to remove.
   */
  public void removeUrl(String url) {

    boolean removed = this.urls.remove(url);
    if (removed) {
      this.modified = true;
    }
  }

  @Override
  protected void doLoad() {

    this.urls.clear();
    Path path = getPath();
    try (BufferedReader br = Files.newBufferedReader(path)) {
      String line;
      do {
        line = br.readLine();
        if (line != null) {
          this.urls.add(line.trim());
        }
      } while (line != null);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load file " + path, e);
    }
  }

  @Override
  protected void doSave() {

    Path path = getPath();
    try (BufferedWriter bw = Files.newBufferedWriter(path, StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
      for (String line : this.urls) {
        bw.write(line + "\n");
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to save file " + path, e);
    }
  }

  @Override
  public OperatingSystem getOs() {

    String name = getName();
    if (name.equals(NAME_URLS)) {
      return null;
    }
    for (OperatingSystem os : OperatingSystem.values()) {
      if (name.startsWith(os.toString())) {
        return os;
      }
    }
    throw new IllegalStateException("Cannot derive operating-system from " + name);
  }

  @Override
  public SystemArchitecture getArch() {

    String name = getName();
    if (name.equals(NAME_URLS)) {
      return null;
    }
    int underscore = name.indexOf('_');
    if (underscore < 0) {
      return null;
    }
    assert (name.endsWith(EXTENSION_URLS));
    // EXTENSION_URLS.length() = ".urls".length() = 5
    String archString = name.substring(underscore + 1, name.length() - 5);
    for (SystemArchitecture arch : SystemArchitecture.values()) {
      if (archString.equals(arch.toString())) {
        return arch;
      }
    }
    throw new IllegalStateException("Cannot derive system-architecture from " + name);
  }

  @Override
  public VersionIdentifier getVersion() {

    UrlVersion version = getParent();
    return version.getVersionIdentifier();
  }

  @Override
  public String getEdition() {

    UrlEdition edition = getParent().getParent();
    return edition.getName();
  }

  @Override
  public String getTool() {

    UrlTool tool = getParent().getParent().getParent();
    return tool.getName();
  }

  @Override
  public String getChecksum() {

    UrlChecksum checksum = getParent().getChecksum(getName());
    if (checksum == null) {
      return null;
    }
    return checksum.getChecksum();
  }
}
