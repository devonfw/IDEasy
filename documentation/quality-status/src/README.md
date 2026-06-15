# Quality Status Generator

This package generates the IDEasy AsciiDoc quality status documents from open GitHub issues.

## Architecture

The implementation is intentionally split into small modules:

- `quality_status/github_client.py` – fetches open issues from the GitHub GraphQL API
- `quality_status/models.py` – contains the core data classes and reusable table abstractions
- `quality_status/classification.py` – classifies issue type and platform scope
- `quality_status/statistics.py` – computes issue statistics, age distribution, top labels, and OS summary data
- `quality_status/charts.py` – renders dependency-free SVG charts from the computed statistics
- `quality_status/documents.py` – assembles document data models from issues and statistics
- `quality_status/renderer.py` – renders the final AsciiDoc files
- `generate_quality_status.py` – CLI entry point

## Why GraphQL?

GitHub's GraphQL API allows the generator to fetch the exact issue fields it needs in a single query, including `issueType`, labels, assignee counts, and
pagination metadata.

## Usage

```bash
export GITHUB_TOKEN=<your_token>
python documentation\quality-status\src\generate_quality_status.py
```

From PowerShell, regenerate the checked-in documentation with:

```powershell
cd documentation/quality-status/src
$env:GITHUB_TOKEN = "<your-token>"
python documentation\quality-status\src\generate_quality_status.py
```

The `--output-dir ..` part is important: without it, the files are written to
the current working directory instead of `documentation/quality-status`.

The output contains the AsciiDoc files and a `quality-status-images` directory
with generated SVG charts. The overview places each chart beside its source
table, so the exact values remain available in text form.

Chart types and table visibility are configured in `quality_status/config.py`.
For every overview section, set `chart` to `"bar"`, `"pie"`, or `"none"` and
set `show_table` to `True` or `False`. For example:

```python
"issue_age": {"chart": "pie", "show_table": True},
```

The `operating_systems` and `issue_age` sections also support `show_links`.
This keeps navigation links visible independently of the optional data table:

```python
"issue_age": {"chart": "pie", "show_table": False, "show_links": True},
```

Pie charts are most meaningful for mutually exclusive values such as issue age
ranges. Issue metrics overlap, so a bar chart is usually clearer there.

## Notes

- Issue type statistics are based on the GitHub **Issue Type** (`Bug`, `Task`, `Feature`, `No Type`).
- Platform grouping is based on labels: `windows`, `linux`, and `macOS`.
- Issues without an OS label are treated as cross-platform.
- Issues with multiple OS labels are treated as multi-OS issues.
