package com.devonfw.tools.ide.common;

import java.util.Collection;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link Tag}.
 */
public class TagTest extends Assertions {

  /**
   * Test various predefined {@link Tag}s.
   */
  @Test
  public void testTags() {

    checkTag(Tag.ROOT, "<root>", null, true);
    checkTag(Tag.MACHINE_LEARNING, "machine-learning", Tag.ROOT);
    checkTag(Tag.ARTIFICIAL_INTELLIGENCE, "artificial-intelligence", Tag.MACHINE_LEARNING);
  }

  private void checkTag(Tag tag, String id, Tag parent) {

    checkTag(tag, id, parent, false);
  }

  private void checkTag(Tag tag, String id, Tag parent, boolean isAbstract) {

    assertThat(tag.getId()).isEqualTo(id);
    assertThat(tag.getParent()).isSameAs(parent);
    assertThat(tag.isAbstract()).isEqualTo(isAbstract);
    assertThat(tag.toString()).isEqualTo(id);
  }

  /**
   * Test of {@link Tag#of(String)}.
   */
  @Test
  public void testOf() {

    checkOf(Tag.ROOT);
    checkOf(Tag.MACHINE_LEARNING, "ml", "machinelearning");
    checkOf(Tag.ARTIFICIAL_INTELLIGENCE, "ai", "artificialintelligence");
  }

  private void checkOf(Tag tag, String... synonyms) {

    assertThat(Tag.of(tag.getId())).isSameAs(tag);
    for (String synonym : synonyms) {
      assertThat(Tag.of(synonym)).isSameAs(tag);
    }
  }

  /**
   * Test of {@link Tag#of(String)} with new tag that do not yet exist.
   */
  @Test
  public void testOfNew() {

    // arrange
    String id = "undefined";
    // act
    Tag tag = Tag.of(id);
    // assert
    checkTag(tag, id, Tag.MISC);
  }

  /**
   * Test of {@link Tag#of(String)} with new tags that do not yet exist and given parent.
   */
  @Test
  public void testOfNewWithParent() {

    // arrange
    String id = "brandnew";
    // act
    Tag tag = Tag.of("ide/" + id);
    // assert
    checkTag(tag, id, Tag.IDE);
  }

  /**
   * Test of {@link Tag#getAll()}.
   */
  @Test
  public void testGetAll() {

    // act
    Collection<Tag> tags = Tag.getAll();
    // assert
    assertThat(tags).contains(Tag.ROOT, Tag.ANDROID_STUDIO, Tag.ECLIPSE, Tag.IDE, Tag.VS_CODE);
    assertThat(tags.size()).isGreaterThan(150);
  }

  /**
   * Test of {@link Tag#parseCsv(String)}.
   */
  @Test
  public void testParseCsv() {

    // arrange
    String csv = " c,c++, c# "; // also test trimming
    // act
    Set<Tag> tags = Tag.parseCsv(csv);
    // assert
    assertThat(tags).containsExactlyInAnyOrder(Tag.C, Tag.CPP, Tag.CS);
  }

  /**
   * Test of {@link Tag#parseCsv(String)} with empty CSV.
   */
  @Test
  public void testParseCsvEmpty() {

    assertThat(Tag.parseCsv(null)).isEmpty();
    assertThat(Tag.parseCsv("")).isEmpty();
    assertThat(Tag.parseCsv(" ")).isEmpty();
  }

  /**
   * Test of {@link Tag#getParent(int)} and {@link Tag#getParentCount()}.
   */
  @Test
  public void testGetParents() {

    assertThat(Tag.ROOT.getParentCount()).isZero();
    assertThat(Tag.DOCUMENTATION.getParentCount()).isOne();
    assertThat(Tag.DOCUMENTATION.getParent(0)).isSameAs(Tag.ROOT);
    assertThat(Tag.ASCII_DOC.getParentCount()).isEqualTo(2);
    assertThat(Tag.ASCII_DOC.getParent(0)).isSameAs(Tag.FORMAT);
    assertThat(Tag.ASCII_DOC.getParent(1)).isSameAs(Tag.DOCUMENTATION);
  }

  /**
   * Test of {@link Tag#isAncestorOf(Tag)}.
   */
  @Test
  public void testIsAncestorOf() {

    assertThat(Tag.QUARKUS.isAncestorOf(Tag.ROOT)).isTrue();
    assertThat(Tag.QUARKUS.isAncestorOf(Tag.DOCUMENTATION)).isFalse();
    assertThat(Tag.QUARKUS.isAncestorOf(Tag.JAVA)).isFalse();
    assertThat(Tag.QUARKUS.isAncestorOf(Tag.QUARKUS)).isFalse();
    boolean includeAdditionalParents = true;
    assertThat(Tag.QUARKUS.isAncestorOf(Tag.JAVA, includeAdditionalParents)).isTrue();
    assertThat(Tag.QUARKUS.isAncestorOf(Tag.LANGUAGE, includeAdditionalParents)).isTrue();
  }
}
