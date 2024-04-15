package com.devonfw.tools.ide.io;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static com.devonfw.tools.ide.io.FileAccessImpl.generatePermissionString;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} with "relative = false". Passing absolute paths as
   * source.
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
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} with "relative = false". Passing relative paths as
   * source.
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
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} with "relative = true". But Windows junctions are used
   * and therefore the fallback from relative to absolute paths is tested.
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
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} with "relative = false". Furthermore, it is tested that
   * the links are broken after moving them.
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
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} with "relative = true". Furthermore, it is tested that
   * the links still work after moving them. Passing relative paths as source.
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
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} with "relative = true". Furthermore, it is tested that
   * the links still work after moving them.
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
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} when Windows junctions are used and the source is a
   * file.
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
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} and whether the source paths are simplified correctly
   * by {@link Path#toRealPath(LinkOption...)}.
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
   * Creates the symlinks with passing relative paths as source. This is used by the tests of
   * {@link FileAccessImpl#symlink(Path, Path, boolean)}.
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
   * Creates the symlinks with passing absolute paths as source. This is used by the tests of
   * {@link FileAccessImpl#symlink(Path, Path, boolean)}.
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
   * Checks if the symlinks are broken. This is used by the tests of
   * {@link FileAccessImpl#symlink(Path, Path, boolean)}.
   *
   * @param dir the {@link Path} to the directory where the symlinks are expected.
   * @param readLinks - {@code true} if the symbolic link shall be read with {@link Files#readSymbolicLink(Path)}, this
   * does not work for Windows junctions.
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
   * @param readLinks - {@code true} if the symbolic link shall be read with {@link Files#readSymbolicLink(Path)}, this
   * does not work for Windows junctions.
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
   * @param readLinks - {@code true} if the symbolic link shall be read with {@link Files#readSymbolicLink(Path)}, this
   * does not work for Windows junctions.
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
   * Checks if the symlink works by checking {@link Path#toRealPath(LinkOption...)}} against the {@code trueTarget}. .
   * This is used by the tests of {@link FileAccessImpl#symlink(Path, Path, boolean)}.
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
   * Checks if the symlink works by checking {@link Files#readSymbolicLink(Path)} against the {@code trueTarget}. This
   * is used by the tests of {@link FileAccessImpl#symlink(Path, Path, boolean)}. Only call this method if junctions are
   * not used, since junctions can not be read with {@link Files#readSymbolicLink(Path)}.
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
   * Test of {@link FileAccessImpl#extractTar(Path, Path, TarCompression)} with {@link TarCompression#NONE} and checks
   * if file permissions are preserved on Unix.
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
   * Test of {@link FileAccessImpl#extractTar(Path, Path, TarCompression)} with {@link TarCompression#GZ} and checks if
   * file permissions are preserved on Unix.
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
   * Test of {@link FileAccessImpl#extractTar(Path, Path, TarCompression)} with {@link TarCompression#BZIP2} and checks
   * if file permissions are preserved on Unix.
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

}
