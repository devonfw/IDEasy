package com.devonfw.tools.ide.merge.xml.matcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.ByteArrayInputStream;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Test of {@link IdComputer}.
 */
class IdComputerTest {

  @ParameterizedTest
  @MethodSource("buildXPathExpressionTestCases")
  void testBuildXPathExpression(String elementXml, String mergeId, String expectedXPath) throws Exception {

    // arrange
    Element element = parse(elementXml);
    IdComputer computer = new IdComputer(mergeId, null);
    // act
    String actual = computer.buildXPathExpression(element);
    // assert
    assertThat(actual).isEqualTo(expectedXPath);
  }

  private static Stream<Arguments> buildXPathExpressionTestCases() {

    return Stream.of(
        arguments("<component name=\"RunManager\" selected=\"Application.IDEasy\"/>", "@name", "component[@name='RunManager']"),
        arguments("<configuration default=\"true\" type=\"JUnit\" name=\"foo\"/>", "@default,@type",
            "configuration[@default='true' and @type='JUnit']"),
        arguments("<configuration id=\"cfg1\" name=\"foo\" type=\"JUnit\"/>", "@@",
            "configuration[@name='foo' and @type='JUnit']"),
        arguments("<configuration default=\"true\" type=\"JUnit\" name=\"foo\"/>", "@@",
            "configuration[@default='true' and @name='foo' and @type='JUnit']"),
        arguments("<component name=\"RunManager\"/>", "name()", "component[local-name()='component']"),
        arguments("<value>myValue</value>", "text()", "value[text()='myValue']"));
  }

  /**
   * Parses the given XML fragment and returns its root {@link Element}.
   *
   * @param xml the XML fragment to parse.
   * @return the root element of the parsed document
   * @throws Exception in case of a parse error
   */
  private static Element parse(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
    return document.getDocumentElement();
  }
}
