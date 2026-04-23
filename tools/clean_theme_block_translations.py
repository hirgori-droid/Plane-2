#!/usr/bin/env python3
from __future__ import annotations

import csv
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
THEME_BLOCKS_PATH = ROOT / "app/src/main/assets/theme_blocks_seed.csv"
ONCE_SUFFIX_REGEX = re.compile(r"\s+once\b", re.IGNORECASE)


def normalize_english(value: str) -> str:
    return " ".join(ONCE_SUFFIX_REGEX.sub("", value).split()).strip()


def main() -> int:
    with THEME_BLOCKS_PATH.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.reader(handle, delimiter=";")
        rows = list(reader)

    header, data_rows = rows[0], rows[1:]
    if header != ["block", "russian", "transcription", "english", "translations", "examples"]:
        raise ValueError(f"Unexpected header in {THEME_BLOCKS_PATH.name}: {header!r}")

    cleaned_rows: list[list[str]] = []
    for row in data_rows:
        if len(row) != 6:
            raise ValueError(f"Unexpected row in {THEME_BLOCKS_PATH.name}: {row!r}")
        block, russian, transcription, english, translations, examples = row
        normalized_english = normalize_english(english.strip())
        primary_translation = translations.split("|", 1)[0].strip() if translations.strip() else normalized_english
        primary_translation = normalize_english(primary_translation)
        cleaned_rows.append([block, russian, transcription, normalized_english, primary_translation, examples])

    with THEME_BLOCKS_PATH.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter=";", lineterminator="\n")
        writer.writerow(header)
        writer.writerows(cleaned_rows)

    print(f"Cleaned theme block translations in {THEME_BLOCKS_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
