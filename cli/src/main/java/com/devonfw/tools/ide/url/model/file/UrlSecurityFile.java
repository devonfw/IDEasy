package com.devonfw.tools.ide.url.model.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * {@link UrlFile} with the security information for an {@link UrlEdition}.
 */
public class UrlSecurityFile extends AbstractUrlFile<UrlEdition> {

  /** {@link #getName() Name} of security file. */
  public static final String FILENAME_SECURITY = "security";

  private static final Logger LOG = LoggerFactory.getLogger(UrlSecurityFile.class);

  private final Set<VersionRange> versionRanges;

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   */
  public UrlSecurityFile(UrlEdition parent) {

    super(parent, FILENAME_SECURITY);
    this.versionRanges = new TreeSet<>();
  }

  /**
   * @return the number of #getUrl
   */
  public int getVersionRangeCount() {

    return this.versionRanges.size();
  }

  /**
   * @param versionRange the {@link VersionRange} add.
   */
  public void addVersionRange(VersionRange versionRange) {

    boolean added = this.versionRanges.add(versionRange);
    if (added) {
      this.modified = true;
    }
  }

  /**
   * @param versionRange the {@link VersionRange} to remove.
   */
  public void removeVersionRange(VersionRange versionRange) {

    boolean removed = this.versionRanges.remove(versionRange);
    if (removed) {
      this.modified = true;
    }
  }

  /**
   * @param version the {@link VersionIdentifier} to check.
   * @return {@code true} if the given {@link VersionIdentifier} is contained in this {@link UrlSecurityFile},
   *         {@code false} otherwise.
   */
  public boolean contains(VersionIdentifier version) {

    for (VersionRange range : this.versionRanges) {
      if (range.contains(version)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void doLoad() {

    this.versionRanges.clear();
    Path path = getPath();
    if (!Files.exists(path)) {
      return;
    }
    try (BufferedReader br = Files.newBufferedReader(path)) {
      String line;
      do {
        line = br.readLine();
        if (line != null) {
          line = line.trim();
          VersionRange range = VersionRange.of(line);
          if (range == null) {
            LOG.warn("Invalid line in file " + path + ": " + line);
          } else {
            this.versionRanges.add(range);
          }
        }
      } while (line != null);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load file " + path, e);
    }
  }

  @Override
  protected void doSave() {

    Path path = getPath();
    if (this.versionRanges.isEmpty() && !Files.exists(path)) {
      return;
    }
    try (BufferedWriter bw = Files.newBufferedWriter(path, StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
      for (VersionRange range : this.versionRanges) {
        bw.write(range + "\n");
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to save file " + path, e);
    }
  }
}
