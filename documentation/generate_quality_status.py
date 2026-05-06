#!/usr/bin/env python3
"""
generate_quality_status.py

Generates quality-status.adoc for devonfw/IDEasy by:
  1. Fetching tool names dynamically from the url-updater source tree
     (url-updater/src/main/java/com/devonfw/tools/ide/url/tool/)
     so the list stays accurate as new tools are added to the codebase.
  2. Fetching all open GitHub issues and mapping them to tools or commandlets
     by matching issue labels against the discovered tool names and the
     COMMANDLETS registry defined in this script.
  3. Writing a quality-status.adoc with:
       - A summary section listing all active blockers and a status-at-a-glance matrix
       - One section per OS, each with a Tools table and a Commandlets table
       - A section for issues that could not be assigned to any known topic

OS LABEL BEHAVIOUR
──────────────────
Issues labelled with a specific OS (windows / linux / macOS) appear *only* in
that OS section.  Issues with *no* OS label are treated as cross-platform and
appear in *every* OS section (Windows, Linux, macOS).

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
# Single source of truth for every managed tool.
#
#   Key          → canonical folder name under url/tool/ (also the default label)
#   "display"    → human-readable name shown in the generated document
#   "labels"     → (optional) additional GitHub label names that map to this tool,
#                  for cases where the label differs from the folder name
#
# TOOL_DISPLAY_OVERRIDES and LABEL_ALIASES are derived automatically below.
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

# ── Commandlet / core-feature registry ───────────────────────────────────────
# Same structure as TOOLS but for IDEasy commandlets and cross-cutting features.
# The key is the GitHub label name used for that area.
# Add or rename entries here as the project evolves.
COMMANDLETS: dict[str, dict] = {
  "ide": {"display": "IDEasy (general)"},
  "install": {"display": "ide install"},
  "uninstall": {"display": "ide uninstall"},
  "update": {"display": "ide update"},
  "create-project": {"display": "ide create-project", "labels": ["create"]},
  "build": {"display": "ide build"},
  "shell": {"display": "Shell integration"},
  "completion": {"display": "Shell completion"},
  "settings": {"display": "Settings / Properties", "labels": ["configuration", "properties"]},
  "security": {"display": "Security / Credentials"},
  "repository": {"display": "Repository management", "labels": ["repo"]},
  "env": {"display": "Environment variables"},
  "plugin": {"display": "Plugin management"},
  "migration": {"display": "Migration (devonfw-ide → IDEasy)"},
  "status": {"display": "ide status"},
  "version": {"display": "Version management"},
}

# ── Derived lookups ────────────────────────────────────────────────────────────
# Do not edit these directly — maintain TOOLS and COMMANDLETS above instead.

TOOL_DISPLAY_OVERRIDES: dict[str, str] = {
  name: cfg["display"] for name, cfg in TOOLS.items()
}
COMMANDLET_DISPLAY_OVERRIDES: dict[str, str] = {
  name: cfg["display"] for name, cfg in COMMANDLETS.items()
}

# Combined alias map: non-canonical label → canonical key (tool or commandlet)
LABEL_ALIASES: dict[str, str] = {
  alias: name
  for registry in (TOOLS, COMMANDLETS)
  for name, cfg in registry.items()
  for alias in cfg.get("labels", [])
}

# Labels that are meta/infrastructure — skipped in unmatched-label reporting
_SKIP_LABELS: frozenset[str] = frozenset({
  BLOCKER_LABEL, "bug", "enhancement", "feature", "task", "question",
  "duplicate", "wontfix", "invalid", "help wanted", "good first issue",
  "documentation", "dependencies", "refactoring", "testing",
  *OS_LABELS.values(),
})


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
  """
  Issues with a specific OS label appear only in that OS section.
  Issues with NO OS label are treated as cross-platform and appear in every
  OS section (Windows, Linux, macOS).
  """
  found = [k for k, lbl in OS_LABELS.items() if lbl in labels]
  return found if found else list(OS_ORDER)


def topic_labels_for_issue(labels: set[str], known_topics: set[str]) -> list[str]:
  """
  Return canonical topic names matched from the issue's labels.
  Checks both exact matches and LABEL_ALIASES so that labels whose names
  differ from the canonical key are still mapped correctly.
  """
  matched: set[str] = set()
  for lbl in labels:
    if lbl in known_topics:
      matched.add(lbl)
    elif lbl in LABEL_ALIASES and LABEL_ALIASES[lbl] in known_topics:
      matched.add(LABEL_ALIASES[lbl])
  return sorted(matched)


# ─── Data aggregation ─────────────────────────────────────────────────────────

# IssueRef: (number, title, url, is_bug, is_blocker)
IssueRef = tuple[int, str, str, bool, bool]


def classify_issues(
    issues: list[dict],
    tool_keys: set[str],
    commandlet_keys: set[str],
) -> tuple[
  dict[str, dict[str, list[IssueRef]]],  # tool_data[os_key][tool]
  dict[str, dict[str, list[IssueRef]]],  # cmd_data[os_key][commandlet]
  list[dict],  # unassigned issues
]:
  """
  Classify every issue into tools, commandlets, or unassigned.
  An issue can match multiple tools/commandlets and multiple OS sections.
  Issues without an OS label appear in every OS section.
  """
  tool_data: dict[str, dict[str, list[IssueRef]]] = defaultdict(lambda: defaultdict(list))
  cmd_data: dict[str, dict[str, list[IssueRef]]] = defaultdict(lambda: defaultdict(list))
  unassigned: list[dict] = []

  for issue in issues:
    labels = label_names(issue)
    bug = is_bug(issue, labels)
    blocker = is_blocker(labels)
    os_keys = os_keys_for_issue(labels)
    ref: IssueRef = (issue["number"], issue["title"], issue["html_url"], bug, blocker)

    tool_matches = topic_labels_for_issue(labels, tool_keys)
    cmd_matches = topic_labels_for_issue(labels, commandlet_keys)

    if not tool_matches and not cmd_matches:
      unassigned.append(issue)
      continue

    for os_key in os_keys:
      for t in tool_matches:
        tool_data[os_key][t].append(ref)
      for c in cmd_matches:
        cmd_data[os_key][c].append(ref)

  return dict(tool_data), dict(cmd_data), unassigned


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
  cleaned = text.replace("|", "-").replace("\n", " ")
  return cleaned[:max_len] + ("..." if len(cleaned) > max_len else "")


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


# ─── AsciiDoc section builders ────────────────────────────────────────────────

def _render_table(
    section_title: str,
    col_header: str,
    rows: list[tuple[str, str]],
    os_data: dict[str, list[IssueRef]],
    level: int = 3,
) -> list[str]:
  """Render one AsciiDoc table. `level` controls the heading depth (= signs)."""
  heading = "=" * level
  lines = [
    f"{heading} {section_title}\n",
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
      "",
    ]
  lines.append("|===\n")
  return lines


def _summary_section(
    tool_data: dict[str, dict[str, list[IssueRef]]],
    cmd_data: dict[str, dict[str, list[IssueRef]]],
    tool_rows: list[tuple[str, str]],
    cmd_rows: list[tuple[str, str]],
) -> str:
  """
  Render the top-level Summary section:
    1. Active Blockers table (🚨 only, deduplicated across OS)
    2. Status-at-a-glance matrix for Tools (non-green rows only)
    3. Status-at-a-glance matrix for Commandlets (non-green rows only)
  """
  lines: list[str] = [
    "== Summary\n",
    "Quick overview of the most critical open issues across all platforms.\n",
  ]

  # ── 1. Active Blockers ────────────────────────────────────────────────────
  lines += [
    "=== 🚨 Active Blockers\n",
    "Issues labelled `blocker` that currently prevent users from working.\n",
  ]

  # Collect all blocker refs, deduplicated by issue number
  seen_blocker_ids: set[int] = set()
  unique_blockers: list[tuple[str, str, IssueRef]] = []  # (category, display, ref)

  for os_key in OS_ORDER:
    for lbl, disp in tool_rows:
      for ref in tool_data.get(os_key, {}).get(lbl, []):
        if ref[4] and ref[0] not in seen_blocker_ids:
          seen_blocker_ids.add(ref[0])
          unique_blockers.append(("Tool", disp, ref))
    for lbl, disp in cmd_rows:
      for ref in cmd_data.get(os_key, {}).get(lbl, []):
        if ref[4] and ref[0] not in seen_blocker_ids:
          seen_blocker_ids.add(ref[0])
          unique_blockers.append(("Commandlet", disp, ref))

  if unique_blockers:
    lines += [
      '[cols="1,2,2,5", options="header"]',
      "|===",
      "| Type | Tool / Commandlet | Issue | Title",
    ]
    for cat, disp, (num, title, url, _, _) in sorted(
        unique_blockers, key=lambda r: r[2][0]
    ):
      lines += [
        f"| {cat}",
        f"| {disp}",
        f"| link:{url}[#{num}]",
        f"| {_safe(title)}",
        "",
      ]
    lines.append("|===\n")
  else:
    lines.append("_No active blockers_ 🎉\n")

  # ── 2. Status matrix — Tools ──────────────────────────────────────────────
  lines += [
    "=== Status Matrix — Tools\n",
    "Only tools with at least one open issue are shown.\n",
    '[cols="3,1,1,1", options="header"]',
    "|===",
    "| Tool | Windows | Linux | macOS",
  ]
  for lbl, disp in tool_rows:
    cells = [
      status_symbol(tool_data.get(os_key, {}).get(lbl, []))
      for os_key in OS_ORDER
    ]
    if any(c != "🟢" for c in cells):
      lines += [f"| {disp}", *[f"| {c}" for c in cells], ""]
  lines.append("|===\n")

  # ── 3. Status matrix — Commandlets ───────────────────────────────────────
  lines += [
    "=== Status Matrix — Commandlets & Core Features\n",
    "Only commandlets with at least one open issue are shown.\n",
    '[cols="3,1,1,1", options="header"]',
    "|===",
    "| Commandlet / Feature | Windows | Linux | macOS",
  ]
  for lbl, disp in cmd_rows:
    cells = [
      status_symbol(cmd_data.get(os_key, {}).get(lbl, []))
      for os_key in OS_ORDER
    ]
    if any(c != "🟢" for c in cells):
      lines += [f"| {disp}", *[f"| {c}" for c in cells], ""]
  lines.append("|===\n")

  return "\n".join(lines)


def _os_section(
    os_key: str,
    tool_data_for_os: dict[str, list[IssueRef]],
    cmd_data_for_os: dict[str, list[IssueRef]],
    tool_rows: list[tuple[str, str]],
    cmd_rows: list[tuple[str, str]],
) -> str:
  os_name = OS_DISPLAY[os_key]
  os_lbl = OS_LABELS[os_key]

  lines: list[str] = [
    f"== {os_name}\n",
    f"Open issues labelled `{os_lbl}` *or without any OS label*.\n",
    "NOTE: Issues without an OS label are cross-platform and therefore "
    "appear in every OS section.\n",
  ]
  lines += _render_table("Tools", "Tool", tool_rows, tool_data_for_os, level=3)
  lines += _render_table(
    "Commandlets & Core Features", "Commandlet / Feature",
    cmd_rows, cmd_data_for_os, level=3,
  )
  return "\n".join(lines)


def _unassigned_section(unassigned: list[dict]) -> str:
  """Render a section listing issues that could not be matched to any topic."""
  lines: list[str] = [
    "== Unassigned Issues\n",
    f"The following {len(unassigned)} issue(s) could not be matched to any known "
    "tool or commandlet. Add the appropriate label on GitHub, or extend "
    "`TOOLS`, `COMMANDLETS`, or their `\"labels\"` lists in this script.\n",
  ]
  if not unassigned:
    lines.append("_All issues are assigned._ 🎉\n")
    return "\n".join(lines)

  lines += [
    '[cols="1,4,3", options="header"]',
    "|===",
    "| Issue | Title | Labels",
  ]
  for issue in sorted(unassigned, key=lambda i: i["number"]):
    num = issue["number"]
    url = issue["html_url"]
    lbls = ", ".join(lbl["name"] for lbl in issue.get("labels", []))
    lines += [
      f"| link:{url}[#{num}]",
      f"| {_safe(issue['title'])}",
      f"| {lbls or '—'}",
      "",
    ]
  lines.append("|===\n")
  return "\n".join(lines)


# ─── Header template ──────────────────────────────────────────────────────────

_HEADER = """\
= Quality Status
:toc: left
:toclevels: 2
:icons: font
:source-highlighter: rouge

