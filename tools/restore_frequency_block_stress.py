#!/usr/bin/env python3
from __future__ import annotations

import csv
import re
from pathlib import Path

from ruaccent import load_accentor

ROOT = Path(__file__).resolve().parent.parent
WORDS_SEED_PATH = ROOT / "app" / "src" / "main" / "assets" / "words_seed.csv"
TARGET_BLOCKS = {"core_3000", "extended_6000"}
GRAMMAR_SUFFIX_PATTERN = re.compile(r"^(.*?)(\s+\((м\.р\.|ж\.р\.|с\.р\.)\))?$")
ACCENTED_TOKEN_PATTERN = re.compile(r"^[а-яё'-]+$", re.IGNORECASE)
VOWELS = "аеёиоуыэюяАЕЁИОУЫЭЮЯ"


def strip_grammar_suffix(text: str) -> tuple[str, str]:
    match = GRAMMAR_SUFFIX_PATTERN.match(text.strip())
    if not match:
        return text.strip(), ""
    return match.group(1).strip(), (match.group(2) or "")


def apostrophe_to_combining(word: str) -> str:
    chars: list[str] = []
    for char in word:
        if char == "'":
            if chars and chars[-1] in VOWELS:
                chars.append("\u0301")
            continue
        chars.append(char)
    return "".join(chars)


def choose_accented_form(raw_prediction: str, original: str) -> str:
    for token in raw_prediction.split():
        cleaned = token.strip()
        if not ACCENTED_TOKEN_PATTERN.fullmatch(cleaned):
            continue
        normalized = cleaned.replace("'", "").lower()
        if normalized == original.lower():
            return apostrophe_to_combining(cleaned)
    return original


def main() -> int:
    accentor = load_accentor(device="cpu")

    with WORDS_SEED_PATH.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.reader(handle, delimiter=";")
        header = next(reader, None)
        if header not in (
            ["block", "russian", "english"],
            ["block", "russian", "english", "examples"],
        ):
            raise SystemExit(f"Unexpected header in {WORDS_SEED_PATH.name}: {header!r}")
        rows = list(reader)

    updated_rows: list[list[str]] = []
    changed = 0
    for row in rows:
        if len(row) == 3:
            row = [row[0], row[1], row[2], ""]
        block_id, russian, english, examples = row
        if block_id in TARGET_BLOCKS:
            base_word, suffix = strip_grammar_suffix(russian)
            predicted = accentor.put_accent(base_word, format="apostrophe")
            accented = choose_accented_form(predicted, base_word)
            new_russian = accented + suffix
            if new_russian != russian:
                changed += 1
            russian = new_russian
        updated_rows.append([block_id, russian, english, examples])

    with WORDS_SEED_PATH.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter=";", lineterminator="\n")
        writer.writerow(["block", "russian", "english", "examples"])
        writer.writerows(updated_rows)

    print(f"Updated stress for {changed} frequency rows in {WORDS_SEED_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
