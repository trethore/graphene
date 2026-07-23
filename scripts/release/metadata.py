#!/usr/bin/env python3

import argparse
import re
import tomllib
from datetime import date
from pathlib import Path

CATALOG_PATH = Path("gradle/libs.versions.toml")
CHANGELOG_PATH = Path("CHANGELOG.md")
UNRELEASED_PATTERN = re.compile(
    r"^## \[Unreleased\]\s*$\n(.*?)(?=^## |\Z)",
    flags=re.MULTILINE | re.DOTALL,
)
VERSION_PATTERN = re.compile(r"^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z][0-9A-Za-z.-]*)?$")


def catalog_version(name: str) -> str:
    with CATALOG_PATH.open("rb") as catalog_file:
        catalog = tomllib.load(catalog_file)
    try:
        return catalog["versions"][name]
    except KeyError as exception:
        raise SystemExit(f"Version catalog does not contain '{name}'") from exception


def validate_version(version: str) -> str:
    if not VERSION_PATTERN.fullmatch(version):
        raise SystemExit(f"Invalid release version: {version}")
    return version


def unreleased_notes(changelog: str) -> tuple[re.Match[str], str]:
    match = UNRELEASED_PATTERN.search(changelog)
    if match is None:
        raise SystemExit("CHANGELOG.md does not contain an Unreleased section")

    notes = match.group(1).strip()
    if not notes:
        raise SystemExit("The Unreleased section in CHANGELOG.md is empty")
    return match, notes


def archive_changelog(version: str, release_date: str) -> None:
    validate_version(version)
    try:
        date.fromisoformat(release_date)
    except ValueError as exception:
        raise SystemExit(f"Invalid release date: {release_date}") from exception

    changelog = CHANGELOG_PATH.read_text()
    if re.search(rf"^## \[{re.escape(version)}\](?:\s|$)", changelog, flags=re.MULTILINE):
        raise SystemExit(f"CHANGELOG.md already contains a {version} section")

    match, notes = unreleased_notes(changelog)
    archived = (
        changelog[: match.start()]
        + "## [Unreleased]\n\n"
        + f"## [{version}] - {release_date}\n\n"
        + notes
        + "\n"
    )
    remaining = changelog[match.end() :].lstrip("\n")
    if remaining:
        archived += "\n" + remaining

    CHANGELOG_PATH.write_text(archived)


def main() -> None:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)

    catalog_parser = subparsers.add_parser("catalog-version")
    catalog_parser.add_argument("name")

    resolve_parser = subparsers.add_parser("resolve-version")
    resolve_parser.add_argument("requested", nargs="?", default="")

    subparsers.add_parser("unreleased")

    archive_parser = subparsers.add_parser("archive-changelog")
    archive_parser.add_argument("version")
    archive_parser.add_argument("release_date")

    arguments = parser.parse_args()
    if arguments.command == "catalog-version":
        print(catalog_version(arguments.name))
    elif arguments.command == "resolve-version":
        print(validate_version(arguments.requested or catalog_version("mod")))
    elif arguments.command == "unreleased":
        print(unreleased_notes(CHANGELOG_PATH.read_text())[1])
    elif arguments.command == "archive-changelog":
        archive_changelog(arguments.version, arguments.release_date)


if __name__ == "__main__":
    main()
