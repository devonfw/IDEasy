package com.devonfw.tools.ide.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A client that delegates requests to a running {@link IdeServiceServer} via the port file.
 */
public class IdeServiceClient {

  private static final Logger LOG = LoggerFactory.getLogger(IdeServiceClient.class);
  public static final String PORT_FILE_NAME = IdeServiceServer.PORT_FILE_NAME;
  private final Path portFile;
  private final Duration connectTimeout = Duration.ofMillis(200);
  private final Duration ioTimeout = Duration.ofSeconds(10);

  public IdeServiceClient(Path portFile) {
    this.portFile = portFile;
  }

  public Optional<ServiceResponse> tryDelegate(ServiceRequest request) {
    Integer port = readPort();
    if (port == null) {
      return Optional.empty();
    }

    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress("127.0.0.1", port), (int) this.connectTimeout.toMillis());
      socket.setSoTimeout((int) this.ioTimeout.toMillis());
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
      IdeServiceProtocol.writeRequest(writer, request);
      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      ServiceResponse response = IdeServiceProtocol.readResponse(reader);
      if (response == null) {
        return Optional.empty();
      }
      return Optional.of(response);
    } catch (IOException e) {
      LOG.debug("No IDEasy service reachable at {}: {}", this.portFile, e.getMessage());
      return Optional.empty();
    }
  }

  private Integer readPort() {
    try {
      if (Files.isRegularFile(this.portFile)) {
        return Integer.valueOf(Files.readString(this.portFile, StandardCharsets.US_ASCII).trim());
      }
    } catch (IOException e) {
      LOG.debug("Could not read service port file {}: {}", this.portFile, e.getMessage());
    }

    return null;
  }

}
