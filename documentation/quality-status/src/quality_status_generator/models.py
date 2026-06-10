from datetime import datetime
from typing import NamedTuple


class IssueRef(NamedTuple):
  """Classified issue record used throughout the rendering pipeline."""
  number: int
  title: str
  url: str
  bug: bool
  blocker: bool
  os_keys: list[str]
  created_at: datetime
