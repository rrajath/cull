#!/usr/bin/env python3
"""Keep a Changelog helper for the release workflow.

  changelog.py notes CHANGELOG.md
      Print the entries under ## [Unreleased] (empty sub-headings dropped).

  changelog.py release CHANGELOG.md VERSION DATE
      Move the Unreleased entries into a new "## [VERSION] - DATE" section,
      leaving an empty ## [Unreleased] heading above it.
"""
import re
import sys

UNRELEASED = "## [Unreleased]"


def split(text):
    start = text.find(UNRELEASED)
    if start == -1:
        sys.exit(f"CHANGELOG has no '{UNRELEASED}' heading")
    body_start = start + len(UNRELEASED)
    nxt = re.search(r"^## \[", text[body_start:], re.MULTILINE)
    body_end = body_start + nxt.start() if nxt else len(text)
    return text[:start], text[body_start:body_end], text[body_end:]


def clean(body):
    """Drop ### sub-headings that have no entries, and surrounding blank lines."""
    sections, current = [], None
    for line in body.strip("\n").splitlines():
        if line.startswith("### "):
            current = [line, []]
            sections.append(current)
        elif current is not None:
            current[1].append(line)
        elif line.strip():
            sections.append([None, [line]])
    out = []
    for heading, lines in sections:
        content = "\n".join(lines).strip("\n")
        if not content.strip():
            continue
        out.append(f"{heading}\n\n{content}" if heading else content)
    return "\n\n".join(out)


def main():
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    mode, path = sys.argv[1], sys.argv[2]
    with open(path, encoding="utf-8") as f:
        text = f.read()
    head, body, rest = split(text)
    entries = clean(body)

    if mode == "notes":
        print(entries or "No notable changes.")
    elif mode == "release" and len(sys.argv) == 5:
        version, date = sys.argv[3], sys.argv[4]
        if re.search(rf"^## \[{re.escape(version)}\]", text, re.MULTILINE):
            sys.exit(f"CHANGELOG already has a section for {version}")
        section = f"## [{version}] - {date}\n\n{entries or 'No notable changes.'}\n\n"
        with open(path, "w", encoding="utf-8") as f:
            f.write(f"{head}{UNRELEASED}\n\n{section}{rest.lstrip(chr(10))}")
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main()
