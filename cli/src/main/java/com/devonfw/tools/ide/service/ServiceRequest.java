package com.devonfw.tools.ide.service;

import java.util.Map;

/**
 * A request sent by a client to the {@link IdeServiceServer}.
 *
 * @param operation the requested {@link ServiceOperation}.
 * @param params the operation parameters, or {@code null} if none.
 */
public record ServiceRequest(ServiceOperation operation, Map<String, String> params) {

  public String getParam(String name) {
    return this.params == null ? null : this.params.get(name);
  }
}
