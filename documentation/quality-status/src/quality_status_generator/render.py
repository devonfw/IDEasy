"""AsciiDoc rendering for the quality status generator."""

from datetime import datetime, timezone
from pathlib import Path

from quality_status_generator.classify import analyze_os_issues, age_bucket
from quality_status_generator.config import (
  AGE_BUCKETS,
  COMMANDLETS,
  OS_DISPLAY,
  OS_FILE_SUFFIX,
  OS_ORDER,
  TOOLS,
)
from quality_status_generator.models import IssueRef
from quality_status_generator.stats import severity_summary


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
| Unassigned     | {unassigned}
| Blockers       | {n_blockers}
| Bugs           | {n_bugs}
| Enhancements   | {n_enhs}

|===


_Generated: {date}_

"""


def safe(text: str, max_len: int = 72) -> str:
  """Sanitise text for AsciiDoc table cells — escape pipes and truncate."""
  sanitised = text.replace("|", "-").replace("\n", " ")
  if len(sanitised) > max_len:
    return sanitised[:max_len] + " …"
  return sanitised

def _severity_group(ref: IssueRef) -> str:
  if ref.blocker:
    return "Blocker"
  if ref.bug:
    return "Bug"
  return "Enhancement"


def os_output_path(main_output: Path, os_key: str) -> Path:
  """Derive the per-OS output file path from the main overview output path."""
  suffix = OS_FILE_SUFFIX[os_key]
  return main_output.with_name(f"{main_output.stem}-{suffix}{main_output.suffix}")

def issue_table(
    title: str,
    refs: list[IssueRef],
    include_os: bool,
) -> str:
  """Render an issue table grouped by age bucket and severity.

  Note:
    `include_os` is currently kept for compatibility with existing call sites.
    Rendering is currently identical in both modes.
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

  col_span = 2
  lines += [
    '[%header, cols="^1,7"]',
    '|===',
    '| Issue | Summary',
  ]

  current_bucket = None
  current_severity = None

  for ref in refs:
    bucket = age_bucket(ref)
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
      f"| {safe(ref.title)}",
      "",
    ]

  lines += [
    '|===',
    "",
  ]

  return "\n".join(lines)


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

  age_distribution = stats["age_distribution"]

  for label, _ in AGE_BUCKETS:
    lines.append(f"| {label} | {age_distribution[label]}")

  lines += [
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


def os_file_links_section(
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
    total, specific, multi, _, _ = os_analysis[os_key]

    lines += [
      f"| {OS_DISPLAY[os_key]}",
      f"| {total}",
      f"| {specific}",
      f"| {multi}",
      f"| link:{filename}[{filename}]",
      "",
    ]

  lines += ["|===", ""]
  return "\n".join(lines)


def render_multi_os_groups(groups: dict[tuple[str, ...], list[IssueRef]], os_key: str) -> str:
  """Render grouped multi-OS issues."""
  lines = ["== Multi-OS Issues", ""]

  if not groups:
    lines += ["_No multi-OS issues._", ""]
    return "\n".join(lines)

  for combo, refs in sorted(groups.items(), key=lambda item: -len(item[1])):
    display = " + ".join(OS_DISPLAY[current_os] for current_os in combo)
    lines += [
      f"=== {display} ({len(refs)})",
      "",
      issue_table("", refs, include_os=False),
    ]

  return "\n".join(lines)


def os_document(
    os_key: str,
    tool_data: dict,
    cmd_data: dict,
    unassigned: list[dict],
    all_issue_refs_fn,
) -> str:
  """Render a standalone status document for one operating system."""
  os_name = OS_DISPLAY[os_key]
  refs = all_issue_refs_fn(tool_data, cmd_data, unassigned, os_key=os_key)

  os_specific = [ref for ref in refs if ref.os_keys == [os_key]]
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
    f"*Summary:* {severity_summary(os_specific)}",
    "",
    issue_table(f"{os_name} Specific Issues", os_specific, include_os=False),
    render_multi_os_groups(multi_os_groups, os_key),
    "== Cross-platform Issues",
    "",
    "Cross-platform issues are listed in the overview document: link:quality-status.adoc[Cross-platform Issues].",
    "",
  ])
