package com.devonfw.tools.ide.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Tests of {@link ServiceGetVersionCommandlet}.
 */
class ServiceGetVersionCommandletTest extends AbstractIdeContextTest {

  private IdeTestContext context;

  private IdeServiceServer server;

  @AfterEach
  void tearDown() throws IOException {
    if (this.server != null) {
      this.server.stop();
    }
    if (this.context != null) {
      Path portFile = this.context.getIdePath().resolve(IdeServiceClient.PORT_FILE_NAME);
      Files.deleteIfExists(portFile);
    }
  }

  private IdeTestContext newContextWithIdeRoot() {
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setIdeRoot(TEST_PROJECTS_COPY.resolve(PROJECT_BASIC));
    return context;
  }

  /**
   * Helper to start a server on a random port with the given context and write the port file.
   */
  private void startServer(IdeTestContext ctx) throws IOException {
    this.server = new IdeServiceServer(ctx);
    Path portFile = ctx.getIdePath().resolve(IdeServiceClient.PORT_FILE_NAME);
    ctx.getFileAccess().mkdirs(portFile.getParent());
    int port = this.server.start(portFile);
    // server already wrote the port file via start(portFile) — the port above is the same value
    assertThat(port).isPositive();
  }

  /**
   * Test that the commandlet resolves the latest version locally when no service is running.
   */
  @Test
  void testServiceGetVersionLocalFallbackWithoutService() {

    // arrange
    this.context = newContext(PROJECT_BASIC);
    ServiceGetVersionCommandlet cmd = this.context.getCommandletManager().getCommandlet(ServiceGetVersionCommandlet.class);
    cmd.getTool().setValueAsString("java", this.context);
    // act
    cmd.run();
    // assert
    assertThat(this.context).log().hasMessageContaining("IDEasy service not running - resolving version locally.");
    assertThat(this.context).log(IdeLogLevel.PROCESSABLE).hasMessage("21.0.8_9");
  }

  /**
   * Test that the commandlet delegates to a running service and does not resolve locally.
   */
  @Test
  void testServiceGetVersionDelegatesToRunningService() throws IOException {

    // arrange
    this.context = newContext(PROJECT_BASIC);
    startServer(this.context);
    ServiceGetVersionCommandlet cmd = this.context.getCommandletManager().getCommandlet(ServiceGetVersionCommandlet.class);
    cmd.getTool().setValueAsString("java", this.context);
    // act
    cmd.run();
    // assert: the local-fallback message must NOT be present
    assertThat(this.context).log().hasNoMessageContaining("IDEasy service not running - resolving version locally.");
    assertThat(this.context).log(IdeLogLevel.PROCESSABLE).hasMessage("21.0.8_9");
  }

  /**
   * Test that a version pattern is resolved via the service.
   */
  @Test
  void testServiceGetVersionWithVersionPatternViaService() throws IOException {

    // arrange
    this.context = newContext(PROJECT_BASIC);
    startServer(this.context);
    ServiceGetVersionCommandlet cmd = this.context.getCommandletManager().getCommandlet(ServiceGetVersionCommandlet.class);
    cmd.getTool().setValueAsString("java", this.context);
    cmd.getVersion().setValue("17.*");
    // act
    cmd.run();
    // assert
    assertThat(this.context).log(IdeLogLevel.PROCESSABLE).hasMessage("17.0.10");
  }

  /**
   * Test that an edition without any version return the server's error as a CliException, without silently falling back to local resolution.
   */
  @Test
  void testServiceGetVersionUnknownEditionReturnsServerError() throws IOException {

    // arrange
    this.context = newContext(PROJECT_BASIC);
    startServer(this.context);
    ServiceGetVersionCommandlet cmd = this.context.getCommandletManager().getCommandlet(ServiceGetVersionCommandlet.class);
    cmd.getTool().setValueAsString("java", this.context);
    // note: "java" is a valid tool commandlet, but has no matching edition in the test urls
    // so we test via the server error path with an edition that does not exist
    cmd.getEdition().setValue("nonexistent-edition");
    // act
    CliException exception = catchThrowableOfType(cmd::run, CliException.class);
    // assert
    assertThat(exception).isNotNull();
    assertThat(exception.getMessage()).contains("Could not find any version matching");
    assertThat(this.context).log().hasNoMessageContaining("IDEasy service not running - resolving version locally.");
  }

  /**
   * Test that the service is read-only no URLs files are modified or created after a request.
   */
  @Test
  void testServiceIsReadOnly() throws IOException {

    // arrange
    this.context = newContext(PROJECT_BASIC);
    Path urlsPath = this.context.getUrlsPath();
    // record a snapshot of the urls folder (file names + last-modified) before
    long before = snapshotMtime(urlsPath);
    startServer(this.context);
    ServiceGetVersionCommandlet cmd =
        this.context.getCommandletManager().getCommandlet(ServiceGetVersionCommandlet.class);
    cmd.getTool().setValueAsString("java", this.context);
    // act
    cmd.run();
    // assert
    long after = snapshotMtime(urlsPath);
    assertThat(after).isEqualTo(before);
  }

  @Test
  void testServiceHandlesParallelRequests() throws Exception {

    // arrange
    this.context = newContextWithIdeRoot();
    startServer(this.context);
    Path portFile = this.context.getIdePath().resolve(IdeServiceClient.PORT_FILE_NAME);
    IdeServiceClient client = new IdeServiceClient(portFile);
    ServiceRequest request = new ServiceRequest(ServiceOperation.GET_VERSION, Map.of("tool", "java"));
    int clients = 10;
    ExecutorService pool = Executors.newFixedThreadPool(clients);
    List<Future<Optional<ServiceResponse>>> futures = new ArrayList<>();
    try {
      // act: 10 clients hit the service at the same time
      for (int i = 0; i < clients; i++) {
        futures.add(pool.submit(() -> client.tryDelegate(request)));
      }
      // assert: every client got a successful response with the same payload
      List<String> payloads = new ArrayList<>();
      for (Future<Optional<ServiceResponse>> future : futures) {
        Optional<ServiceResponse> response = future.get();
        assertThat(response).isPresent();
        assertThat(response.get().success()).isTrue();
        payloads.add(response.get().payload());
      }
      assertThat(payloads).allSatisfy(payload -> assertThat(payload).isEqualTo("21.0.8_9"));
    } finally {
      pool.shutdownNow();
    }
  }

  private static long snapshotMtime(Path root) throws IOException {
    long sum = 0;
    try (var stream = Files.walk(root)) {
      for (Path p : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
        sum += Files.getLastModifiedTime(p).toMillis();
      }
    }
    return sum;
  }
}
