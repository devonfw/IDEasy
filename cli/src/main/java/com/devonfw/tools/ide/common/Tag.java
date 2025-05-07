package com.devonfw.tools.ide.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link Tag} represents a classifier or category. A tool and plugin can be associated with {@link Tag}s allowing end users to find them.
 */
public final class Tag {

  private static final Tag[] NO_TAGS = new Tag[0];

  private static final Map<String, Tag> TAG_MAP = new HashMap<>(128);

  private static final Collection<Tag> ALL_TAGS = Collections.unmodifiableCollection(TAG_MAP.values());

  /** The root {@link Tag}. */
  public static final Tag ROOT = new Tag();

  static {
    TAG_MAP.put(ROOT.id, ROOT);
  }

  /** {@link #getParent() Parent} for miscellaneous (undefined) tags. */
  public static final Tag MISC = create("miscellaneous", ROOT, true, "misc");

  /** {@link #getParent() Parent} for programming-languages. */
  public static final Tag LANGUAGE = create("language", ROOT, true, "programming");

  /** {@link Tag} for JVM (Java Virtual Machine). */
  public static final Tag JVM = create("java-virtual-machine", LANGUAGE, false, "jvm");

  /** {@link Tag} for Java. */
  public static final Tag JAVA = create("java", JVM);

  /** {@link Tag} for Kotlin. */
  public static final Tag KOTLIN = create("kotlin", JVM);

  /** {@link Tag} for Scala. */
  public static final Tag SCALA = create("scala", JVM);

  /** {@link Tag} for DotNet (.NET). */
  public static final Tag DOTNET = create("dotnet", LANGUAGE, false, "net");

  /** {@link Tag} for C#. */
  public static final Tag CS = create("c#", DOTNET, false, "cs", "csharp");

  /** {@link Tag} for C. */
  public static final Tag C = create("c", LANGUAGE);

  /** {@link Tag} for Rust. */
  public static final Tag RUST = create("rust", LANGUAGE, false, "rs");

  /** {@link Tag} for C++. */
  public static final Tag CPP = create("c++", LANGUAGE, false, "cpp");

  /** {@link Tag} for Python. */
  public static final Tag PYTHON = create("python", LANGUAGE);

  /** {@link Tag} for Ruby. */
  public static final Tag RUBY = create("ruby", LANGUAGE);

  /** {@link Tag} for Perl. */
  public static final Tag PERL = create("perl", LANGUAGE);

  /** {@link Tag} for Shell scripting. */
  public static final Tag SHELL = create("shell", JVM, false, "script");

  /** {@link Tag} for Bash. */
  public static final Tag BASH = create("bash", SHELL, false, "terminal");

  /** {@link Tag} for TypeScript. */
  public static final Tag JAVA_SCRIPT = create("javascript", LANGUAGE, false, "js");

  /** {@link Tag} for TypeScript. */
  public static final Tag TYPE_SCRIPT = create("typescript", JAVA_SCRIPT, false, "ts");

  /** {@link #getParent() Parent} for programming-languages. */
  public static final Tag IDE = create("ide", ROOT);

  /** {@link Tag} for Eclipse. */
  public static final Tag ECLIPSE = create("eclipse", IDE);

  /** {@link Tag} for IDEA (JetBrains IDE Platform). */
  public static final Tag IDEA = create("idea", IDE);

  /** {@link Tag} for IntelliJ. */
  public static final Tag INTELLIJ = create("intellij", IDEA);

  /** {@link Tag} for Android-Studio. */
  public static final Tag ANDROID_STUDIO = create("android-studio", IDEA);

  /** {@link Tag} for Pycharm. */
  public static final Tag PYCHARM = create("pycharm", IDEA);

  /** {@link Tag} for VS-Code. */
  public static final Tag VS_CODE = create("vscode", IDE, false, "visualstudiocode");

  /** {@link Tag} for (code-)generators (including template-engines, etc.). */
  public static final Tag GENERATOR = create("generator", ROOT);

  /** {@link #getParent() Parent} for frameworks. */
  public static final Tag FRAMEWORK = create("framework", ROOT, true);

  /** {@link Tag} for Spring(framework). */
  public static final Tag SPRING = create("spring", FRAMEWORK, false, new String[] { "springframework", "springboot" },
      JAVA);

