package com.devonfw.tools.security;

import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoImpl;

import java.io.FileFilter;
public class UrlFileFilter implements FileFilter {

    final private SystemInfo systemInfo;
    private final String os;

    public UrlFileFilter() {
        this.systemInfo = new SystemInfoImpl();
        this.os = this.systemInfo.getOs().toString();
    }

    @Override
    public boolean accept(java.io.File pathname) {
        boolean isUrlFile = pathname.toString().endsWith(".urls");
        boolean isCorrectOs = pathname.toString().contains(this.os);
        return isUrlFile && isCorrectOs;
    }
}
