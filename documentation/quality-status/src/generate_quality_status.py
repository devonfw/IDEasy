#!/usr/bin/env python3
"""CLI entry point for generating IDEasy quality status documents."""

import argparse
import logging
import os
import sys
import urllib.error
from pathlib import Path

from quality_status_generator.classify import classify_issues
from quality_status_generator.config import (
  COMMANDLETS,
  LABEL_ALIASES,
  OS_ORDER,
  REPO,
  SKIP_LABELS,
  TOOLS,
)
from quality_status_generator.github_api import fetch_all_issues, fetch_tool_names
from quality_status_generator.render import os_output_path
from quality_status_generator.service import generate_documents

log = logging.getLogger(__name__)


def main() -> None:
  """Parse arguments, fetch GitHub data, generate documents, and write output files."""
  logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")

  parser = argparse.ArgumentParser(
    description=f"Generate quality-status.adoc for {REPO}",
  )
  parser.add_argument(
    "--token",
    default=os.environ.get("GITHUB_TOKEN"),
    help="GitHub personal access token (or set the GITHUB_TOKEN env var)",
  )
  parser.add_argument("--output", default="quality-status.adoc")
  parser.add_argument("--state", default="open", choices=["open", "all"])
  args = parser.parse_args()

  if not args.token:
    log.warning("No GITHUB_TOKEN set — requests will be rate-limited to 60/hour.")

  log.info("Fetching tool list from source tree ...")
  tool_names = fetch_tool_names(args.token)
  if tool_names:
    log.info("  %d tools found: %s", len(tool_names), ", ".join(tool_names))
  else:
    log.warning("  No tools found — check token permissions and network connectivity.")

  log.info("Fetching %s issues from %s ...", args.state, REPO)
  try:
    issues = fetch_all_issues(args.token, state=args.state)
  except urllib.error.HTTPError as exc:
    log.error("GitHub API request failed: HTTP %s %s", exc.code, exc.reason)
    sys.exit(1)

  log.info("  %d issues loaded.", len(issues))

  tool_keys = set(tool_names) | set(TOOLS.keys())
  cmd_keys = set(COMMANDLETS.keys())
  _, _, unassigned = classify_issues(issues, tool_keys, cmd_keys)

  matched = len(issues) - len(unassigned)
  pct = f" ({matched / len(issues) * 100:.1f}%)" if issues else ""
  log.info("  %d assigned%s, %d unassigned.", matched, pct, len(unassigned))

  all_known = tool_keys | cmd_keys | set(LABEL_ALIASES)
  all_labels = {
    label["name"]
    for issue in issues
    for label in issue.get("labels", [])
  }
  unmatched = all_labels - all_known - SKIP_LABELS
  if unmatched:
    log.info(
      "  Labels with no mapping (consider adding to TOOLS/COMMANDLETS): %s",
      ", ".join(sorted(unmatched)),
    )

  main_output = Path(args.output)
  os_output_paths = {
    os_key: os_output_path(main_output, os_key)
    for os_key in OS_ORDER
  }
  os_filenames = {
    os_key: path.name
    for os_key, path in os_output_paths.items()
  }

  documents = generate_documents(issues, tool_names, os_filenames)

  main_output.parent.mkdir(parents=True, exist_ok=True)
  main_output.write_text(documents["overview"], encoding="utf-8")
  log.info("Written to %s", main_output)

  for os_key, output_path in os_output_paths.items():
    output_path.write_text(documents[os_key], encoding="utf-8")
    log.info("Written to %s", output_path)


if __name__ == "__main__":
  main()
