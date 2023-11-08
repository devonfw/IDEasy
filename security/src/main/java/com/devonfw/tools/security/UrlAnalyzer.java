package com.devonfw.tools.security;

import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.AbstractFileTypeAnalyzer;
import org.owasp.dependencycheck.analyzer.AnalysisPhase;
import org.owasp.dependencycheck.analyzer.exception.AnalysisException;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.exception.InitializationException;

import java.io.FileFilter;

public class UrlAnalyzer extends AbstractFileTypeAnalyzer {

    //    The file filter used to filter supported files.
    private FileFilter fileFilter = null;

    public UrlAnalyzer() {

        fileFilter = new UrlFileFilter();

    }

    @Override
    protected void analyzeDependency(Dependency dependency, Engine engine) throws AnalysisException {
        System.out.println("analyzeDependency");
        System.out.println("analyzeDependency");
    }

    @Override
    protected String getAnalyzerEnabledSettingKey() {
        return null;
    }

    @Override
    protected FileFilter getFileFilter() {
        return fileFilter;
    }

    @Override
    protected void prepareFileTypeAnalyzer(Engine engine) throws InitializationException {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public AnalysisPhase getAnalysisPhase() {
        return null;
    }
}
