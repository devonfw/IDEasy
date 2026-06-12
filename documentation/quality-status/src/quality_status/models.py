from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from datetime import date, datetime


@dataclass(frozen=True)
class Issue:
    number: int
    title: str
    url: str
    labels: tuple[str, ...]
    issue_type: str | None
    created_at: datetime
    is_assigned: bool

    def age_days_at(self, reference_date: date) -> int:
        return max(0, (reference_date - self.created_at.date()).days)


@dataclass(frozen=True)
class AgeBucket:
    key: str
    title: str
    min_days: int
    max_days: int | None

    def contains(self, age_days: int) -> bool:
        if age_days < self.min_days:
            return False
        if self.max_days is None:
            return True
        return age_days <= self.max_days


@dataclass(frozen=True)
class OsGroup:
    name: str
    labels: tuple[str, ...]
    output_file: str


@dataclass
class IssueGroups:
    cross_platform: list[Issue] = field(default_factory=list)
    os_specific: dict[str, list[Issue]] = field(default_factory=dict)
    multi_os: dict[str, list[Issue]] = field(default_factory=dict)


@dataclass(frozen=True)
class TableRow:
    cells: tuple[str, ...]


class AsciiDocTable(ABC):
    @abstractmethod
    def render(self, delimiter: str = '|') -> str:
        raise NotImplementedError


@dataclass
class KeyValueTable(AsciiDocTable):
    headers: tuple[str, ...]
    rows: list[TableRow]
    cols: str

    def render(self, delimiter: str = '|') -> str:
        lines = [
            f'[%header, cols="{self.cols}"]',
            f'{delimiter}===',
            f'{delimiter} ' + f' {delimiter} '.join(self.headers),
        ]
        for row in self.rows:
            for cell in row.cells:
                lines.append(f'{delimiter} {cell}')
        lines.append(f'{delimiter}===')
        return '\n'.join(lines)


@dataclass
class IssueTable(AsciiDocTable):
    issues_by_bucket: list[tuple[str, str, list[Issue]]]
    anchor_prefix: str | None = None

    def render(self, delimiter: str = '|') -> str:
        lines = [
            '[%header, cols="^1,7"]',
            f'{delimiter}===',
            f'{delimiter} Issue {delimiter} Summary',
        ]
        for bucket_key, bucket_title, issues in self.issues_by_bucket:
            if not issues:
                continue
            if self.anchor_prefix:
                lines.append(f'2+^a{delimiter} [[{self.anchor_prefix}-{bucket_key}]] *{bucket_title}*')
            else:
                lines.append(f'2+^{delimiter} *{bucket_title}*')
            lines.append('')
            for issue in issues:
                lines.append(f'{delimiter} link:{issue.url}[#{issue.number}]')
                lines.append(f'{delimiter} {truncate_title(issue.title)}')
                lines.append('')
        lines.append(f'{delimiter}===')
        return '\n'.join(lines)


def truncate_title(value: str, max_length: int = 80) -> str:
    if len(value) <= max_length:
        return value
    return value[: max_length - 1].rstrip() + '…'
