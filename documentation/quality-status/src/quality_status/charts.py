from __future__ import annotations

from html import escape
from math import cos, pi, sin
from pathlib import Path
from typing import Sequence

from .config import VISUALIZATION_CONFIG_SCHEMA, VISUALIZATIONS
from .documents import OverviewDocument

CHART_DIRECTORY = "quality-status-images"

BACKGROUND = "var(--chart-background)"
TEXT = "var(--chart-text)"
MUTED_TEXT = "var(--chart-muted-text)"
BAR_TRACK = "var(--chart-bar-track)"
BAR_COLORS = ("#0969da", "#1f883d", "#bf8700", "#8250df", "#cf222e")
STACK_COLORS = ("#0969da", "#8250df", "#8c959f")

THEME_STYLE = """<style>
  :root {
    color-scheme: light dark;
    --chart-background: #ffffff;
    --chart-text: #24292f;
    --chart-muted-text: #57606a;
    --chart-bar-track: #f0f3f6;
  }
  @media (prefers-color-scheme: dark) {
    :root {
      --chart-background: #0d1117;
      --chart-text: #f0f6fc;
      --chart-muted-text: #8b949e;
      --chart-bar-track: #30363d;
    }
  }
</style>"""


def _svg_document(width: int, height: int, body: Sequence[str]) -> str:
    return "\n".join([
        '<?xml version="1.0" encoding="UTF-8"?>',
        (
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
            f'viewBox="0 0 {width} {height}" role="img">'
        ),
        THEME_STYLE,
        f'<rect width="{width}" height="{height}" rx="12" fill="{BACKGROUND}"/>',
        *body,
        "</svg>",
        "",
    ])


def render_horizontal_bar_chart(title: str, values: Sequence[tuple[str, int]]) -> str:
    width = 640
    left = 150
    right = 54
    top = 68
    row_height = 38
    bar_height = 20
    height = top + max(1, len(values)) * row_height + 28
    chart_width = width - left - right
    maximum = max((value for _, value in values), default=1) or 1

    body = [
        f'<title>{escape(title)}</title>',
        f'<text x="24" y="34" fill="{TEXT}" font-family="sans-serif" font-size="20" font-weight="600">{escape(title)}</text>',
    ]
    for index, (label, value) in enumerate(values):
        y = top + index * row_height
        bar_width = chart_width * value / maximum
        color = BAR_COLORS[index % len(BAR_COLORS)]
        body.extend([
            (
                f'<text x="{left - 12}" y="{y + 15}" text-anchor="end" fill="{TEXT}" '
                f'font-family="sans-serif" font-size="14">{escape(label)}</text>'
            ),
            f'<rect x="{left}" y="{y}" width="{chart_width}" height="{bar_height}" rx="4" fill="{BAR_TRACK}"/>',
            f'<rect x="{left}" y="{y}" width="{bar_width:.1f}" height="{bar_height}" rx="4" fill="{color}"/>',
            (
                f'<text x="{left + bar_width + 8:.1f}" y="{y + 15}" fill="{MUTED_TEXT}" '
                f'font-family="sans-serif" font-size="13" font-weight="600">{value}</text>'
            ),
        ])
    return _svg_document(width, height, body)


def _polar_point(center_x: float, center_y: float, radius: float, angle: float) -> tuple[float, float]:
    return center_x + radius * cos(angle), center_y + radius * sin(angle)


def render_pie_chart(title: str, values: Sequence[tuple[str, int]]) -> str:
    values = [(label, value) for label, value in values if value > 0]
    width = 640
    height = max(300, 92 + len(values) * 30)
    center_x = 170.0
    center_y = height / 2 + 14
    radius = 100.0
    total = sum(value for _, value in values)

    body = [
        f'<title>{escape(title)}</title>',
        f'<text x="24" y="34" fill="{TEXT}" font-family="sans-serif" font-size="20" font-weight="600">{escape(title)}</text>',
    ]
    if total == 0:
        body.append(
            f'<text x="{width / 2}" y="{height / 2}" text-anchor="middle" fill="{MUTED_TEXT}" '
            'font-family="sans-serif" font-size="16">No data</text>'
        )
        return _svg_document(width, height, body)

    start_angle = -pi / 2
    for index, (label, value) in enumerate(values):
        color = BAR_COLORS[index % len(BAR_COLORS)]
        sweep = 2 * pi * value / total
        end_angle = start_angle + sweep
        start_x, start_y = _polar_point(center_x, center_y, radius, start_angle)
        end_x, end_y = _polar_point(center_x, center_y, radius, end_angle)
        large_arc = 1 if sweep > pi else 0

        if len(values) == 1:
            body.append(f'<circle cx="{center_x}" cy="{center_y}" r="{radius}" fill="{color}"/>')
        else:
            body.append(
                f'<path d="M {center_x:.1f} {center_y:.1f} L {start_x:.1f} {start_y:.1f} '
                f'A {radius} {radius} 0 {large_arc} 1 {end_x:.1f} {end_y:.1f} Z" fill="{color}"/>'
            )

        legend_y = 78 + index * 30
        percentage = value / total * 100
        body.extend([
            f'<rect x="330" y="{legend_y - 12}" width="14" height="14" rx="2" fill="{color}"/>',
            (
                f'<text x="354" y="{legend_y}" fill="{TEXT}" font-family="sans-serif" font-size="13">'
                f'{escape(label)}: {value} ({percentage:.1f}%)</text>'
            ),
        ])
        start_angle = end_angle

    body.extend([
        f'<circle cx="{center_x}" cy="{center_y}" r="50" fill="{BACKGROUND}"/>',
        (
            f'<text x="{center_x}" y="{center_y - 2}" text-anchor="middle" fill="{TEXT}" '
            f'font-family="sans-serif" font-size="24" font-weight="600">{total}</text>'
        ),
        (
            f'<text x="{center_x}" y="{center_y + 20}" text-anchor="middle" fill="{MUTED_TEXT}" '
            'font-family="sans-serif" font-size="12">total</text>'
        ),
    ])
    return _svg_document(width, height, body)


