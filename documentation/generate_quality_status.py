#!/usr/bin/env python3
"""
generate_quality_status.py

Generates quality-status.adoc for devonfw/IDEasy by:
  1. Fetching tool names dynamically from the url-updater source tree
     (url-updater/src/main/java/com/devonfw/tools/ide/url/tool/).
  2. Fetching all open GitHub issues and mapping them to tools or commandlets
     via labels (including aliases for non-standard label names).
  3. Writing a compact quality-status.adoc:
       - Overview with legend and issue stats
       - Summary: active blockers only
       - Tools: one cross-OS table (Win / Linux / macOS columns), non-green rows only
       - Commandlets: same
       - Unassigned Issues

OS LABEL BEHAVIOUR
──────────────────
Issues with a specific OS label appear only in that OS column.
Issues with NO OS label are cross-platform: they count toward every OS column
and are shown without an OS tag in the Issues cell.

ISSUE TYPE ICONS IN ISSUES CELL
────────────────────────────────
  🚨 #N  — blocker
  🔴 #N  — bug (or legacy "bug" label)
  🟡 #N  — enhancement / feature / task (no bug, no blocker)

OS tags are appended in backticks only for OS-specific issues, e.g.:
  🔴 link:…[#42] `win`   Title snippet
  🟡 link:…[#99]         Title snippet   (cross-platform: no tag)

Usage:
    python generate_quality_status.py [--token TOKEN] [--output PATH]
    export GITHUB_TOKEN=ghp_yourtoken && python generate_quality_status.py
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
TOOL_FOLDER_PATH = "cli/src/main/java/com/devonfw/tools/ide/tool"

OS_LABELS = {"windows": "windows", "linux": "linux", "mac": "macOS"}
OS_ORDER = ["windows", "linux", "mac"]
OS_DISPLAY = {"windows": "Windows", "linux": "Linux", "mac": "macOS"}
OS_SHORT = {"windows": "win", "linux": "linux", "mac": "mac"}

BUG_TYPE_NAMES = {"Bug", "bug"}
BLOCKER_LABEL = "blocker"

# ── Tool registry ──────────────────────────────────────────────────────────────
# Key      → canonical folder name (= default label)
# display  → human-readable name
# labels   → extra GitHub label names that also map to this tool
TOOLS: dict[str, dict] = {
  # ── IDEs & editors ────────────────────────────────────────────────────────
  "androidstudio": {"display": "Android Studio", "labels": ["android-studio"]},
  "eclipse": {"display": "Eclipse"},
  "intellij": {"display": "IntelliJ IDEA", "labels": ["intellij-idea"]},
  "pycharm": {"display": "PyCharm"},
  "vscode": {"display": "VS Code", "labels": ["vsc"]},
  # ── JVM / build tools ─────────────────────────────────────────────────────
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
  # ── Python ────────────────────────────────────────────────────────────────
  "python": {"display": "Python"},
  "pip": {"display": "pip"},
  "uv": {"display": "uv (Python package mgr)"},
  # ── JavaScript / Node.js ─────────────────────────────────────────────────
  "node": {"display": "Node.js", "labels": ["nodejs"]},
  "npm": {"display": "npm"},
  "ng": {"display": "Angular CLI"},
  "yarn": {"display": "Yarn"},
  "corepack": {"display": "Corepack"},
  # ── Go ────────────────────────────────────────────────────────────────────
  "go": {"display": "Go"},
  # ── Container / cloud / DevOps ────────────────────────────────────────────
  "docker": {"display": "Docker", "labels": ["rancher", "docker"]},
  "lazydocker": {"display": "Lazydocker"},
  "kubectl": {"display": "kubectl"},
  "oc": {"display": "OpenShift CLI"},
  "helm": {"display": "Helm"},
  "terraform": {"display": "Terraform"},
  "aws": {"display": "AWS CLI"},
  "az": {"display": "Azure CLI", "labels": ["azure-cli"]},
  "dotnet": {"display": "dotnet"},
  # ── Developer tools ───────────────────────────────────────────────────────
  "gh": {"display": "GitHub CLI"},
  "copilot": {"display": "GitHub Copilot", "labels": ["github-copilot"]},
  "sonar": {"display": "SonarQube", "labels": ["sonarqube"]},
  # ── Database & data ───────────────────────────────────────────────────────
  "pgadmin": {"display": "pgAdmin"},
  "squirrelsql": {"display": "SQuirreL SQL"},
  # ── IDEasy tool mechanisms ────────────────────────────────────────────────
  "custom": {"display": "Custom tool support"},
  "extra": {"display": "Extra tools"},
  "gui": {"display": "GUI / IDE launcher", "labels": ["GUI"]},
  # ── Manually maintained (no CLI folder but label exists on issues) ─────────
  "git": {"display": "git"},
}

# ── Commandlet / core-feature registry ────────────────────────────────────────
COMMANDLETS: dict[str, dict] = {
  # ── Core IDEasy functionality ──────────────────────────────────────────────
  "ide": {"display": "IDEasy (general)", "labels": ["CLI", "commandlet", "integration"]},
  "core": {"display": "Core / Runtime", "labels": ["progressbar"]},
  "install": {"display": "ide install"},
  "uninstall": {"display": "ide uninstall"},
  "update": {"display": "ide update"},
  "create-project": {"display": "ide create-project", "labels": ["create"]},
  "build": {"display": "ide build"},
  "status": {"display": "ide status"},
  "version": {"display": "Version management"},
  # ── Configuration & settings ───────────────────────────────────────────────
  "settings": {"display": "Settings / Properties", "labels": ["configuration", "json", "upgrade-settings"]},
  "icd": {"display": "IDE Configuration Doc"},
  "merger": {"display": "Settings merger"},
  "env": {"display": "Environment variables"},
  # ── Download & installation pipeline ──────────────────────────────────────
  "download": {"display": "Download & Extraction", "labels": ["unpack", "urls"]},
  # ── Shell & terminal ───────────────────────────────────────────────────────
  "shell": {"display": "Shell integration", "labels": ["PowerShell"]},
  "completion": {"display": "Shell completion"},
  # ── Infrastructure & networking ────────────────────────────────────────────
  "proxy": {"display": "Proxy / Network"},
  "security": {"display": "Security / Credentials"},
  # ── Repository & workspace ─────────────────────────────────────────────────
  "repository": {"display": "Repository management", "labels": ["SCM"]},
  "workspace": {"display": "Workspace management"},
  # ── Plugin management (also a CLI folder) ─────────────────────────────────
  "plugin": {"display": "Plugin management", "labels": ["plugins"]},
  # ── Observability ─────────────────────────────────────────────────────────
  "logging": {"display": "Logging / Debug output"},
  # ── Migration ─────────────────────────────────────────────────────────────
  "migration": {"display": "Migration (devonfw-ide → IDEasy)"},
}

# ── Derived lookups (do not edit) ─────────────────────────────────────────────
TOOL_DISPLAY_OVERRIDES: dict[str, str] = {
  n: c["display"] for n, c in TOOLS.items()
}
COMMANDLET_DISPLAY_OVERRIDES: dict[str, str] = {
  n: c["display"] for n, c in COMMANDLETS.items()
}
LABEL_ALIASES: dict[str, str] = {
  alias: name
  for registry in (TOOLS, COMMANDLETS)
  for name, cfg in registry.items()
  for alias in cfg.get("labels", [])
}
_SKIP_LABELS: frozenset[str] = frozenset({
  # issue types
  BLOCKER_LABEL, "bug", "bugfix", "enhancement", "feature", "task",
  # workflow / triage status
  "Epic", "ready-to-implement", "waiting for feedback", "release",
  # meta / project management
  "AI", "ARM", "claude", "internal", "process", "rewrite", "software", "workflow",
  # testing
  "test", "integration-tests", "testing",
  # other infra
  "question", "duplicate", "wontfix", "invalid", "help wanted",
  "good first issue", "documentation", "dependencies", "refactoring",
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
  try:
    entries = _get(f"/repos/{REPO}/contents/{TOOL_FOLDER_PATH}", token)
    return sorted(e["name"] for e in entries if isinstance(e, dict) and e.get("type") == "dir")
  except urllib.error.HTTPError as exc:
    hint = "" if token else " Set GITHUB_TOKEN to avoid rate limiting."
    print(f"WARNING: Could not fetch tool list ({exc.code} {exc.reason}). "
          f"Only commandlet sections will be generated.{hint}", file=sys.stderr)
    return []
  except Exception as exc:
    print(f"WARNING: Could not fetch tool list ({exc}).", file=sys.stderr)
    return []


def fetch_all_issues(token: str | None, state: str = "open") -> list[dict]:
  issues, page = [], 1
  while True:
    batch = _get(f"/repos/{REPO}/issues", token,
                 {"state": state, "per_page": 100, "page": page})
    if not isinstance(batch, list) or not batch:
      break
    issues.extend(i for i in batch if "pull_request" not in i)
    if len(batch) < 100:
      break
    page += 1
  return issues


# ─── Issue classification ─────────────────────────────────────────────────────

def label_names(issue: dict) -> set[str]:
  return {lbl["name"] for lbl in issue.get("labels", [])}


def is_bug(issue: dict, labels: set[str]) -> bool:
  t = issue.get("type")
  if isinstance(t, dict) and t.get("name") in BUG_TYPE_NAMES:
    return True
  return "bug" in labels or BLOCKER_LABEL in labels


def is_blocker(labels: set[str]) -> bool:
  return BLOCKER_LABEL in labels


def os_keys_for_issue(labels: set[str]) -> list[str]:
  """OS-specific → that OS only. No OS label → all platforms."""
  found = [k for k, lbl in OS_LABELS.items() if lbl in labels]
  return found if found else list(OS_ORDER)


def topic_matches(labels: set[str], known: set[str]) -> list[str]:
  matched: set[str] = set()
  for lbl in labels:
    if lbl in known:
      matched.add(lbl)
    elif lbl in LABEL_ALIASES and LABEL_ALIASES[lbl] in known:
      matched.add(LABEL_ALIASES[lbl])
  return sorted(matched)


# ─── Data model ───────────────────────────────────────────────────────────────

# IssueRef: (number, title, url, is_bug, is_blocker, os_keys)
# os_keys is the list of OS keys this issue applies to (for display tagging)
IssueRef = tuple[int, str, str, bool, bool, list[str]]


def classify_issues(
    issues: list[dict],
    tool_keys: set[str],
    commandlet_keys: set[str],
) -> tuple[
  dict[str, dict[str, list[IssueRef]]],  # tool_data[os_key][topic]
  dict[str, dict[str, list[IssueRef]]],  # cmd_data[os_key][topic]
  list[dict],  # unassigned
]:
  tool_data: dict = defaultdict(lambda: defaultdict(list))
  cmd_data: dict = defaultdict(lambda: defaultdict(list))
  unassigned: list[dict] = []

  for issue in issues:
    labels = label_names(issue)
    bug = is_bug(issue, labels)
    blocker = is_blocker(labels)
    os_keys = os_keys_for_issue(labels)
    ref: IssueRef = (issue["number"], issue["title"], issue["html_url"], bug, blocker, os_keys)

    t_matches = topic_matches(labels, tool_keys)
    c_matches = topic_matches(labels, commandlet_keys)

    if not t_matches and not c_matches:
      unassigned.append(issue)
      continue

    for os_key in os_keys:
      for t in t_matches:
        tool_data[os_key][t].append(ref)
      for c in c_matches:
        cmd_data[os_key][c].append(ref)

  return dict(tool_data), dict(cmd_data), unassigned


# ─── Rendering helpers ────────────────────────────────────────────────────────

def _safe(text: str, max_len: int = 70) -> str:
  s = text.replace("|", "-").replace("\n", " ")
  return s[:max_len] + ("…" if len(s) > max_len else "")


def issue_icon(bug: bool, blocker: bool) -> str:
  if blocker: return "🚨"
  if bug:     return "🔴"
  return "🟡"


def status_symbol(refs: list[IssueRef]) -> str:
  if not refs:                              return "🟢"
  if any(r[4] for r in refs):              return "🚨"
  if any(r[3] for r in refs):              return "🔴"
  return "🟡"


def fmt_issues_cell(lbl: str, data: dict[str, dict[str, list[IssueRef]]]) -> str:
  """
  Collect all issues for `lbl` across all OS, deduplicate, and format as:
    icon link:#N[#N] `os-tag`  Title snippet
  Issues that apply to all OS get no tag. OS-specific ones get a short tag.
  Issues are sorted: blockers first, then bugs, then enhancements; by number within each group.
  """
  seen: dict[int, IssueRef] = {}
  for os_key in OS_ORDER:
    for ref in data.get(os_key, {}).get(lbl, []):
      if ref[0] not in seen:
        seen[ref[0]] = ref

  if not seen:
    return "-"

  def sort_key(r: IssueRef):
    return (0 if r[4] else 1 if r[3] else 2, r[0])

  parts = []
  all_os = set(OS_ORDER)
  for ref in sorted(seen.values(), key=sort_key):
    num, title, url, bug, blocker, ref_os_keys = ref
    icon = issue_icon(bug, blocker)
    tag = "" if set(ref_os_keys) == all_os else " `" + "/".join(OS_SHORT[k] for k in ref_os_keys) + "`"
    parts.append(f"{icon} link:{url}[#{num}]{tag} {_safe(title)}")

  return " +\n".join(parts)


# ─── AsciiDoc section builders ────────────────────────────────────────────────

def _cross_os_table(
    heading: str,
    col_header: str,
    rows: list[tuple[str, str]],
    data: dict[str, dict[str, list[IssueRef]]],
    level: int = 2,
) -> list[str]:
  """
  Single table with columns: Name | Win | Linux | macOS | Issues.
  Only rows with at least one non-green OS cell are emitted.
  """
  h = "=" * level
  lines = [
    f"{h} {heading}\n",
    '[cols="2,1,1,1,6", options="header"]',
    "|===",
    f"| {col_header} | Win | Linux | macOS | Issues",
  ]
  any_row = False
  for lbl, disp in rows:
    cells = [status_symbol(data.get(ok, {}).get(lbl, [])) for ok in OS_ORDER]
    if all(c == "🟢" for c in cells):
      continue
    any_row = True
    lines += [
      f"| {disp}",
      *[f"| {c}" for c in cells],
      f"| {fmt_issues_cell(lbl, data)}",
      "",
    ]
  if not any_row:
    lines.append("| _No open issues_ | | | |\n")
  lines.append("|===\n")
  return lines


def _summary_section(
    tool_data: dict,
    cmd_data: dict,
    tool_rows: list[tuple[str, str]],
    cmd_rows: list[tuple[str, str]],
) -> str:
  """Active blockers table (deduplicated across OS) + compact status legend."""
  lines = ["== Summary\n"]

  # Collect unique blockers
  seen_ids: set[int] = set()
  blockers: list[tuple[str, str, IssueRef]] = []
  for registry_rows, data, cat in ((tool_rows, tool_data, "Tool"),
                                   (cmd_rows, cmd_data, "Commandlet")):
    for lbl, disp in registry_rows:
      for os_key in OS_ORDER:
        for ref in data.get(os_key, {}).get(lbl, []):
          if ref[4] and ref[0] not in seen_ids:
            seen_ids.add(ref[0])
            blockers.append((cat, disp, ref))

  lines.append("=== 🚨 Active Blockers\n")
  if blockers:
    lines += [
      '[cols="1,2,1,5", options="header"]',
      "|===",
      "| Type | Tool / Commandlet | Issue | Title",
    ]
    for cat, disp, (num, title, url, *_rest) in sorted(blockers, key=lambda r: r[2][0]):
      lines += [f"| {cat}", f"| {disp}", f"| link:{url}[#{num}]", f"| {_safe(title)}", ""]
    lines.append("|===\n")
  else:
    lines.append("_No active blockers_ 🎉\n")

  return "\n".join(lines)


def _unassigned_section(unassigned: list[dict]) -> str:
  lines = [
    "== Unassigned Issues\n",
    f"{len(unassigned)} issue(s) matched no known tool or commandlet. "
    "Add the appropriate label on GitHub or extend `TOOLS`/`COMMANDLETS` "
    "in this script.\n",
  ]
  if not unassigned:
    lines.append("_All issues are assigned._ 🎉\n")
    return "\n".join(lines)
  lines += ['[cols="1,4,3", options="header"]', "|===", "| # | Title | Labels"]
  for issue in sorted(unassigned, key=lambda i: i["number"]):
    lbls = ", ".join(l["name"] for l in issue.get("labels", []))
    lines += [
      f"| link:{issue['html_url']}[#{issue['number']}]",
      f"| {_safe(issue['title'])}",
      f"| {lbls or '—'}",
      "",
    ]
  lines.append("|===\n")
  return "\n".join(lines)


# ─── Header & document assembly ───────────────────────────────────────────────

_HEADER = """\
= Quality Status
:toc: left
:toclevels: 2
:icons: font
:source-highlighter: rouge

