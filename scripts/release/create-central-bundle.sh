#!/usr/bin/env bash

set -euo pipefail

repository_root="$(git rev-parse --show-toplevel)"
cd "$repository_root"

usage="Usage: create-central-bundle.sh <release-version> <artifact-id>..."
release_version="${1:?$usage}"
shift
if [[ $# -eq 0 ]]; then
  echo "$usage" >&2
  exit 1
fi

staging_directory="build/central-portal/staging"
bundle_path="build/central-portal/central-bundle.zip"

for artifact_id in "$@"; do
  component_directory="${staging_directory}/io/github/trethore/${artifact_id}/${release_version}"
  if [[ ! -d "$component_directory" ]]; then
    echo "Expected staged Maven component at ${component_directory}" >&2
    exit 1
  fi
done

find "$staging_directory" -type f -name 'maven-metadata.xml*' -delete

while IFS= read -r -d '' artifact; do
  md5sum "$artifact" | cut -d ' ' -f1 > "${artifact}.md5"
  sha1sum "$artifact" | cut -d ' ' -f1 > "${artifact}.sha1"
  sha256sum "$artifact" | cut -d ' ' -f1 > "${artifact}.sha256"
  sha512sum "$artifact" | cut -d ' ' -f1 > "${artifact}.sha512"
done < <(
  find "$staging_directory" -type f \
    ! -name '*.md5' \
    ! -name '*.sha1' \
    ! -name '*.sha256' \
    ! -name '*.sha512' \
    -print0
)

rm -f "$bundle_path"
(
  cd "$staging_directory"
  zip --quiet --recurse-paths ../central-bundle.zip .
)
