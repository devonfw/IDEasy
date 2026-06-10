"""Configuration and static mappings for the quality status generator."""

REPO = "devonfw/IDEasy"
API_BASE = "https://api.github.com"
TOOL_FOLDER_PATH = "cli/src/main/java/com/devonfw/tools/ide/tool"

GITHUB_ACCEPT_HEADER = "application/vnd.github+json"
GITHUB_API_VERSION = "2022-11-28"
GITHUB_PAGE_SIZE = 100

OS_LABELS = {"windows": "windows", "linux": "linux", "mac": "macOS"}
OS_ORDER = ["windows", "linux", "mac"]
OS_DISPLAY = {"windows": "Windows", "linux": "Linux", "mac": "macOS"}
OS_SHORT = {"windows": "Win", "linux": "Linux", "mac": "macOS"}
OS_FILE_SUFFIX = {
  "windows": "windows",
  "linux": "linux",
  "mac": "macos",
}

BUG_TYPE_NAMES = {"Bug", "bug"}
BLOCKER_LABEL = "blocker"

# Number of columns in the rendered issue tables.
TABLE_COLSPAN = 2

# Central age bucket configuration:
# (label, upper_bound_in_days), with None meaning open-ended.
AGE_BUCKETS = [
  ("0-10 days", 10),
  ("11-30 days", 30),
  ("31-60 days", 60),
  ("61-90 days", 90),
  ("90+ days", None),
]

# --- Tool registry ---
# Each key is the canonical tool folder name under TOOL_FOLDER_PATH, which is
# also the default GitHub label. "display" is the human-readable table name.
# "labels" lists any additional GitHub label names that map to this entry.

TOOLS: dict[str, dict] = {
  # IDEs & Editors
  "androidstudio": {"display": "Android Studio", "labels": ["android-studio"]},
  "eclipse": {"display": "Eclipse"},
  "intellij": {"display": "IntelliJ IDEA", "labels": ["intellij-idea"]},
  "pycharm": {"display": "PyCharm"},
  "vscode": {"display": "VS Code", "labels": ["vsc"]},

  # JVM & Build
  "java": {"display": "Java (JDK)", "labels": ["jdk"]},
  "graalvm": {"display": "GraalVM"},
  "kotlinc": {"display": "Kotlin Compiler"},
  "mvn": {"display": "Maven", "labels": ["maven"]},
  "gradle": {"display": "Gradle"},
  "spring": {"display": "Spring Boot CLI"},
  "quarkus": {"display": "Quarkus CLI"},
  "tomcat": {"display": "Apache Tomcat"},
  "jasypt": {"display": "Jasypt"},
  "jmc": {"display": "JDK Mission Control"},
  "gcviewer": {"display": "GCViewer"},

  # Python
  "python": {"display": "Python"},
  "pip": {"display": "pip"},
  "uv": {"display": "uv"},

  # JavaScript / Node.js
  "node": {"display": "Node.js", "labels": ["nodejs"]},
  "npm": {"display": "npm"},
  "ng": {"display": "Angular CLI"},
  "yarn": {"display": "Yarn"},
  "corepack": {"display": "Corepack"},

  # Go
  "go": {"display": "Go"},

  # Cloud & DevOps
  "docker": {"display": "Docker"},
  "lazydocker": {"display": "Lazydocker"},
  "kubectl": {"display": "kubectl"},
  "oc": {"display": "OpenShift CLI"},
  "helm": {"display": "Helm"},
  "terraform": {"display": "Terraform"},
  "aws": {"display": "AWS CLI"},
  "az": {"display": "Azure CLI", "labels": ["azure-cli"]},
  "dotnet": {"display": "dotnet"},

  # Developer Tools
  "gh": {"display": "GitHub CLI"},
  "copilot": {"display": "GitHub Copilot", "labels": ["github-copilot"]},
  "sonar": {"display": "SonarQube", "labels": ["sonarqube"]},

  # Database
  "pgadmin": {"display": "pgAdmin"},
  "squirrelsql": {"display": "SQuirreL SQL"},

  # IDEasy extensions / manually maintained entries without a tool folder
  "custom": {"display": "Custom tool support"},
  "extra": {"display": "Extra tools"},
  "gui": {"display": "GUI / IDE launcher", "labels": ["GUI"]},
  "git": {"display": "git"},
  "rancher": {"display": "Rancher Desktop"},
}

# --- Commandlet / core-feature registry ---
# Same structure as TOOLS. The key is the GitHub label for that functional area.

