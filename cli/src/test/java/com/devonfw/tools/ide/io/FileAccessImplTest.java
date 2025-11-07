package com.devonfw.tools.ide.io;

import static com.devonfw.tools.ide.io.FileAccessImpl.generatePermissionString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;

/**
 * Test of {@link FileAccessImpl}.
 */
public class FileAccessImplTest extends AbstractIdeContextTest {

  /**
   * Checks if Windows junctions are used.
   *
   * @param context the {@link IdeContext} to get system info and file access from.
   * @param dir the {@link Path} to the directory which is used as temp directory.
   * @return {@code true} if Windows junctions are used, {@code false} otherwise.
   */
  private boolean windowsJunctionsAreUsed(IdeContext context, Path dir) {

    if (!context.getSystemInfo().isWindows()) {
      return false;
    }

    Path source = dir.resolve("checkIfWindowsJunctionsAreUsed");
    Path link = dir.resolve("checkIfWindowsJunctionsAreUsedLink");
    context.getFileAccess().mkdirs(source);
    try {
      Files.createSymbolicLink(link, source);
      return false;
    } catch (IOException e) {
      return true;
    }
  }

  /**
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} with "relative = false". Passing absolute paths as source.
   */
  @Test
  public void testSymlinkAbsolute(@TempDir Path tempDir) {

    // relative links are checked in testRelativeLinksWorkAfterMoving

    // arrange
    IdeContext context = IdeTestContextMock.get();
    FileAccess fileAccess = new FileAccessImpl(context);
    Path dir = tempDir.resolve("parent");
    createDirs(fileAccess, dir);
    boolean readLinks = !windowsJunctionsAreUsed(context, tempDir);
    boolean relative = false;

    // act
    createSymlinks(fileAccess, dir, relative);

    // assert
    assertSymlinksExist(dir);
    assertSymlinksWork(dir, readLinks);
  }

  /**
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} with "relative = false". Passing relative paths as source.
   */
  @Test
  public void testSymlinkAbsolutePassingRelativeSource(@TempDir Path tempDir) {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    FileAccess fileAccess = new FileAccessImpl(context);
    Path dir = tempDir.resolve("parent");
    createDirs(fileAccess, dir);
    boolean readLinks = !windowsJunctionsAreUsed(context, tempDir);
    boolean relative = false;

    // act
    createSymlinksByPassingRelativeSource(fileAccess, dir, relative);

    // assert
    assertSymlinksExist(dir);
    assertSymlinksWork(dir, readLinks);
  }

  /**
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} with "relative = true". But Windows junctions are used and therefore the fallback from relative
   * to absolute paths is tested.
   */
  @Test
  public void testSymlinkAbsoluteAsFallback(@TempDir Path tempDir) {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    if (!windowsJunctionsAreUsed(context, tempDir)) {
      context.info(
          "Can not check the Test: testSymlinkAbsoluteAsFallback since windows junctions are not used and fallback "
              + "from relative to absolute paths as link target is not used.");
      return;
    }
    FileAccess fileAccess = new FileAccessImpl(context);
    Path dir = tempDir.resolve("parent");
    createDirs(fileAccess, dir);
    boolean readLinks = false; // bc windows junctions are used, which can't be read with Files.readSymbolicLink(link);
    boolean relative = true; // set to true, such that the fallback to absolute paths is used since junctions are used

    // act
    createSymlinks(fileAccess, dir, relative);

    // assert
    assertSymlinksExist(dir);
    assertSymlinksWork(dir, readLinks);
  }

  /**
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} with "relative = false". Furthermore, it is tested that the links are broken after moving
   * them.
   */
  @Test
  public void testSymlinkAbsoluteBreakAfterMoving(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    FileAccess fileAccess = new FileAccessImpl(context);
    Path dir = tempDir.resolve("parent");
    createDirs(fileAccess, dir);
    boolean relative = false;
    createSymlinks(fileAccess, dir, relative);
    boolean readLinks = !windowsJunctionsAreUsed(context, tempDir);

    // act
    Path sibling = dir.resolveSibling("parent2");
    fileAccess.move(dir, sibling);

    // assert
    assertSymlinksExist(sibling);
    assertSymlinksAreBroken(sibling, readLinks);
  }

