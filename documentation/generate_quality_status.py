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
from pathlib import Path
import textwrap

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
OS_FILE_SUFFIX = {"windows": "windows", "linux": "linux", "mac": "macos",}

BUG_TYPE_NAMES = {"Bug", "bug"}
BLOCKER_LABEL = "blocker"

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

# --- Derived lookups ---
# Computed from the registries above — do not edit directly.

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
  created_at: datetime


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

def _age_bucket(ref: IssueRef) -> str:
  age_days = (datetime.now(timezone.utc) - ref.created_at).days

  if age_days > 90:
    return "90+ days"
  elif age_days > 60:
    return "61-90 days"
  elif age_days > 30:
    return "31-60 days"
  elif age_days > 10:
    return "11-30 days"
  else:
    return "0-10 days"
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
      created_at=datetime.fromisoformat(issue["created_at"].replace("Z", "+00:00")),
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
def _all_issue_refs(
    tool_data: dict,
    cmd_data: dict,
    unassigned: list[dict],
    os_key: str | None = None,
) -> list[IssueRef]:
  """Collect and deduplicate issue references.

  If os_key is None, all issues across all operating systems are returned.
  If os_key is set, only issues relevant for that operating system are returned.
  """
  seen: dict[int, IssueRef] = {}
  selected_os_keys = [os_key] if os_key else OS_ORDER

  for current_os_key in selected_os_keys:
    for topic_refs in tool_data.get(current_os_key, {}).values():
      for ref in topic_refs:
        if ref.number not in seen:
          seen[ref.number] = ref

    for topic_refs in cmd_data.get(current_os_key, {}).values():
      for ref in topic_refs:
        if ref.number not in seen:
          seen[ref.number] = ref

  for issue in unassigned:
    labels = label_names(issue)
    issue_os_keys = os_keys_for_issue(labels)

    if os_key and os_key not in issue_os_keys:
      continue

    issue_number = issue["number"]
    if issue_number in seen:
      continue

    seen[issue_number] = IssueRef(
      number=issue_number,
      title=issue["title"],
      url=issue["html_url"],
      bug=is_bug(issue, labels),
      blocker=is_blocker(labels),
      os_keys=issue_os_keys,
      created_at=datetime.fromisoformat(issue["created_at"].replace("Z", "+00:00")),
      )

  return sorted(seen.values(), key=_severity_sort_key)


def _os_cell(ref: IssueRef) -> str:
  return " ".join(
    "✓" if os in ref.os_keys else "—"
    for os in OS_ORDER
  )
def _severity_summary(refs: list[IssueRef]) -> str:
  blockers = sum(1 for ref in refs if ref.blocker)
  bugs = sum(1 for ref in refs if ref.bug and not ref.blocker)
  enhancements = sum(1 for ref in refs if not ref.bug and not ref.blocker)
  return f"{blockers} blocker(s), {bugs} bug(s), {enhancements} enhancement(s)"

def _severity_label(ref: IssueRef) -> str:
  if ref.blocker:
    return "BLOCKER"
  if ref.bug:
    return "BUG"
  return "ENH"

def _issue_table(
    title: str,
    refs: list[IssueRef],
    include_os: bool,
) -> str:
  lines: list[str] = [
    f"== {title}",
    "",
  ]

  if not refs:
    lines += [
      "_No open issues._",
      "",
    ]
    return "\n".join(lines)

  def _age_bucket(ref: IssueRef) -> str:
    age_days = (datetime.now(timezone.utc) - ref.created_at).days

    if age_days > 90:
      return "90+ days"
    elif age_days > 60:
      return "61-90 days"
    elif age_days > 30:
      return "31-60 days"
    elif age_days > 10:
      return "11-30 days"
    else:
      return "0-10 days"

