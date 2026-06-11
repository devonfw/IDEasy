from __future__ import annotations

import argparse

from quality_status.config import DEFAULT_OWNER, DEFAULT_REPO
from quality_status.documents import build_documents
from quality_status.github_client import GitHubGraphQLClient
from quality_status.renderer import write_documents


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate IDEasy quality status AsciiDoc documents.")
    parser.add_argument("--owner", default=DEFAULT_OWNER, help="GitHub owner or organization")
    parser.add_argument("--repo", default=DEFAULT_REPO, help="GitHub repository name")
    parser.add_argument(
        "--output-dir",
        default=".",
        help="Where the .adoc files should be written",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    client = GitHubGraphQLClient()
    issues = client.fetch_open_issues(owner=args.owner, repo=args.repo)
    bundle = build_documents(owner=args.owner, repo=args.repo, issues=issues)
    write_documents(output_dir=args.output_dir, bundle=bundle)
    print(f"Generated files in: {args.output_dir}")


if __name__ == "__main__":
    main()
