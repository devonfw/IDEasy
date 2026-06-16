from __future__ import annotations

import json
import os
from datetime import datetime, timezone
from urllib.request import Request, urlopen

from .models import Issue


class GitHubGraphQLClient:
    endpoint = "https://api.github.com/graphql"

    def __init__(self, token: str | None = None) -> None:
        self.token = token or os.getenv("GITHUB_TOKEN")
        if not self.token:
            raise ValueError("A GitHub token is required. Set GITHUB_TOKEN or pass token explicitly.")

    def fetch_open_issues(self, owner: str, repo: str) -> list[Issue]:
        issues: list[Issue] = []
        cursor: str | None = None

        while True:
            payload = self._execute_query(self._build_query(), {
                "owner": owner,
                "repo": repo,
                "cursor": cursor,
            })
            issue_connection = payload["data"]["repository"]["issues"]
            for item in issue_connection["nodes"]:
                issues.append(self._to_issue(item))

            page_info = issue_connection["pageInfo"]
            if not page_info["hasNextPage"]:
                break
            cursor = page_info["endCursor"]

        return issues

    def _execute_query(self, query: str, variables: dict[str, str | None]) -> dict:
        request = Request(
            self.endpoint,
            data=json.dumps({"query": query, "variables": variables}).encode("utf-8"),
            headers={
                "Authorization": f"Bearer {self.token}",
                "Content-Type": "application/json",
                "Accept": "application/vnd.github+json",
                "User-Agent": "quality-status-generator",
            },
            method="POST",
        )
        with urlopen(request) as response:
            payload = json.loads(response.read().decode("utf-8"))
        if "errors" in payload:
            raise RuntimeError(f"GitHub GraphQL error: {payload['errors']}")
        return payload

    @staticmethod
    def _build_query() -> str:
        return """
        query($owner: String!, $repo: String!, $cursor: String) {
          repository(owner: $owner, name: $repo) {
            issues(first: 100, states: OPEN, after: $cursor, orderBy: {field: CREATED_AT, direction: DESC}) {
              nodes {
                number
                title
                url
                createdAt
                issueType {
                  name
                }
                labels(first: 50) {
                  nodes {
                    name
                  }
                }
                assignees(first: 1) {
                  totalCount
                }
              }
              pageInfo {
                hasNextPage
                endCursor
              }
            }
          }
        }
        """

    @staticmethod
    def _to_issue(item: dict) -> Issue:
        issue_type = item.get("issueType") or {}
        labels = tuple(label["name"] for label in item.get("labels", {}).get("nodes", []))
        created_at = datetime.strptime(item["createdAt"], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)
        return Issue(
            number=item["number"],
            title=item["title"],
            url=item["url"],
            labels=labels,
            issue_type=issue_type.get("name"),
            created_at=created_at,
            is_assigned=(item.get("assignees", {}).get("totalCount", 0) > 0),
        )
