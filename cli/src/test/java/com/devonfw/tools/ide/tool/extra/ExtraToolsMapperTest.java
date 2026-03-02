package com.devonfw.tools.ide.tool.extra;

import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link ExtraToolsMapper}.
 */
class ExtraToolsMapperTest extends Assertions {

  @Test
  void testLoadAndSaveJson(@TempDir Path tmpDir) {
    // arrange
    Path testPath = Path.of("src/test/resources/extra-tools");
    ExtraToolsMapper mapper = ExtraToolsMapper.get();
    // act
    ExtraTools extraTools = mapper.loadJsonFromFolder(testPath);
    // assert
    assertThat(extraTools.getSortedToolNames()).containsExactly("dotnet", "java");
    assertThat(extraTools.getExtraInstallations("dotnet")).containsExactly(new ExtraToolInstallation("legacy", VersionIdentifier.of("6.0.428"), null));
    assertThat(extraTools.getExtraInstallations("java")).containsExactly(new ExtraToolInstallation("client", VersionIdentifier.of("11.0.27_6"), "azul"),
        new ExtraToolInstallation("process-engine", VersionIdentifier.of("21.*"), null));
    // act
    mapper.saveJsonToFolder(extraTools, tmpDir);
    // assert
    String filename = mapper.getStandardFilename();
    assertThat(tmpDir.resolve(filename)).hasSameTextualContentAs(testPath.resolve(filename));
  }

}