  /**
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} with "relative = true". Furthermore, it is tested that the links still work after moving them.
   * Passing relative paths as source.
   */
  @Test
  public void testSymlinkRelativeWorkAfterMovingPassingRelativeSource(@TempDir Path tempDir) {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    if (windowsJunctionsAreUsed(context, tempDir)) {
      context.info("Can not check the Test: testRelativeLinksWorkAfterMoving since windows junctions are used.");
      return;
    }
    FileAccess fileAccess = new FileAccessImpl(context);
    Path dir = tempDir.resolve("parent");
    createDirs(fileAccess, dir);
    boolean relative = true;
    createSymlinksByPassingRelativeSource(fileAccess, dir, relative);
    boolean readLinks = true; // junctions are not used, so links can be read with Files.readSymbolicLink(link);

    // act
    Path sibling = dir.resolveSibling("parent2");
    fileAccess.move(dir, sibling);

    // assert
    assertSymlinksExist(sibling);
    assertSymlinksWork(sibling, readLinks);
  }

  /**
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} with "relative = true". Furthermore, it is tested that the links still work after moving them.
   */
  @Test
  public void testSymlinkRelativeWorkAfterMoving(@TempDir Path tempDir) {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    if (windowsJunctionsAreUsed(context, tempDir)) {
      context.info("Can not check the Test: testRelativeLinksWorkAfterMoving since windows junctions are used.");
      return;
    }
    FileAccess fileAccess = new FileAccessImpl(context);
    Path dir = tempDir.resolve("parent");
    createDirs(fileAccess, dir);
    boolean relative = true;
    createSymlinks(fileAccess, dir, relative);
    boolean readLinks = true; // junctions are not used, so links can be read with Files.readSymbolicLink(link);

    // act
    Path sibling = dir.resolveSibling("parent2");
    fileAccess.move(dir, sibling);

    // assert
    assertSymlinksExist(sibling);
    assertSymlinksWork(sibling, readLinks);
  }

  /**
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} when Windows junctions are used and the source is a file.
   */
  @Test
  public void testSymlinkWindowsJunctionsCanNotPointToFiles(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    if (!windowsJunctionsAreUsed(context, tempDir)) {
      context.info(
          "Can not check the Test: testWindowsJunctionsCanNotPointToFiles since windows junctions are not used.");
      return;
    }
    Path file = tempDir.resolve("file");
    Files.createFile(file);
    FileAccess fileAccess = new FileAccessImpl(context);

    // act & assert
    IllegalStateException e1 = assertThrows(IllegalStateException.class, () -> {
      fileAccess.symlink(file, tempDir.resolve("linkToFile"));
    });
    assertThat(e1).hasMessageContaining("These junctions can only point to directories or other junctions");
  }

  /**
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} and whether the source paths are simplified correctly by
   * {@link Path#toRealPath(LinkOption...)}.
   */
  @Test
  public void testSymlinkShortcutPaths(@TempDir Path tempDir) {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    FileAccess fileAccess = new FileAccessImpl(context);
    Path dir = tempDir.resolve("parent");
    createDirs(fileAccess, dir);
    fileAccess.mkdirs(dir.resolve("d3"));
    boolean readLinks = !windowsJunctionsAreUsed(context, tempDir);

    // act
    fileAccess.symlink(dir.resolve("d3/../d1"), dir.resolve("link1"), false);
    fileAccess.symlink(Path.of("d3/../d1"), dir.resolve("link2"), false);
    fileAccess.symlink(dir.resolve("d3/../d1"), dir.resolve("link3"), true);
    fileAccess.symlink(Path.of("d3/../d1"), dir.resolve("link4"), true);
    fileAccess.delete(dir.resolve("d3"));

    // assert
    assertSymlinkToRealPath(dir.resolve("link1"), dir.resolve("d1"));
    assertSymlinkToRealPath(dir.resolve("link2"), dir.resolve("d1"));
    assertSymlinkToRealPath(dir.resolve("link3"), dir.resolve("d1"));
    assertSymlinkToRealPath(dir.resolve("link4"), dir.resolve("d1"));
    if (readLinks) {
      assertSymlinkRead(dir.resolve("link1"), dir.resolve("d1"));
      assertSymlinkRead(dir.resolve("link2"), dir.resolve("d1"));
      assertSymlinkRead(dir.resolve("link3"), dir.resolve("d1"));
      assertSymlinkRead(dir.resolve("link4"), dir.resolve("d1"));
    }
  }

