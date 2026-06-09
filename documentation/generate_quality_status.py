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
    )

  return sorted(seen.values(), key=_severity_sort_key)

def _os_cell(ref: IssueRef) -> str:
  """Format operating systems for the overview table."""
  if set(ref.os_keys) == set(OS_ORDER):
    return "Windows, Linux, macOS"
  return ", ".join(OS_DISPLAY[os_key] for os_key in ref.os_keys)

def _issue_table(
    title: str,
    refs: list[IssueRef],
    include_os: bool,
) -> str:
  """Render a simple issue table.

  Overview documents include an OS column.
  OS-specific documents only include issue number and summary.
  """
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

  if include_os:
    lines += [
      '[%header, cols="^1,6,2"]',
      "|===",
      "| Issue | Summary | OS",
    ]

    for ref in refs:
      lines += [
        f"| link:{ref.url}[#{ref.number}]",
        f"| {_safe(ref.title)}",
        f"| {_os_cell(ref)}",
        "",
      ]
  else:
    lines += [
      '[%header, cols="^1,7"]',
      "|===",
      "| Issue | Summary",
    ]

    for ref in refs:
      lines += [
        f"| link:{ref.url}[#{ref.number}]",
        f"| {_safe(ref.title)}",
        "",
      ]

  lines += [
    "|===",
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
  """Sort key: blockers first, then bugs, then enhancements, then by number."""
  return (0 if ref.blocker else 1 if ref.bug else 2, ref.number)

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
The tool list is discovered from the source tree at
`{tool_path}`.

NOTE: Issues without an OS label are treated as cross-platform and appear in
every OS section. Issues with a specific OS label appear only in that
OS section.

Issue Statistics
[%header, cols="2,^1"]
|===
| Scope | Total

| All platforms (deduplicated) | {total}
| Unassigned (no label match)  | {unassigned}
|===

_Generated: {date}_

"""

# --- Document output helpers ---

def _os_output_path(main_output: Path, os_key: str) -> Path:
  """Derive the per-OS output file path from the main overview output path."""
  suffix = OS_FILE_SUFFIX[os_key]
  return main_output.with_name(f"{main_output.stem}-{suffix}{main_output.suffix}")
def _os_file_links_section(os_filenames: dict[str, str]) -> str:
  """Render links from the overview document to the per-OS status documents."""
  lines = [
    "== Operating System Status Files",
    "",
    "The detailed tool status is split into one generated file per operating system.",
    "",
    '[%header, cols="2,4"]',
    "|===",
    "| Operating System | Status File",
  ]

  for os_key in OS_ORDER:
    filename = os_filenames[os_key]
    lines += [
      f"| {OS_DISPLAY[os_key]}",
      f"| link:{filename}[{filename}]",
      "",
    ]

  lines += ["|===", ""]
  return "\n".join(lines)

def _os_document(
    os_key: str,
    tool_data: dict,
    cmd_data: dict,
    unassigned: list[dict],
) -> str:
  """Render a standalone status document for one operating system."""
  os_name = OS_DISPLAY[os_key]
  refs = _all_issue_refs(tool_data, cmd_data, unassigned, os_key=os_key)

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
    _issue_table(f"{os_name} Open Issues", refs, include_os=False),
  ])
# --- Document assembly ---

def generate_documents(
    issues: list[dict],
    tool_names: list[str],
    os_filenames: dict[str, str],
) -> dict[str, str]:
  """Assemble the generated AsciiDoc quality-status documents."""
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

  overview_refs = _all_issue_refs(tool_data, cmd_data, unassigned)

  overview_parts = [
    header,
    _os_file_links_section(os_filenames),
    _issue_table("Open Issues", overview_refs, include_os=True),
    f"""\
    == How to update this document

    This document is generated automatically from open GitHub issues in
    https://github.com/{REPO}[{REPO}].

    Issues are assigned to operating systems based on their labels:
    `windows`, `linux`, or `macOS`.

    Issues without an operating system label are treated as cross-platform and
    therefore appear in all operating system specific status files.
    """,
    ]

  documents = {
    "overview": "\n".join(overview_parts),
  }

  for os_key in OS_ORDER:
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