  /** {@link Tag} for Quarkus. */
  public static final Tag QUARKUS = create("quarkus", FRAMEWORK, false, null, JAVA);

  /** {@link Tag} for Micronaut. */
  public static final Tag MICRONAUT = create("micronaut", FRAMEWORK, false, null, JAVA);

  /** {@link Tag} for Angular. */
  public static final Tag ANGULAR = create("angular", FRAMEWORK, false, new String[] { "ng", "angularjs" },
      TYPE_SCRIPT);

  /** {@link Tag} for React. */
  public static final Tag REACT = create("react", FRAMEWORK, false, null, TYPE_SCRIPT);

  /** {@link Tag} for Vue. */
  public static final Tag VUE = create("vue", FRAMEWORK, false, null, TYPE_SCRIPT);

  /** {@link Tag} for Cordova. */
  public static final Tag CORDOVA = create("cordova", FRAMEWORK, false, null, JAVA_SCRIPT);

  /** {@link Tag} for Ionic. */
  public static final Tag IONIC = create("ionic", CORDOVA, false);

  /** {@link #getParent() Parent} for quality-assurance. */
  public static final Tag QA = create("quality-assurance", ROOT, false, "qa", "quality");

  /** {@link Tag} for everything related to testing. */
  public static final Tag TEST = create("testing", QA, false, "test");

  /** {@link Tag} for everything related to testing. */
  public static final Tag MOCK = create("mocking", TEST, false, "mock");

  /** {@link Tag} for everything related to testing. */
  public static final Tag CODE_QA = create("static-code-analysis", QA, false, "codeqa");

  /** {@link #getParent() Parent} for linters. */
  public static final Tag LINTING = create("linter", QA, false, "lint", "linting");

  /** {@link Tag} for everything related to documentation. */
  public static final Tag DOCUMENTATION = create("documentation", ROOT, false, "doc");

  /** {@link #getParent() Parent} for file formats. */
  public static final Tag FORMAT = create("format", ROOT, true);

  /** {@link Tag} for JSON. */
  public static final Tag JSON = create("json", FORMAT);

  /** {@link Tag} for YAML. */
  public static final Tag YAML = create("yaml", FORMAT, false, "yml");

  /** {@link Tag} for CSS. */
  public static final Tag CSS = create("css", FORMAT);

  /** {@link Tag} for Properties. */
  public static final Tag PROPERTIES = create("properties", FORMAT);

  /** {@link Tag} for AsciiDoc. */
  public static final Tag ASCII_DOC = create("ascii-doc", FORMAT, false, new String[] { "adoc" }, DOCUMENTATION);

  /** {@link Tag} for MarkDown. */
  public static final Tag MARK_DOWN = create("markdown", DOCUMENTATION, false, new String[] { "md" }, DOCUMENTATION);

  /** {@link Tag} for YAML. */
  public static final Tag PDF = create("pdf", FORMAT, false, null, DOCUMENTATION);

  /** {@link Tag} for HTML. */
  public static final Tag HTML = create("html", FORMAT, false, null, DOCUMENTATION);

  /** {@link Tag} for machine-learning. */
  public static final Tag MACHINE_LEARNING = create("machine-learning", ROOT, false, "ml");

  /** {@link Tag} for artificial-intelligence. */
  public static final Tag ARTIFICIAL_INTELLIGENCE = create("artificial-intelligence", MACHINE_LEARNING, false, "ai");

  /** {@link Tag} for data-science. */
  public static final Tag DATA_SCIENCE = create("data-science", ROOT);

  /** {@link Tag} for business-intelligence. */
  public static final Tag BUSINESS_INTELLIGENCE = create("business-intelligence", ROOT, false, "bi", "datawarehouse",
      "dwh");

  /** {@link #Tag} for productivity. */
  public static final Tag ARCHITECTURE = create("architecture", ROOT);

  /** {@link Tag} for AsciiDoc. */
  public static final Tag UML = create("uml", ARCHITECTURE, false, null, DOCUMENTATION);

  /** {@link #Tag} for security. */
  public static final Tag SECURITY = create("security", ROOT, false, "cve");

