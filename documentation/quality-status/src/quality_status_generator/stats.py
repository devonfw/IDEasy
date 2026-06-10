"""Statistics and summaries for the quality status generator."""

from collections import defaultdict

from quality_status_generator.classify import _all_issue_refs, age_bucket
from quality_status_generator.config import AGE_BUCKETS
from quality_status_generator.models import IssueRef


def severity_summary(refs: list[IssueRef]) -> str:
  """Return a short severity summary string for a list of issues."""
  blockers = sum(1 for ref in refs if ref.blocker)
  bugs = sum(1 for ref in refs if ref.bug and not ref.blocker)
  enhancements = sum(1 for ref in refs if not ref.bug and not ref.blocker)
  return f"{blockers} blocker(s), {bugs} bug(s), {enhancements} enhancement(s)"

def compute_stats(
    tool_data: dict,
    cmd_data: dict,
    unassigned: list[dict],
) -> dict:
  """Compute quality statistics (age distribution + top areas)."""

  all_refs = _all_issue_refs(tool_data, cmd_data, unassigned)

  age_distribution = {label: 0 for label, _ in AGE_BUCKETS}
  for ref in all_refs:
    age_distribution[age_bucket(ref)] += 1

  area_counts: dict[str, set[int]] = defaultdict(set)

  for os_bucket in tool_data.values():
    for key, refs in os_bucket.items():
      for ref in refs:
        area_counts[key].add(ref.number)

  for os_bucket in cmd_data.values():
    for key, refs in os_bucket.items():
      for ref in refs:
        area_counts[key].add(ref.number)

  top_areas = sorted(
    ((key, len(issue_numbers)) for key, issue_numbers in area_counts.items()),
    key=lambda x: x[1],
    reverse=True,
  )[:10]

  return {
    "age_distribution": age_distribution,
    "top_areas": top_areas,
  }