== Overview

This document gives an overview of the quality and support status of IDEasy tools
and commandlets across operating systems.
It is *automatically generated* from open GitHub issues in the
https://github.com/{repo}[{repo}] repository.
The tool list is discovered dynamically from the source tree, so it stays
accurate as new tools are added.

*OS label behaviour:* Issues labelled with a specific OS appear only in that OS
section. Issues with *no OS label* are treated as cross-platform and appear in
*every* OS section (Windows, Linux, macOS).

Legend:

[cols="1,4"]
|===
| Symbol | Meaning

| 🟢 | No known open issues
| 🟡 | Open feature requests / enhancements only (no bugs)
| 🔴 | At least one open bug
| 🚨 | At least one *blocker* (fully prevents users from working)
|===

_Last generated: {date} — {total} issues loaded, {matched} assigned, {unassigned} unassigned_

"""


# ─── Main generation ──────────────────────────────────────────────────────────

def generate_adoc(issues: list[dict], tool_names: list[str]) -> str:
  tool_rows: list[tuple[str, str]] = [
    (name, TOOL_DISPLAY_OVERRIDES.get(name, name.replace("-", " ").title()))
    for name in tool_names
  ]
  cmd_rows: list[tuple[str, str]] = [
    (name, COMMANDLET_DISPLAY_OVERRIDES[name])
    for name in COMMANDLETS
  ]

  tool_keys = {r[0] for r in tool_rows}
  commandlet_keys = {r[0] for r in cmd_rows}

  tool_data, cmd_data, unassigned = classify_issues(issues, tool_keys, commandlet_keys)

  date = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
  header = _HEADER.format(
    repo=REPO, date=date,
    total=len(issues),
    matched=len(issues) - len(unassigned),
    unassigned=len(unassigned),
  )

  parts = [header]
  parts.append(_summary_section(tool_data, cmd_data, tool_rows, cmd_rows))

  for os_key in OS_ORDER:
    parts.append(_os_section(
      os_key,
      tool_data.get(os_key, {}),
      cmd_data.get(os_key, {}),
      tool_rows,
      cmd_rows,
    ))

  parts.append(_unassigned_section(unassigned))

  parts.append("""\