  private void createDirs(FileAccess fileAccess, Path dir) {

    fileAccess.mkdirs(dir.resolve("d1/d11/d111/d1111"));
    fileAccess.mkdirs(dir.resolve("d2/d22/d222"));
  }

  /**
   * Creates the symlinks with passing relative paths as source. This is used by the tests of {@link FileAccessImpl#symlink(Path, Path, boolean)}.
   *
   * @param fa the {@link FileAccess} to use.
   * @param dir the {@link Path} to the directory where the symlinks shall be created.
   * @param relative - {@code true} if the symbolic link shall be relative, {@code false} if it shall be absolute.
   */
  private void createSymlinksByPassingRelativeSource(FileAccess fa, Path dir, boolean relative) {

    fa.symlink(Path.of("."), dir.resolve("d1/d11/link_to_d1"), relative);
    // test if symbolic links or junctions can be overwritten with symlink()
    fa.symlink(Path.of(".."), dir.resolve("d1/d11/link_to_d1"), relative);

    fa.symlink(Path.of("."), dir.resolve("d1/d11/link_to_d11"), relative);
    fa.symlink(Path.of("d111"), dir.resolve("d1/d11/link_to_d111"), relative);
    fa.symlink(Path.of("d111/d1111"), dir.resolve("d1/d11/link_to_d1111"), relative);
    fa.symlink(Path.of("../../d1/../d2"), dir.resolve("d1/d11/link_to_d2"), relative);
    fa.symlink(Path.of("../../d2/d22"), dir.resolve("d1/d11/link_to_d22"), relative);
    fa.symlink(Path.of("../../d2/d22/d222"), dir.resolve("d1/d11/link_to_d222"), relative);

    fa.symlink(Path.of("../../d1/d11/link_to_d1"), dir.resolve("d2/d22/link_to_link_to_d1"), relative);
    fa.symlink(Path.of("../d1/d11/link_to_d1"), dir.resolve("d2/another_link_to_link_to_d1"), relative);
    fa.symlink(Path.of("d2/another_link_to_link_to_d1"), dir.resolve("link_to_another_link_to_link_to_d1"), relative);
  }

  /**
   * Creates the symlinks with passing absolute paths as source. This is used by the tests of {@link FileAccessImpl#symlink(Path, Path, boolean)}.
   *
   * @param fa the {@link FileAccess} to use.
   * @param dir the {@link Path} to the directory where the symlinks shall be created.
   * @param relative - {@code true} if the symbolic link shall be relative, {@code false} if it shall be absolute.
   */
  private void createSymlinks(FileAccess fa, Path dir, boolean relative) {

    fa.symlink(dir.resolve("d1/d11"), dir.resolve("d1/d11/link_to_d1"), relative);
    // test if symbolic links or junctions can be overwritten with symlink()
    fa.symlink(dir.resolve("d1"), dir.resolve("d1/d11/link_to_d1"), relative);

    fa.symlink(dir.resolve("d1/d11"), dir.resolve("d1/d11/link_to_d11"), relative);
    fa.symlink(dir.resolve("d1/d11/d111"), dir.resolve("d1/d11/link_to_d111"), relative);
    fa.symlink(dir.resolve("d1/d11/d111/d1111"), dir.resolve("d1/d11/link_to_d1111"), relative);
    fa.symlink(dir.resolve("d1/../d2"), dir.resolve("d1/d11/link_to_d2"), relative);
    fa.symlink(dir.resolve("d2/d22"), dir.resolve("d1/d11/link_to_d22"), relative);
    fa.symlink(dir.resolve("d2/d22/d222"), dir.resolve("d1/d11/link_to_d222"), relative);

    fa.symlink(dir.resolve("d1/d11/link_to_d1"), dir.resolve("d2/d22/link_to_link_to_d1"), relative);
    fa.symlink(dir.resolve("d1/d11/link_to_d1"), dir.resolve("d2/another_link_to_link_to_d1"), relative);
    fa.symlink(dir.resolve("d2/another_link_to_link_to_d1"), dir.resolve("link_to_another_link_to_link_to_d1"),
        relative);
  }

