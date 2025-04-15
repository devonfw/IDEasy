package com.devonfw.tools.IDEasy.dev;

import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.url.updater.UpdateManager;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.AbstractFileTypeAnalyzer;
import org.owasp.dependencycheck.analyzer.AnalysisPhase;
import org.owasp.dependencycheck.dependency.Confidence;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.Evidence;
import org.owasp.dependencycheck.dependency.EvidenceType;
import org.owasp.dependencycheck.exception.InitializationException;

import java.io.FileFilter;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Analyzes file paths to detect tool, edition and version of software listed in a directory structure like this: .../<tool>/<edition>/<version>/<file>
 */
public class UrlAnalyzer extends AbstractFileTypeAnalyzer {

    // The file filter is used to filter supported files.
    private final FileFilter fileFilter;

    private static final String ANALYZER_NAME = "UrlAnalyzer";

    private final UpdateManager updateManager;

    /**
     * The constructor which initializes its {@link FileFilter} with a {@link UrlFileFilter}.
     *
     * @param updateManager the {@link UpdateManager} used to convert IDEasys tool/edition/version naming to the naming conventions of official CPEs.
     */
    public UrlAnalyzer(UpdateManager updateManager) {

        this.fileFilter = new UrlFileFilter();
        this.updateManager = updateManager;
    }

    /**
     * Analyzes the given {@link Dependency} and adds {@link Evidence} to it. Namely, CPE vendor, product, edition and version.
     *
     * @param dependency the dependency to analyze.
     * @param engine     not actually used here, but needs to match the signature of the super class.
     */
    @Override
    protected void analyzeDependency(Dependency dependency, Engine engine) {

        Path versionFolder = Paths.get(dependency.getFilePath()).getParent();
        String tool = versionFolder.getParent().getParent().getFileName().toString();
        String edition = versionFolder.getParent().getFileName().toString();

        AbstractUrlUpdater urlUpdater = this.updateManager.retrieveUrlUpdater(tool, edition);

        if (urlUpdater == null) {
            return;
        }

        String cpeVendor = urlUpdater.getCpeVendor();
        String cpeProduct = urlUpdater.getCpeProduct();
        String cpeEdition = urlUpdater.getCpeEdition();
        String cpeVersion = urlUpdater.mapUrlVersionToCpeVersion(versionFolder.getFileName().toString());

        if (cpeVendor.isBlank() || cpeProduct.isBlank()) {
            return;
        }
        Evidence evidence;
        evidence = new Evidence(ANALYZER_NAME, "CpeVendor", cpeVendor, Confidence.HIGH);
        dependency.addEvidence(EvidenceType.VENDOR, evidence);

        evidence = new Evidence(ANALYZER_NAME, "CpeProduct", cpeProduct, Confidence.HIGH);
        dependency.addEvidence(EvidenceType.PRODUCT, evidence);

        if (cpeEdition.isBlank()) {

            evidence = new Evidence(ANALYZER_NAME, "CpeEdition", cpeEdition, Confidence.HIGH);
            dependency.addEvidence(EvidenceType.PRODUCT, evidence);
        }

        evidence = new Evidence(ANALYZER_NAME, "CpeVersion", cpeVersion, Confidence.HIGH);
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

        return this.fileFilter;
    }

    @Override
    protected void prepareFileTypeAnalyzer(Engine engine) throws InitializationException {

        // nothing to prepare
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
