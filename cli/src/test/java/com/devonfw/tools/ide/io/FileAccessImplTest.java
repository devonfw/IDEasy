package com.devonfw.tools.ide.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;

public class FileAccessImplTest extends Assertions {

  void arrangetestRelativeSymlinks(Path tempDir, FileAccess fileAccess) {

  }

  @Test
  void testSymlink(@TempDir Path tempDir) {

    IdeContext context = IdeTestContextMock.get();
    FileAccess fileAccess = new FileAccessImpl(context);
    // create a new directory
    Path dir = tempDir.resolve("dir");
    fileAccess.mkdirs(dir);

    // create a new file using nio
    Path file = tempDir.resolve("file");
    try {
      Files.write(file, "Hello World!".getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // try to create a symlink to the file using Files.createSymbolicLink
    Path link = tempDir.resolve("link");
    Path linkToLink = tempDir.resolve("linkToLink");

    boolean junctionsUsed = false;
    try {
      Files.createSymbolicLink(link, file);
    } catch (IOException e) { // if permission is not hold, then junctions are used instead of symlinks (on Windows)
      if (context.getSystemInfo().isWindows()) {
        junctionsUsed = true;
        // this should work
        fileAccess.symlink(dir, link);
        fileAccess.symlink(dir, link); // should work again
        fileAccess.symlink(link, linkToLink);

        IllegalStateException e1 = assertThrows(IllegalStateException.class, () -> {
          fileAccess.symlink(file, link);
        });
        assertThat(e1).hasMessageContaining("These junctions can only point to directories or other junctions");

        IllegalStateException e2 = assertThrows(IllegalStateException.class, () -> {
          fileAccess.symlink(Paths.get("dir"), link);
        });
        assertThat(e2).hasMessageContaining("These junctions can only point to absolute paths");
      } else {
        throw new RuntimeException(
            "Creating symbolic link with Files.createSymbolicLink failed and junctions can not be used since the OS is not windows: "
                + e.getMessage());
      }
    }

    // test for normal symlinks (not junctions)
    if (!junctionsUsed) {
      try {
        fileAccess.symlink(file, link); // should work again
        fileAccess.symlink(link, linkToLink);
      } catch (Exception e) {
        fail("Creating symbolic links failed: " + e.getMessage());
      }
      try {
        assertEquals(linkToLink.toRealPath(), file);
        assertEquals(Files.readSymbolicLink(linkToLink), link);
      } catch (IOException e) {
        fail("Reading symbolic links failed: " + e.getMessage());
      }
    }
  }

  @Test
  void testMakeSymlinkRelative(@TempDir Path tempDir) {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    FileAccess fileAccess = new FileAccessImpl(context);
    Path parent = tempDir.resolve("parent");
    Path d1 = parent.resolve("d1");
    Path d11 = d1.resolve("d11");
    Path d111 = d11.resolve("d111");
    Path d1111 = d111.resolve("d1111");
    Path d2 = parent.resolve("d2");
    Path d22 = d2.resolve("d22");
    Path d222 = d22.resolve("d222");
    Path[] dirPaths = new Path[] { parent, d1, d11, d111, d1111, d2, d22, d222 };
    for (Path dirPath : dirPaths) {
      fileAccess.mkdirs(dirPath);
    }
    Path link_d11_d1 = d11.resolve("link_d11_d1");
    fileAccess.symlink(d1, link_d11_d1);

    Path link_d11_d11 = d11.resolve("link_d11_d11");
    fileAccess.symlink(d11, link_d11_d11);

    Path link_d11_d111 = d11.resolve("link_d11_d111");
    fileAccess.symlink(d111, link_d11_d111);

    Path link_d11_d1111 = d11.resolve("link_d11_d1111");
    fileAccess.symlink(d1111, link_d11_d1111);

    Path link_d11_d2 = d11.resolve("link_d11_d2");
    fileAccess.symlink(d2, link_d11_d2);

    Path link_d11_d22 = d11.resolve("link_d11_d22");
    fileAccess.symlink(d22, link_d11_d22);

    Path link_d11_d222 = d11.resolve("link_d11_d222");
    fileAccess.symlink(d222, link_d11_d222);

    Path link_d22_link_d11_d1 = d22.resolve("link_d22_link_d11_d1");
    fileAccess.symlink(link_d11_d1, link_d22_link_d11_d1);

    Path link_d2_link_d11_d1 = d2.resolve("link_d2_link_d11_d1");
    fileAccess.symlink(link_d11_d1, link_d2_link_d11_d1);

    Path link_parent_link_d2_link_d11_d1 = parent.resolve("link_parent_link_d2_link_d11_d1");
    fileAccess.symlink(link_d2_link_d11_d1, link_parent_link_d2_link_d11_d1);

    Path[] links = new Path[] { link_d11_d1, link_d11_d11, link_d11_d111, link_d11_d1111, link_d11_d2, link_d11_d22,
    link_d11_d222, link_d22_link_d11_d1, link_d2_link_d11_d1, link_parent_link_d2_link_d11_d1 };

    // act: check if moving breaks absolute symlinks
    Path parent2 = tempDir.resolve("parent2");
    fileAccess.move(parent, parent2);

    // assert: check if moving breaks absolute symlinks
    Function<Path, Path> transformPath = path -> {
      String newPath = path.toString().replace("_parent_", "_par_").replace("parent", "parent2").replace("_par_",
          "_parent_");
      return Paths.get(newPath);
    };
    for (Path link : links) {
      try {
        Path linkInParent2 = transformPath.apply(link);
        assertThat(linkInParent2).existsNoFollowLinks();
        Path realPath = linkInParent2.toRealPath();
        if (Files.exists(realPath)) {
          fail("The link target " + realPath + " (from toRealPath) should not exist");
        }
        Path readPath = Files.readSymbolicLink(linkInParent2);
        if (!Files.exists(readPath)) {
          fail("The link target " + readPath + "  (from readSymbolicLink) should not exist");
        }
      } catch (IOException e) {
        assertThat(e).isInstanceOf(IOException.class);
      }
    }

    // assert: Can't call makeSymlinkRelative since it is not a symbolic link
    IllegalStateException e1 = assertThrows(IllegalStateException.class, () -> {
      fileAccess.makeSymlinkRelative(d1);
    });
    assertThat(e1).hasMessageContaining("is not a symbolic link");

    boolean junctionsUsed = false;
    try {
      Files.createSymbolicLink(tempDir.resolve("my_test_link"), parent2);
    } catch (IOException e) {
      if (!context.getSystemInfo().isWindows()) {
        fail("Creating symbolic link with Files.createSymbolicLink failed and junctions can not be used"
            + " since the OS is not windows: " + e.getMessage());
      }
      junctionsUsed = true;
      IllegalStateException e2 = assertThrows(IllegalStateException.class, () -> {
        fileAccess.makeSymlinkRelative(link_d2_link_d11_d1);
      });
      assertThat(e2).hasMessageContaining("is not a symbolic link");
    }

    // act: make symlinks relative and move
    fileAccess.move(parent2, parent); // revert previous move
    if (!junctionsUsed) {
      for (Path link : links) {
        if (link.equals(link_d2_link_d11_d1)) {
          fileAccess.makeSymlinkRelative(link, false);
        } else {
          fileAccess.makeSymlinkRelative(link, true);
        }
      }

      // redo move, and check later if symlinks still work
      fileAccess.move(parent, parent2);

      // assert
      for (Path link : links) {
        Path linkInParent2 = transformPath.apply(link);
        if (link.equals(link_d2_link_d11_d1)) {
          try { // checking if the transformation of absolute to relative path with flag followTarget=false works
            Path correct = transformPath.apply(link_d11_d1);
            assertEquals(correct, linkInParent2.getParent().resolve(Files.readSymbolicLink(linkInParent2))
                .toRealPath(LinkOption.NOFOLLOW_LINKS));
          } catch (IOException e) {
            throw new RuntimeException("Couldn't get path of link where followTarget was set to false: ", e);
          }
        }
        assertThat(linkInParent2).existsNoFollowLinks();
        try {
          Path realPath = linkInParent2.toRealPath();
          assertThat(realPath).existsNoFollowLinks();
          assertThat(realPath).exists();
        } catch (IOException e) {
          throw new RuntimeException("Could not call toRealPath on moved relative link: " + linkInParent2, e);
        }
        try {
          Path readPath = Files.readSymbolicLink(linkInParent2);
          assertThat(linkInParent2.getParent().resolve(readPath)).existsNoFollowLinks();
          assertThat(linkInParent2.getParent().resolve(readPath)).exists();
        } catch (IOException e) {
          throw new RuntimeException("Could not call Files.readSymbolicLink on moved relative link: " + linkInParent2,
              e);
        }
      }
    }
  }
}
