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
import logging
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from collections import defaultdict
from datetime import datetime, timezone
from typing import Any, NamedTuple

log = logging.getLogger(__name__)

# --- Repository configuration ---

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

BUG_TYPE_NAMES = {"Bug", "bug"}
BLOCKER_LABEL = "blocker"

# Number of columns in the status tables, used for AsciiDoc column spanning.
TABLE_COLSPAN = 3

# --- Tool registry ---
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

# --- Commandlet / core-feature registry ---
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

# --- Category groupings ---
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


# --- Data model ---

class IssueRef(NamedTuple):
  """Classified issue record used throughout the rendering pipeline."""
  number: int
  title: str
  url: str
  bug: bool
  blocker: bool
  os_keys: list[str]


# --- GitHub API helpers ---

def _get(path: str, token: str | None, params: dict | None = None) -> Any:
  """Send an authenticated GET to the GitHub REST API and return parsed JSON.

  Raises urllib.error.HTTPError on 4xx/5xx responses.
  """
  url = f"{API_BASE}{path}"
  if params:
    url += "?" + urllib.parse.urlencode(params)
  req = urllib.request.Request(url)
  req.add_header("Accept", GITHUB_ACCEPT_HEADER)
  req.add_header("X-GitHub-Api-Version", GITHUB_API_VERSION)
  if token:
    req.add_header("Authorization", f"Bearer {token}")
  with urllib.request.urlopen(req) as resp:
    return json.loads(resp.read().decode())


def fetch_tool_names(token: str | None) -> list[str]:
  """Return sorted tool folder names from the source tree.

  Queries the GitHub Contents API for sub-directories of TOOL_FOLDER_PATH.
  Each directory name is also its default issue label.  Returns an empty list
  if the request fails so the rest of the pipeline can still run.
  """
  try:
    entries = _get(f"/repos/{REPO}/contents/{TOOL_FOLDER_PATH}", token)
    return sorted(
      entry["name"] for entry in entries
      if isinstance(entry, dict) and entry.get("type") == "dir"
    )
  except urllib.error.HTTPError as exc:
    hint = "" if token else " Set GITHUB_TOKEN to avoid rate limiting."
    log.warning(
      "Could not fetch tool list from %s (%s %s). "
      "Only commandlet sections will be generated.%s",
      TOOL_FOLDER_PATH, exc.code, exc.reason, hint,
    )
    return []
  except (urllib.error.URLError, json.JSONDecodeError) as exc:
    log.warning("Could not fetch tool list from %s: %s", TOOL_FOLDER_PATH, exc, exc_info=True)
    return []


def fetch_all_issues(token: str | None, state: str = "open") -> list[dict]:
  """Fetch all non-pull-request issues from the repository, handling pagination."""
  issues: list[dict] = []
  page = 1
  while True:
    batch = _get(
      f"/repos/{REPO}/issues", token,
      {"state": state, "per_page": GITHUB_PAGE_SIZE, "page": page},
    )
    if not isinstance(batch, list) or not batch:
      break
    issues.extend(item for item in batch if "pull_request" not in item)
    if len(batch) < GITHUB_PAGE_SIZE:
      break
    page += 1
  return issues


# --- Issue classification ---

def label_names(issue: dict) -> set[str]:
  return {label["name"] for label in issue.get("labels", [])}


def is_bug(issue: dict, labels: set[str]) -> bool:
  """Check whether the issue counts as a bug.

  Detection: GitHub issue type field first, then legacy "bug" label,
  then "blocker" (which implies a severe bug by definition).
  """
  issue_type = issue.get("type")
  if isinstance(issue_type, dict) and issue_type.get("name") in BUG_TYPE_NAMES:
    return True
  return "bug" in labels or BLOCKER_LABEL in labels


def is_blocker(labels: set[str]) -> bool:
  return BLOCKER_LABEL in labels


def os_keys_for_issue(labels: set[str]) -> list[str]:
  """Determine which OS keys apply.  No OS label means cross-platform."""
  found = [key for key, label in OS_LABELS.items() if label in labels]
  return found if found else list(OS_ORDER)