def _issue_table(
    title: str,
    refs: list[IssueRef],
    include_os: bool,
) -> str:
  lines: list[str] = [
    f"== {title}",
    "",
  ]

  if not refs:
    lines += [
      "_No open issues._",
      "",
    ]
    return "\n".join(lines)

  def _age_bucket(ref: IssueRef) -> str:
    age_days = (datetime.now(timezone.utc) - ref.created_at).days
    if age_days > 90:
      return "90+ days"
    if age_days > 60:
      return "61-90 days"
    if age_days > 30:
      return "31-60 days"
    if age_days > 10:
      return "11-30 days"
    return "0-10 days"

  def _severity_group(ref: IssueRef) -> str:
    if ref.blocker:
      return "Blocker"
    if ref.bug:
      return "Bug"
    return "Enhancement"

  col_span = 2
  lines += [
    '[%header, cols="^1,7"]',
    '|===',
    '| Issue | Summary',
  ]

  current_bucket = None
  current_severity = None

  for ref in refs:
    bucket = _age_bucket(ref)
    if bucket != current_bucket:
      current_bucket = bucket
      current_severity = None
      lines += [
        "",
        f"{col_span}+^| *{bucket}*",
      ]

    severity = _severity_group(ref)
    if severity != current_severity:
      current_severity = severity
      lines += [
        "",
        f"{col_span}+^| *{severity}*",
      ]

    lines += [
      f"| link:{ref.url}[#{ref.number}]",
      f"| {_safe(ref.title)}",
      "",
    ]

  lines += [
    '|===',
    "",
  ]

  return "\n".join(lines)


def _safe(text: str, max_len: int = 72) -> str:
  """Sanitise text for AsciiDoc table cells — escapes pipes, truncates."""
  sanitised = text.replace("|", "-").replace("\n", " ")
  if len(sanitised) > max_len:
    return sanitised[:max_len] + " \u2026"
  return sanitised


def _severity_sort_key(ref: IssueRef) -> tuple:
  age_days = (datetime.now(timezone.utc) - ref.created_at).days

  # 1. PRIMARY: Age bucket
  if age_days > 90:
    age_bucket = 0
  elif age_days > 60:
    age_bucket = 1
  elif age_days > 30:
    age_bucket = 2
  elif age_days > 10:
    age_bucket = 3
  else:
    age_bucket = 4

  # 2. SECONDARY: Severity
  severity_bucket = 0 if ref.blocker else 1 if ref.bug else 2

  # 3. TERTIARY: oldest first (wichtig!)
  return (
    age_bucket,
    severity_bucket,
    ref.created_at.timestamp(),
  )

# --- Section renderers ---

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

# --- Document header template ---

HEADER_TEMPLATE = """\
= IDEasy Quality Status
:toc: left
:toclevels: 3
:icons: font
:source-highlighter: rouge
:nofooter:

== Overview

Automatically generated open issue overview for
https://github.com/{repo}[{repo}].


### Issue Statistics
[%header, cols="2,^1"]
|===
| Scope | Total

| All platforms  | {total}
| Assigned       | {assigned}
| Unassigned                    | {unassigned}
| Blockers                      | {n_blockers}
| Bugs                          | {n_bugs}
| Enhancements                  | {n_enhs}

|===


_Generated: {date}_

"""

# --- Document output helpers ---
def analyze_os_issues(
    refs: list[IssueRef],
    os_key: str,
) -> tuple[int, int, int, int, dict[tuple[str, ...], list[IssueRef]]]:
  """Return counts + multi-OS grouping for a given OS"""

  os_specific = [r for r in refs if r.os_keys == [os_key]]

  cross_platform = [
    r for r in refs
    if set(r.os_keys) == set(OS_ORDER)
  ]

  multi_os_groups: dict[tuple[str, ...], list[IssueRef]] = {}

  for r in refs:
    keys = tuple(sorted(r.os_keys))

    if (
      os_key in r.os_keys
      and len(r.os_keys) > 1
      and set(r.os_keys) != set(OS_ORDER)
    ):
      multi_os_groups.setdefault(keys, []).append(r)

  total = len(refs)
  count_specific = len(os_specific)
  count_cross = len(cross_platform)
  count_multi = sum(len(v) for v in multi_os_groups.values())

  return total, count_specific, count_multi, count_cross, multi_os_groups

def _os_output_path(main_output: Path, os_key: str) -> Path:
  """Derive the per-OS output file path from the main overview output path."""
  suffix = OS_FILE_SUFFIX[os_key]
  return main_output.with_name(f"{main_output.stem}-{suffix}{main_output.suffix}")

