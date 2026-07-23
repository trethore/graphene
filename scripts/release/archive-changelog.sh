#!/usr/bin/env bash

set -euo pipefail

repository_root="$(git rev-parse --show-toplevel)"
cd "$repository_root"

metadata_script="scripts/release/metadata.py"
version="$("$metadata_script" resolve-version "${1:-}")"
release_date="${2:-$(date -u +%F)}"

tag="v$version"
if ! git rev-parse --verify --quiet "refs/tags/$tag" >/dev/null; then
  echo "Release tag $tag was not found. Run this script after the release and fetch tags first." >&2
  exit 1
fi

"$metadata_script" archive-changelog "$version" "$release_date"

echo "Archived CHANGELOG.md Unreleased section as $version ($release_date)."
