from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone

from .classification import group_issues
from .config import ASSIGNMENT_STAT_DEFINITIONS, TYPE_STAT_DEFINITIONS, OS_GROUPS
from .models import Issue, IssueGroups, KeyValueTable, IssueTable, TableRow
from .statistics import age_distribution, bucket_issues_by_age, issue_statistics, os_summary, top_functional_labels

@dataclass
class OverviewDocument:
    owner: str
    repo: str
    generated_at: str
    assignment_stats_table: KeyValueTable
    type_stats_table: KeyValueTable
    os_summary_table: KeyValueTable
    age_distribution_table: KeyValueTable
    top_labels_table: KeyValueTable
    cross_platform_table: IssueTable
    groups: IssueGroups
    assignment_stats: list[tuple[str, int]]
    type_stats: list[tuple[str, int]]
    os_stats: list[tuple[str, int, int, int]]
    age_stats: list[tuple[str, int]]
    top_label_stats: list[tuple[str, int]]


@dataclass
class OsDocument:
    os_name: str
    total_issues: int
    specific_count: int
    multi_count: int
    cross_platform_count: int
    specific_table: IssueTable
    multi_tables: list[tuple[str, IssueTable]]


@dataclass
class DocumentBundle:
    overview: OverviewDocument
    os_documents: dict[str, OsDocument]


def build_documents(owner: str, repo: str, issues: list[Issue]) -> DocumentBundle:
    groups = group_issues(issues, OS_GROUPS)
    generated_datetime = datetime.now(timezone.utc)
    generated_at = generated_datetime.strftime('%Y-%m-%d %H:%M UTC')
    reference_date = generated_datetime.date()

    stats = issue_statistics(issues)

    assignment_rows = [
        TableRow((label, str(stats.get(key, 0))))
        for key, label in ASSIGNMENT_STAT_DEFINITIONS
    ]
    assignment_stats_table = KeyValueTable(
        headers=("Assignment", "Total"),
        rows=assignment_rows,
        cols="2,^1",
    )

    type_rows = [
        TableRow((label, str(stats.get(key, 0))))
        for key, label in TYPE_STAT_DEFINITIONS
    ]
    type_stats_table = KeyValueTable(
        headers=("Issue Type", "Total"),
        rows=type_rows,
        cols="2,^1",
    )

    os_summary_rows = os_summary(groups, OS_GROUPS)
    os_rows = [
        TableRow((row['name'], str(row['total']), str(row['specific']), str(row['multi']), f"link:{row['file']}[{row['file']}]") )
        for row in os_summary_rows
    ]
    os_summary_table = KeyValueTable(
        headers=('Operating System', 'Total', 'Specific', 'Multi', 'Status File'),
        rows=os_rows,
        cols='2,1,1,1,4',
    )

    age_distribution_rows = age_distribution(issues, reference_date)
    age_rows = [
        TableRow((title, str(count)))
        for bucket_key, title, count in age_distribution_rows
    ]
    age_distribution_table = KeyValueTable(headers=('Age Range', 'Count'), rows=age_rows, cols='2,^1')

    top_label_stats = top_functional_labels(issues)
    top_label_rows = [TableRow((label, str(count))) for label, count in top_label_stats]
    top_labels_table = KeyValueTable(headers=('Label', 'Issues'), rows=top_label_rows, cols='3,^1')

    cross_platform_table = IssueTable(
        issues_by_bucket=bucket_issues_by_age(
            sorted(groups.cross_platform, key=lambda issue: issue.created_at, reverse=True),
            reference_date,
        ),
        anchor_prefix='cross-platform',
    )

    overview = OverviewDocument(
        owner=owner,
        repo=repo,
        generated_at=generated_at,
        assignment_stats_table=assignment_stats_table,
        type_stats_table=type_stats_table,
        os_summary_table=os_summary_table,
        age_distribution_table=age_distribution_table,
        top_labels_table=top_labels_table,
        cross_platform_table=cross_platform_table,
        groups=groups,
        assignment_stats=[
            (label, stats.get(key, 0))
            for key, label in ASSIGNMENT_STAT_DEFINITIONS
        ],
        type_stats=[
            (label, stats.get(key, 0))
            for key, label in TYPE_STAT_DEFINITIONS
        ],
        os_stats=[
            (str(row["name"]), int(row["specific"]), int(row["multi"]), len(groups.cross_platform))
            for row in os_summary_rows
        ],
        age_stats=[(title, count) for _, title, count in age_distribution_rows],
        top_label_stats=top_label_stats,
    )

    os_documents: dict[str, OsDocument] = {}
    for group in OS_GROUPS:
        specific = sorted(groups.os_specific[group.name], key=lambda issue: issue.created_at, reverse=True)
        relevant_multi = {
            key: sorted(values, key=lambda issue: issue.created_at, reverse=True)
            for key, values in groups.multi_os.items()
            if group.name in key.split(' + ')
        }
        specific_table = IssueTable(issues_by_bucket=bucket_issues_by_age(specific, reference_date))
        multi_tables = [
            (key, IssueTable(issues_by_bucket=bucket_issues_by_age(values, reference_date)))
            for key, values in sorted(relevant_multi.items())
        ]
        cross_platform_count = len(groups.cross_platform)
        multi_count = sum(len(values) for values in relevant_multi.values())
        os_documents[group.name] = OsDocument(
            os_name=group.name,
            total_issues=cross_platform_count + len(specific) + multi_count,
            specific_count=len(specific),
            multi_count=multi_count,
            cross_platform_count=cross_platform_count,
            specific_table=specific_table,
            multi_tables=multi_tables,
        )

    return DocumentBundle(overview=overview, os_documents=os_documents)
