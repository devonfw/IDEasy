package com.devonfw.tools.ide.io;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;

/**
 * Test of {@link FileAccessImpl}.
 */
public class FileAccessImplTest extends AbstractIdeContextTest {

  private boolean windowsJunctionsAreUsed(IdeContext context, Path dir) {

    Path source = dir.resolve("checkIfWindowsJunctionsAreUsed");
    Path link = dir.resolve("checkIfWindowsJunctionsAreUsedLink");
    context.getFileAccess().mkdirs(source);
    try {
      Files.createSymbolicLink(link, source);
    } catch (IOException e) {
      return context.getSystemInfo().isWindows();
    }
    return false;
  }

  /**
   * Test of {@link FileAccessImpl#symlink(Path, Path, boolean)} with "relative = false".
   */
  @Test
  public void testSymlinkNotRelative(@TempDir Path tempDir) {

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
  public void testAbsoluteLinksBreakAfterMoving(@TempDir Path tempDir) throws IOException {

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
   * the links still work after moving them.
   */
  @Test
  public void testRelativeLinksWorkAfterMoving(@TempDir Path tempDir) {

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
  public void testWindowsJunctionsCanNotPointToFiles(@TempDir Path tempDir) throws IOException {

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

  private void createDirs(FileAccess fileAccess, Path dir) {

    fileAccess.mkdirs(dir.resolve("d1/d11/d111/d1111"));
    fileAccess.mkdirs(dir.resolve("d2/d22/d222"));
  }

  private void createSymlinks(FileAccess fa, Path dir, boolean relative) {

    fa.symlink(dir.resolve("d1/d11"), dir.resolve("d1/d11/link1"), relative);
    // test if symbolic links or junctions can be overwritten with symlink()
    fa.symlink(dir.resolve("d1"), dir.resolve("d1/d11/link1"), relative);

    fa.symlink(dir.resolve("d1/d11"), dir.resolve("d1/d11/link2"), relative);
    fa.symlink(dir.resolve("d1/d11/d111"), dir.resolve("d1/d11/link3"), relative);
    fa.symlink(dir.resolve("d1/d11/d111/d1111"), dir.resolve("d1/d11/link4"), relative);
    fa.symlink(dir.resolve("d2"), dir.resolve("d1/d11/link5"), relative);
    fa.symlink(dir.resolve("d2/d22"), dir.resolve("d1/d11/link6"), relative);
    fa.symlink(dir.resolve("d2/d22/d222"), dir.resolve("d1/d11/link7"), relative);

    fa.symlink(dir.resolve("d1/d11/link1"), dir.resolve("d2/d22/link8"), relative);
    fa.symlink(dir.resolve("d1/d11/link1"), dir.resolve("d2/link9"), relative);
    fa.symlink(dir.resolve("d2/link9"), dir.resolve("link10"), relative);
  }

  private void assertSymlinksExist(Path dir) {

    assertThat(dir.resolve("d1/d11/link1")).existsNoFollowLinks();
    assertThat(dir.resolve("d1/d11/link2")).existsNoFollowLinks();
    assertThat(dir.resolve("d1/d11/link3")).existsNoFollowLinks();
    assertThat(dir.resolve("d1/d11/link4")).existsNoFollowLinks();
    assertThat(dir.resolve("d1/d11/link5")).existsNoFollowLinks();
    assertThat(dir.resolve("d1/d11/link6")).existsNoFollowLinks();
    assertThat(dir.resolve("d1/d11/link7")).existsNoFollowLinks();
    assertThat(dir.resolve("d2/d22/link8")).existsNoFollowLinks();
    assertThat(dir.resolve("d2/link9")).existsNoFollowLinks();
    assertThat(dir.resolve("link10")).existsNoFollowLinks();
  }

  private void assertSymlinksAreBroken(Path dir, boolean readLinks) throws IOException {

    assertSymlinkIsBroken(dir.resolve("d1/d11/link1"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d1/d11/link2"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d1/d11/link3"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d1/d11/link4"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d1/d11/link5"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d1/d11/link6"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d1/d11/link7"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d2/d22/link8"), readLinks);
    assertSymlinkIsBroken(dir.resolve("d2/link9"), readLinks);
    assertSymlinkIsBroken(dir.resolve("link10"), readLinks);
  }

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


   // only pass readLinks = true when junctions are not used.
  private void assertSymlinksWork(Path dir, boolean readLinks) {

    assertSymlinkToRealPath(dir.resolve("d1/d11/link1"), dir.resolve("d1"));
    assertSymlinkToRealPath(dir.resolve("d1/d11/link2"), dir.resolve("d1/d11"));
    assertSymlinkToRealPath(dir.resolve("d1/d11/link3"), dir.resolve("d1/d11/d111"));
    assertSymlinkToRealPath(dir.resolve("d1/d11/link4"), dir.resolve("d1/d11/d111/d1111"));
    assertSymlinkToRealPath(dir.resolve("d1/d11/link5"), dir.resolve("d2"));
    assertSymlinkToRealPath(dir.resolve("d1/d11/link6"), dir.resolve("d2/d22"));
    assertSymlinkToRealPath(dir.resolve("d1/d11/link7"), dir.resolve("d2/d22/d222"));
    assertSymlinkToRealPath(dir.resolve("d2/d22/link8"), dir.resolve("d1"));
    assertSymlinkToRealPath(dir.resolve("d2/link9"), dir.resolve("d1"));
    assertSymlinkToRealPath(dir.resolve("link10"), dir.resolve("d1"));

    if (readLinks) {
      assertSymlinkRead(dir.resolve("d1/d11/link1"), dir.resolve("d1"));
      assertSymlinkRead(dir.resolve("d1/d11/link2"), dir.resolve("d1/d11"));
      assertSymlinkRead(dir.resolve("d1/d11/link3"), dir.resolve("d1/d11/d111"));
      assertSymlinkRead(dir.resolve("d1/d11/link4"), dir.resolve("d1/d11/d111/d1111"));
      assertSymlinkRead(dir.resolve("d1/d11/link5"), dir.resolve("d2"));
      assertSymlinkRead(dir.resolve("d1/d11/link6"), dir.resolve("d2/d22"));
      assertSymlinkRead(dir.resolve("d1/d11/link7"), dir.resolve("d2/d22/d222"));
      assertSymlinkRead(dir.resolve("d2/d22/link8"), dir.resolve("d1/d11/link1"));
      assertSymlinkRead(dir.resolve("d2/link9"), dir.resolve("d1/d11/link1"));
      assertSymlinkRead(dir.resolve("link10"), dir.resolve("d2/link9"));
    }
  }

  private void assertSymlinkToRealPath(Path link, Path trueTarget) {

    Path realPath = null;
    try {
      realPath = link.toRealPath();
    } catch (IOException e) {
      fail("Could not call toRealPath on link: " + link, e);
    }
    assertThat(realPath).exists();
    assertThat(realPath).existsNoFollowLinks();
    assertThat(realPath).isEqualTo(trueTarget);
  }

  private void assertSymlinkRead(Path link, Path trueTarget) {

    // only call this method if junctions are not used

    Path readPath = null;
    try {
      readPath = Files.readSymbolicLink(link);
    } catch (IOException e) {
      fail("Could not call Files.readSymbolicLink on link: " + link, e);
    }
    assertThat(link.resolveSibling(readPath)).existsNoFollowLinks();
    assertThat(link.resolveSibling(readPath)).exists();
    try {
      assertThat(link.resolveSibling(readPath).toRealPath(LinkOption.NOFOLLOW_LINKS)).isEqualTo(trueTarget);
    } catch (IOException e) {
      fail("Couldn't link.resolveSibling(readPath).toRealPath() in assertSymlinkRead:", e);
    }
  }
}