  /**
   * Checks if the symlinks exist. This is used by the tests of {@link FileAccessImpl#symlink(Path, Path, boolean)}.
   *
   * @param dir the {@link Path} to the directory where the symlinks are expected.
   */
  private void assertSymlinksExist(Path dir) {

    assertThat(dir.resolve("d1/d11/link_to_d1")).existsNoFollowLinks();
    assertThat(dir.resolve("d1/d11/link_to_d11")).existsNoFollowLinks();
    assertThat(dir.resolve("d1/d11/link_to_d111")).existsNoFollowLinks();
    assertThat(dir.resolve("d1/d11/link_to_d1111")).existsNoFollowLinks();
    assertThat(dir.resolve("d1/d11/link_to_d2")).existsNoFollowLinks();
    assertThat(dir.resolve("d1/d11/link_to_d22")).existsNoFollowLinks();
    assertThat(dir.resolve("d1/d11/link_to_d222")).existsNoFollowLinks();
    assertThat(dir.resolve("d2/d22/link_to_link_to_d1")).existsNoFollowLinks();
    assertThat(dir.resolve("d2/another_link_to_link_to_d1")).existsNoFollowLinks();
    assertThat(dir.resolve("link_to_another_link_to_link_to_d1")).existsNoFollowLinks();
  }

  /**
   * Checks if the symlinks are broken. This is used by the tests of {@link FileAccessImpl#symlink(Path, Path, boolean)}.
   *
   * @param dir the {@link Path} to the directory where the symlinks are expected.
   * @param readLinks - {@code true} if the symbolic link shall be read with {@link Files#readSymbolicLink(Path)}, this does not work for Windows
   *     junctions.
   */
  private void assertSymlinksAreBroken(Path dir, boolean readLinks) throws IOException {

    assertSymlinkIsBroken(dir.resolve("d1/d11/link_to_d1"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d1/d11/link_to_d11"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d1/d11/link_to_d111"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d1/d11/link_to_d1111"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d1/d11/link_to_d2"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d1/d11/link_to_d22"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d1/d11/link_to_d222"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d2/d22/link_to_link_to_d1"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d2/another_link_to_link_to_d1"), readLinks);
    assertSymlinkIsBroken(dir.resolve("link_to_another_link_to_link_to_d1"), readLinks);
  }

  /**
   * Checks if the symlink is broken. This is used by the tests of {@link FileAccessImpl#symlink(Path, Path, boolean)}.
   *
   * @param link the {@link Path} to the link.
   * @param readLinks - {@code true} if the symbolic link shall be read with {@link Files#readSymbolicLink(Path)}, this does not work for Windows
   *     junctions.
   */
  private void assertSymlinkIsBroken(Path link, boolean readLinks) throws IOException {

    try {
      Path realPath = link.toRealPath();
      if (Files.exists(realPath)) {
        fail("The link target " + realPath + " (from toRealPath) should not exist");
      }
    } catch (IOException e) { // toRealPath() throws exception for junctions
      assertThat(e).isInstanceOf(NoSuchFileException.class);
    }
    if (readLinks) {
      Path readPath = Files.readSymbolicLink(link);
      if (Files.exists(readPath)) {
        fail("The link target " + readPath + " (from readSymbolicLink) should not exist");
      }
    }
  }

