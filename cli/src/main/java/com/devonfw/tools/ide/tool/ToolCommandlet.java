package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.common.Tags;
import com.devonfw.tools.ide.os.MacOsHelper;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link Commandlet} for a tool managed by IDEasy.
 */
public interface ToolCommandlet extends Commandlet, Tags {

}




