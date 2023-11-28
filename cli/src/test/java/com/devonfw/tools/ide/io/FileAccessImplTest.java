package com.devonfw.tools.ide.io;

import static com.devonfw.tools.ide.logging.Log.info;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;

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

  @Test
  public void testSymlinkNotRelative(@TempDir Path tempDir) {

    // relative links are checked in testRelativeLinksWorkAfterMoving

    // arrange
    IdeContext context = IdeTestContextMock.get();
    FileAccess fileAccess = new FileAccessImpl(context);
    Path dir = tempDir.resolve("parent");
    createDirs(fileAccess, dir);

    // act
    createSymlinks(fileAccess, dir, false);

    // assert
    assertSymlinksExist(dir);
    assertSymlinksWork(dir, false, false);
  }

  @Test
  public void testSymlinkAbsoluteAsFallback(@TempDir Path tempDir) {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    if (!windowsJunctionsAreUsed(context, tempDir)) {
      info("Can not check the Test: testSymlinkAbsoluteAsFallback since windows junctions are not used.");
      return;
    }
    FileAccess fileAccess = new FileAccessImpl(context);
    Path dir = tempDir.resolve("parent");
    createDirs(fileAccess, dir);

    // act
    createSymlinks(fileAccess, dir, true);

    // assert
    assertSymlinksExist(dir);
    assertSymlinksWork(dir, false, false);
  }

  @Test
  public void testAbsoluteLinksBreakAfterMoving(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    if (windowsJunctionsAreUsed(context, tempDir)) {
      info("Can not check the Test: testAbsoluteLinksBreakAfterMoving since windows junctions are used.");
      return;
    }
    FileAccess fileAccess = new FileAccessImpl(context);
    Path dir = tempDir.resolve("parent");
    createDirs(fileAccess, dir);
    createSymlinks(fileAccess, dir, false);

    // act
    Path sibling = dir.resolveSibling("parent2");
    fileAccess.move(dir, sibling);

    // assert
    assertSymlinksExist(sibling);
    assertSymlinksAreBroken(sibling);
  }

  @Test
  public void testRelativeLinksWorkAfterMoving(@TempDir Path tempDir) {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    if (windowsJunctionsAreUsed(context, tempDir)) {
      info("Can not check the Test: testRelativeLinksWorkAfterMoving since windows junctions are used.");
      return;
    }
    FileAccess fileAccess = new FileAccessImpl(context);
    Path dir = tempDir.resolve("parent");
    createDirs(fileAccess, dir);
    createSymlinks(fileAccess, dir, true);

    // act
    Path sibling = dir.resolveSibling("parent2");
    fileAccess.move(dir, sibling);

    // assert
    assertSymlinksExist(sibling);
    assertSymlinksWork(sibling, true, false);
  }

  public void testMakeSymlinksRelative(@TempDir Path tempDir) {

    // test follow target true and false

    // test on junctions, dir and file,
  }

  @Test
  public void testWindowsJunctionsCanNotPointToFiles(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    if (!windowsJunctionsAreUsed(context, tempDir)) {
      info("Can not check the Test: testWindowsJunctionsCanNotPointToFiles since windows junctions are not used.");
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

  private void assertSymlinksAreBroken(Path dir) throws IOException {

    assertSymlinkIsBroken(dir.resolve("d1/d11/link1"));
    assertSymlinkIsBroken(dir.resolve("d1/d11/link2"));
    assertSymlinkIsBroken(dir.resolve("d1/d11/link3"));
    assertSymlinkIsBroken(dir.resolve("d1/d11/link4"));
    assertSymlinkIsBroken(dir.resolve("d1/d11/link5"));
    assertSymlinkIsBroken(dir.resolve("d1/d11/link6"));
    assertSymlinkIsBroken(dir.resolve("d1/d11/link7"));
    assertSymlinkIsBroken(dir.resolve("d2/d22/link8"));
    assertSymlinkIsBroken(dir.resolve("d2/link9"));
    assertSymlinkIsBroken(dir.resolve("link10"));
  }

  private void assertSymlinkIsBroken(Path link) throws IOException {

    Path realPath = link.toRealPath();
    if (Files.exists(realPath)) {
      fail("The link target " + realPath + " (from toRealPath) should not exist");
    }
    Path readPath = Files.readSymbolicLink(link);
    if (!Files.exists(readPath)) {
      fail("The link target " + readPath + " (from readSymbolicLink) should not exist");
    }
  }

  /***
   *
   * @param readLinks - {@code true} only pass this when junctions are not used.
   */
  private void assertSymlinksWork(Path dir, boolean readLinks, boolean followTarget) {

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
      if (followTarget) {
        assertSymlinkRead(dir.resolve("d2/d22/link8"), dir.resolve("d1"));
        assertSymlinkRead(dir.resolve("d2/link9"), dir.resolve("d1"));
        assertSymlinkRead(dir.resolve("link10"), dir.resolve("d1"));
      } else {
        assertSymlinkRead(dir.resolve("d2/d22/link8"), dir.resolve("d1/d11/link1"));
        assertSymlinkRead(dir.resolve("d2/link9"), dir.resolve("d1/d11/link1"));
        assertSymlinkRead(dir.resolve("link10"), dir.resolve("d2/link9"));
      }
    }
  }

  private void assertSymlinkToRealPath(Path link, Path trueTarget) {

    Path realPath = null;
    try {
      realPath = link.toRealPath();
    } catch (IOException e) {
      fail("Could not call toRealPath on link: " + link, e); // TODO this may also be moved to method declarations
      // "throws IOException"
    }
    assertThat(realPath).exists();
    assertThat(realPath).existsNoFollowLinks();
    assertThat(realPath).isEqualTo(trueTarget);

  }

  private void assertSymlinkRead(Path link, Path trueTarget) {

    // only call this on relative links

    Path readPath = null;
    try {
      readPath = Files.readSymbolicLink(link);
    } catch (IOException e) {
      fail("Could not call Files.readSymbolicLink on link: " + link, e);
    }
    assertThat(link.resolveSibling(readPath)).existsNoFollowLinks();
    assertThat(link.resolveSibling(readPath)).exists();
    assertThat(readPath.getFileName()).isEqualTo((trueTarget.getFileName()));
  }
}
