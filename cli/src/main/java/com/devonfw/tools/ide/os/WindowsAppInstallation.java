package com.devonfw.tools.ide.os;

/**
 * Represents Windows application installation information from the registry.
 */
public record WindowsAppInstallation(
    String version,
    String icon,
    String uninstallString,
    String installLocation
) {

}
