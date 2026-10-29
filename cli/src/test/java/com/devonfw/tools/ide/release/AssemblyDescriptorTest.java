package com.devonfw.tools.ide.release;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Guard against https://github.com/devonfw/IDEasy/issues/2753 - the {@code bin/} native launcher must be
 * archived with an explicit {@code fileMode=0755}. Without it the Windows PE is archived without the
 * executable bit, so {@code sh}/Git Bash refuses to run {@code ./bin/ideasy} with "Permission denied" when the
 * archive is produced by the release build. The Linux and macOS descriptors already set this mode; this test
 * keeps all active descriptors in sync.
 */
class AssemblyDescriptorTest extends Assertions {

  private static final Path ASSEMBLY_DIR = Path.of("src", "main", "assembly");

  /**
   * Every active (non-{@code .disabled}) release assembly descriptor archives the {@code bin/} launcher with an
   * explicit {@code fileMode=0755}.
   */
  @Test
  void testNativeLauncherIsArchivedWithExecutableBit() throws Exception {

    // arrange
    List<Path> descriptors;
    try (var paths = Files.list(ASSEMBLY_DIR)) {
      descriptors = paths
          .filter(path -> {
            String name = path.getFileName().toString();
            return name.endsWith(".xml") && !name.endsWith(".disabled");
          })
          .sorted()
          .toList();
    }

    // act
    descriptors.forEach(this::assertBinFileSetIsExecutable);

    // assert
    assertThat(descriptors).isNotEmpty();
  }

  private void assertBinFileSetIsExecutable(Path descriptor) {

    Element binFileSet = findBinFileSet(descriptor);
    assertThat(binFileSet)
        .as("descriptor %s must archive the bin/ native launcher", descriptor.getFileName())
        .isNotNull();
    assertThat(fileMode(binFileSet))
        .as("descriptor %s must set an explicit fileMode so the launcher keeps the executable bit", descriptor.getFileName())
        .isPresent()
        .contains("0755");
  }

  private Element findBinFileSet(Path descriptor) {

    Document document = parse(descriptor);
    NodeList fileSets = document.getElementsByTagName("fileSet");
    for (int i = 0; i < fileSets.getLength(); i++) {
      Element fileSet = (Element) fileSets.item(i);
      if (isBinOutputDirectory(fileSet)) {
        return fileSet;
      }
    }
    return null;
  }

  private boolean isBinOutputDirectory(Element fileSet) {

    String outputDirectory = childText(fileSet, "outputDirectory").orElse("");
    return Path.of(outputDirectory).normalize().equals(Path.of("bin"));
  }

  private Optional<String> fileMode(Element fileSet) {

    return childText(fileSet, "fileMode");
  }

  private Optional<String> childText(Element element, String name) {

    NodeList children = element.getElementsByTagName(name);
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (node.getParentNode() == element) {
        return Optional.of(node.getTextContent().trim());
      }
    }
    return Optional.empty();
  }

  private Document parse(Path descriptor) {

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setNamespaceAware(false);
      return factory.newDocumentBuilder().parse(descriptor.toFile());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse assembly descriptor " + descriptor, e);
    }
  }
}
