from __future__ import annotations

from pathlib import Path

from .charts import CHART_DIRECTORY, write_charts
from .config import OUTPUT_FILES, OS_GROUPS, VISUALIZATIONS
from .documents import DocumentBundle, OsDocument, OverviewDocument
from .models import AsciiDocTable

HEADER = """= {title}
:toc: left
:toclevels: 3
:icons: font
:source-highlighter: rouge
:nofooter:
"""


def render_visualization(section: str, image_file: str, alt_text: str, table: AsciiDocTable) -> str:
    settings = VISUALIZATIONS[section]
    if "chart" not in settings:
        raise ValueError(
            f'Invalid visualization config for "{section}": expected '
            f'{{"chart": "bar|pie|none", "show_table": True|False}}.'
        )
    chart_type = str(settings["chart"])
    show_chart = chart_type != "none"
    show_table = bool(settings["show_table"])

    if not show_chart and not show_table:
        return ""
    if not show_chart:
        return table.render()
    image = f'image::{CHART_DIRECTORY}/{image_file}[{chart_type.title()} chart: {alt_text},width=100%]'
    if not show_table:
        return image

    return '\n'.join([
        '[cols="3,2", frame=none, grid=none]',
        '|===',
        'a|',
        image,
        '',
        'a|',
        table.render(delimiter='!'),
        '|===',
    ])


def render_overview(document: OverviewDocument) -> str:
    lines = [
        HEADER.format(title='IDEasy Quality Status').strip(),
        '',
        '== Overview',
        '',
        'Automatically generated open issue overview for',
        f'https://github.com/{document.owner}/{document.repo}[{document.owner}/{document.repo}].',
        '',
        '',
        '### Issue Statistics',
        render_visualization(
            'issue_statistics',
            'issue-statistics.svg',
            'open issue metrics',
            document.issue_stats_table,
        ),
        '',
        '',
        f'_Generated: {document.generated_at}_',
        '',
        '',
        '== Operating System Status Files',
        '',
        'Issues are assigned to operating systems based on their labels:',
        '`windows`, `linux`, or `macOS`.',
        '',
        'Issues without an operating system label are treated as cross-platform.',
        f'*A total of {len(document.groups.cross_platform)} cross-platform issues* are documented centrally in this document and are therefore not repeated in the operating system specific files.',
        '',
        'The detailed tool status is split into one generated file per operating system.',
        '',
        render_visualization(
            'operating_systems',
            'operating-systems.svg',
            'issues by operating system',
            document.os_summary_table,
        ),
        '',
        '== Quality Insights',
        '',
        '=== Issue Age Distribution',
        render_visualization(
            'issue_age',
            'issue-age.svg',
            'issue age distribution',
            document.age_distribution_table,
        ),
        '',
        '=== Most common functional labels',
        '',
        'Top GitHub labels based on number of issues. This statistic excludes generic labels such as bug/task/enhancement, operating-system labels, and workflow or maintenance labels (for example documentation, dependencies, or help wanted).',
        '',
        render_visualization(
            'functional_labels',
            'functional-labels.svg',
            'most common functional labels',
            document.top_labels_table,
        ),
        '',
        '',
        '== Cross-platform Issues',
        '',
        document.cross_platform_table.render(),
        '',
        '== How to update this document',
        '',
        'This document is generated automatically from open GitHub issues in',
        f'https://github.com/{document.owner}/{document.repo}[{document.owner}/{document.repo}].',
    ]
    return '\n'.join(lines).rstrip() + '\n'


def render_os_document(document: OsDocument) -> str:
    lines = [
        HEADER.format(title=f'IDEasy Quality Status — {document.os_name}').strip(),
        '',
        f'Automatically generated open issue overview for {document.os_name}.',
        '',
        f'link:{OUTPUT_FILES["overview"]}[Back to overview]',
        '',
        f'*Total Issues:* {document.total_issues}',
        '',
        f'* OS-specific: {document.specific_count}',
        '',
        f'* Multi-OS: {document.multi_count}',
        '',
        f'* Cross-platform: {document.cross_platform_count}',
        '',
        f'== {document.os_name} Specific Issues',
        '',
        document.specific_table.render(),
        '',
        '== Multi-OS Issues',
        '',
    ]
    if not document.multi_tables:
        lines.extend(['No multi-OS issues.', ''])
    else:
        for title, table in document.multi_tables:
            issue_count = sum(len(issues) for _, _, issues in table.issues_by_bucket)
            lines.extend([
                f'=== {title} ({issue_count})',
                '',
                table.render(),
                '',
            ])
    lines.extend([
        '== Cross-platform Issues',
        '',
        f'Cross-platform issues are listed in the overview document: link:{OUTPUT_FILES["overview"]}[Cross-platform Issues].',
    ])
    return '\n'.join(lines).rstrip() + '\n'


def write_documents(output_dir: str | Path, bundle: DocumentBundle) -> None:
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)
    (output_path / OUTPUT_FILES['overview']).write_text(render_overview(bundle.overview), encoding='utf-8')
    write_charts(output_path, bundle.overview)
    for group in OS_GROUPS:
        document = bundle.os_documents[group.name]
        (output_path / group.output_file).write_text(render_os_document(document), encoding='utf-8')
