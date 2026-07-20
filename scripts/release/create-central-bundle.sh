#!/usr/bin/env bash

set -euo pipefail

repository_root="$(git rev-parse --show-toplevel)"
cd "$repository_root"

release_version="${1:?Usage: create-central-bundle.sh <release-version>}"
staging_directory="build/central-portal/staging"
component_directory="${staging_directory}/io/github/trethore/graphene-ui/${release_version}"
bundle_path="build/central-portal/central-bundle.zip"

if [[ ! -d "$component_directory" ]]; then
  echo "Expected staged Maven component at ${component_directory}" >&2
  exit 1
fi

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
