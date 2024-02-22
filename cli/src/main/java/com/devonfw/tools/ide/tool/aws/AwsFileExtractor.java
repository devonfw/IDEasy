package com.devonfw.tools.ide.tool.aws;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import com.devonfw.tools.ide.commandlet.CommandletFileExtractorImpl;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * Implements a {@link CommandletFileExtractorImpl} for {@link Aws}
 */
public class AwsFileExtractor extends CommandletFileExtractorImpl {
  public AwsFileExtractor(IdeContext context, ToolCommandlet commandlet) {

    super(context, commandlet);
  }

  @Override
  public void moveAndProcessExtraction(Path from, Path to) {

    if (this.context.getSystemInfo().isLinux()) {
      // make binary executable using java nio because unpacking didn't preserve the file permissions
      // TODO this can be removed if issue #132 is fixed
      Path awsInDistPath = from.resolve("dist").resolve("aws");
      Path awsCompleterInDistPath = from.resolve("dist").resolve("aws_completer");
      makeExecutable(awsInDistPath);
      makeExecutable(awsCompleterInDistPath);

      // running the install-script that aws shipped
      ProcessContext pc = this.context.newProcess();
      Path linuxInstallScript = from.resolve("install");
      pc.executable(linuxInstallScript);
      pc.addArgs("-i", from.toString(), "-b", from.toString());
      pc.run();

      // The install-script that aws ships creates symbolic links to binaries but using absolute paths.
      // Since the current process happens in a temporary dir, these links wouldn't be valid after moving the
      // installation files to the target dir. So the absolute paths are replaced by relative ones.
      for (String file : new String[] { "aws", "aws_completer", Path.of("v2").resolve("current").toString() }) {
        Path link = from.resolve(file);
        try {
          this.context.getFileAccess().symlink(link.toRealPath(), link, true);
        } catch (IOException e) {
          throw new RuntimeException(
              "Failed to replace absolute link (" + link + ") provided by AWS install script with relative link.", e);
        }
      }
      this.context.getFileAccess().delete(linuxInstallScript);
      this.context.getFileAccess().delete(from.resolve("dist"));
    }
    super.moveAndProcessExtraction(from, to);
  }

  private void makeExecutable(Path file) {

    // TODO this can be removed if issue #132 is fixed. See https://github.com/devonfw/IDEasy/issues/132
    Set<PosixFilePermission> permissions = null;
    try {
      permissions = Files.getPosixFilePermissions(file);
      permissions.add(PosixFilePermission.GROUP_EXECUTE);
      permissions.add(PosixFilePermission.OWNER_EXECUTE);
      permissions.add(PosixFilePermission.OTHERS_EXECUTE);
      Files.setPosixFilePermissions(file, permissions);
    } catch (IOException e) {
      throw new RuntimeException("Adding execution permission for Group, Owner and Others did not work for " + file, e);
    }
  }

}