  /**
   * Checks if the symlinks work. This is used by the tests of {@link FileAccessImpl#symlink(Path, Path, boolean)}.
   *
   * @param dir the {@link Path} to the directory where the symlinks are expected.
   * @param readLinks - {@code true} if the symbolic link shall be read with {@link Files#readSymbolicLink(Path)}, this does not work for Windows
   *     junctions.
   */
  private void assertSymlinksWork(Path dir, boolean readLinks) {

    assertSymlinkToRealPath(dir.resolve("d1/d11/link_to_d1"), dir.resolve("d1"));
    assertSymlinkToRealPath(dir.resolve("d1/d11/link_to_d11"), dir.resolve("d1/d11"));
    assertSymlinkToRealPath(dir.resolve("d1/d11/link_to_d111"), dir.resolve("d1/d11/d111"));
    assertSymlinkToRealPath(dir.resolve("d1/d11/link_to_d1111"), dir.resolve("d1/d11/d111/d1111"));
    assertSymlinkToRealPath(dir.resolve("d1/d11/link_to_d2"), dir.resolve("d2"));
    assertSymlinkToRealPath(dir.resolve("d1/d11/link_to_d22"), dir.resolve("d2/d22"));
    assertSymlinkToRealPath(dir.resolve("d1/d11/link_to_d222"), dir.resolve("d2/d22/d222"));
    assertSymlinkToRealPath(dir.resolve("d2/d22/link_to_link_to_d1"), dir.resolve("d1"));
    assertSymlinkToRealPath(dir.resolve("d2/another_link_to_link_to_d1"), dir.resolve("d1"));
    assertSymlinkToRealPath(dir.resolve("link_to_another_link_to_link_to_d1"), dir.resolve("d1"));

    if (readLinks) {
      assertSymlinkRead(dir.resolve("d1/d11/link_to_d1"), dir.resolve("d1"));
      assertSymlinkRead(dir.resolve("d1/d11/link_to_d11"), dir.resolve("d1/d11"));
      assertSymlinkRead(dir.resolve("d1/d11/link_to_d111"), dir.resolve("d1/d11/d111"));
      assertSymlinkRead(dir.resolve("d1/d11/link_to_d1111"), dir.resolve("d1/d11/d111/d1111"));
      assertSymlinkRead(dir.resolve("d1/d11/link_to_d2"), dir.resolve("d2"));
      assertSymlinkRead(dir.resolve("d1/d11/link_to_d22"), dir.resolve("d2/d22"));
      assertSymlinkRead(dir.resolve("d1/d11/link_to_d222"), dir.resolve("d2/d22/d222"));
      assertSymlinkRead(dir.resolve("d2/d22/link_to_link_to_d1"), dir.resolve("d1/d11/link_to_d1"));
      assertSymlinkRead(dir.resolve("d2/another_link_to_link_to_d1"), dir.resolve("d1/d11/link_to_d1"));
      assertSymlinkRead(dir.resolve("link_to_another_link_to_link_to_d1"),
          dir.resolve("d2/another_link_to_link_to_d1"));
    }
  }

  /**
   * Checks if the symlink works by checking {@link Path#toRealPath(LinkOption...)}} against the {@code trueTarget}. . This is used by the tests of
   * {@link FileAccessImpl#symlink(Path, Path, boolean)}.
   *
   * @param link the {@link Path} to the link.
   * @param trueTarget the {@link Path} to the true target.
   */
  private void assertSymlinkToRealPath(Path link, Path trueTarget) {

    Path realPath = null;
    try {
      realPath = link.toRealPath();
    } catch (IOException e) {
      fail("In method assertSymlinkToRealPath() could not call toRealPath() on link " + link, e);
    }
    assertThat(realPath).exists();
    assertThat(realPath).existsNoFollowLinks();
    assertThat(realPath).isEqualTo(trueTarget);
  }

  /**
   * Checks if the symlink works by checking {@link Files#readSymbolicLink(Path)} against the {@code trueTarget}. This is used by the tests of
   * {@link FileAccessImpl#symlink(Path, Path, boolean)}. Only call this method if junctions are not used, since junctions can not be read with
   * {@link Files#readSymbolicLink(Path)}.
   *
   * @param link the {@link Path} to the link.
   * @param trueTarget the {@link Path} to the true target.
   */
  private void assertSymlinkRead(Path link, Path trueTarget) {

    Path readPath = null;
    try {
      readPath = Files.readSymbolicLink(link);
    } catch (IOException e) {
      fail("In method assertSymlinkRead() could not call readSymbolicLink() on link " + link, e);
    }
    assertThat(link.resolveSibling(readPath)).existsNoFollowLinks();
    assertThat(link.resolveSibling(readPath)).exists();
    try {
      assertThat(link.resolveSibling(readPath).toRealPath(LinkOption.NOFOLLOW_LINKS)).isEqualTo(trueTarget);
    } catch (IOException e) {
      fail("In method assertSymlinkRead() could not call toRealPath() on link.resolveSibling(readPath) for link " + link
          + " and readPath " + readPath, e);
    }
  }