def _os_file_links_section(
    os_filenames: dict[str, str],
    os_analysis: dict[str, tuple[int, int, int, int, dict]],
) -> str:
  """Render links from the overview document to the per-OS status documents."""

  cross_total = os_analysis[OS_ORDER[0]][3]

  lines = [
    "== Operating System Status Files",
    "",
    "Issues are assigned to operating systems based on their labels:",
    "`windows`, `linux`, or `macOS`.",
    "",
    "Issues without an operating system label are treated as cross-platform.",
    f"A total of {cross_total} cross-platform issues are documented centrally in this document and are therefore not repeated in the operating system specific files.",
    "",
    "The detailed tool status is split into one generated file per operating system.",
    "",
    '[%header, cols="2,1,1,1,4"]',
    "|===",
    "| Operating System | Total | Specific | Multi | Status File",
  ]

  for os_key in OS_ORDER:
    filename = os_filenames[os_key]
    total, spec, multi, _, _ = os_analysis[os_key]

    lines += [
      f"| {OS_DISPLAY[os_key]}",
      f"| {total}",
      f"| {spec}",
      f"| {multi}",
      f"| link:{filename}[{filename}]",
      "",
    ]

  lines += ["|===", ""]
  return "\n".join(lines)

def _render_multi_os_groups(groups: dict, os_key: str) -> str:
  lines = ["== Multi-OS Issues", ""]

  if not groups:
    lines += ["_No multi-OS issues._", ""]
    return "\n".join(lines)

  for combo, refs in sorted(groups.items(), key=lambda x: -len(x[1])):
    display = " + ".join(OS_DISPLAY[o] for o in combo)

    lines += [
      f"=== {display} ({len(refs)})",
      "",
      _issue_table("", refs, include_os=False),
    ]

  return "\n".join(lines)

def _os_document(
    os_key: str,
    tool_data: dict,
    cmd_data: dict,
    unassigned: list[dict],
) -> str:
  os_name = OS_DISPLAY[os_key]

  refs = _all_issue_refs(tool_data, cmd_data, unassigned, os_key=os_key)

  os_specific = [r for r in refs if r.os_keys == [os_key]]

  cross_platform = [
    r for r in refs
    if set(r.os_keys) == set(OS_ORDER)
  ]

  total, count_specific, count_multi, count_cross, multi_os_groups = analyze_os_issues(refs, os_key)

  return "\n".join([
    f"= IDEasy Quality Status — {os_name}",
    ":toc: left",
    ":toclevels: 3",
    ":icons: font",
    ":source-highlighter: rouge",
    ":nofooter:",
    "",
    f"Automatically generated open issue overview for {os_name}.",
    "",
    "link:quality-status.adoc[Back to overview]",
    "",

    f"*Total Issues:* {total}",
    "",
    f"* OS-specific: {count_specific}",
    "",
    f"* Multi-OS: {count_multi}",
    "",
    f"* Cross-platform: {count_cross}",
    "",
    f"*Summary:* {_severity_summary(os_specific)}",
    "",
    _issue_table(f"{os_name} Specific Issues", os_specific, include_os=False),
    _render_multi_os_groups(multi_os_groups, os_key),
    "== Cross-platform Issues",
    "",
    "Cross-platform issues are listed in the overview document: link:quality-status.adoc[Cross-platform Issues].",
    "",
  ])

# --- Document assembly ---
def compute_stats(
    tool_data: dict,
    cmd_data: dict,
    unassigned: list[dict],
) -> dict:
  """Compute quality statistics (top problem areas + age distribution)."""

  now = datetime.now(timezone.utc)

  # --- Collect all unique issues ---
  all_refs = _all_issue_refs(tool_data, cmd_data, unassigned)

  # --- Age distribution (in days) buckets ---
  age_buckets = {
    "0-7": 0,
    "8-30": 0,
    "31-90": 0,
    "90+": 0,
  }

  for ref in all_refs:
    age_days = (now - ref.created_at).days

    if age_days <= 7:
      age_buckets["0-7"] += 1
    elif age_days <= 30:
      age_buckets["8-30"] += 1
    elif age_days <= 90:
      age_buckets["31-90"] += 1
    else:
      age_buckets["90+"] += 1

  area_counts: dict[str, set[int]] = defaultdict(set)

  for os_bucket in tool_data.values():
    for key, refs in os_bucket.items():
      for ref in refs:
        area_counts[key].add(ref.number)

  for os_bucket in cmd_data.values():
    for key, refs in os_bucket.items():
      for ref in refs:
        area_counts[key].add(ref.number)


  # Sort descending
  top_areas = sorted(
    ((k, len(v)) for k, v in area_counts.items()),
    key=lambda x: x[1],
    reverse=True
  )[:10]

  return {
    "age_distribution": age_buckets,
    "top_areas": top_areas,
  }

