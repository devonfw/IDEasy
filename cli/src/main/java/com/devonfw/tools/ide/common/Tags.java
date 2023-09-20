package com.devonfw.tools.ide.common;

import java.util.Set;

/**
 * TODO hohwille This type ...
 *
 */
public interface Tags {

  /** {@link #getTags() Tag} for Java and JVM related tools. */
  String TAG_JAVA = "java";

  /** {@link #getTags() Tag} for build tools. */
  String TAG_BUILD = "build";

  /** {@link #getTags() Tag} for quality assurance (QA) tools. */
  String TAG_QA = "qa";

  /** {@link #getTags() Tag} for artificial intelligence (AI) and machine learning (ML) tools. */
  String TAG_AI = "ai";

  /** {@link #getTags() Tag} for documentation tools. */
  String TAG_DOCUMENTATION = "doc";

  /** {@link #getTags() Tag} for tools supporting AsciiDoc. */
  String TAG_ASCIIDOC = "adoc";

  /** {@link #getTags() Tag} for angular related tools. */
  String TAG_ANGULAR = "angular";

  /** {@link #getTags() Tag} for TypeScript related tools. */
  String TAG_TYPE_SCRIPT = "ts";

  /** {@link #getTags() Tag} for generic tools that increase productivity. */
  String TAG_PRODUCTIVITY = "productivity";

  /** {@link #getTags() Tag} for DotNet related tools. */
  String TAG_DOT_NET = ".net";

  /** {@link #getTags() Tag} for Python related tools. */
  String TAG_PYTHON = "python";

  /** {@link #getTags() Tag} for tools that actually represent an IDE (Integrated Development Environment). */
  String TAG_IDE = "ide";

  /** {@link #getTags() Tag} for tools providing a runtime environment (the core of a programming language). */
  String TAG_RUNTIME = "runtime";

  /**
   * {@link #getTags() Tag} for cloud tools (e.g. CLI to manage infrastructure in the cloud). This is not limited to the
   * hyper-scalers but also used for other platforms providing automation like openshift or even github.
   */
  String TAG_CLOUD = "cloud";

  /** {@link #getTags() Tag} for infrastructure-as-code (IAC) tools. */
  String TAG_IAC = "iac";

  /**
   * @return a {@link Set} with the tags classifying this object. E.g. for mvn (maven) the tags {@link #TAG_JAVA java}
   *         and {@link #TAG_BUILD build} could be associated.
   */
  Set<String> getTags();
}