  /**
   * Test of {@link FileAccessImpl#extractZip(Path, Path)}
   */
  @Test
  public void testUnzip(@TempDir Path tempDir) {

    // arrange
    IdeContext context = IdeTestContextMock.get();

    // act
    context.getFileAccess()
        .extractZip(Path.of("src/test/resources/com/devonfw/tools/ide/io/executable_and_non_executable.zip"),
            tempDir);

    // assert
    assertThat(tempDir.resolve("executableFile.txt")).exists();
    assertThat(tempDir.resolve("nonExecutableFile.txt")).exists();
  }

  /**
   * Test of {@link FileAccessImpl#extractTar(Path, Path, TarCompression)} with {@link TarCompression#NONE} and checks if file permissions are preserved on
   * Unix.
   */
  @Test
  public void testUntarWithNoneCompressionWithFilePermissions(@TempDir Path tempDir) {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    if (context.getSystemInfo().isWindows()) {
      return;
    }

    // act
    context.getFileAccess()
        .extractTar(Path.of("src/test/resources/com/devonfw/tools/ide/io").resolve("executable_and_non_executable.tar"),
            tempDir, TarCompression.NONE);

    // assert
    assertPosixFilePermissions(tempDir.resolve("executableFile.txt"), "rwxrwxr-x");
    assertPosixFilePermissions(tempDir.resolve("nonExecutableFile.txt"), "rw-rw-r--");
  }

  /**
   * Test of {@link FileAccessImpl#extractTar(Path, Path, TarCompression)} with {@link TarCompression#GZ} and checks if file permissions are preserved on Unix.
   */
  @Test
  public void testUntarWithGzCompressionWithFilePermissions(@TempDir Path tempDir) {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    if (context.getSystemInfo().isWindows()) {
      return;
    }

    // act
    context.getFileAccess().extractTar(
        Path.of("src/test/resources/com/devonfw/tools/ide/io").resolve("executable_and_non_executable.tar.gz"), tempDir,
        TarCompression.GZ);

    // assert
    assertPosixFilePermissions(tempDir.resolve("executableFile.txt"), "rwxrwxr-x");
    assertPosixFilePermissions(tempDir.resolve("nonExecutableFile.txt"), "rw-rw-r--");
  }

  /**
   * Test of {@link FileAccessImpl#extractTar(Path, Path, TarCompression)} with {@link TarCompression#BZIP2} and checks if file permissions are preserved on
   * Unix.
   */
  @Test
  public void testUntarWithBzip2CompressionWithFilePermissions(@TempDir Path tempDir) {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    if (context.getSystemInfo().isWindows()) {
      return;
    }

    // act
    context.getFileAccess().extractTar(
        Path.of("src/test/resources/com/devonfw/tools/ide/io").resolve("executable_and_non_executable.tar.bz2"),
        tempDir, TarCompression.BZIP2);

    // assert
    assertPosixFilePermissions(tempDir.resolve("executableFile.txt"), "rwxrwxr-x");
    assertPosixFilePermissions(tempDir.resolve("nonExecutableFile.txt"), "rw-rw-r--");
  }