def topic_matches(labels: set[str], known: set[str]) -> list[str]:
  """Return canonical topic keys matched by the given label set.

  Checks both direct matches against ``known`` and indirect matches via
  LABEL_ALIASES so that non-standard label names resolve to canonical keys.
  """
  matched: set[str] = set()
  for label in labels:
    if label in known:
      matched.add(label)
    elif label in LABEL_ALIASES and LABEL_ALIASES[label] in known:
      matched.add(LABEL_ALIASES[label])
  return sorted(matched)


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

  Returns a three-tuple of:
  - tool_data[os_key][topic]  — IssueRef lists for tool rows
  - cmd_data[os_key][topic]   — IssueRef lists for commandlet rows
  - unassigned                — issues that matched no known topic
  """
  tool_data: dict = defaultdict(lambda: defaultdict(list))
  cmd_data: dict = defaultdict(lambda: defaultdict(list))
  unassigned: list[dict] = []

  for issue in issues:
    labels = label_names(issue)
    bug = is_bug(issue, labels)
    blocker = is_blocker(labels)
    os_keys = os_keys_for_issue(labels)
    ref = IssueRef(
      number=issue["number"],
      title=issue["title"],
      url=issue["html_url"],
      bug=bug,
      blocker=blocker,
      os_keys=os_keys,
    )
    tool_matches = topic_matches(labels, tool_keys)
    cmd_matches = topic_matches(labels, cmd_keys)

    if not tool_matches and not cmd_matches:
      unassigned.append(issue)
      continue

    for os_key in os_keys:
      for tool_key in tool_matches:
        tool_data[os_key][tool_key].append(ref)
      for cmd_key in cmd_matches:
        cmd_data[os_key][cmd_key].append(ref)

  return dict(tool_data), dict(cmd_data), unassigned


# --- AsciiDoc rendering helpers ---

def _safe(text: str, max_len: int = 72) -> str:
  """Sanitise text for AsciiDoc table cells — escapes pipes, truncates."""
  sanitised = text.replace("|", "-").replace("\n", " ")
  if len(sanitised) > max_len:
    return sanitised[:max_len] + " \u2026"
  return sanitised


def _severity_sort_key(ref: IssueRef) -> tuple:
  """Sort key: blockers first, then bugs, then enhancements, then by number."""
  return (0 if ref.blocker else 1 if ref.bug else 2, ref.number)


def _severity_label(bug: bool, blocker: bool) -> str:
  if blocker:
    return "Blocker"
  if bug:
    return "Bug"
  return "Enhancement"


def _severity_icon(bug: bool, blocker: bool) -> str:
  if blocker:
    return "🚨"
  if bug:
    return "🔴"
  return "🟡"


def _status_cell(refs: list[IssueRef]) -> str:
  """Return the worst-case status string for a list of issue refs."""
  if not refs:
    return "🟢 OK"
  if any(ref.blocker for ref in refs):
    return "🚨 Blocker"
  if any(ref.bug for ref in refs):
    return "🔴 Bug"
  return "🟡 Enhancement"


def _issue_cell(refs: list[IssueRef]) -> str:
  """Format issue refs as an AsciiDoc line-break-separated cell.

  Sorted by severity descending, then issue number ascending.
  """
  if not refs:
    return "—"
  parts = [
    f"{_severity_icon(ref.bug, ref.blocker)} link:{ref.url}[#{ref.number}] {_safe(ref.title)}"
    for ref in sorted(refs, key=_severity_sort_key)
  ]
  return " +\n".join(parts)


# --- Section renderers ---

def _bugs_all_platforms_section(
    tool_data: dict,
    cmd_data: dict,
    tool_display: dict[str, str],
    cmd_display: dict[str, str],
    unassigned: list[dict],
) -> str:
  """Render the global Bugs and Blockers section.

  Collects every bug/blocker from tool and commandlet data across all OS
  buckets, deduplicates by issue number, and includes unassigned bugs/blockers.
  Enhancements are excluded — they belong in the per-OS and commandlet tables.
  """
  seen: dict[int, tuple[str, str, IssueRef]] = {}

  for label, display_name in tool_display.items():
    for os_key in OS_ORDER:
      os_bucket = tool_data.get(os_key, {})
      for ref in os_bucket.get(label, []):
        if (ref.bug or ref.blocker) and ref.number not in seen:
          seen[ref.number] = ("Tool", display_name, ref)

  for label, display_name in cmd_display.items():
    for os_key in OS_ORDER:
      os_bucket = cmd_data.get(os_key, {})
      for ref in os_bucket.get(label, []):
        if (ref.bug or ref.blocker) and ref.number not in seen:
          seen[ref.number] = ("Commandlet", display_name, ref)

  for issue in unassigned:
    labels = {label["name"] for label in issue.get("labels", [])}
    bug = is_bug(issue, labels)
    blocker = is_blocker(labels)
    if not (bug or blocker):
      continue
    issue_number = issue["number"]
    if issue_number in seen:
      continue
    os_keys = os_keys_for_issue(labels)
    ref = IssueRef(
      number=issue_number,
      title=issue["title"],
      url=issue["html_url"],
      bug=bug,
      blocker=blocker,
      os_keys=os_keys,
    )
    seen[issue_number] = ("\u2014", "Unassigned", ref)

  blocker_count = sum(1 for _, _, ref in seen.values() if ref.blocker)
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
    return (0 if ref.blocker else 1, ref.number)

  for _, (_, display_name, ref) in sorted(seen.items(), key=lambda x: _row_sort(x[1])):
    severity = f"{_severity_icon(ref.bug, ref.blocker)} {_severity_label(ref.bug, ref.blocker)}"
    os_checks = ["✓" if os_key in ref.os_keys else "—" for os_key in OS_ORDER]
    lines += [
      f"| link:{ref.url}[#{ref.number}]",
      f"| {severity}",
      f"| {display_name}",
      f"| {_safe(ref.title)}",
      *[f"| {check}" for check in os_checks],
      "",
    ]

  lines.append("|===\n")
  return "\n".join(lines)


def _commandlets_global_table(cmd_data: dict) -> str:
  """Render the cross-OS Commandlets and Core Features section.

  Issue refs from all OS buckets are merged and deduplicated per commandlet.
  OS-specific issues carry a platform tag in brackets; cross-platform ones don't.
  Components with no open issues are omitted.
  """
  all_os = set(OS_ORDER)
  line_break = " +\n"

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

  for cat_name, keys in COMMANDLET_CATEGORIES:
    cat_rows = []
    for key in keys:
      if key not in CMD_DISPLAY:
        continue
      merged: dict[int, IssueRef] = {}
      for os_key in OS_ORDER:
        os_bucket = cmd_data.get(os_key, {})
        for ref in os_bucket.get(key, []):
          if ref.number not in merged:
            merged[ref.number] = ref
      if merged:
        cat_rows.append((key, list(merged.values())))

    if not cat_rows:
      continue

    lines += ["", f"{TABLE_COLSPAN}+^h| {cat_name}"]
    for key, refs in cat_rows:
      parts = []
      for ref in sorted(refs, key=_severity_sort_key):
        icon = _severity_icon(ref.bug, ref.blocker)
        os_tag = (
          "" if set(ref.os_keys) == all_os
          else " [" + "/".join(OS_SHORT[k] for k in ref.os_keys) + "]"
        )
        parts.append(f"{icon} link:{ref.url}[#{ref.number}]{os_tag} {_safe(ref.title)}")

      issue_cell = line_break.join(parts)
      lines += [
        f"| {CMD_DISPLAY[key]}",
        f"| {_status_cell(refs)}",
        f"| {issue_cell}",
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

  When grouped is False, rows are flattened and sorted alphabetically
  (used for tools).  When True, category spanning headers are emitted
  (used for commandlets).  Components with no open issues are omitted.
  """
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
        for cat_keys in (keys for _, keys in categories)
        for key in cat_keys
        if key in display_map
      ),
      key=lambda pair: pair[1].lower(),
    )
    for key, display_name in flat:
      refs = os_data.get(key, [])
      if refs:
        lines += [f"| {display_name}", f"| {_status_cell(refs)}", f"| {_issue_cell(refs)}", ""]
  else:
    for cat_name, keys in categories:
      cat_rows = [
        (key, os_data.get(key, []))
        for key in keys
        if key in display_map and os_data.get(key)
      ]
      if not cat_rows:
        continue
      lines.append(f"\n{TABLE_COLSPAN}+^h| {cat_name}")
      for key, refs in cat_rows:
        lines += [f"| {display_map[key]}", f"| {_status_cell(refs)}", f"| {_issue_cell(refs)}", ""]

  lines.append("|===\n")
  return lines