def render_stats(stats: dict) -> str:
  """Render statistics section as AsciiDoc."""

  lines = [
    "== Quality Insights",
    "",
    "=== Issue Age Distribution",
    '[%header, cols="2,^1"]',
    "|===",
    "| Age Range | Count",
  ]

  age = stats["age_distribution"]

  lines += [
    f"| 0–7 days   | {age['0-7']}",
    f"| 8–30 days  | {age['8-30']}",
    f"| 31–90 days | {age['31-90']}",
    f"| 90+ days   | {age['90+']}",
    "|===",
    "",
    "=== Components with the highest number of assigned issues (no severity or age weighting applied).",
    "",
    "Components represent logical groupings of tools and features based on issue labels. Multiple GitHub labels may map to a single component. Counts reflect the number of unique issues assigned to each component.",
    "",
    '[%header, cols="3,^1"]',
    "|===",
    "| Component | Issues",
  ]

  for key, count in stats["top_areas"]:
    # schöner Name fallback
    display = (
      TOOLS.get(key, {}).get("display")
      or COMMANDLETS.get(key, {}).get("display")
      or key
    )

    lines += [
      f"| {display}",
      f"| {count}",
      "",
    ]

  lines += ["|===", ""]

  return "\n".join(lines)


def generate_documents(
    issues: list[dict],
    tool_names: list[str],
    os_filenames: dict[str, str],
) -> dict[str, str]:
  """Assemble the generated AsciiDoc quality-status documents."""
  tool_keys = set(tool_names) | set(TOOLS.keys())
  cmd_keys = set(COMMANDLETS.keys())

  tool_data, cmd_data, unassigned = classify_issues(issues, tool_keys, cmd_keys)
  os_analysis = {}

  for os_key in OS_ORDER:
    refs = _all_issue_refs(tool_data, cmd_data, unassigned, os_key=os_key)
    os_analysis[os_key] = analyze_os_issues(refs, os_key)

  stats = compute_stats(tool_data, cmd_data, unassigned)

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
    assigned=len(seen_refs),
    n_blockers=blocker_count,
    n_bugs=bug_count,
    n_enhs=enhancement_count,
    unassigned=len(unassigned),
    total=len(seen_refs) + len(unassigned),
  )

  how_to_update = textwrap.dedent(f"""\
  == How to update this document

  This document is generated automatically from open GitHub issues in
  https://github.com/{REPO}[{REPO}].
  """)

  overview_refs = [
    ref for ref in _all_issue_refs(tool_data, cmd_data, unassigned)
    if set(ref.os_keys) == set(OS_ORDER)
  ]

  overview_parts = [
    header,
    _os_file_links_section(os_filenames, os_analysis),
    render_stats(stats),
    f"*Summary:* {_severity_summary(overview_refs)}",
    "",
    _issue_table("Cross-platform Issues", overview_refs, include_os=True),
    how_to_update,
    ]

  documents = {
    "overview": "\n".join(overview_parts),
  }

  for os_key in sorted(OS_ORDER, key=lambda o: os_analysis[o][0], reverse=True):
    documents[os_key] = _os_document(os_key, tool_data, cmd_data, unassigned)

  return documents

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

  main_output = Path(args.output)
  os_output_paths = {
    os_key: _os_output_path(main_output, os_key)
    for os_key in OS_ORDER
  }
  os_filenames = {
    os_key: path.name
    for os_key, path in os_output_paths.items()
  }

  documents = generate_documents(issues, tool_names, os_filenames)

  main_output.parent.mkdir(parents=True, exist_ok=True)
  main_output.write_text(documents["overview"], encoding="utf-8")
  log.info("Written to %s", main_output)

  for os_key, output_path in os_output_paths.items():
    output_path.write_text(documents[os_key], encoding="utf-8")
    log.info("Written to %s", output_path)

if __name__ == "__main__":
  main()
