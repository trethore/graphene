#!/usr/bin/env bash

set -euo pipefail

repository_root="$(git rev-parse --show-toplevel)"
cd "$repository_root"

requested_version="${1:-}"
release_github="${2:-false}"

read_catalog_version() {
  local version_name="$1"
  python - "$version_name" <<'PY'
import sys
import tomllib

with open("gradle/libs.versions.toml", "rb") as catalog_file:
    catalog = tomllib.load(catalog_file)

print(catalog["versions"][sys.argv[1]])
PY
}

version="$requested_version"
if [[ -z "$version" ]]; then
  version="$(read_catalog_version mod)"
fi

if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z][0-9A-Za-z.-]*)?$ ]]; then
  echo "Invalid release version: $version" >&2
  exit 1
fi

tag="v${version}"

if [[ "$release_github" == "true" ]]; then
  jcefgithub_version="$(read_catalog_version jcefgithub)"
  if [[ -z "$jcefgithub_version" ]]; then
    echo "jcefgithub is missing from the version catalog" >&2
    exit 1
  fi

  if git rev-parse --verify --quiet "refs/tags/${tag}" >/dev/null; then
    echo "Tag ${tag} already exists" >&2
    exit 1
  fi

  : "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required for a GitHub release}"
  : "${GITHUB_SERVER_URL:?GITHUB_SERVER_URL is required for a GitHub release}"

  previous_tag="$(gh api "repos/${GITHUB_REPOSITORY}/releases/latest" --jq '.tag_name' 2>/dev/null || true)"
  changelog_notes="$(python <<'PY'
import re
from pathlib import Path

changelog = Path("CHANGELOG.md").read_text()
match = re.search(
    r"^## \[Unreleased\]\s*$\n(.*?)(?=^## |\Z)",
    changelog,
    flags=re.MULTILINE | re.DOTALL,
)
if match is None:
    raise SystemExit("CHANGELOG.md does not contain an Unreleased section")

notes = match.group(1).strip()
if not notes:
    raise SystemExit("The Unreleased section in CHANGELOG.md is empty")

print(notes)
PY
)"

  {
    echo "## Versions"
    echo
    echo "- Graphene: \`${version}\`"
    echo "- jcefgithub: \`${jcefgithub_version}\`"
    echo
    echo "## Changes"
    echo
    echo "$changelog_notes"
    echo

    if [[ -n "$previous_tag" ]] && git rev-parse --verify --quiet "refs/tags/${previous_tag}" >/dev/null; then
      range="${previous_tag}..HEAD"
      echo "## Commits since ${previous_tag}"
    else
      range="HEAD"
      echo "## Recent changes"
    fi

    echo
    commit_count="$(git rev-list --count "$range")"
    if [[ "$commit_count" -eq 0 ]]; then
      echo "No commits since the previous release."
    else
      git log "$range" --max-count=10 --format='%H%x09%h%x09%s' |
        while IFS=$'\t' read -r full_hash short_hash subject; do
          printf -- '- %s ([`%s`](%s/%s/commit/%s))\n' \
            "$subject" "$short_hash" "$GITHUB_SERVER_URL" "$GITHUB_REPOSITORY" "$full_hash"
        done

      if [[ "$commit_count" -gt 10 ]]; then
        echo
        echo "_Showing the 10 most recent of ${commit_count} commits._"
      fi
    fi
  } > RELEASE_NOTES.md
fi

echo "version=${version}"
echo "tag=${tag}"
