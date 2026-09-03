package com.devonfw.tools.ide.merge;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.ini.IniFile;
import com.devonfw.tools.ide.io.ini.IniFileImpl;
import com.devonfw.tools.ide.io.ini.IniSection;

/**
 * Implementation of {@link FileMerger} for {@code .ini} files.
 */
public class IniMerger extends FileMerger {

  private static final Logger LOG = LoggerFactory.getLogger(IniMerger.class);

  /**
   * The constructor.
   *
   * @param context the {@link #context}.
   */
  public IniMerger(IdeContext context) {
    super(context);
  }

  @Override
  protected void doMerge(Path setup, Path update, EnvironmentVariables resolver, Path workspace) {

    FileAccess fileAccess = this.context.getFileAccess();
    IniFile mergedIni = new IniFileImpl();
    boolean updateFileExists = Files.exists(update);
    Path template = setup;
    if (Files.exists(workspace)) {
      if (!updateFileExists) {
        LOG.trace("Nothing to do as update file does not exist: {}", update);
        return; // nothing to do ...
      }
      fileAccess.readIniFile(workspace, mergedIni);
    } else if (Files.exists(setup)) {
      fileAccess.readIniFile(setup, mergedIni);
    }
    if (updateFileExists) {
      IniFile updateIni = new IniFileImpl();
      fileAccess.readIniFile(update, updateIni);
      mergeIniInto(updateIni, mergedIni);
      template = update;
    }

    resolve(mergedIni, resolver, template.toString());
    fileAccess.writeIniFile(mergedIni, workspace, true);
    LOG.trace("Saved merged ini to: {}", workspace);
  }

  /**
   * Merge the properties from {@code source} into {@code target}. Keys that exist in both: target gets overwritten with source. Keys that exist only in target:
   * preserved (user modifications).
   *
   * @param source the source INI (update template).
   * @param target the target INI (workspace or setup base).
   */
  private void mergeIniInto(IniFile source, IniFile target) {
    for (String sectionName : source.getSectionNames()) {
      IniSection srcSection = source.getSection(sectionName);
      IniSection tgtSection = target.getOrCreateSection(sectionName);
      for (String key : srcSection.getPropertyKeys()) {
        tgtSection.setProperty(key, srcSection.getPropertyValue(key));
      }
    }
    IniSection srcInitial = source.getInitialSection();
    IniSection tgtInitial = target.getOrCreateSection("");
    for (String key : srcInitial.getPropertyKeys()) {
      tgtInitial.setProperty(key, srcInitial.getPropertyValue(key));
    }
  }

  private void resolve(IniFile iniFile, EnvironmentVariables resolver, String src) {

    for (String sectionName : iniFile.getSectionNames()) {
      IniSection section = iniFile.getSection(sectionName);
      for (String key : section.getPropertyKeys()) {
        String value = section.getPropertyValue(key);
        String resolved = resolver.resolve(value, src, this.legacySupport);
        section.setProperty(key, resolved);
      }
    }
    IniSection initial = iniFile.getInitialSection();
    for (String key : initial.getPropertyKeys()) {
      String value = initial.getPropertyValue(key);
      String resolved = resolver.resolve(value, src, this.legacySupport);
      initial.setProperty(key, resolved);
    }
  }

  @Override
  public void inverseMerge(Path workspace, EnvironmentVariables variables, boolean addNewProperties, Path update) {

    if (!Files.exists(workspace)) {
      LOG.trace("Workspace file does not exist: {}", workspace);
      return;
    }
    if (!Files.exists(update)) {
      LOG.trace("Update file does not exist: {}", update);
      return;
    }
    Object src = workspace.getFileName();
    FileAccess fileAccess = this.context.getFileAccess();
    IniFile updateIni = fileAccess.readIniFile(update);
    IniFile workspaceIni = fileAccess.readIniFile(workspace);
    IniFile mergedIni = new IniFileImpl();
    copyIniInto(updateIni, mergedIni);
    boolean updated = false;

    for (String sectionName : workspaceIni.getSectionNames()) {
      IniSection wsSection = workspaceIni.getSection(sectionName);
      IniSection mergedSection = mergedIni.getOrCreateSection(sectionName);
      for (String key : wsSection.getPropertyKeys()) {
        String wsValue = wsSection.getPropertyValue(key);
        String updateValue = mergedSection.getPropertyValue(key);
        if ((updateValue != null) || addNewProperties) {
          String updateValueResolved = updateValue != null
              ? variables.resolve(updateValue, src, this.legacySupport)
              : null;
          if (!wsValue.equals(updateValueResolved)) {
            String wsValueInverseResolved = variables.inverseResolve(wsValue, src);
            mergedSection.setProperty(key, wsValueInverseResolved);
            updated = true;
          }
        }
      }
    }

    IniSection wsInitial = workspaceIni.getInitialSection();
    IniSection mergedInitial = mergedIni.getOrCreateSection("");
    for (String key : wsInitial.getPropertyKeys()) {
      String wsValue = wsInitial.getPropertyValue(key);
      String updateValue = mergedInitial.getPropertyValue(key);
      if ((updateValue != null) || addNewProperties) {
        String updateValueResolved = updateValue != null
            ? variables.resolve(updateValue, src, this.legacySupport)
            : null;
        if (!wsValue.equals(updateValueResolved)) {
          String wsValueInverseResolved = variables.inverseResolve(wsValue, src);
          mergedInitial.setProperty(key, wsValueInverseResolved);
          updated = true;
        }
      }
    }

    if (updated) {
      fileAccess.writeIniFile(mergedIni, update, true);
      LOG.debug("Saved changes from: {} to: {}", workspace.getFileName(), update);
    } else {
      LOG.trace("No changes for: {}", update);
    }
  }

  @Override
  protected boolean doUpgrade(Path workspaceFile) throws Exception {

    return doUpgradeTextContent(workspaceFile);
  }

  /**
   * Copy all sections and properties from source INI to target INI.
   *
   * @param src the source INI.
   * @param tgt the target INI.
   */
  private void copyIniInto(IniFile src, IniFile tgt) {
    for (String sectionName : src.getSectionNames()) {
      IniSection srcSection = src.getSection(sectionName);
      IniSection tgtSection = tgt.getOrCreateSection(sectionName);
      for (String key : srcSection.getPropertyKeys()) {
        tgtSection.setProperty(key, srcSection.getPropertyValue(key));
      }
    }
    IniSection srcInit = src.getInitialSection();
    IniSection tgtInit = tgt.getOrCreateSection("");
    for (String key : srcInit.getPropertyKeys()) {
      tgtInit.setProperty(key, srcInit.getPropertyValue(key));
    }
  }
}
