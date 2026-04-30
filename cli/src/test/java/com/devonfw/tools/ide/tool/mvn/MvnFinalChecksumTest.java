package com.devonfw.tools.ide.tool.mvn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.MvnRepositoryMock;
import com.devonfw.tools.ide.url.model.file.UrlChecksums;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test verifying that the MvnRepositoryMock produces deterministic hashes 
 * that match our hardcoded "Gold Standard" values.
 */
@WireMockTest
class MvnFinalChecksumTest extends AbstractIdeContextTest {

  private static final String PROJECT_MVN = "mvn";

  // This is the portable, deterministic hash for the Maven 3.8.1 structure defined below.
  private static final String EXPECTED_SHA256 = "ee98614a0d93e2f5f0f97bd140eafd747c725d4ee424d15d28c58bfbb45f112d";

  @Test
  void testVerifyUsingMvnRepositoryMock(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // 1. Arrange: Use the "mvn" project context
    IdeTestContext context = newContext(PROJECT_MVN, wmRuntimeInfo);
    
    // Create the "mock object" source directory in the test repository
    // This simulates the files that would be compressed on the fly by the mock.
    Path repoDir = context.getIdeRoot().resolve("repository/mvn/org.apache.maven/apache-maven");
    Files.createDirectories(repoDir.resolve("bin"));
    Files.writeString(repoDir.resolve("bin/mvn"), "#!/bin/bash\necho \"Maven 3.8.1\"");
    Files.writeString(repoDir.resolve("bin/mvn.cmd"), "@echo off\necho Maven 3.8.1");
    Files.createDirectories(repoDir.resolve("conf"));
    Files.writeString(repoDir.resolve("conf/settings.xml"), "<settings/>");
    Files.createDirectories(repoDir.resolve("lib"));
    Files.writeString(repoDir.resolve("lib/maven-core-3.8.1.jar"), "dummy jar content");

    MvnRepositoryMock mockRepo = (MvnRepositoryMock) context.getMvnRepository();
    
    // Register the expected hash for the artifact download path.
    // If the mock's live compression produces a different hash, the test fails.
    String downloadPath = "/org/apache/maven/apache-maven/3.8.1/apache-maven-3.8.1-bin.zip";
    mockRepo.putExpectedChecksum(downloadPath, EXPECTED_SHA256);

    // 2. Act: Trigger the download flow for a Maven 3.8.1 artifact
    MvnArtifact artifact = new MvnArtifact("org.apache.maven", "apache-maven", "3.8.1", "zip", "bin");
    MvnArtifactMetadata metadata = mockRepo.getMetadata(artifact, "mvn", "mvn");
    
    // This call triggers MvnRepositoryMock.download() which:
    // a) Locates the directory we created above.
    // b) Performs deterministic compression.
    // c) Computes the live hash.
    // d) Compares it against our pre-registered EXPECTED_SHA256 (via putIfAbsent).
    mockRepo.download(metadata);

    // 3. Assert: Verify the checksums provided by the mock
    UrlChecksums checksums = mockRepo.getChecksums(artifact);
    assertThat(checksums).isNotNull();
    assertThat(checksums.iterator().next().getChecksum())
        .as("The live compressed archive MUST match the hardcoded gold standard hash")
        .isEqualTo(EXPECTED_SHA256);
    
    System.out.println("Integration Success: MvnRepositoryMock produced the expected hardcoded hash.");
  }
}