  /** {@link #Tag} for collaboration. */
  public static final Tag COLLABORATION = create("collaboration", ROOT, false, "collab");

  /** {@link #Tag} for virtualization. */
  public static final Tag VIRTUALIZATION = create("virtualization", ROOT, false, "vm");

  /** {@link #Tag} for docker. */
  public static final Tag DOCKER = create("docker", VIRTUALIZATION);

  /** {@link #Tag} for docker. */
  public static final Tag KUBERNETES = create("kubernetes", DOCKER, false, "k8s");

  /** {@link #Tag} for WSL. */
  public static final Tag WSL = create("wsl", VIRTUALIZATION);

  /** {@link Tag} for everything related to databases. */
  public static final Tag DB = create("database", ROOT);

  /** {@link Tag} for administration tools. */
  public static final Tag ADMIN = create("admin", ROOT);

  /** {@link #Tag} for network. */
  public static final Tag NETWORK = create("network", ROOT, false, "remote");

  /** {@link #Tag} for HTTP. */
  public static final Tag HTTP = create("http", NETWORK);

  /** {@link #Tag} for REST. */
  public static final Tag REST = create("rest", HTTP);

  /** {@link #Tag} for secure-shell. */
  public static final Tag SSH = create("secure-shell", NETWORK, false, "ssh", "scp");

  /** {@link #Tag} for capture. */
  public static final Tag CAPTURE = create("capture", ROOT, false, "capturing");

  /** {@link #Tag} for capture. */
  public static final Tag SCREENSHOT = create("screenshot", CAPTURE);

  /** {@link #Tag} for capture. */
  public static final Tag SCREEN_RECORDING = create("screenrecording", CAPTURE, false, "videocapture");

  /** {@link #Tag} for productivity. */
  public static final Tag PRODUCTIVITY = create("productivity", ROOT);

  /** {@link #Tag} for regular-expression. */
  public static final Tag REGEX = create("regular-expression", PRODUCTIVITY, false, "regex", "regexp");

  /** {@link #Tag} for search. */
  public static final Tag SEARCH = create("search", PRODUCTIVITY, false, "find");

  /** {@link #Tag} for spellchecker. */
  public static final Tag SPELLCHECKER = create("spellchecker", PRODUCTIVITY, false, "spellcheck", "spellchecking");

  /** {@link #Tag} for analyse. */
  public static final Tag ANALYSE = create("analyse", ROOT, false, "analyze", "analysis");

  /** {@link #Tag} for monitoring. */
  public static final Tag MONITORING = create("monitoring", ANALYSE, false, "monitor");

  /** {@link #Tag} for formatter. */
  public static final Tag FORMATTER = create("formatter", ROOT, false, "codeformat", "codeformatter");

  /** {@link #Tag} for user-experience. */
  public static final Tag UX = create("user-experience", PRODUCTIVITY, false, "ux");

  /** {@link #Tag} for style. */
  public static final Tag STYLE = create("style", UX, false, "theme", "icon", "skin");

  /** {@link #Tag} for style. */
  public static final Tag KEYBINDING = create("keybinding", UX, false, "keybindings", "keymap");

  /** {@link #Tag} for draw(ing). */
  public static final Tag DRAW = create("draw", UX, false, "diagram", "paint");

  /** {@link #Tag} for cloud. */
  public static final Tag CLOUD = create("cloud", ROOT);

  /** {@link #Tag} for infrastructure-as-code. */
  public static final Tag IAC = create("infrastructure-as-code", CLOUD, false, "iac");

  /** {@link #Tag} for software-configuration-management. */
  public static final Tag CONFIG_MANAGEMENT = create("software-configuration-management", ROOT, false,
      "configmanagement", "configurationmanagement");

  /** {@link #Tag} for build-management. */
  public static final Tag BUILD = create("build-management", CONFIG_MANAGEMENT, false, "build");

  /** {@link #Tag} for version-control. */
  public static final Tag VCS = create("version-control", CONFIG_MANAGEMENT, false, "vcs", "versioncontrolsystem");

  /** {@link #Tag} for issue-management. */
  public static final Tag ISSUE = create("issue-management", CONFIG_MANAGEMENT, false, "issue");

