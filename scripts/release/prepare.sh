#!/usr/bin/env bash

set -euo pipefail

repository_root="$(git rev-parse --show-toplevel)"
cd "$repository_root"

requested_version="${1:-}"
release_github="${2:-false}"

read_gradle_property() {
  local property_name="$1"
  awk -F= -v property_name="$property_name" \
    '$1 == property_name { print substr($0, index($0, "=") + 1); exit }' \
    gradle.properties
}

version="$requested_version"
if [[ -z "$version" ]]; then
  version="$(read_gradle_property mod_version)"
fi

if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z][0-9A-Za-z.-]*)?$ ]]; then
  echo "Invalid release version: $version" >&2
  exit 1
fi

tag="v${version}"

if [[ "$release_github" == "true" ]]; then
  jcefgithub_version="$(read_gradle_property jcefgithub_version)"
  if [[ -z "$jcefgithub_version" ]]; then
    echo "jcefgithub_version is missing from gradle.properties" >&2
    exit 1
  fi

  if git rev-parse --verify --quiet "refs/tags/${tag}" >/dev/null; then
    echo "Tag ${tag} already exists" >&2
    exit 1
  fi

  : "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required for a GitHub release}"
  : "${GITHUB_SERVER_URL:?GITHUB_SERVER_URL is required for a GitHub release}"

  previous_tag="$(gh api "repos/${GITHUB_REPOSITORY}/releases/latest" --jq '.tag_name' 2>/dev/null || true)"

  {
    echo "## Versions"
    echo
    echo "- Graphene: \`${version}\`"
    echo "- jcefgithub: \`${jcefgithub_version}\`"
    echo

    if [[ -n "$previous_tag" ]] && git rev-parse --verify --quiet "refs/tags/${previous_tag}" >/dev/null; then
      range="${previous_tag}..HEAD"
      echo "## Changes since ${previous_tag}"
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
