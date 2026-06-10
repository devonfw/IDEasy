"""Issue classification and aggregation logic."""

from collections import defaultdict
from datetime import datetime, timezone
from typing import NamedTuple

from quality_status_generator.config import (
  BLOCKER_LABEL,
  BUG_TYPE_NAMES,
  LABEL_ALIASES,
  OS_LABELS,
  OS_ORDER,
  AGE_BUCKETS
)

from quality_status_generator.models import IssueRef


def label_names(issue: dict) -> set[str]:
  """Extract label names from a GitHub issue."""
  return {label["name"] for label in issue.get("labels", [])}


def is_bug(issue: dict, labels: set[str]) -> bool:
  """Check whether the issue counts as a bug."""
  issue_type = issue.get("type")

  if isinstance(issue_type, dict) and issue_type.get("name") in BUG_TYPE_NAMES:
    return True

  return "bug" in labels or BLOCKER_LABEL in labels


def is_blocker(labels: set[str]) -> bool:
  return BLOCKER_LABEL in labels


def os_keys_for_issue(labels: set[str]) -> list[str]:
  """Determine which OS keys apply. No OS label means cross-platform."""
  found = [key for key, label in OS_LABELS.items() if label in labels]
  return found if found else list(OS_ORDER)


def topic_matches(labels: set[str], known: set[str]) -> list[str]:
  """Return canonical topic keys matched by labels."""
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
  """Classify issues into tool data, commandlet data, or unassigned."""

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


def _all_issue_refs(
  tool_data: dict,
  cmd_data: dict,
  unassigned: list[dict],
  os_key: str | None = None,
) -> list[IssueRef]:
  """Collect and deduplicate issue references."""

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

  return sorted(seen.values(), key=severity_sort_key)


def analyze_os_issues(
  refs: list[IssueRef],
  os_key: str,
) -> tuple[int, int, int, int, dict[tuple[str, ...], list[IssueRef]]]:
  """Return counts + multi-OS grouping for a given OS."""

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

def age_bucket(ref: IssueRef) -> str:
  age_days = (datetime.now(timezone.utc) - ref.created_at).days

  for label, upper_bound in AGE_BUCKETS:
    if upper_bound is None or age_days <= upper_bound:
      return label

  raise ValueError(f"No age bucket matched for {age_days} days.")

def severity_sort_key(ref: IssueRef) -> tuple:
  age_days = (datetime.now(timezone.utc) - ref.created_at).days

  age_bucket_index = len(AGE_BUCKETS) - 1
  for index, (_, upper_bound) in enumerate(AGE_BUCKETS):
    if upper_bound is None or age_days <= upper_bound:
      age_bucket_index = index
      break

  severity_bucket = 0 if ref.blocker else 1 if ref.bug else 2

  return (
    age_bucket_index,
    severity_bucket,
    ref.created_at.timestamp(),
  )