def _os_section(os_key: str, tool_data_for_os: dict[str, list[IssueRef]]) -> str:
  """Render the Tools status section for one operating system."""
  os_name = OS_DISPLAY[os_key]
  os_label = OS_LABELS[os_key]

  all_refs = {ref.number: ref for refs in tool_data_for_os.values() for ref in refs}
  blocker_count = sum(1 for ref in all_refs.values() if ref.blocker)
  bug_count = sum(1 for ref in all_refs.values() if ref.bug and not ref.blocker)
  enhancement_count = sum(1 for ref in all_refs.values() if not ref.bug and not ref.blocker)

  if all_refs:
    stat_str = f"{blocker_count} blocker(s), {bug_count} bug(s), {enhancement_count} enhancement(s)"
  else:
    stat_str = "no open issues"

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

  Lists every issue that matched no tool or commandlet label so maintainers
  can either fix labels on GitHub or extend the registries in this script.
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
  for issue in sorted(unassigned, key=lambda item: item["number"]):
    label_str = ", ".join(label["name"] for label in issue.get("labels", []))
    lines += [
      f"| link:{issue['html_url']}[#{issue['number']}]",
      f"| {_safe(issue['title'])}",
      f"| {label_str or '\u2014'}",
      "",
    ]
  lines.append("|===\n")
  return "\n".join(lines)


