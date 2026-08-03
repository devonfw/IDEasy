package com.devonfw.tools.ide.url.model.file.json;

import java.util.HashSet;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link StatusJson}.
 */
class StatusJsonTest extends Assertions {

  @Test
  void testHashCollision() {

    // arrange
    Set<String> urls = Set.of("https://archive.eclipse.org/technology/epp/downloads/release/2022-09/R/eclipse-cpp-2022-09-R-linux-gtk-aarch64.tar.gz",
        "https://archive.eclipse.org/technology/epp/downloads/release/2022-09/R/eclipse-cpp-2022-09-R-linux-gtk-x86_64.tar.gz",
        "https://archive.eclipse.org/technology/epp/downloads/release/2022-09/R/eclipse-cpp-2022-09-R-macosx-cocoa-aarch64.tar.gz",
        "https://archive.eclipse.org/technology/epp/downloads/release/2022-09/R/eclipse-cpp-2022-09-R-macosx-cocoa-x86_64.tar.gz",
        "https://archive.eclipse.org/technology/epp/downloads/release/2022-09/R/eclipse-cpp-2022-09-R-win32-x86_64.zip");
    Set<Integer> hashes = new HashSet<>(urls.size());

    // act
    for (String url : urls) {
      boolean added = hashes.add(StatusJson.computeKey(url));
      // assert
      assertThat(added).as("hash of %s is unique").isTrue();
    }
  }

}
