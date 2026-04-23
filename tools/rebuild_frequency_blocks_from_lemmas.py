#!/usr/bin/env python3
from __future__ import annotations

import csv
import re
import unicodedata
from pathlib import Path

from pymorphy3 import MorphAnalyzer

ROOT = Path(__file__).resolve().parent.parent
WORDS_SEED_PATH = ROOT / "app" / "src" / "main" / "assets" / "words_seed.csv"
SOURCE_PATH = ROOT / "tmp" / "100000-russian-words.txt"
TARGET_BLOCKS = ("core_3000", "extended_6000")
TARGET_COUNTS = {"core_3000": 3000, "extended_6000": 6000}
WORD_PATTERN = re.compile(r"^[а-яё]+(?:-[а-яё]+)*$")
EXCLUDED_POS = {"GRND", "PRTF", "PRTS", "INTJ", "PNCT", "UNKN"}
ALLOWED_POS = {"NOUN", "ADJF", "VERB", "INFN", "ADVB", "NUMR", "COMP"}
EXCLUDED_TAG_MARKERS = {"Geox", "Name", "Surn", "Patr", "Orgn", "Trad", "Abbr", "Arch"}
GENDER_SUFFIXES = {"masc": "м.р.", "femn": "ж.р.", "neut": "с.р."}
ALLOWED_APRO_NORMAL_FORMS = {"этот", "который", "свой", "мой"}
ALLOWED_SPECIAL_NORMAL_FORMS = {"можно", "надо"}
PRIORITY_WORDS = ("этот", "который", "свой", "мой", "можно", "надо")
EXCLUDED_NORMAL_FORMS = {
    "тот",
    "весь",
    "один",
    "такой",
    "наш",
    "самый",
    "сам",
    "какой",
    "ваш",
    "ничто",
    "себя",
    "нельзя",
    "уж",
    "что-то",
    "советский",
    "ховать",
}
SPECIAL_DISPLAY_FORMS = {
    # Keep plural-only everyday nouns in their real dictionary form instead of
    # the artificial singular lemma produced by morphology libraries.
    "деньга": "деньги",
}


def normalize_key(text: str) -> str:
    text = text.replace("ё", "е").replace("Ё", "Е")
    return "".join(
        ch for ch in unicodedata.normalize("NFD", text)
        if unicodedata.category(ch) != "Mn"
    ).lower()


def strip_grammar_suffix(text: str) -> str:
    return re.sub(r"\s+\((м\.р\.|ж\.р\.|с\.р\.)\)$", "", text.strip())


def transliterate(word: str) -> str:
    mapping = {
        "а": "a",
        "б": "b",
        "в": "v",
        "г": "g",
        "д": "d",
        "е": "ye",
        "ё": "yo",
        "ж": "zh",
        "з": "z",
        "и": "i",
        "й": "y",
        "к": "k",
        "л": "l",
        "м": "m",
        "н": "n",
        "о": "o",
        "п": "p",
        "р": "r",
        "с": "s",
        "т": "t",
        "у": "u",
        "ф": "f",
        "х": "kh",
        "ц": "ts",
        "ч": "ch",
        "ш": "sh",
        "щ": "shch",
        "ъ": "",
        "ы": "y",
        "ь": "",
        "э": "e",
        "ю": "yu",
        "я": "ya",
        "-": "-",
    }
    return "/" + "".join(mapping.get(ch.lower(), ch) for ch in word) + "/"


def with_gender_suffix(base: str, tag) -> str:
    for marker, suffix in GENDER_SUFFIXES.items():
        if marker in tag:
            return f"{base} ({suffix})"
    return base


