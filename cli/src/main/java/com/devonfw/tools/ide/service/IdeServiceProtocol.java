package com.devonfw.tools.ide.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads and writes {@link ServiceRequest} and {@link ServiceResponse} over a socket as one JSON object per line.
 */
public class IdeServiceProtocol {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static ServiceRequest readRequest(BufferedReader reader) throws IOException {
    String line = reader.readLine();
    return line == null ? null : MAPPER.readValue(line, ServiceRequest.class);
  }

  public static void writeResponse(BufferedWriter writer, ServiceResponse response) throws IOException {
    MAPPER.writeValue(writer, response);
    writer.write('\n');
    writer.flush();
  }

  public static void writeRequest(BufferedWriter writer, ServiceRequest request) throws IOException {
    String line = MAPPER.writeValueAsString(request);
    writer.write(line);
    writer.write('\n');
    writer.flush();
  }

  public static ServiceResponse readResponse(BufferedReader reader) throws IOException {
    String line = reader.readLine();
    return line == null ? null : MAPPER.readValue(line, ServiceResponse.class);
  }
}
