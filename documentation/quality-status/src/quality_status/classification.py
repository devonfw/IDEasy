from __future__ import annotations

from collections import defaultdict

from .config import ISSUE_TYPE_MAP, OS_GROUPS
from .models import Issue, IssueGroups, OsGroup


def classify_issue_type(issue: Issue) -> str:
    if not issue.issue_type:
        return "no_type"
    normalized = issue.issue_type.casefold().strip()
    return ISSUE_TYPE_MAP.get(normalized, "no_type")


def matching_os_groups(issue: Issue, os_groups: tuple[OsGroup, ...] = OS_GROUPS) -> list[str]:
    issue_labels = {label.casefold() for label in issue.labels}
    matches: list[str] = []
    for group in os_groups:
        if issue_labels & {label.casefold() for label in group.labels}:
            matches.append(group.name)
    return matches


def group_issues(issues: list[Issue], os_groups: tuple[OsGroup, ...] = OS_GROUPS) -> IssueGroups:
    grouped = IssueGroups(
        cross_platform=[],
        os_specific={group.name: [] for group in os_groups},
        multi_os=defaultdict(list),
    )

    for issue in issues:
        matches = matching_os_groups(issue, os_groups)
        if not matches:
            grouped.cross_platform.append(issue)
        elif len(matches) == 1:
            grouped.os_specific[matches[0]].append(issue)
        else:
            key = " + ".join(sorted(matches))
            grouped.multi_os[key].append(issue)

    return grouped
