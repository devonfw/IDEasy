package com.devonfw.tools.ide.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.url.model.UrlMetadata;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * A local socket-based service that resolves tool versions on behalf of clients without them loading the full URL metadata.
 */
public class IdeServiceServer {

  public static final Logger LOG = LoggerFactory.getLogger(IdeServiceServer.class);

  public static final String PORT_FILE_NAME = "service.port";

  private final UrlMetadata urlMetadata;
  private final Object urlMetadataLock = new Object();
  private ServerSocket serverSocket;

  public IdeServiceServer(IdeContext context) {
    if (context.getUrlsPath() == null) {
      throw new CliException("cannot load URL metadata");
    }

    UrlRepository repository = UrlRepository.load(context.getUrlsPath());
    this.urlMetadata = new UrlMetadata(context, repository);
  }

  public ServiceResponse handle(ServiceRequest request) {
    synchronized (this.urlMetadataLock) {
      try {
        switch (request.operation()) {
          case GET_VERSION:
            return handleGetVersion(request);
          default:
            return ServiceResponse.error("Unsupported operation " + request.operation());
        }
      } catch (Exception e) {
        return ServiceResponse.error(e.getMessage());
      }
    }
  }

  private ServiceResponse handleGetVersion(ServiceRequest request) {
    String tool = request.getParam("tool");
    if (tool == null || tool.isBlank()) {
      return ServiceResponse.error("Missing required parameter tool");
    }
    String edition = request.getParam("edition");
    if (edition == null || edition.isBlank()) {
      edition = tool;
    }
    String versionPattern = request.getParam("version");
    VersionIdentifier resolved = this.urlMetadata.resolveVersion(
        tool,
        edition,
        versionPattern == null || versionPattern.isBlank() ? null : VersionIdentifier.of(versionPattern),
        null
    );

    return ServiceResponse.ok(resolved.toString());
  }

  public int start(Path portFile) throws IOException {
    this.serverSocket = new ServerSocket(0);
    int port = this.serverSocket.getLocalPort();
    Files.writeString(portFile, Integer.toString(port), StandardCharsets.US_ASCII);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      stop();

      try {
        Files.deleteIfExists(portFile);
      } catch (IOException e) {
        LOG.debug("Could not delete port file {}: {}", portFile, e.getMessage());
      }
    }));
    Thread.ofVirtual().name("ide-service-server").start(this::serve);
    return port;
  }

  private void serve() {
    while (!this.serverSocket.isClosed()) {
      try {
        Socket socket = this.serverSocket.accept();
        Thread.ofVirtual().name("ide-service-client-handler").start(() -> handleConnection(socket));
      } catch (IOException e) {
        if (!this.serverSocket.isClosed()) {
          LOG.debug("Client connection handling aborted: {}", e.getMessage());
        }
      }
    }
  }

  private void handleConnection(Socket socket) {
    try (socket;
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),
            StandardCharsets.UTF_8));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),
            StandardCharsets.UTF_8))) {
      while (!socket.isClosed()) {
        ServiceRequest request = IdeServiceProtocol.readRequest(reader);
        if (request == null) {
          break;
        }
        IdeServiceProtocol.writeResponse(writer, this.handle(request));
      }
    } catch (IOException e) {
      LOG.debug("Connection handling aborted: {}", e.getMessage());
    }
  }

  public void stop() {
    try {
      if (this.serverSocket != null && !this.serverSocket.isClosed()) {
        this.serverSocket.close();
      }
    } catch (IOException e) {
      LOG.debug("Failed to close server socket: {}", e.getMessage());
    }
  }
}