  /** {@link #Tag} for git. */
  public static final Tag GIT = create("git", VCS);

  /** {@link #Tag} for github. */
  public static final Tag GITHUB = create("github", GIT);


  /** {@link #Tag} for diff (tools that compare files and determine the difference). */
  public static final Tag DIFF = create("diff", CONFIG_MANAGEMENT, false, "patch");

  /** {@link #Tag} for diff (tools that compare files and determine the difference). */
  public static final Tag RUNTIME = create("runtime", ROOT);

  /** {@link #getParent() Parent} for operating-system. */
  public static final Tag OS = create("operating-system", ROOT, true, "os");

  /** {@link #Tag} for Windows. */
  public static final Tag WINDOWS = create("windows", OS);

  /** {@link #Tag} for Mac. */
  public static final Tag MAC = create("mac", OS, false, "macos", "osx");

  /** {@link #Tag} for Linux. */
  public static final Tag LINUX = create("linux", OS, false);

  /** {@link #getParent() Parent} for cryptography. */
  public static final Tag CRYPTO = create("cryptography", ROOT, false, "crypto");

  /** {@link #Tag} for encryption. */
  public static final Tag ENCRYPTION = create("encryption", CRYPTO);

  private final String id;

  private final Tag parent;

  private final boolean isAbstract;

  private final Tag[] additionalParents;

  private Tag() {

    this.id = "<root>";
    this.parent = null;
    this.isAbstract = true;
    this.additionalParents = NO_TAGS;
  }

  private Tag(String id, Tag parent, boolean isAbstract, Tag... additionalParents) {

    super();
    Objects.requireNonNull(id);
    Objects.requireNonNull(parent);
    assert (id.toLowerCase(Locale.ROOT).equals(id));
    this.id = id;
    this.parent = parent;
    this.isAbstract = isAbstract;
    this.additionalParents = additionalParents;
  }

  /**
   * @return the identifier and name of this tag.
   */
  public String getId() {

    return this.id;
  }

  /**
   * @return the parent {@link Tag} or {@code null} if this is the root tag.
   */
  public Tag getParent() {

    return this.parent;
  }

  /**
   * @param i the index of the requested parent. Should be in the range from {@code 0} to
   *     <code>{@link #getParentCount()}-1</code>.
   * @return the requested {@link Tag}.
   */
  public Tag getParent(int i) {

    if (i == 0) {
      return this.parent;
    }
    return this.additionalParents[i - 1];
  }

  /**
   * @return the number of {@link #getParent(int) parents} available.
   */
  public int getParentCount() {

    if (this.parent == null) {
      return 0;
    }
    return this.additionalParents.length + 1;
  }

  /**
   * @return {@code true} if this {@link Tag} is abstract and cannot be selected since it is just a generic parent container, {@code false} otherwise.
   */
  public boolean isAbstract() {

    return this.isAbstract;
  }

  public boolean isAncestorOf(Tag tag) {

    return isAncestorOf(tag, false);
  }

  /**
   * @param tag the {@link Tag} to check.
   * @param includeAdditionalParents - {@code true} if {@link #getParent(int) additional parents} should be included, {@code false} otherwise (only consider
   *     {@link #getParent() primary parent}).
   * @return {@code true} if the given {@link Tag} is an ancestor of this tag, {@code false} otherwise. An ancestor is a direct or indirect
   *     {@link #getParent() parent}. Therefore, if {@link #ROOT} is given as {@link Tag} parameter, this method should always return {@code true}.
   */
  public boolean isAncestorOf(Tag tag, boolean includeAdditionalParents) {

    Tag ancestor = this.parent;
    while (ancestor != null) {
      if (ancestor == tag) {
        return true;
      }
      ancestor = ancestor.parent;
    }
    if (includeAdditionalParents) {
      for (Tag p : this.additionalParents) {
        do {
          if (p == tag) {
            return true;
          }
          p = p.parent;
        } while (p != null);
      }
    }
    return false;
  }

  @Override
  public String toString() {

    return this.id;
  }

  /**
   * @param id the {@link #getId() ID} of the tag.
   * @param parent the {@link #getParent() parent tag}.
   * @param isAbstract the {@link #isAbstract() abstract flag}.
   * @return the new {@link Tag}.
   */
  static Tag create(String id, Tag parent) {

    return create(id, parent, false);
  }