# --- Document header template ---

HEADER_TEMPLATE = """\
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


# --- Document assembly ---

def generate_adoc(issues: list[dict], tool_names: list[str]) -> str:
  """Assemble the complete AsciiDoc quality-status document.

  Merges dynamically fetched tool folder names with the static TOOLS registry
  so that tools present in the registry but absent from the source tree
  (e.g. git, rancher) are still recognised.
  """
  tool_keys = set(tool_names) | set(TOOLS.keys())
  cmd_keys = set(COMMANDLETS.keys())

  tool_data, cmd_data, unassigned = classify_issues(issues, tool_keys, cmd_keys)

  # Deduplicate across all OS buckets for global statistics
  seen_refs: dict[int, IssueRef] = {}
  for os_key in OS_ORDER:
    tool_bucket = tool_data.get(os_key, {})
    cmd_bucket = cmd_data.get(os_key, {})
    for refs in (*tool_bucket.values(), *cmd_bucket.values()):
      for ref in refs:
        if ref.number not in seen_refs:
          seen_refs[ref.number] = ref

  blocker_count = sum(1 for ref in seen_refs.values() if ref.blocker)
  bug_count = sum(1 for ref in seen_refs.values() if ref.bug and not ref.blocker)
  enhancement_count = sum(1 for ref in seen_refs.values() if not ref.bug and not ref.blocker)

  header = HEADER_TEMPLATE.format(
    repo=REPO,
    tool_path=TOOL_FOLDER_PATH,
    date=datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC"),
    total=len(seen_refs),
    n_blockers=blocker_count,
    n_bugs=bug_count,
    n_enhs=enhancement_count,
    unassigned=len(unassigned),
  )

  parts = [
    header,
    _bugs_all_platforms_section(tool_data, cmd_data, TOOL_DISPLAY, CMD_DISPLAY, unassigned),
    _commandlets_global_table(cmd_data),
    *[_os_section(os_key, tool_data.get(os_key, {})) for os_key in OS_ORDER],
    _unassigned_section(unassigned),
    f"""\
== How to update this document

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
  """Entry point: parse arguments, fetch data, write the output file."""
  logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")

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
    log.warning("No GITHUB_TOKEN set — requests will be rate-limited to 60/hour.")

  log.info("Fetching tool list from source tree ...")
  tool_names = fetch_tool_names(args.token)
  if tool_names:
    log.info("  %d tools found: %s", len(tool_names), ", ".join(tool_names))
  else:
    log.warning("  No tools found — check token permissions and network connectivity.")

  log.info("Fetching %s issues from %s ...", args.state, REPO)
  try:
    issues = fetch_all_issues(args.token, state=args.state)
  except urllib.error.HTTPError as exc:
    log.error("GitHub API request failed: HTTP %s %s", exc.code, exc.reason)
    sys.exit(1)
  log.info("  %d issues loaded.", len(issues))

  # Classify once here for stats, then pass issues to generate_adoc which
  # classifies again internally.  The duplication is minor compared to the
  # API calls above, and keeps generate_adoc self-contained.
  tool_keys = set(tool_names) | set(TOOLS.keys())
  cmd_keys = set(COMMANDLETS.keys())
  _, _, unassigned = classify_issues(issues, tool_keys, cmd_keys)
  matched = len(issues) - len(unassigned)
  pct = f" ({matched / len(issues) * 100:.1f}%)" if issues else ""
  log.info("  %d assigned%s, %d unassigned.", matched, pct, len(unassigned))

  all_known = tool_keys | cmd_keys | set(LABEL_ALIASES)
  all_labels = {label["name"] for issue in issues for label in issue.get("labels", [])}
  unmatched = all_labels - all_known - SKIP_LABELS
  if unmatched:
    log.info(
      "  Labels with no mapping (consider adding to TOOLS/COMMANDLETS): %s",
      ", ".join(sorted(unmatched)),
    )

  adoc = generate_adoc(issues, tool_names)
  with open(args.output, "w", encoding="utf-8") as output_file:
    output_file.write(adoc)
  log.info("Written to %s", args.output)


if __name__ == "__main__":
  main()
