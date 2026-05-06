#!/usr/bin/env python3
"""
generate_quality_status.py

Generates quality-status.adoc for devonfw/IDEasy by:
  1. Fetching tool names dynamically from the url-updater source tree
     (url-updater/src/main/java/com/devonfw/tools/ide/url/tool/)
     so the list stays accurate as new tools are added to the codebase.
  2. Fetching all open GitHub issues and mapping them to tools/commandlets
     by matching issue labels against the discovered tool names and the
     hard-coded commandlet/core label sets.
  3. Writing a quality-status.adoc with one section per OS containing
     a Tools table.

Usage:
    python generate_quality_status.py [--token TOKEN] [--output PATH]

Recommended: set GITHUB_TOKEN to avoid the 60 req/hour rate limit.
    export GITHUB_TOKEN=ghp_yourtoken
    python generate_quality_status.py

─── LABEL / TYPE STRUCTURE (as used in devonfw/IDEasy) ──────────────────────

  GitHub Issue TYPES  (modern feature, checked via issue["type"]["name"]):
    Bug      → something is broken
    Feature  → enhancement / new capability
    Task     → maintenance / internal work

  OS labels (exact case):
    windows   "specific for Microsoft Windows OS"
    linux     "specific for linux OS (debian, ubuntu, suse, etc.)"
    macOS     "specific for Apple MacOS"      ← capital OS, NOT 'mac'

  Severity label:
    blocker   "severe bug that blocks users in their daily work"

  Tool labels:
    One label per tool, name == the folder name under url/tool/ in the repo.
    Fetched dynamically via the GitHub Contents API.
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

# ─── Configuration ────────────────────────────────────────────────────────────

REPO = "devonfw/IDEasy"
API_BASE = "https://api.github.com"

# Path inside the repo where one sub-folder == one tool label
# Must be relative to the repo root (no leading ../)
TOOL_FOLDER_PATH = "url-updater/src/main/java/com/devonfw/tools/ide/url/tool"

# ── OS labels (exact case as used in the repo) ────────────────────────────────
OS_LABELS = {"windows": "windows", "linux": "linux", "mac": "macOS"}
OS_ORDER = ["windows", "linux", "mac"]
OS_DISPLAY = {"windows": "Windows", "linux": "Linux", "mac": "macOS"}

# ── Bug detection ─────────────────────────────────────────────────────────────
BUG_TYPE_NAMES = {"Bug", "bug"}  # GitHub Types name OR legacy label
BLOCKER_LABEL = "blocker"

# ── Tool registry ─────────────────────────────────────────────────────────────
# Single source of truth for every tool.
#
#   Key          → canonical folder name under url/tool/ (also the default label)
#   "display"    → human-readable name shown in the generated document
#   "labels"     → (optional) additional GitHub label names that map to this tool,
#                  for cases where the label differs from the folder name
#
# TOOL_DISPLAY_OVERRIDES and LABEL_ALIASES are derived from this dict
# automatically — do not edit them directly.
TOOLS: dict[str, dict] = {
  "android-studio": {"display": "Android Studio"},
  "aws": {"display": "AWS CLI"},
  "az": {"display": "Azure CLI", "labels": ["azure-cli"]},
  "copilot": {"display": "GitHub Copilot", "labels": ["github-copilot"]},
  "corepack": {"display": "Corepack"},
  "docker": {"display": "Docker"},
  "dotnet": {"display": "dotnet"},
  "eclipse": {"display": "Eclipse"},
  "gcloud": {"display": "gcloud CLI"},
  "gcviewer": {"display": "GCViewer"},
  "gh": {"display": "GitHub CLI"},
  "git": {"display": "git"},
  "gradle": {"display": "Gradle"},
  "helm": {"display": "Helm"},
  "intellij": {"display": "IntelliJ IDEA", "labels": ["intellij-idea"]},
  "jackson": {"display": "Jackson"},
  "java": {"display": "Java (JDK)", "labels": ["jdk"]},
  "jasypt": {"display": "Jasypt"},
  "jmc": {"display": "JDK Mission Control"},
  "kubectl": {"display": "kubectl"},
  "mvn": {"display": "Maven", "labels": ["maven"]},
  "node": {"display": "Node.js / npm", "labels": ["nodejs", "npm"]},
  "oc": {"display": "OpenShift CLI"},
  "pip": {"display": "pip"},
  "python": {"display": "Python"},
  "quarkus": {"display": "Quarkus CLI"},
  "sonar": {"display": "SonarQube", "labels": ["sonarqube"]},
  "terraform": {"display": "Terraform"},
  "vscode": {"display": "VS Code", "labels": ["vsc"]},
}

# Derived lookups — single source is TOOLS above, do not edit manually.
TOOL_DISPLAY_OVERRIDES: dict[str, str] = {
  name: cfg["display"] for name, cfg in TOOLS.items()
}
LABEL_ALIASES: dict[str, str] = {
  alias: name
  for name, cfg in TOOLS.items()
  for alias in cfg.get("labels", [])
}


# ─── GitHub API helpers ───────────────────────────────────────────────────────

def _get(path: str, token: str | None, params: dict | None = None):
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
  """
  Fetch the sub-folder names under TOOL_FOLDER_PATH via the GitHub Contents
  API. Each folder == one tool, and its name == the issue label used for that
  tool. Returns a sorted list of names.
  Falls back to an empty list if the API call fails (e.g. rate-limited without
  a token), in which case only commandlet/core sections will appear.
  """
  try:
    entries = _get(f"/repos/{REPO}/contents/{TOOL_FOLDER_PATH}", token)
    return sorted(
      e["name"] for e in entries
      if isinstance(e, dict) and e.get("type") == "dir"
    )
  except urllib.error.HTTPError as exc:
    hint = "" if token else " Set GITHUB_TOKEN to avoid rate limiting."
    print(f"WARNING: Could not fetch tool list ({exc.code} {exc.reason}). "
          f"Only commandlet/core sections will be generated.{hint}",
          file=sys.stderr)
    return []
  except Exception as exc:
    print(f"WARNING: Could not fetch tool list ({exc}). "
          "Only commandlet/core sections will be generated.", file=sys.stderr)
    return []


def fetch_all_issues(token: str | None, state: str = "open") -> list[dict]:
  """Fetch all issues (not PRs) with pagination."""
  issues: list[dict] = []
  page = 1
  while True:
    batch = _get(
      f"/repos/{REPO}/issues", token,
      {"state": state, "per_page": 100, "page": page},
    )
    if not isinstance(batch, list) or not batch:
      break
    for item in batch:
      if "pull_request" not in item:
        issues.append(item)
    if len(batch) < 100:
      break
    page += 1
  return issues


# ─── Issue classification ─────────────────────────────────────────────────────

def label_names(issue: dict) -> set[str]:
  return {lbl["name"] for lbl in issue.get("labels", [])}


def is_bug(issue: dict, labels: set[str]) -> bool:
  """
  Detect bugs via (in priority order):
  1. GitHub issue type name  — modern issues use issue["type"]["name"]
  2. Legacy lowercase 'bug' label — older issues before GitHub Types existed
  3. 'blocker' label — implies a severe bug regardless of type field
  """
  issue_type = issue.get("type")
  if isinstance(issue_type, dict) and issue_type.get("name") in BUG_TYPE_NAMES:
    return True
  if "bug" in labels:
    return True
  if BLOCKER_LABEL in labels:
    return True
  return False


def is_blocker(labels: set[str]) -> bool:
  return BLOCKER_LABEL in labels


def os_keys_for_issue(labels: set[str]) -> list[str]:
  """Issues without any OS label affect all platforms."""
  found = [k for k, lbl in OS_LABELS.items() if lbl in labels]
  return found if found else list(OS_ORDER)


def topic_labels_for_issue(labels: set[str], known_topics: set[str]) -> list[str]:
  """
  Return canonical topic names (tool folder names) matched from the issue's
  labels. Checks both exact matches and entries in LABEL_ALIASES so that
  labels whose names differ from the folder name are still mapped correctly.
  """
  matched: set[str] = set()
  for lbl in labels:
    if lbl in known_topics:
      matched.add(lbl)  # exact match
    elif lbl in LABEL_ALIASES:
      canonical = LABEL_ALIASES[lbl]
      if canonical in known_topics:
        matched.add(canonical)  # alias → canonical tool name
  return sorted(matched)


# ─── Data aggregation ─────────────────────────────────────────────────────────

# IssueRef: (number, title, url, is_bug, is_blocker)
IssueRef = tuple[int, str, str, bool, bool]


def build_table(
    issues: list[dict],
    known_topics: set[str],
) -> dict[str, dict[str, list[IssueRef]]]:
  """Return data[os_key][topic_label] = list[IssueRef]."""
  data: dict[str, dict[str, list[IssueRef]]] = defaultdict(lambda: defaultdict(list))
  for issue in issues:
    labels = label_names(issue)
    topics = topic_labels_for_issue(labels, known_topics)
    if not topics:
      continue
    bug = is_bug(issue, labels)
    blocker = is_blocker(labels)
    os_keys = os_keys_for_issue(labels)
    ref: IssueRef = (issue["number"], issue["title"], issue["html_url"], bug, blocker)
    for os_key in os_keys:
      for topic in topics:
        data[os_key][topic].append(ref)
  return data


# ─── Status symbol ────────────────────────────────────────────────────────────

def status_symbol(refs: list[IssueRef]) -> str:
  if not refs:
    return "🟢"
  if any(blk for *_, blk in refs):
    return "🚨"
  if any(bug for _, _, _, bug, _ in refs):
    return "🔴"
  return "🟡"


# ─── AsciiDoc cell helpers ────────────────────────────────────────────────────

def _safe(text: str, max_len: int = 75) -> str:
  """Strip characters that break AsciiDoc table cells."""
  return text.replace("|", "-").replace("\n", " ")[:max_len] + (
    "..." if len(text) > max_len else ""
  )


def fmt_links(refs: list[IssueRef]) -> str:
  if not refs:
    return "-"
  return " ".join(f"link:{url}[#{num}]" for num, _, url, _, _ in sorted(refs))


def fmt_notes(refs: list[IssueRef]) -> str:
  if not refs:
    return "No open issues"
  parts = [
    f"{'🚨 ' if blk else ''}#{num}: {_safe(title)}"
    for num, title, _, _, blk in sorted(refs)
  ]
  return "; ".join(parts)


# ─── AsciiDoc generation ──────────────────────────────────────────────────────

_HEADER = """\
= Quality Status
:toc: left
:toclevels: 2
:icons: font
:source-highlighter: rouge