  /**
   * @param id the {@link #getId() ID} of the tag.
   * @param parent the {@link #getParent() parent tag}.
   * @param isAbstract the {@link #isAbstract() abstract flag}.
   * @return the new {@link Tag}.
   */
  static Tag create(String id, Tag parent, boolean isAbstract) {

    return create(id, parent, isAbstract, null, NO_TAGS);
  }

  /**
   * @param id the {@link #getId() ID} of the tag.
   * @param parent the {@link #getParent() parent tag}.
   * @param isAbstract the {@link #isAbstract() abstract flag}.
   * @return the new {@link Tag}.
   */
  static Tag create(String id, Tag parent, boolean isAbstract, String synonym) {

    return create(id, parent, isAbstract, new String[] { synonym }, NO_TAGS);
  }

  /**
   * @param id the {@link #getId() ID} of the tag.
   * @param parent the {@link #getParent() parent tag}.
   * @param isAbstract the {@link #isAbstract() abstract flag}.
   * @return the new {@link Tag}.
   */
  static Tag create(String id, Tag parent, boolean isAbstract, String... synonyms) {

    return create(id, parent, isAbstract, synonyms, NO_TAGS);
  }

  /**
   * @param id the {@link #getId() ID} of the tag.
   * @param parent the {@link #getParent() parent tag}.
   * @param isAbstract the {@link #isAbstract() abstract flag}.
   * @return the new {@link Tag}.
   */
  static Tag create(String id, Tag parent, boolean isAbstract, String[] synonyms, Tag... additionalParents) {

    Tag tag = new Tag(id, parent, isAbstract, additionalParents);
    add(id, tag);
    if (synonyms != null) {
      for (String synonym : synonyms) {
        add(synonym, tag);
      }
    }
    return tag;
  }

  private static void add(String key, Tag tag) {

    Tag duplicate = TAG_MAP.put(normalizeKey(key), tag);
    if (duplicate != null) {
      throw new IllegalStateException("Duplicate tag for " + key);
    }
  }

  private static String normalizeKey(String key) {

    return key.replace("-", "").replace(".", "");
  }

  private static Tag require(String key) {

    Tag tag = TAG_MAP.get(normalizeKey(key));
    if (tag == null) {
      throw new IllegalStateException("Could not find required tag " + key);
    }
    return tag;
  }

  /**
   * @param id the {@link #getId() ID} of the requested {@link Tag}.
   * @return the {@link Tag} with the given {@link #getId() ID}. Will be lazily created as child of {@link #MISC} if not already exists.
   */
  public static Tag of(String id) {

    final String tagId = id.trim();
    int slash = tagId.indexOf('/');
    if (slash >= 0) {
      String parentId = tagId.substring(0, slash);
      Tag parent = require(parentId);
      String childId = tagId.substring(slash + 1);
      return TAG_MAP.computeIfAbsent(normalizeKey(childId), i -> new Tag(childId, parent, false, NO_TAGS));
    }
    return TAG_MAP.computeIfAbsent(normalizeKey(tagId), i -> new Tag(tagId, MISC, false, NO_TAGS));
  }

  /**
   * @return the {@link Collections} of all available {@link Tag}s.
   */
  public static Collection<Tag> getAll() {

    return ALL_TAGS;
  }

  /**
   * @param tagsCsv the tags as {@link String} in CSV format («first-tag»,...,«last-tag»). May be {@code null} or empty.
   * @return the parsed {@link Set} of {@link Tag}s.
   */
  public static Set<Tag> parseCsv(String tagsCsv) {

    if (tagsCsv == null) {
      return Collections.emptySet();
    }
    tagsCsv = tagsCsv.trim().toLowerCase(Locale.ROOT);
    if (tagsCsv.isEmpty()) {
      return Collections.emptySet();
    }
    String[] tagArray = tagsCsv.split(",");
    Set<Tag> tags = new HashSet<>(tagArray.length);
    for (String tag : tagArray) {
      tags.add(of(tag));
    }
    assert (tags.size() == tagArray.length);
    return Set.of(tags.toArray(new Tag[tags.size()]));
  }

}
