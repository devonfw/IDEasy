from __future__ import annotations

from .models import AgeBucket, OsGroup

DEFAULT_OWNER = "devonfw"
DEFAULT_REPO = "IDEasy"

# Change these values to control the generated overview:
# - chart: "bar", "pie", or "none"
# - show_table: True or False
# - show_links: True or False (supported for operating_systems and issue_age)
VISUALIZATION_CONFIG_SCHEMA = (
    '{"chart": "bar|pie|none", "show_table": True|False, '
    '"show_links": True|False}'
)

VISUALIZATIONS = {
    "issue_assignment": {"chart": "pie", "show_table": False},
    "issue_types": {"chart": "pie", "show_table": False},
    "operating_systems": {"chart": "bar", "show_table": False, "show_links": True},
    "issue_age": {"chart": "pie", "show_table": False, "show_links": True},
    "functional_labels": {"chart": "bar", "show_table": False},
}

OUTPUT_FILES = {
    "overview": "quality-status.adoc",
    "Windows": "quality-status-windows.adoc",
    "Linux": "quality-status-linux.adoc",
    "macOS": "quality-status-macos.adoc",
}

OS_GROUPS: tuple[OsGroup, ...] = (
    OsGroup(name="Windows", labels=("windows",), output_file=OUTPUT_FILES["Windows"]),
    OsGroup(name="Linux", labels=("linux",), output_file=OUTPUT_FILES["Linux"]),
    OsGroup(name="macOS", labels=("macos", "macosx", "osx"), output_file=OUTPUT_FILES["macOS"]),
)

AGE_BUCKETS: tuple[AgeBucket, ...] = (
    AgeBucket(key="0-10-days", title="0-10 days", min_days=0, max_days=10),
    AgeBucket(key="11-30-days", title="11-30 days", min_days=11, max_days=30),
    AgeBucket(key="31-60-days", title="31-60 days", min_days=31, max_days=60),
    AgeBucket(key="61-90-days", title="61-90 days", min_days=61, max_days=90),
    AgeBucket(key="90plus-days", title="90+ days", min_days=91, max_days=None),
)

ISSUE_STAT_DEFINITIONS: tuple[tuple[str, str], ...] = (
    ("all", "Open Issues"),
    ("assigned", "Assigned"),
    ("unassigned", "Unassigned"),
    ("blockers", "Blockers"),
    ("bugs", "Bugs"),
    ("tasks", "Tasks"),
    ("features", "Features"),
    ("no_type", "No Type"),
)

ASSIGNMENT_STAT_DEFINITIONS: tuple[tuple[str, str], ...] = (
    ("assigned", "Assigned"),
    ("unassigned", "Unassigned"),
)

TYPE_STAT_DEFINITIONS: tuple[tuple[str, str], ...] = (
    ("bugs", "Bugs"),
    ("tasks", "Tasks"),
    ("features", "Features"),
    ("no_type", "No Type"),
)


BLOCKER_LABELS = {"blocker", "critical"}

EXCLUDED_FUNCTIONAL_LABELS = {
    # labels are normalized using casefold(), comparison is case-insensitive
    "epic",
    "ready-to-implement",
    "bug",
    "task",
    "feature",
    "enhancement",
    "windows",
    "linux",
    "macos",
    "macosx",
    "osx",
    "documentation",
    "dependencies",
    "help wanted",
    "question",
    "invalid",
    "duplicate",
    "wontfix",
    "maintenance",
    "ci",
    "workflow",
    "github-actions",
    "build",
    "tests",
    "test",
}

ISSUE_TYPE_MAP = {
    "bug": "bug",
    "feature": "feature",
    "task": "task",
    "no type": "no_type",
}