  private void assertPosixFilePermissions(Path file, String permissions) {

    try {
      Set<PosixFilePermission> posixPermissions = Files.getPosixFilePermissions(file);
      String permissionStr = PosixFilePermissions.toString(posixPermissions);
      assertThat(permissions).isEqualTo(permissionStr);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Test of {@link FileAccessImpl#generatePermissionString(int)}.
   */
  @Test
  public void testGeneratePermissionString() {

    assertThat(generatePermissionString(0)).isEqualTo("---------");
    assertThat(generatePermissionString(436)).isEqualTo("rw-rw-r--");
    assertThat(generatePermissionString(948)).isEqualTo("rw-rw-r--");
    assertThat(generatePermissionString(509)).isEqualTo("rwxrwxr-x");
    assertThat(generatePermissionString(511)).isEqualTo("rwxrwxrwx");

  }

  /**
   * Tests if extract was called with disabled extract param, the archive will be moved.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testDisabledExtractMovesArchive(@TempDir Path tempDir) {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    FileAccessImpl fileAccess = new FileAccessImpl(context);
    Path downloadArchive = tempDir.resolve("downloaded.zip");
    fileAccess.touch(downloadArchive);
    Path installationPath = tempDir.resolve("installation");
    Path targetPath = installationPath.resolve("downloaded.zip");
    boolean extract = false;
    // act
    fileAccess.extract(downloadArchive, installationPath, f -> {
    }, extract);
    // assert
    assertThat(targetPath).exists();
  }

  /**
   * Tests if a tgz archive with a sub folder can be extracted to a target folder properly.
   *
   * @param tempDir temporary directory to use.
   * @throws IOException when a file could not be created.
   */
  @Test
  public void testExtractTgzArchive(@TempDir Path tempDir) throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    FileAccessImpl fileAccess = new FileAccessImpl(context);
    Path downloadedTgz = tempDir.resolve("downloaded.tgz");
    fileAccess.touch(downloadedTgz);
    try (GzipCompressorOutputStream gzipOut = new GzipCompressorOutputStream(Files.newOutputStream(downloadedTgz, StandardOpenOption.WRITE));
        TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzipOut)) {

      // Create a subfolder entry
      TarArchiveEntry subfolderEntry = new TarArchiveEntry("subfolder/");
      subfolderEntry.setMode(TarArchiveEntry.DEFAULT_DIR_MODE);
      tarOut.putArchiveEntry(subfolderEntry);
      tarOut.closeArchiveEntry();

      // Add a file to the subfolder
      TarArchiveEntry fileEntry = new TarArchiveEntry("subfolder/testfile2.txt");
      fileEntry.setSize(12);
      tarOut.putArchiveEntry(fileEntry);
      tarOut.write("Hello World2".getBytes());
      tarOut.closeArchiveEntry();

      // create a file in the root of the archive
      TarArchiveEntry entry = new TarArchiveEntry("testfile.txt");
      entry.setSize(11);
      tarOut.putArchiveEntry(entry);
      tarOut.write("Hello World".getBytes());
      tarOut.closeArchiveEntry();
    }
    Path installationPath = tempDir.resolve("installation");
    // act
    fileAccess.extractTar(downloadedTgz, installationPath, TarCompression.GZ);
    // assert
    assertThat(installationPath.resolve("testfile.txt")).exists();
    assertThat(installationPath.resolve("subfolder").resolve("testfile2.txt")).exists();
  }

  /**
   * Tests if a file can be found within a list of folders.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testFindExistingFileInFolders(@TempDir Path tempDir) {
    IdeContext context = IdeTestContextMock.get();
    FileAccessImpl fileAccess = new FileAccessImpl(context);
    Path subfolder1 = tempDir.resolve("subfolder1");
    fileAccess.mkdirs(subfolder1);
    fileAccess.touch(subfolder1.resolve("testfile"));
    Path subfolder2 = tempDir.resolve("subfolder2");
    fileAccess.mkdirs(subfolder2);
    fileAccess.touch(subfolder2.resolve("targetfile"));
    List<Path> pathList = new ArrayList<>();
    pathList.add(subfolder1);
    pathList.add(subfolder2);
    Path foundFile = fileAccess.findExistingFile("targetfile", pathList);
    assertThat(foundFile).exists();
  }


  /**
   * Tests if {@link FileAccess#download(String, Path)} of a file path as URL will copy the file and use a progress-bar if the file size is bigger than 100Kb.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testDownloadLargeFileWithProgressBar(@TempDir Path tempDir) throws IOException {

    //arrange
    String taskName = "Copying";
    IdeTestContext context = newContext(tempDir);
    Path tempFile = tempDir.resolve("tempFile");
    Path archiveFile = tempDir.resolve("targetFolder");
    FileAccess fileAccess = context.getFileAccess();
    String line = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
    List<String> lines = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      lines.add(line);
    }
    fileAccess.writeFileLines(lines, tempFile);
    long fileSize = Files.size(tempFile);
    String source = tempFile.toString();

    //act
    fileAccess.download(source, archiveFile);

    //assert
    assertProgressBar(context, "Copying", fileSize);
    assertThat(archiveFile).hasSize(fileSize).hasSameBinaryContentAs(tempFile);
    IdeProgressBarTestImpl progressBar = context.getProgressBarMap().get(taskName);
    assertThat(progressBar.getMaxSize()).isEqualTo(fileSize);
  }


  /**
   * Tests if {@link FileAccess#download(String, Path)} of a file path as URL will copy the file and not use a progress-bar if the file size is small.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testDownloadSmallFileWithoutProgressBar(@TempDir Path tempDir) throws IOException {

    //arrange
    String taskName = "Copying";
    IdeTestContext context = newContext(tempDir);
    Path tempFile = tempDir.resolve("tempFile");
    Path archiveFile = tempDir.resolve("targetFolder");
    FileAccess fileAccess = context.getFileAccess();
    String line = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
    fileAccess.writeFileContent(line, tempFile);
    long fileSize = 100;
    String source = tempFile.toString();

    //act
    fileAccess.download(source, archiveFile);

    //assert
    assertThat(context.getProgressBarMap()).isEmpty();
    assertThat(archiveFile).hasSize(fileSize).hasSameBinaryContentAs(tempFile);
  }

  /**
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} when broken junctions exist. This simulates the scenario
   * described in issue #1169 where mklink fails with "Cannot create a file when that file already exists" when trying
   * to create a junction over a broken junction.
   */
  @Test
  public void testSymlinkOverwritesBrokenJunction(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    if (!windowsJunctionsAreUsed(context, tempDir)) {
      context.info("Test skipped: Windows junctions are not used in this environment.");
      return;
    }

    FileAccess fileAccess = new FileAccessImpl(context);
    Path sourceDir = tempDir.resolve("source");
    Path targetLink = tempDir.resolve("junction");

    // Create initial source directory and junction
    fileAccess.mkdirs(sourceDir);
    fileAccess.symlink(sourceDir, targetLink, false);

    // Verify junction was created and works
    assertThat(targetLink).existsNoFollowLinks();
    assertThat(targetLink.toRealPath()).isEqualTo(sourceDir);

    // Simulate the scenario: delete the source directory to break the junction
    fileAccess.delete(sourceDir);

    // Verify the junction is now broken (exists but points to non-existent target)
    assertThat(targetLink).existsNoFollowLinks(); // junction still exists
    assertThat(sourceDir).doesNotExist(); // but target is gone

    // Create a new source directory at different location
    Path newSourceDir = tempDir.resolve("newSource");
    fileAccess.mkdirs(newSourceDir);

    // act - This should not fail even though a broken junction exists at targetLink
    fileAccess.symlink(newSourceDir, targetLink, false);

    // assert - The junction should now point to the new source
    assertThat(targetLink).existsNoFollowLinks();
    assertThat(targetLink.toRealPath()).isEqualTo(newSourceDir);
  }

  /**
   * Test of enhanced {@link FileAccessImpl#isJunction(Path)} method to ensure it handles broken junctions gracefully.
   * This simulates the enhanced logic for detecting broken junctions on non-Windows systems.
   */
  @Test
  public void testIsJunctionHandlesBrokenLinks(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    FileAccess fileAccess = new FileAccessImpl(context);

    if (!context.getSystemInfo().isWindows()) {
      // On non-Windows, create a broken symlink to simulate a broken junction
      Path sourceDir = tempDir.resolve("source");
      Path brokenLink = tempDir.resolve("brokenLink");

      fileAccess.mkdirs(sourceDir);
      fileAccess.symlink(sourceDir, brokenLink, false);

      // Verify link works initially
      assertThat(brokenLink).existsNoFollowLinks();
      assertThat(brokenLink.toRealPath()).isEqualTo(sourceDir);

      // Delete the source to break the link
      fileAccess.delete(sourceDir);

      // The broken symlink should still exist but point to nothing
      assertThat(brokenLink).existsNoFollowLinks();
      assertThat(sourceDir).doesNotExist();

      // act & assert - the enhanced symlink method should handle the broken link
      Path newSource = tempDir.resolve("newSource");
      fileAccess.mkdirs(newSource);

      // This should not fail, even with the broken symlink
      fileAccess.symlink(newSource, brokenLink, false);
      assertThat(brokenLink.toRealPath()).isEqualTo(newSource);
    } else {
      context.info("Test adapted for Windows environment - testing basic junction functionality");
      // On Windows, just test that basic junction functionality works
      Path sourceDir = tempDir.resolve("source");
      Path junctionLink = tempDir.resolve("junction");

      fileAccess.mkdirs(sourceDir);
      fileAccess.symlink(sourceDir, junctionLink, false);

      assertThat(junctionLink).existsNoFollowLinks();
      assertThat(junctionLink.toRealPath()).isEqualTo(sourceDir);
    }
  }

}