COMMANDLETS: dict[str, dict] = {
  # Core commands
  "cli": {"display": "CLI", "labels": ["CLI"]},
  "commandlet": {"display": "Commandlet", "labels": ["commandlet"]},
  "integration": {"display": "Integration", "labels": ["integration"]},
  "core": {"display": "Core / Runtime", "labels": ["progressbar"]},
  "install": {"display": "ide install"},
  "uninstall": {"display": "ide uninstall"},
  "update": {"display": "ide update"},
  "create-project": {"display": "ide create-project", "labels": ["create"]},
  "build": {"display": "ide build"},
  "status": {"display": "ide status"},
  "version": {"display": "Version management"},

  # Configuration
  "settings": {
    "display": "Settings / Properties",
    "labels": ["configuration", "json", "upgrade-settings"],
  },
  "icd": {"display": "IDE Configuration Doc"},
  "merger": {"display": "Settings merger"},
  "env": {"display": "Environment variables"},

  # Download & install
  "download": {"display": "Download & Extraction", "labels": ["unpack", "urls"]},

  # Shell & terminal
  "shell": {"display": "Shell integration", "labels": ["PowerShell"]},
  "completion": {"display": "Shell completion"},

  # Infrastructure
  "proxy": {"display": "Proxy / Network"},
  "security": {"display": "Security / Credentials"},

  # Repository & workspace
  "repository": {"display": "Repository management", "labels": ["SCM"]},
  "workspace": {"display": "Workspace management"},

  # Plugin management
  "plugin": {"display": "Plugin management", "labels": ["plugins"]},

  # Observability
  "logging": {"display": "Logging / Debug output"},

  # Migration
  "migration": {"display": "Migration (devonfw-ide to IDEasy)"},
}

# --- Optional category groupings ---
# Useful if grouped rendering is reintroduced later.

TOOL_CATEGORIES: list[tuple[str, list[str]]] = [
  ("IDEs and Editors", ["androidstudio", "eclipse", "intellij", "pycharm", "vscode"]),
  (
    "JVM and Build Tools",
    ["java", "graalvm", "kotlinc", "mvn", "gradle", "spring", "quarkus", "tomcat", "jasypt", "jmc", "gcviewer"],
  ),
  ("Python", ["python", "pip", "uv"]),
  ("JavaScript / Node.js", ["node", "npm", "ng", "yarn", "corepack"]),
  ("Go", ["go"]),
  (
    "Cloud and DevOps",
    ["docker", "lazydocker", "kubectl", "oc", "helm", "terraform", "aws", "az", "dotnet"],
  ),
  ("Developer Tools", ["gh", "copilot", "sonar"]),
  ("Database", ["pgadmin", "squirrelsql"]),
  ("IDEasy Extensions", ["custom", "extra", "gui", "git", "rancher"]),
]

COMMANDLET_CATEGORIES: list[tuple[str, list[str]]] = [
  ("Core Commands", ["cli", "commandlet", "integration", "core", "install", "uninstall", "update", "create-project", "build", "status", "version"]),
  ("Configuration", ["settings", "icd", "merger", "env"]),
  ("Download and Install", ["download"]),
  ("Shell and Terminal", ["shell", "completion"]),
  ("Infrastructure", ["proxy", "security"]),
  ("Repository and Workspace", ["repository", "workspace"]),
  ("Plugin Management", ["plugin"]),
  ("Observability", ["logging"]),
  ("Migration", ["migration"]),
]

# --- Derived lookups ---
# Computed from the registries above — do not edit directly.

TOOL_DISPLAY: dict[str, str] = {name: cfg["display"] for name, cfg in TOOLS.items()}
CMD_DISPLAY: dict[str, str] = {name: cfg["display"] for name, cfg in COMMANDLETS.items()}

LABEL_ALIASES: dict[str, str] = {
  alias: name
  for registry in (TOOLS, COMMANDLETS)
  for name, cfg in registry.items()
  for alias in cfg.get("labels", [])
}

SKIP_LABELS: frozenset[str] = frozenset({
  BLOCKER_LABEL, "bug", "bugfix", "enhancement", "feature", "task",
  "Epic", "ready-to-implement", "waiting for feedback", "release",
  "AI", "ARM", "claude", "internal", "process", "rewrite", "software", "workflow",
  "test", "integration-tests", "testing",
  "question", "duplicate", "wontfix", "invalid", "help wanted",
  "good first issue", "documentation", "dependencies", "refactoring",
  *OS_LABELS.values(),
})