== Overview

This document gives an overview of the quality and support status of IDEasy tools across
operating systems.
It is *automatically generated* from open GitHub issues in the
https://github.com/{repo}[{repo}] repository.
The tool list is discovered dynamically from the source tree, so it stays
accurate as new tools are added.

Legend:

[cols="1,4"]
|===
| Symbol | Meaning

| 🟢 | No known open issues
| 🟡 | Open feature requests / enhancements only (no bugs)
| 🔴 | At least one open bug
| 🚨 | At least one *blocker* (fully prevents users from working)
|===

_Last generated: {date}_

"""


def _table_section(
    section_title: str,
    col_header: str,
    rows: list[tuple[str, str]],  # (label, display_name)
    os_data: dict[str, list[IssueRef]],
) -> list[str]:
  """Render one [cols=...] AsciiDoc table for a category."""
  lines = [
    f"=== {section_title}\n",
    '[cols="2,1,4,5", options="header"]',
    "|===",
    f"| {col_header} | Status | Issues | Notes",
  ]
  for lbl, disp in rows:
    refs = os_data.get(lbl, [])
    lines += [
      f"| {disp}",
      f"| {status_symbol(refs)}",
      f"| {fmt_links(refs)}",
      f"| {fmt_notes(refs)}",
      "",  # blank line = row separator in AsciiDoc tables
    ]
  lines.append("|===\n")
  return lines


def _os_section(
    os_key: str,
    os_data: dict[str, list[IssueRef]],
    tool_rows: list[tuple[str, str]],
) -> str:
  os_name = OS_DISPLAY[os_key]
  os_lbl = OS_LABELS[os_key]

  lines = [
    f"== {os_name}\n",
    f"Open issues labelled `{os_lbl}` "
    f"(or without any OS label — those affect all platforms).\n",
  ]
  lines += _table_section("Tools", "Tool", tool_rows, os_data)
  return "\n".join(lines)


def generate_adoc(
    issues: list[dict],
    tool_names: list[str],
) -> str:
  tool_rows: list[tuple[str, str]] = [
    (name, TOOL_DISPLAY_OVERRIDES.get(name, name.replace("-", " ").title()))
    for name in tool_names
  ]

  known_topics: set[str] = {t[0] for t in tool_rows}

  table = build_table(issues, known_topics)
  date = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
  header = _HEADER.format(repo=REPO, date=date)

  parts = [header]
  for os_key in OS_ORDER:
    parts.append(_os_section(os_key, table.get(os_key, {}), tool_rows))

  parts.append("""\