def build_display_word(word: str, morph: MorphAnalyzer) -> str | None:
    parse = morph.parse(word)[0]
    pos = parse.tag.POS
    if parse.normal_form in ALLOWED_SPECIAL_NORMAL_FORMS:
        return parse.normal_form
    if pos not in ALLOWED_POS or pos in EXCLUDED_POS:
        return None
    if any(marker in parse.tag for marker in EXCLUDED_TAG_MARKERS):
        return None
    if parse.normal_form in EXCLUDED_NORMAL_FORMS:
        return None
    if parse.normal_form in SPECIAL_DISPLAY_FORMS:
        return SPECIAL_DISPLAY_FORMS[parse.normal_form]

    if pos == "NOUN":
        if "Pltm" in parse.tag:
            return None
        inflected = parse.inflect({"nomn", "sing"})
        base = inflected.word if inflected else parse.normal_form
        return with_gender_suffix(base, parse.tag)

    if pos == "ADJF":
        if "Apro" in parse.tag and parse.normal_form not in ALLOWED_APRO_NORMAL_FORMS:
            return None
        base = parse.normal_form
        return with_gender_suffix(base, {"masc"})

    if pos in {"VERB", "INFN"}:
        return parse.normal_form

    return parse.normal_form


def load_existing_rows() -> list[list[str]]:
    with WORDS_SEED_PATH.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.reader(handle, delimiter=";")
        header = next(reader, None)
        if header not in (
            ["block", "russian", "english"],
            ["block", "russian", "english", "examples"],
        ):
            raise SystemExit(f"Unexpected header in {WORDS_SEED_PATH.name}: {header!r}")
        rows = []
        for row in reader:
            if len(row) == 3:
                rows.append([row[0], row[1], row[2], ""])
            elif len(row) == 4:
                rows.append(row)
            else:
                raise SystemExit(f"Unexpected row in {WORDS_SEED_PATH.name}: {row!r}")
        return rows


def build_old_english_map(rows: list[list[str]]) -> dict[str, str]:
    mapping: dict[str, str] = {}
    for _block, russian, english, _examples in rows:
        key = normalize_key(strip_grammar_suffix(russian))
        mapping.setdefault(key, english.strip())
    return mapping


def rebuild_rows() -> list[list[str]]:
    if not SOURCE_PATH.exists():
        raise SystemExit(f"Source file not found: {SOURCE_PATH}")

    morph = MorphAnalyzer()
    existing_rows = load_existing_rows()
    english_map = build_old_english_map(existing_rows)
    preserved_rows = [row for row in existing_rows if row[0] not in TARGET_BLOCKS]

    seen_display_keys = {
        normalize_key(strip_grammar_suffix(row[1]))
        for row in preserved_rows
    }

    rebuilt_rows: list[list[str]] = []

    for priority_word in PRIORITY_WORDS:
        display_word = build_display_word(priority_word, morph)
        if not display_word:
            continue
        display_key = normalize_key(strip_grammar_suffix(display_word))
        if display_key in seen_display_keys:
            continue
        seen_display_keys.add(display_key)
        english = english_map.get(display_key, strip_grammar_suffix(display_word))
        rebuilt_rows.append(["", display_word, english, ""])

    for raw_word in SOURCE_PATH.read_text(encoding="utf-8").splitlines():
        word = raw_word.strip().lower()
        if not word or not WORD_PATTERN.fullmatch(word):
            continue

        display_word = build_display_word(word, morph)
        if not display_word:
            continue

        display_key = normalize_key(strip_grammar_suffix(display_word))
        if display_key in seen_display_keys:
            continue

        seen_display_keys.add(display_key)
        english = english_map.get(display_key, strip_grammar_suffix(display_word))
        rebuilt_rows.append(["", display_word, english, ""])

        if len(rebuilt_rows) >= TARGET_COUNTS["core_3000"] + TARGET_COUNTS["extended_6000"]:
            break

    expected_total = TARGET_COUNTS["core_3000"] + TARGET_COUNTS["extended_6000"]
    if len(rebuilt_rows) < expected_total:
        raise SystemExit(
            f"Not enough unique normalized words: expected {expected_total}, found {len(rebuilt_rows)}"
        )

    output_rows = list(preserved_rows)
    cursor = 0
    for block_id in TARGET_BLOCKS:
        count = TARGET_COUNTS[block_id]
        for row in rebuilt_rows[cursor:cursor + count]:
            output_rows.append([block_id, row[1], row[2], row[3]])
        cursor += count

    return output_rows


def main() -> int:
    output_rows = rebuild_rows()
    with WORDS_SEED_PATH.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter=";", lineterminator="\n")
        writer.writerow(["block", "russian", "english", "examples"])
        writer.writerows(output_rows)

    print(f"Rebuilt {TARGET_BLOCKS} in {WORDS_SEED_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