== Overview

Automatically generated quality and support status for https://github.com/{repo}[{repo}].
Tool list is discovered dynamically from the source tree.

*OS behaviour:* Issues labelled with a specific OS appear only in that OS column.
Issues with *no OS label* are cross-platform and count toward every OS column (no tag shown).

[cols="1,1,1,1,1", options="header"]
|===
| 🟢 | 🟡 | 🔴 | 🚨 | Issue icon
| No issues | Enhancements only | Bug(s) | Blocker | 🟡 enhancement · 🔴 bug · 🚨 blocker
|===

_Generated: {date} · {total} issues · {matched} assigned · {unassigned} unassigned_

"""


def generate_adoc(issues: list[dict], tool_names: list[str]) -> str:
  # Merge API-fetched names with statically registered TOOLS so that tools
  # present in the registry but not (yet) in the url-updater source tree
  # (e.g. git, kubectl) are still recognised and shown.
  all_tool_names = sorted(set(tool_names) | set(TOOLS.keys()))
  tool_rows: list[tuple[str, str]] = [
    (n, TOOL_DISPLAY_OVERRIDES.get(n, n.replace("-", " ").title()))
    for n in all_tool_names
  ]
  cmd_rows: list[tuple[str, str]] = [
    (n, COMMANDLET_DISPLAY_OVERRIDES[n]) for n in COMMANDLETS
  ]

  tool_keys = {r[0] for r in tool_rows}  # merged fetched + static
  commandlet_keys = {r[0] for r in cmd_rows}
  tool_data, cmd_data, unassigned = classify_issues(issues, tool_keys, commandlet_keys)

  date = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
  header = _HEADER.format(
    repo=REPO, date=date,
    total=len(issues), matched=len(issues) - len(unassigned), unassigned=len(unassigned),
  )

  parts = [header]
  parts.append(_summary_section(tool_data, cmd_data, tool_rows, cmd_rows))
  parts += _cross_os_table("Tools", "Tool", tool_rows, tool_data, level=2)
  parts += _cross_os_table("Commandlets & Core Features", "Commandlet / Feature",
                           cmd_rows, cmd_data, level=2)
  parts.append(_unassigned_section(unassigned))
  parts.append("""\
