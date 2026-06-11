# Quality Status Generator

This package generates the IDEasy AsciiDoc quality status documents from open GitHub issues.

## Architecture

The implementation is intentionally split into small modules:

- `quality_status/github_client.py` – fetches open issues from the GitHub GraphQL API
- `quality_status/models.py` – contains the core data classes and reusable table abstractions
- `quality_status/classification.py` – classifies issue type and platform scope
- `quality_status/statistics.py` – computes issue statistics, age distribution, top labels, and OS summary data
- `quality_status/documents.py` – assembles document data models from issues and statistics
- `quality_status/renderer.py` – renders the final AsciiDoc files
- `generate_quality_status.py` – CLI entry point

## Why GraphQL?

GitHub's GraphQL API allows the generator to fetch the exact issue fields it needs in a single query, including `issueType`, labels, assignee counts, and pagination metadata.

## Usage

```bash
export GITHUB_TOKEN=<your_token>
python generate_quality_status.py --owner devonfw --repo IDEasy --output-dir generated-quality-status
```

## Notes

- Issue type statistics are based on the GitHub **Issue Type** (`Bug`, `Task`, `Feature`, `No Type`).
- Platform grouping is based on labels: `windows`, `linux`, and `macOS`.
- Issues without an OS label are treated as cross-platform.
- Issues with multiple OS labels are treated as multi-OS issues.
