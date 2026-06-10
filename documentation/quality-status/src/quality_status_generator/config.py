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
OS_FILE_SUFFIX = {
  "windows": "windows",
  "linux": "linux",
  "mac": "macos",
}

BUG_TYPE_NAMES = {"Bug", "bug"}
BLOCKER_LABEL = "blocker"

TOOLS: dict[str, dict] = {
  "androidstudio": {"display": "Android Studio", "labels": ["android-studio"]},
  "eclipse": {"display": "Eclipse"},
  "intellij": {"display": "IntelliJ IDEA", "labels": ["intellij-idea"]},

AGE_BUCKETS = [
  ("0-10 days", 10),
  ("11-30 days", 30),
  ("31-60 days", 60),
  ("61-90 days", 90),
  ("90+ days", None),
]
