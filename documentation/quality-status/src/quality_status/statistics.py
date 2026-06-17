from __future__ import annotations

from collections import Counter
from datetime import date

from .classification import classify_issue_type
from .config import AGE_BUCKETS, BLOCKER_LABELS, EXCLUDED_FUNCTIONAL_LABELS, ISSUE_STAT_DEFINITIONS, OS_GROUPS
from .models import AgeBucket, Issue, IssueGroups, OsGroup


def is_blocker(issue: Issue) -> bool:
    return bool({label.casefold() for label in issue.labels} & BLOCKER_LABELS)


def issue_statistics(issues: list[Issue]) -> dict[str, int]:
    stats = {key: 0 for key, _ in ISSUE_STAT_DEFINITIONS}
    stats['all'] = len(issues)

    for issue in issues:
        if issue.is_assigned:
            stats['assigned'] += 1
        else:
            stats['unassigned'] += 1

        if is_blocker(issue):
            stats['blockers'] += 1

        kind = classify_issue_type(issue)
        if kind == 'bug':
            stats['bugs'] += 1
        elif kind == 'feature':
            stats['features'] += 1
        elif kind == 'task':
            stats['tasks'] += 1
        elif kind == 'no_type':
            stats['no_type'] += 1

    return stats


def bucket_issues_by_age(
    issues: list[Issue],
    reference_date: date,
    age_buckets: tuple[AgeBucket, ...] = AGE_BUCKETS,
) -> list[tuple[str, str, list[Issue]]]:
    grouped: list[tuple[str, str, list[Issue]]] = []
    for bucket in age_buckets:
        bucket_issues = [issue for issue in issues if bucket.contains(issue.age_days_at(reference_date))]
        grouped.append((bucket.key, bucket.title, bucket_issues))
    return grouped


def age_distribution(
    issues: list[Issue],
    reference_date: date,
    age_buckets: tuple[AgeBucket, ...] = AGE_BUCKETS,
) -> list[tuple[str, str, int]]:
    rows: list[tuple[str, str, int]] = []
    for bucket in age_buckets:
        count = sum(1 for issue in issues if bucket.contains(issue.age_days_at(reference_date)))
        rows.append((bucket.key, bucket.title, count))
    return rows


def top_functional_labels(issues: list[Issue], limit: int = 10) -> list[tuple[str, int]]:
    counter: Counter[str] = Counter()
    for issue in issues:
        for label in issue.labels:
            if label.casefold() not in EXCLUDED_FUNCTIONAL_LABELS:
                counter[label] += 1
    return sorted(counter.items(), key=lambda item: (-item[1], item[0].casefold()))[:limit]


def os_summary(groups: IssueGroups, os_groups: tuple[OsGroup, ...] = OS_GROUPS) -> list[dict[str, int | str]]:
    rows: list[dict[str, int | str]] = []
    cross_count = len(groups.cross_platform)
    for group in os_groups:
        specific_count = len(groups.os_specific[group.name])
        multi_count = sum(len(issues) for key, issues in groups.multi_os.items() if group.name in key.split(' + '))
        rows.append({
            'name': group.name,
            'total': cross_count + specific_count + multi_count,
            'specific': specific_count,
            'multi': multi_count,
            'file': group.output_file,
        })
    return rows