def render_os_chart(title: str, values: Sequence[tuple[str, int, int, int]]) -> str:
    width = 640
    left = 110
    right = 40
    top = 82
    row_height = 48
    bar_height = 24
    height = top + max(1, len(values)) * row_height + 54
    chart_width = width - left - right
    maximum = max((sum(parts) for _, *parts in values), default=1) or 1
    legend = (("Specific", STACK_COLORS[0]), ("Multi-OS", STACK_COLORS[1]), ("Cross-platform", STACK_COLORS[2]))

    body = [
        f'<title>{escape(title)}</title>',
        f'<text x="24" y="34" fill="{TEXT}" font-family="sans-serif" font-size="20" font-weight="600">{escape(title)}</text>',
    ]
    legend_x = 24
    for label, color in legend:
        body.extend([
            f'<rect x="{legend_x}" y="48" width="12" height="12" rx="2" fill="{color}"/>',
            f'<text x="{legend_x + 18}" y="59" fill="{MUTED_TEXT}" font-family="sans-serif" font-size="12">{label}</text>',
        ])
        legend_x += 112

    for index, (label, specific, multi, cross_platform) in enumerate(values):
        y = top + index * row_height
        body.append(
            f'<text x="{left - 12}" y="{y + 17}" text-anchor="end" fill="{TEXT}" font-family="sans-serif" font-size="14">{escape(label)}</text>'
        )
        x = float(left)
        for value, color in zip((specific, multi, cross_platform), STACK_COLORS):
            segment_width = chart_width * value / maximum
            if segment_width > 0:
                body.append(
                    f'<rect x="{x:.1f}" y="{y}" width="{segment_width:.1f}" height="{bar_height}" fill="{color}"/>'
                )
            x += segment_width
        total = specific + multi + cross_platform
        body.append(
            f'<text x="{x + 8:.1f}" y="{y + 17}" fill="{MUTED_TEXT}" font-family="sans-serif" font-size="13" font-weight="600">{total}</text>'
        )
    return _svg_document(width, height, body)


def render_chart(chart_type: str, title: str, values: Sequence[tuple[str, int]]) -> str:
    if chart_type == "bar":
        return render_horizontal_bar_chart(title, values)
    if chart_type == "pie":
        return render_pie_chart(title, values)
    raise ValueError(f"Unsupported chart type: {chart_type!r}. Use 'bar', 'pie', or 'none'.")


def write_charts(output_dir: str | Path, document: OverviewDocument) -> None:
    chart_path = Path(output_dir) / CHART_DIRECTORY
    chart_path.mkdir(parents=True, exist_ok=True)

    chart_definitions = {
        "issue_assignment": (
            "issue-assignment.svg",
            "Assigned vs unassigned issues",
            document.assignment_stats,
        ),
        "issue_types": (
            "issue-types.svg",
            "Issue type distribution",
            document.type_stats,
        ),
        "operating_systems": (
            "operating-systems.svg",
            "Issues by operating system",
            [(name, specific + multi + cross_platform) for name, specific, multi, cross_platform in document.os_stats],
        ),
        "issue_age": (
            "issue-age.svg",
            "Issue age",
            document.age_stats,
        ),
        "functional_labels": (
            "functional-labels.svg",
            "Most common functional labels",
            document.top_label_stats,
        ),
    }

    for section, (filename, title, values) in chart_definitions.items():
        settings = VISUALIZATIONS[section]
        if "chart" not in settings:
            raise ValueError(
                f'Invalid visualization config for "{section}": expected '
                f'{VISUALIZATION_CONFIG_SCHEMA}.'
            )

        chart_type = str(settings["chart"])
        output_file = chart_path / filename

        if chart_type == "none":
            output_file.unlink(missing_ok=True)
            continue

        if section == "operating_systems" and chart_type == "bar":
            content = render_os_chart(title, document.os_stats)
        else:
            content = render_chart(chart_type, title, values)

        output_file.write_text(content, encoding="utf-8")
