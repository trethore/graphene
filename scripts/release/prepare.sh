#!/usr/bin/env bash

set -euo pipefail

repository_root="$(git rev-parse --show-toplevel)"
cd "$repository_root"

requested_version="${1:-}"
release_github="${RELEASE_GITHUB:-false}"
draft="${DRAFT:-false}"
github_release_action=none
metadata_script="scripts/release/metadata.py"

if [[ "$release_github" != "true" && "$release_github" != "false" ]]; then
  echo "Invalid GitHub release flag: $release_github" >&2
  exit 1
fi
if [[ "$draft" != "true" && "$draft" != "false" ]]; then
  echo "Invalid draft flag: $draft" >&2
  exit 1
fi

version="$("$metadata_script" resolve-version "$requested_version")"
tag="v${version}"

if [[ "$release_github" == "true" ]]; then
  github_release_action=create
  jcefgithub_version="$("$metadata_script" catalog-version jcefgithub)"

  : "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required for a GitHub release}"
  : "${GITHUB_SERVER_URL:?GITHUB_SERVER_URL is required for a GitHub release}"

  set +e
  release_data="$(
    gh api "repos/$GITHUB_REPOSITORY/releases/tags/$tag" \
      --jq '[.draft, .target_commitish] | @tsv' 2>&1
  )"
  release_status=$?
  set -e
  if [[ $release_status -ne 0 ]]; then
    if [[ "$release_data" == *"HTTP 404"* ]]; then
      release_data=""
    else
      echo "Could not inspect GitHub release $tag:" >&2
      echo "$release_data" >&2
      exit $release_status
    fi
  fi
  if [[ -n "$release_data" ]]; then
    IFS=$'\t' read -r is_draft target_commitish <<< "$release_data"
    if [[ "$is_draft" != "true" ]]; then
      echo "GitHub release $tag already exists and is not a draft" >&2
      exit 1
    fi
    if [[ "$draft" == "true" ]]; then
      echo "Draft GitHub release $tag already exists" >&2
      exit 1
    fi

    current_commit="$(git rev-parse HEAD)"
    if [[ "$target_commitish" != "$current_commit" ]]; then
      echo "Draft GitHub release $tag targets $target_commitish instead of $current_commit" >&2
      exit 1
    fi
    github_release_action=promote
  elif git rev-parse --verify --quiet "refs/tags/${tag}" >/dev/null; then
    echo "Tag ${tag} already exists without a draft GitHub release" >&2
    exit 1
  fi

  previous_tag="$(
    git describe --tags --abbrev=0 --match 'v*' --exclude "$tag" HEAD 2>/dev/null || true
  )"
  changelog_notes="$("$metadata_script" unreleased)"

  {
    echo "## Versions"
    echo
    echo "- Graphene: \`${version}\`"
    echo "- jcefgithub: \`${jcefgithub_version}\`"
    echo
    echo "## Changes"
    echo
    echo "$changelog_notes"

    if [[ -n "$previous_tag" ]]; then
      echo
      printf '**Full Changelog:** [`%s...%s`](%s/%s/compare/%s...%s)\n' \
        "$previous_tag" "$tag" "$GITHUB_SERVER_URL" "$GITHUB_REPOSITORY" "$previous_tag" "$tag"
    fi
  } > RELEASE_NOTES.md
fi

echo "version=${version}"
echo "tag=${tag}"
echo "github_release_action=${github_release_action}"
