"""Document assembly service for the quality status generator."""

from datetime import datetime, timezone

from quality_status_generator.classify import (
  _all_issue_refs,
  analyze_os_issues,
  classify_issues,
)
from quality_status_generator.config import COMMANDLETS, OS_ORDER, REPO, TOOLS, TOOL_FOLDER_PATH
from quality_status_generator.render import (
  HEADER_TEMPLATE,
  issue_table,
  os_document,
  os_file_links_section,
  render_stats,
)
from quality_status_generator.stats import compute_stats, severity_summary


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

  all_refs = _all_issue_refs(tool_data, cmd_data, unassigned)

  blocker_count = sum(1 for ref in all_refs if ref.blocker)
  bug_count = sum(1 for ref in all_refs if ref.bug and not ref.blocker)
  enhancement_count = sum(1 for ref in all_refs if not ref.bug and not ref.blocker)

  header = HEADER_TEMPLATE.format(
    repo=REPO,
    tool_path=TOOL_FOLDER_PATH,
    date=datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC"),
    assigned=len(all_refs),
    n_blockers=blocker_count,
    n_bugs=bug_count,
    n_enhs=enhancement_count,
    unassigned=len(unassigned),
    total=len(all_refs) + len(unassigned),
  )

  cross_platform_refs = [
    ref for ref in all_refs
    if set(ref.os_keys) == set(OS_ORDER)
  ]

  how_to_update = "\n".join([
    "== How to update this document",
    "",
    "This document is generated automatically from open GitHub issues in",
    f"https://github.com/{REPO}[{REPO}].",
    "",
  ])

  overview_parts = [
    header,
    os_file_links_section(os_filenames, os_analysis),
    render_stats(stats),
    f"*Summary:* {severity_summary(cross_platform_refs)}",
    "",
    issue_table("Cross-platform Issues", cross_platform_refs, include_os=True),
    how_to_update,
  ]

  documents = {
    "overview": "\n".join(overview_parts),
  }

  for os_key in sorted(OS_ORDER, key=lambda current_os: os_analysis[current_os][0], reverse=True):
    documents[os_key] = os_document(
      os_key,
      tool_data,
      cmd_data,
      unassigned,
      _all_issue_refs,
    )

  return documents
