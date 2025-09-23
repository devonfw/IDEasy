package com.devonfw.tools.ide.context;

import com.devonfw.tools.ide.tool.repository.MvnRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

public class MvnRepositoryMock extends MvnRepository {

  /** Base URL for Maven Central repository */
  public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

  /** Base URL for Maven Snapshots repository */
  public static final String MAVEN_SNAPSHOTS = "https://central.sonatype.com/repository/maven-snapshots";
  private final WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   */
  public MvnRepositoryMock(IdeContext context, WireMockRuntimeInfo wmRuntimeInfo) {
    super(context);
    this.wmRuntimeInfo = wmRuntimeInfo;
  }
}
