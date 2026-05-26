package com.devonfw.tools.ide.url.tool.msvc;

import java.util.Set;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;

public class MsvcUrlUpdater extends AbstractUrlUpdater {
    
    @Override
    public String getTool() {
        return "msvc";
    }

    @Override
    protected Set<String> getVersions() {
        return Set.of("17.0");
    }

    @Override
    protected String getVersionBaseUrl() {
        return "https://aka.ms/vs";
    }

    @Override
    protected String getDownloadBaseUrl() {
        return "https://aka.ms/vs";
    }

    @Override
    protected void addVersion(UrlVersion urlVersion) {
        doAddVersion(urlVersion, "https://aka.ms/vs/17/release/vs_BuildTools.exe", OperatingSystem.WINDOWS);
    }
}
