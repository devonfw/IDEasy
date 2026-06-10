"""GitHub API access for the quality status generator."""

import json
import logging
import urllib.error
import urllib.parse
import urllib.request
from typing import Any

from quality_status_generator.config import (
  API_BASE,
  GITHUB_ACCEPT_HEADER,
  GITHUB_API_VERSION,
  GITHUB_PAGE_SIZE,
  REPO,
  TOOL_FOLDER_PATH,
)

log = logging.getLogger(__name__)


def _get(path: str, token: str | None, params: dict | None = None) -> Any:
  """Send an authenticated GET to the GitHub REST API and return parsed JSON."""
  url = f"{API_BASE}{path}"
  if params:
    url += "?" + urllib.parse.urlencode(params)

  req = urllib.request.Request(url)
  req.add_header("Accept", GITHUB_ACCEPT_HEADER)
  req.add_header("X-GitHub-Api-Version", GITHUB_API_VERSION)
  if token:
    req.add_header("Authorization", f"Bearer {token}")

  with urllib.request.urlopen(req) as resp:
    return json.loads(resp.read().decode())


def fetch_tool_names(token: str | None) -> list[str]:
  """Return sorted tool folder names from the source tree."""
  try:
    entries = _get(f"/repos/{REPO}/contents/{TOOL_FOLDER_PATH}", token)
    return sorted(
      entry["name"] for entry in entries
      if isinstance(entry, dict) and entry.get("type") == "dir"
    )
  except urllib.error.HTTPError as exc:
    hint = "" if token else " Set GITHUB_TOKEN to avoid rate limiting."
    log.warning(
      "Could not fetch tool list from %s (%s %s). "
      "Only commandlet sections will be generated.%s",
      TOOL_FOLDER_PATH, exc.code, exc.reason, hint,
    )
    return []
  except (urllib.error.URLError, json.JSONDecodeError) as exc:
    log.warning(
      "Could not fetch tool list from %s: %s",
      TOOL_FOLDER_PATH,
      exc,
      exc_info=True,
    )
    return []


def fetch_all_issues(token: str | None, state: str = "open") -> list[dict]:
  """Fetch all non-pull-request issues from the repository, handling pagination."""