== How to Contribute

=== Adding a new tool
Adding a new tool is automatic: as soon as a new folder is added under
`url-updater/src/main/java/com/devonfw/tools/ide/url/tool/` and issues are
labelled with that folder name, the tool appears in the next generated document.
To override the display name, add an entry to `TOOLS` in this script.

=== Adding a new commandlet
Add an entry to the `COMMANDLETS` dict in this script. The key is the GitHub
label name; `"display"` is the human-readable name; `"labels"` lists any
alternative label names that should also map to this commandlet.

=== Mapping a non-standard label
If a GitHub label does not match any key in `TOOLS` or `COMMANDLETS`, add the
label name to the `"labels"` list of the appropriate registry entry.
Any remaining unmatched issues will appear in the *Unassigned Issues* section.
""")

  return "\n".join(parts)


# ─── Main entry point ─────────────────────────────────────────────────────────

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

  # ── Fetch tool list ──────────────────────────────────────────────────────
  print("Fetching tool list from source tree ...", file=sys.stderr)
  tool_names = fetch_tool_names(args.token)
  if tool_names:
    print(f"  {len(tool_names)} tools found: {', '.join(tool_names)}", file=sys.stderr)
  else:
    print("  No tools found — check GITHUB_TOKEN and network access.", file=sys.stderr)

  # ── Fetch issues ─────────────────────────────────────────────────────────
  print(f"Fetching {args.state} issues from {REPO} ...", file=sys.stderr)
  try:
    issues = fetch_all_issues(args.token, state=args.state)
  except urllib.error.HTTPError as exc:
    print(f"GitHub API error: HTTP {exc.code} {exc.reason}", file=sys.stderr)
    sys.exit(1)
  print(f"  {len(issues)} issues loaded.", file=sys.stderr)

  # ── Assignment statistics ─────────────────────────────────────────────────
  tool_keys = set(tool_names)
  commandlet_keys = set(COMMANDLETS.keys())
  _, _, unassigned = classify_issues(issues, tool_keys, commandlet_keys)
  matched_count = len(issues) - len(unassigned)
  pct = f" ({matched_count / len(issues) * 100:.1f}% of total)" if issues else ""

  print(f"  {matched_count} issues assigned{pct}", file=sys.stderr)
  print(f"  {len(unassigned)} issues unassigned", file=sys.stderr)

  # ── Unmatched label report ────────────────────────────────────────────────
  all_known = tool_keys | commandlet_keys | set(LABEL_ALIASES)
  all_issue_labels = {
    lbl["name"] for issue in issues for lbl in issue.get("labels", [])
  }
  unmatched_labels = all_issue_labels - all_known - _SKIP_LABELS
  if unmatched_labels:
    print(
      f"  Unmatched labels (consider adding to TOOLS/COMMANDLETS/labels): "
      f"{', '.join(sorted(unmatched_labels))}",
      file=sys.stderr,
    )

  # ── Write output ─────────────────────────────────────────────────────────
  adoc = generate_adoc(issues, tool_names)
  with open(args.output, "w", encoding="utf-8") as fh:
    fh.write(adoc)
  print(f"Written to {args.output}", file=sys.stderr)


if __name__ == "__main__":
  main()