== How to Contribute

*New tool:* Automatic once a folder exists under `url-updater/.../url/tool/` and issues use that name as label.
Override the display name in `TOOLS["display"]`.

*New commandlet:* Add an entry to `COMMANDLETS` — key = GitHub label, `"display"` = name, `"labels"` = aliases.

*Non-standard label:* Add it to the `"labels"` list of the matching `TOOLS` or `COMMANDLETS` entry.
Unmatched issues appear in the *Unassigned Issues* section.
""")

  return "\n".join(parts)


# ─── Main ─────────────────────────────────────────────────────────────────────

def main() -> None:
  parser = argparse.ArgumentParser(description=f"Generate quality-status.adoc for {REPO}")
  parser.add_argument("--token", default=os.environ.get("GITHUB_TOKEN"))
  parser.add_argument("--output", default="quality-status.adoc")
  parser.add_argument("--state", default="open", choices=["open", "all"])
  args = parser.parse_args()

  if not args.token:
    print("WARNING: No GITHUB_TOKEN — rate-limited to 60 req/hour.", file=sys.stderr)

  print("Fetching tool list ...", file=sys.stderr)
  tool_names = fetch_tool_names(args.token)
  print(f"  {len(tool_names)} tools found." if tool_names
        else "  No tools found — check token and network.", file=sys.stderr)

  print(f"Fetching {args.state} issues from {REPO} ...", file=sys.stderr)
  try:
    issues = fetch_all_issues(args.token, state=args.state)
  except urllib.error.HTTPError as exc:
    print(f"GitHub API error: HTTP {exc.code} {exc.reason}", file=sys.stderr)
    sys.exit(1)
  print(f"  {len(issues)} issues loaded.", file=sys.stderr)

  # Merge fetched names with statically registered TOOLS (same logic as generate_adoc)
  tool_keys = set(tool_names) | set(TOOLS.keys())
  commandlet_keys = set(COMMANDLETS.keys())
  _, _, unassigned = classify_issues(issues, tool_keys, commandlet_keys)
  matched = len(issues) - len(unassigned)
  pct = f" ({matched / len(issues) * 100:.1f}%)" if issues else ""
  print(f"  {matched} assigned{pct}, {len(unassigned)} unassigned.", file=sys.stderr)

  all_known = tool_keys | commandlet_keys | set(LABEL_ALIASES)
  all_labels = {l["name"] for i in issues for l in i.get("labels", [])}
  unmatched = all_labels - all_known - _SKIP_LABELS
  if unmatched:
    print(f"  Unmatched labels: {', '.join(sorted(unmatched))}", file=sys.stderr)

  adoc = generate_adoc(issues, tool_names)
  with open(args.output, "w", encoding="utf-8") as fh:
    fh.write(adoc)
  print(f"Written to {args.output}", file=sys.stderr)


if __name__ == "__main__":
  main()
