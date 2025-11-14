package com.devonfw.tools.ide.url.model.file.json;

import java.util.List;

import com.devonfw.tools.ide.version.VersionRange;

/**
 * Model to represent a CVE of a tool (inside a "security.json" file).
 *
 * @param id the unique identifier (e.g. "CVE-2021-44228").
 * @param severity the severity in the range from (0,10.0] where 10.0 is most critical.
 * @param versions the {@link VersionRange}s of the affected versions. Typically one entry but might also affect multiple ranges (e.g. "[1.0,1.2)" and
 *     "[2.0,2.2)"). Should never be {@code null} or {@link List#isEmpty() empty}.
 */
public record Cve(String id, double severity, List<VersionRange> versions) {

}
