#!/usr/bin/env python3
"""Generates quality-status.adoc for the devonfw/IDEasy GitHub repository.

The script fetches open issues via the GitHub REST API and maps them to known
tools and commandlets by matching issue labels against the registries defined
in this module.  The output document follows standard QA reporting structure:

1. Overview         — metadata, severity legend, issue statistics
2. Bugs & Blockers  — deduplicated table of all bugs/blockers with OS columns
3. Commandlets      — one cross-OS table grouped by functional category
4. Windows Tools    — per-OS tool status table
5. Linux Tools      — per-OS tool status table
6. macOS Tools      — per-OS tool status table
7. Unassigned       — issues that matched no known label
8. How to Contribute

OS label behaviour:
    Issues labelled with a specific OS (windows / linux / macOS) appear only
    in that OS section.  Issues without any OS label are treated as
    cross-platform and appear in every OS section.

Severity model:
    Blocker     — labelled "blocker"; fully prevents users from working
    Bug         — GitHub issue type "Bug", legacy "bug" label, or "blocker"
    Enhancement — everything else (feature request, improvement, task)

Usage::

    python generate_quality_status.py [--token TOKEN] [--output PATH]
    export GITHUB_TOKEN=ghp_... && python generate_quality_status.py
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from collections import defaultdict
from datetime import datetime, timezone

# ─── Repository configuration ─────────────────────────────────────────────────

REPO = "devonfw/IDEasy"
API_BASE = "https://api.github.com"
TOOL_FOLDER_PATH = "cli/src/main/java/com/devonfw/tools/ide/tool"

OS_LABELS = {"windows": "windows", "linux": "linux", "mac": "macOS"}
OS_ORDER = ["windows", "linux", "mac"]
OS_DISPLAY = {"windows": "Windows", "linux": "Linux", "mac": "macOS"}
OS_SHORT = {"windows": "Win", "linux": "Linux", "mac": "macOS"}

BUG_TYPE_NAMES = {"Bug", "bug"}
BLOCKER_LABEL = "blocker"

# ─── Tool registry ────────────────────────────────────────────────────────────
# Each key is the canonical tool folder name under TOOL_FOLDER_PATH, which is
# also the default GitHub label.  "display" is the human-readable table name.
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

# ─── Commandlet / core-feature registry ───────────────────────────────────────
# Same structure as TOOLS.  The key is the GitHub label for that functional area.

COMMANDLETS: dict[str, dict] = {
  # Core commands
  "ide": {"display": "IDEasy (general)", "labels": ["CLI", "commandlet", "integration"]},
  "core": {"display": "Core / Runtime", "labels": ["progressbar"]},
  "install": {"display": "ide install"},
  "uninstall": {"display": "ide uninstall"},
  "update": {"display": "ide update"},
  "create-project": {"display": "ide create-project", "labels": ["create"]},
  "build": {"display": "ide build"},
  "status": {"display": "ide status"},
  "version": {"display": "Version management"},
  # Configuration
  "settings": {"display": "Settings / Properties", "labels": ["configuration", "json", "upgrade-settings"]},
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

# ─── Category groupings ────────────────────────────────────────────────────────
# Ordered list of (category_label, [canonical_keys]).  Controls the spanning
# header rows and row order in the rendered status tables.

TOOL_CATEGORIES: list[tuple[str, list[str]]] = [
  ("IDEs and Editors", ["androidstudio", "eclipse", "intellij", "pycharm", "vscode"]),
  ("JVM and Build Tools", ["java", "graalvm", "kotlinc", "mvn", "gradle",
                           "spring", "quarkus", "tomcat", "jasypt", "jmc", "gcviewer"]),
  ("Python", ["python", "pip", "uv"]),
  ("JavaScript / Node.js", ["node", "npm", "ng", "yarn", "corepack"]),
  ("Go", ["go"]),
  ("Cloud and DevOps", ["docker", "lazydocker", "kubectl", "oc", "helm",
                        "terraform", "aws", "az", "dotnet"]),
  ("Developer Tools", ["gh", "copilot", "sonar"]),
  ("Database", ["pgadmin", "squirrelsql"]),
  ("IDEasy Extensions", ["custom", "extra", "gui", "git", "rancher"]),
]

COMMANDLET_CATEGORIES: list[tuple[str, list[str]]] = [
  ("Core Commands", ["ide", "core", "install", "uninstall", "update",
                     "create-project", "build", "status", "version"]),
  ("Configuration", ["settings", "icd", "merger", "env"]),
  ("Download and Install", ["download"]),
  ("Shell and Terminal", ["shell", "completion"]),
  ("Infrastructure", ["proxy", "security"]),
  ("Repository and Workspace", ["repository", "workspace"]),
  ("Plugin Management", ["plugin"]),
  ("Observability", ["logging"]),
  ("Migration", ["migration"]),
]

# ─── Derived lookups ──────────────────────────────────────────────────────────
# These are computed from the registries above and must not be edited directly.

TOOL_DISPLAY: dict[str, str] = {n: c["display"] for n, c in TOOLS.items()}
CMD_DISPLAY: dict[str, str] = {n: c["display"] for n, c in COMMANDLETS.items()}

LABEL_ALIASES: dict[str, str] = {
  alias: name
  for registry in (TOOLS, COMMANDLETS)
  for name, cfg in registry.items()
  for alias in cfg.get("labels", [])
}

_SKIP_LABELS: frozenset[str] = frozenset({
  BLOCKER_LABEL, "bug", "bugfix", "enhancement", "feature", "task",
  "Epic", "ready-to-implement", "waiting for feedback", "release",
  "AI", "ARM", "claude", "internal", "process", "rewrite", "software", "workflow",
  "test", "integration-tests", "testing",
  "question", "duplicate", "wontfix", "invalid", "help wanted",
  "good first issue", "documentation", "dependencies", "refactoring",
  *OS_LABELS.values(),
})


# ─── GitHub API helpers ───────────────────────────────────────────────────────

def _get(path: str, token: str | None, params: dict | None = None) -> object:
  """Send an authenticated GET request to the GitHub REST API.

  Args:
    path:   API path relative to API_BASE, e.g. "/repos/owner/repo/issues".
    token:  Personal access token.  When ``None`` requests are unauthenticated
            and subject to a 60 req/hour rate limit.
    params: Optional query-string parameters.

  Returns:
    The parsed JSON response body.

  Raises:
    urllib.error.HTTPError: When the server returns a 4xx or 5xx status.
  """
  url = f"{API_BASE}{path}"
  if params:
    url += "?" + urllib.parse.urlencode(params)
  req = urllib.request.Request(url)
  req.add_header("Accept", "application/vnd.github+json")
  req.add_header("X-GitHub-Api-Version", "2022-11-28")
  if token:
    req.add_header("Authorization", f"Bearer {token}")
  with urllib.request.urlopen(req) as resp:
    return json.loads(resp.read().decode())


def fetch_tool_names(token: str | None) -> list[str]:
  """Return the sorted list of tool folder names from the source tree.

  Queries the GitHub Contents API for the sub-directories of TOOL_FOLDER_PATH.
  Each directory name corresponds to a tool and is also its default issue label.
  Returns an empty list and prints a warning if the request fails.

  Args:
    token: GitHub personal access token.
  """
  try:
    entries = _get(f"/repos/{REPO}/contents/{TOOL_FOLDER_PATH}", token)
    return sorted(
      e["name"] for e in entries
      if isinstance(e, dict) and e.get("type") == "dir"
    )
  except urllib.error.HTTPError as exc:
    hint = "" if token else " Set GITHUB_TOKEN to avoid rate limiting."
    print(
      f"WARNING: Could not fetch tool list ({exc.code} {exc.reason}). "
      f"Only commandlet sections will be generated.{hint}",
      file=sys.stderr,
    )
    return []
  except Exception as exc:
    print(f"WARNING: Could not fetch tool list ({exc}).", file=sys.stderr)
    return []


def fetch_all_issues(token: str | None, state: str = "open") -> list[dict]:
  """Fetch all non-pull-request issues from the repository with pagination.

  Args:
    token: GitHub personal access token.
    state: Issue state filter — ``"open"`` or ``"all"``.

  Returns:
    A flat list of issue objects as returned by the GitHub API.
  """
  issues, page = [], 1
  while True:
    batch = _get(
      f"/repos/{REPO}/issues", token,
      {"state": state, "per_page": 100, "page": page},
    )
    if not isinstance(batch, list) or not batch:
      break
    issues.extend(i for i in batch if "pull_request" not in i)
    if len(batch) < 100:
      break
    page += 1
  return issues


# ─── Issue classification ─────────────────────────────────────────────────────

def label_names(issue: dict) -> set[str]:
  """Return the set of label name strings attached to an issue."""
  return {lbl["name"] for lbl in issue.get("labels", [])}


def is_bug(issue: dict, labels: set[str]) -> bool:
  """Return True if the issue is classified as a bug.

  Detection priority:
  1. GitHub issue type field (``issue["type"]["name"]``).
  2. Legacy ``"bug"`` label, predating GitHub issue types.
  3. ``"blocker"`` label — implies a severe bug by definition.
  """
  t = issue.get("type")
  if isinstance(t, dict) and t.get("name") in BUG_TYPE_NAMES:
    return True
  return "bug" in labels or BLOCKER_LABEL in labels


def is_blocker(labels: set[str]) -> bool:
  """Return True if the issue carries the ``"blocker"`` label."""
  return BLOCKER_LABEL in labels


def os_keys_for_issue(labels: set[str]) -> list[str]:
  """Return the OS keys an issue applies to.

  Issues labelled with a specific OS (e.g. ``"windows"``) are scoped to that
  OS only.  Issues without any OS label are treated as cross-platform and
  return all keys from OS_ORDER.
  """
  found = [k for k, lbl in OS_LABELS.items() if lbl in labels]
  return found if found else list(OS_ORDER)


def topic_matches(labels: set[str], known: set[str]) -> list[str]:
  """Return the canonical topic keys matched by the given label set.

  Checks both exact matches against ``known`` and indirect matches via
  LABEL_ALIASES, enabling non-standard label names to map to canonical keys.

  Args:
    labels: Label names attached to an issue.
    known:  Canonical key set to match against (tool keys or commandlet keys).

  Returns:
    Sorted list of matching canonical keys.
  """
  matched: set[str] = set()
  for lbl in labels:
    if lbl in known:
      matched.add(lbl)
    elif lbl in LABEL_ALIASES and LABEL_ALIASES[lbl] in known:
      matched.add(LABEL_ALIASES[lbl])
  return sorted(matched)


# ─── Data model ───────────────────────────────────────────────────────────────

#: Immutable record for a classified issue.
#: Fields: (number, title, url, is_bug, is_blocker, os_keys)
IssueRef = tuple[int, str, str, bool, bool, list[str]]


def classify_issues(
    issues: list[dict],
    tool_keys: set[str],
    cmd_keys: set[str],
) -> tuple[
  dict[str, dict[str, list[IssueRef]]],
  dict[str, dict[str, list[IssueRef]]],
  list[dict],
]:
  """Classify every issue into tool data, commandlet data, or unassigned.

  An issue may match multiple topics and multiple OS sections simultaneously.
  Issues without an OS label are inserted into every OS bucket.

  Args:
    issues:    Raw issue objects from the GitHub API.
    tool_keys: Canonical tool key set used for label matching.
    cmd_keys:  Canonical commandlet key set used for label matching.

  Returns:
    A three-tuple of:
    - ``tool_data[os_key][topic]``  — IssueRef lists for tool rows
    - ``cmd_data[os_key][topic]``   — IssueRef lists for commandlet rows
    - ``unassigned``                — issues that matched no known topic
  """
  tool_data: dict = defaultdict(lambda: defaultdict(list))
  cmd_data: dict = defaultdict(lambda: defaultdict(list))
  unassigned: list[dict] = []

  for issue in issues:
    labels = label_names(issue)
    bug = is_bug(issue, labels)
    blocker = is_blocker(labels)
    os_keys = os_keys_for_issue(labels)
    ref: IssueRef = (
      issue["number"], issue["title"], issue["html_url"], bug, blocker, os_keys
    )
    t_hits = topic_matches(labels, tool_keys)
    c_hits = topic_matches(labels, cmd_keys)

    if not t_hits and not c_hits:
      unassigned.append(issue)
      continue

    for ok in os_keys:
      for t in t_hits:
        tool_data[ok][t].append(ref)
      for c in c_hits:
        cmd_data[ok][c].append(ref)

  return dict(tool_data), dict(cmd_data), unassigned


# ─── AsciiDoc rendering helpers ───────────────────────────────────────────────

def _safe(text: str, max_len: int = 72) -> str:
  """Sanitise text for use inside an AsciiDoc table cell.

  Replaces pipe characters (which would break cell boundaries) and newlines,
  then truncates to ``max_len`` characters, appending an ellipsis if needed.
  """
  s = text.replace("|", "-").replace("\n", " ")
  return s[:max_len] + (" \u2026" if len(s) > max_len else "")


def _severity_label(bug: bool, blocker: bool) -> str:
  """Return the human-readable severity label for a given bug/blocker pair."""
  if blocker: return "Blocker"
  if bug:     return "Bug"
  return "Enhancement"


def _severity_icon(bug: bool, blocker: bool) -> str:
  """Return the status emoji for a given bug/blocker pair."""
  if blocker: return "🚨"
  if bug:     return "🔴"
  return "🟡"


def _status_cell(refs: list[IssueRef]) -> str:
  """Return the worst-case status cell string for a list of issue refs.

  Evaluates blockers before bugs before enhancements, so a single blocker
  in an otherwise green component produces ``"🚨 Blocker"``.
  """
  if not refs:                         return "🟢 OK"
  if any(r[4] for r in refs):          return "🚨 Blocker"
  if any(r[3] for r in refs):          return "🔴 Bug"
  return "🟡 Enhancement"


def _issue_cell(refs: list[IssueRef]) -> str:
  """Format a list of issue refs as an AsciiDoc line-break-separated cell.

  Entries are sorted by severity descending (blocker → bug → enhancement),
  then by issue number ascending.  Each entry renders as:
  ``<icon> link:<url>[#N] <title>``
  """
  if not refs:
    return "—"

  def _sort_key(r: IssueRef) -> tuple:
    return (0 if r[4] else 1 if r[3] else 2, r[0])

  parts = [
    f"{_severity_icon(bug, blocker)} link:{url}[#{num}] {_safe(title)}"
    for num, title, url, bug, blocker, _ in sorted(refs, key=_sort_key)
  ]
  return " +\n".join(parts)


# ─── Section renderers ────────────────────────────────────────────────────────

def _bugs_all_platforms_section(
    tool_data: dict,
    cmd_data: dict,
    tool_disp: dict[str, str],
    cmd_disp: dict[str, str],
    unassigned: list[dict],
) -> str:
  """Render the global Bugs and Blockers section.

  Collects every bug and blocker from tool_data and cmd_data across all OS
  buckets, then deduplicates by issue number so each issue appears once.
  Also includes any bugs or blockers from the unassigned list (e.g. issues
  labelled only ``"blocker"`` without a tool or commandlet label).

  Enhancements are excluded; they belong in the per-OS and commandlet tables.

  Columns: Issue | Severity | Component | Summary | Win | Linux | macOS
  Rows sorted: blockers first, then bugs, both ascending by issue number.

  Args:
    tool_data:  Classified tool issue data from :func:`classify_issues`.
    cmd_data:   Classified commandlet issue data from :func:`classify_issues`.
    tool_disp:  Mapping of tool key to display name.
    cmd_disp:   Mapping of commandlet key to display name.
    unassigned: Issues that matched no known topic.
  """
  seen: dict[int, tuple[str, str, IssueRef]] = {}

  for lbl, disp in tool_disp.items():
    for ok in OS_ORDER:
      for ref in tool_data.get(ok, {}).get(lbl, []):
        if (ref[3] or ref[4]) and ref[0] not in seen:
          seen[ref[0]] = ("Tool", disp, ref)

  for lbl, disp in cmd_disp.items():
    for ok in OS_ORDER:
      for ref in cmd_data.get(ok, {}).get(lbl, []):
        if (ref[3] or ref[4]) and ref[0] not in seen:
          seen[ref[0]] = ("Commandlet", disp, ref)

  for issue in unassigned:
    lbls = {lbl["name"] for lbl in issue.get("labels", [])}
    bug = is_bug(issue, lbls)
    blocker = is_blocker(lbls)
    if not (bug or blocker):
      continue
    num = issue["number"]
    if num in seen:
      continue
    os_keys = os_keys_for_issue(lbls)
    ref: IssueRef = (num, issue["title"], issue["html_url"], bug, blocker, os_keys)
    seen[num] = ("\u2014", "Unassigned", ref)

  blocker_count = sum(1 for _, _, r in seen.values() if r[4])
  bug_count = len(seen) - blocker_count

  lines: list[str] = [
    "== Bugs and Blockers\n",
    f"All open bugs and blockers across all platforms — "
    f"{blocker_count} blocker(s), {bug_count} bug(s).\n",
    "NOTE: Issues without an OS label are cross-platform and marked with a "
    "checkmark in every OS column.\n",
  ]

  if not seen:
    lines.append("_No open bugs or blockers._ \U0001f389\n")
    return "\n".join(lines)

  lines += [
    '[%header, cols="^1,^2,3,4,^1,^1,^1"]',
    "|===",
    "| # | Severity | Component | Summary | Win | Linux | macOS",
  ]

  def _row_sort(item: tuple) -> tuple:
    _, _, ref = item
    return (0 if ref[4] else 1, ref[0])

  for _, (_, disp, ref) in sorted(seen.items(), key=lambda x: _row_sort(x[1])):
    num, title, url, bug, blocker, ref_os = ref
    sev = f"{_severity_icon(bug, blocker)} {_severity_label(bug, blocker)}"
    os_checks = ["✓" if ok in ref_os else "—" for ok in OS_ORDER]
    lines += [
      f"| link:{url}[#{num}]",
      f"| {sev}",
      f"| {disp}",
      f"| {_safe(title)}",
      *[f"| {c}" for c in os_checks],
      "",
    ]

  lines.append("|===\n")
  return "\n".join(lines)


def _commandlets_global_table(cmd_data: dict) -> str:
  """Render the single cross-OS Commandlets and Core Features section.

  Issue refs from all OS buckets are merged and deduplicated per commandlet
  so that each issue appears only once.  Issues specific to a single OS carry
  a platform tag in brackets (e.g. ``[win]``); cross-platform issues carry
  no tag.  Rows are grouped by functional category; components with no open
  issues are omitted.

  Args:
    cmd_data: Classified commandlet issue data from :func:`classify_issues`.
  """
  all_os = set(OS_ORDER)
  N = 3

  lines = [
    "== Commandlets and Core Features",
    "",
    "NOTE: Components with no open issues are omitted. "
    "OS-specific issues are tagged with their platform in brackets.",
    "",
    '[%header, cols="3,^2,7"]',
    "|===",
    "| Commandlet / Feature | Status | Issues",
  ]

  def _sort_key(r: IssueRef) -> tuple:
    return (0 if r[4] else 1 if r[3] else 2, r[0])

  for cat_name, keys in COMMANDLET_CATEGORIES:
    cat_rows = []
    for key in keys:
      if key not in CMD_DISPLAY:
        continue
      merged: dict[int, IssueRef] = {}
      for ok in OS_ORDER:
        for ref in cmd_data.get(ok, {}).get(key, []):
          if ref[0] not in merged:
            merged[ref[0]] = ref
      if merged:
        cat_rows.append((key, list(merged.values())))

    if not cat_rows:
      continue

    lines += ["", f"{N}+^h| {cat_name}"]
    for key, refs in cat_rows:
      parts = []
      for r in sorted(refs, key=_sort_key):
        num, title, url, bug, blocker, ref_os = r
        icon = _severity_icon(bug, blocker)
        os_tag = (
          "" if set(ref_os) == all_os
          else " [" + "/".join(OS_SHORT[k] for k in ref_os) + "]"
        )
        parts.append(f"{icon} link:{url}[#{num}]{os_tag} {_safe(title)}")
      lines += [
        f"| {CMD_DISPLAY[key]}",
        f"| {_status_cell(refs)}",
        f"| {' +' + chr(10) + ''.join(parts[0:1]) if len(parts) == 1 else (' +' + chr(10)).join(parts)}",
        "",
      ]

  lines += ["|===", ""]
  return "\n".join(lines)


def _os_status_table(
    section_heading: str,
    categories: list[tuple[str, list[str]]],
    display_map: dict[str, str],
    os_data: dict[str, list[IssueRef]],
    col_header: str,
    grouped: bool = True,
) -> list[str]:
  """Render a status table for one OS section.

  Columns: Component | Status | Issues.
  Components with no open issues are omitted; a note above the table states
  this explicitly.

  Args:
    section_heading: AsciiDoc heading text for the sub-section.
    categories:      Ordered category/key groupings from TOOL_CATEGORIES or
                     COMMANDLET_CATEGORIES.
    display_map:     Mapping of canonical key to display name.
    os_data:         ``{topic: [IssueRef]}`` for the target OS.
    col_header:      Header label for the first column.
    grouped:         When ``True``, emit spanning category header rows between
                     groups (suitable for commandlets).  When ``False``, flatten
                     all rows and sort alphabetically by display name (suitable
                     for tools).
  """
  N = 3
  lines: list[str] = [
    f"=== {section_heading}\n",
    "NOTE: Components with no open issues are omitted from this table.\n",
    '[%header, cols="3,^2,7"]',
    "|===",
    f"| {col_header} | Status | Issues",
  ]

  if not grouped:
    flat = sorted(
      (
        (key, display_map[key])
        for cat_keys in (k for _, k in categories)
        for key in cat_keys
        if key in display_map
      ),
      key=lambda x: x[1].lower(),
    )
    for key, disp in flat:
      refs = os_data.get(key, [])
      if refs:
        lines += [f"| {disp}", f"| {_status_cell(refs)}", f"| {_issue_cell(refs)}", ""]
  else:
    for cat_name, keys in categories:
      cat_rows = [
        (k, os_data.get(k, []))
        for k in keys
        if k in display_map and os_data.get(k)
      ]
      if not cat_rows:
        continue
      lines.append(f"\n{N}+^h| {cat_name}")
      for key, refs in cat_rows:
        lines += [f"| {display_map[key]}", f"| {_status_cell(refs)}", f"| {_issue_cell(refs)}", ""]

  lines.append("|===\n")
  return lines


def _os_section(os_key: str, tool_data_for_os: dict[str, list[IssueRef]]) -> str:
  """Render the Tools status section for one operating system.

  Generates a sub-heading with deduplicated issue counts and delegates to
  :func:`_os_status_table` for the actual table.  Commandlets are intentionally
  excluded here; they are covered by the global :func:`_commandlets_global_table`.

  Args:
    os_key:            One of the keys from OS_ORDER (``"windows"``, etc.).
    tool_data_for_os:  ``{topic: [IssueRef]}`` pre-filtered for this OS.
  """
  os_name = OS_DISPLAY[os_key]
  os_label = OS_LABELS[os_key]

  all_refs = {r[0]: r for refs in tool_data_for_os.values() for r in refs}
  n_blockers = sum(1 for r in all_refs.values() if r[4])
  n_bugs = sum(1 for r in all_refs.values() if r[3] and not r[4])
  n_enhs = sum(1 for r in all_refs.values() if not r[3] and not r[4])

  stat_str = (
    f"{n_blockers} blocker(s), {n_bugs} bug(s), {n_enhs} enhancement(s)"
    if all_refs else "no open issues"
  )

  lines: list[str] = [
    f"== {os_name} Tools \u2014 {stat_str}\n",
    f"Open issues labelled `{os_label}` or without any OS label "
    f"(cross-platform issues appear in every OS section).\n",
  ]
  lines += _os_status_table(
    "Tools", TOOL_CATEGORIES, TOOL_DISPLAY, tool_data_for_os, "Tool",
    grouped=False,
  )
  return "\n".join(lines)


def _unassigned_section(unassigned: list[dict]) -> str:
  """Render the Unassigned Issues section.

  Lists every issue that matched no tool or commandlet label, including its
  raw label set.  This section guides maintainers in either adding labels to
  the issues on GitHub or extending the registries in this script.

  Args:
    unassigned: Issues collected by :func:`classify_issues`.
  """
  lines: list[str] = [
    "== Unassigned Issues\n",
    f"{len(unassigned)} issue(s) matched no known tool or commandlet label. "
    "Apply the appropriate label on GitHub, or add an entry to ``TOOLS``, "
    "``COMMANDLETS``, or their ``labels`` list in this script.\n",
  ]
  if not unassigned:
    lines.append("_All issues are assigned._\n")
    return "\n".join(lines)

  lines += ['[%header, cols="^1,4,3"]', "|===", "| Issue | Summary | Labels"]
  for issue in sorted(unassigned, key=lambda i: i["number"]):
    lbls = ", ".join(lbl["name"] for lbl in issue.get("labels", []))
    lines += [
      f"| link:{issue['html_url']}[#{issue['number']}]",
      f"| {_safe(issue['title'])}",
      f"| {lbls or '\u2014'}",
      "",
    ]
  lines.append("|===\n")
  return "\n".join(lines)


# ─── Document header template ─────────────────────────────────────────────────

_HEADER = """\
= IDEasy Quality Status
:toc: left
:toclevels: 3
:icons: font
:source-highlighter: rouge
:nofooter:

== Overview

Automatically generated quality and support status for
https://github.com/{repo}[{repo}].
The tool list is discovered from the source tree at
`{tool_path}`.

NOTE: Issues without an OS label are treated as cross-platform and appear in
every OS section. Issues with a specific OS label appear only in that
OS section.

.Severity Legend
[%header, cols="^1,^2,5"]
|===
| Icon | Severity | Description

| 🚨 | Blocker  | Fully prevents users from working; must be fixed immediately
| 🔴 | Bug      | Confirmed defect with functional impact
| 🟡 | Enhancement | Feature request, improvement, or task (no functional breakage)
| 🟢 | OK       | No open issues
|===

.Issue Statistics
[%header, cols="2,^1,^1,^1,^1"]
|===
| Scope | Total | Blockers | Bugs | Enhancements

| All platforms (deduplicated) | {total} | {n_blockers} | {n_bugs} | {n_enhs}
| Unassigned (no label match)  | {unassigned} | — | — | —
|===

_Generated: {date}_

"""


# ─── Document assembly ────────────────────────────────────────────────────────

def generate_adoc(issues: list[dict], tool_names: list[str]) -> str:
  """Assemble and return the complete AsciiDoc quality-status document.

  Merges the dynamically fetched tool folder names with the statically
  registered TOOLS entries so that tools present in the registry but absent
  from the source tree (e.g. ``git``, ``rancher``) are still recognised.

  Args:
    issues:     Raw issue objects from :func:`fetch_all_issues`.
    tool_names: Tool folder names from :func:`fetch_tool_names`.

  Returns:
    The rendered AsciiDoc document as a string.
  """
  tool_keys = set(tool_names) | set(TOOLS.keys())
  cmd_keys = set(COMMANDLETS.keys())

  tool_data, cmd_data, unassigned = classify_issues(issues, tool_keys, cmd_keys)

  seen_refs: dict[int, IssueRef] = {}
  for ok in OS_ORDER:
    for refs in (*tool_data.get(ok, {}).values(), *cmd_data.get(ok, {}).values()):
      for r in refs:
        if r[0] not in seen_refs:
          seen_refs[r[0]] = r

  n_blockers = sum(1 for r in seen_refs.values() if r[4])
  n_bugs = sum(1 for r in seen_refs.values() if r[3] and not r[4])
  n_enhs = sum(1 for r in seen_refs.values() if not r[3] and not r[4])

  header = _HEADER.format(
    repo=REPO,
    tool_path=TOOL_FOLDER_PATH,
    date=datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC"),
    total=len(seen_refs),
    n_blockers=n_blockers,
    n_bugs=n_bugs,
    n_enhs=n_enhs,
    unassigned=len(unassigned),
  )

  parts = [
    header,
    _bugs_all_platforms_section(tool_data, cmd_data, TOOL_DISPLAY, CMD_DISPLAY, unassigned),
    _commandlets_global_table(cmd_data),
    *[_os_section(ok, tool_data.get(ok, {})) for ok in OS_ORDER],
    _unassigned_section(unassigned),
    f"""\
== How to Contribute

=== Adding a new tool
Once a folder is added under `{TOOL_FOLDER_PATH}` and issues use that folder
name as a label, the tool appears automatically in the next generated document.
Override the display name by adding an entry to ``TOOLS`` in this script.

=== Adding a new commandlet or core feature
Add an entry to ``COMMANDLETS``: key = GitHub label, ``display`` = name,
``labels`` = list of alternative label names.  Add the key to the appropriate
category in ``COMMANDLET_CATEGORIES``.

=== Mapping a non-standard label
Add the label to the ``labels`` list of the matching ``TOOLS`` or
``COMMANDLETS`` entry.  Issues that still match nothing appear in the
*Unassigned Issues* section.
""",
  ]
  return "\n".join(parts)


def main() -> None:
  """Parse command-line arguments, fetch data, and write the output file."""
  parser = argparse.ArgumentParser(
    description=f"Generate quality-status.adoc for {REPO}",
  )
  parser.add_argument(
    "--token", default=os.environ.get("GITHUB_TOKEN"),
    help="GitHub personal access token (or set the GITHUB_TOKEN env var)",
  )
  parser.add_argument("--output", default="quality-status.adoc")
  parser.add_argument("--state", default="open", choices=["open", "all"])
  args = parser.parse_args()

  if not args.token:
    print("WARNING: No GITHUB_TOKEN — rate-limited to 60 req/hour.", file=sys.stderr)

  print("Fetching tool list from source tree ...", file=sys.stderr)
  tool_names = fetch_tool_names(args.token)
  if tool_names:
    print(f"  {len(tool_names)} tools found: {', '.join(tool_names)}", file=sys.stderr)
  else:
    print("  No tools found — check token and network.", file=sys.stderr)

  print(f"Fetching {args.state} issues from {REPO} ...", file=sys.stderr)
  try:
    issues = fetch_all_issues(args.token, state=args.state)
  except urllib.error.HTTPError as exc:
    print(f"GitHub API error: HTTP {exc.code} {exc.reason}", file=sys.stderr)
    sys.exit(1)
  print(f"  {len(issues)} issues loaded.", file=sys.stderr)

  tool_keys = set(tool_names) | set(TOOLS.keys())
  cmd_keys = set(COMMANDLETS.keys())
  _, _, unassigned = classify_issues(issues, tool_keys, cmd_keys)
  matched = len(issues) - len(unassigned)
  pct = f" ({matched / len(issues) * 100:.1f}%)" if issues else ""
  print(f"  {matched} assigned{pct}, {len(unassigned)} unassigned.", file=sys.stderr)

  all_known = tool_keys | cmd_keys | set(LABEL_ALIASES)
  all_labels = {lbl["name"] for issue in issues for lbl in issue.get("labels", [])}
  unmatched = all_labels - all_known - _SKIP_LABELS
  if unmatched:
    print(
      f"  Labels with no mapping (consider adding to TOOLS/COMMANDLETS): "
      f"{', '.join(sorted(unmatched))}",
      file=sys.stderr,
    )

  adoc = generate_adoc(issues, tool_names)
  with open(args.output, "w", encoding="utf-8") as fh:
    fh.write(adoc)
  print(f"Written to {args.output}", file=sys.stderr)


if __name__ == "__main__":
  main()