== How to contribute

*Adding a new tool* is automatic: as soon as a new folder is added under
`url-updater/src/main/java/com/devonfw/tools/ide/url/tool/` and issues are
labelled with that folder name, the tool appears in the next generated document.
To add a nicer display name, add an entry to `TOOL_DISPLAY_OVERRIDES` in this script.
To map a non-standard label name to a tool, add an entry to `LABEL_ALIASES`.
""")

  return "\n".join(parts)


def main() -> None:
  parser = argparse.ArgumentParser(description=f"Generate quality-status.adoc for {REPO}")
  parser.add_argument("--token", default=os.environ.get("GITHUB_TOKEN"),
                      help="GitHub personal access token (or set GITHUB_TOKEN env var)")
  parser.add_argument("--output", default="quality-status.adoc",
                      help="Output file path (default: quality-status.adoc)")
  parser.add_argument("--state", default="open", choices=["open", "all"],
                      help="Issue state to include (default: open)")
  args = parser.parse_args()

  if not args.token:
    print("WARNING: No GITHUB_TOKEN set — unauthenticated requests are limited "
          "to 60/hour and the tool list fetch also counts against that quota.",
          file=sys.stderr)

  print("Fetching tool list from source tree ...", file=sys.stderr)
  tool_names = fetch_tool_names(args.token)
  if tool_names:
    print(f"  {len(tool_names)} tools found: {', '.join(tool_names)}", file=sys.stderr)
  else:
    print("  No tools found — check GITHUB_TOKEN and network access.", file=sys.stderr)

  print(f"Fetching {args.state} issues from {REPO} ...", file=sys.stderr)
  try:
    issues = fetch_all_issues(args.token, state=args.state)
  except urllib.error.HTTPError as exc:
    print(f"GitHub API error: HTTP {exc.code} {exc.reason}", file=sys.stderr)
    sys.exit(1)
  print(f"  {len(issues)} issues loaded.", file=sys.stderr)

  # Debug: report any issue labels that matched neither a tool nor an alias.
  # Useful for discovering new LABEL_ALIASES entries to add.
  known_topics: set[str] = set(tool_names)
  os_label_values = set(OS_LABELS.values())
  skip_labels = os_label_values | {BLOCKER_LABEL, "bug", "enhancement", "question",
                                   "duplicate", "wontfix", "invalid", "help wanted",
                                   "good first issue", "documentation"}
  all_issue_labels = {lbl["name"] for issue in issues for lbl in issue.get("labels", [])}
  unmatched = all_issue_labels - known_topics - set(LABEL_ALIASES) - skip_labels
  if unmatched:
    print(f"  Unmatched labels (consider adding to LABEL_ALIASES): "
          f"{', '.join(sorted(unmatched))}", file=sys.stderr)

  adoc = generate_adoc(issues, tool_names)
  with open(args.output, "w", encoding="utf-8") as fh:
    fh.write(adoc)
  print(f"Written to {args.output}", file=sys.stderr)


if __name__ == "__main__":
  main()
