#!/usr/bin/env python3
from __future__ import annotations

import csv
import re
import sys
import time
from pathlib import Path

from deep_translator import GoogleTranslator

ROOT = Path(__file__).resolve().parent.parent
WORDS_SEED_PATH = ROOT / "app/src/main/assets/words_seed.csv"
CACHE_PATH = ROOT / "tmp" / "translation_cache" / "en_ru_frequency.tsv"
TARGET_BLOCKS = {"core_3000", "extended_6000"}
MAX_BATCH_ITEMS = 40
MAX_BATCH_CHARS = 3000
INFINITIVE_PATTERN = re.compile(r"(ться|тись|чься|ть|ти|чь)$")
GRAMMAR_SUFFIX_PATTERN = re.compile(r"\s+\((м\.р\.|ж\.р\.|с\.р\.)\)$")


def load_rows() -> list[list[str]]:
    with WORDS_SEED_PATH.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.reader(handle, delimiter=";")
        header = next(reader, None)
        if header not in (
            ["block", "russian", "english"],
            ["block", "russian", "english", "examples"],
        ):
            raise ValueError(f"Unexpected header in {WORDS_SEED_PATH.name}: {header!r}")
        rows = []
        for row in reader:
            if len(row) == 3:
                rows.append([row[0], row[1], row[2], ""])
            elif len(row) == 4:
                rows.append(row)
            else:
                raise ValueError(f"Unexpected row in {WORDS_SEED_PATH.name}: {row!r}")
        return rows


def load_cache() -> dict[str, str]:
    cache: dict[str, str] = {}
    if not CACHE_PATH.exists():
        return cache
    with CACHE_PATH.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.reader(handle, delimiter="\t")
        for source, translated in reader:
            cache[source] = translated
    return cache


def save_cache(cache: dict[str, str]) -> None:
    CACHE_PATH.parent.mkdir(parents=True, exist_ok=True)
    with CACHE_PATH.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        for source, translated in cache.items():
            writer.writerow([source, translated])


def batches(items: list[str]) -> list[list[str]]:
    current: list[str] = []
    current_chars = 0
    result: list[list[str]] = []
    for item in items:
        addition = len(item) + 16
        if current and (len(current) >= MAX_BATCH_ITEMS or current_chars + addition > MAX_BATCH_CHARS):
            result.append(current)
            current = []
            current_chars = 0
        current.append(item)
        current_chars += addition
    if current:
        result.append(current)
    return result


def build_batch_payload(batch: list[str]) -> str:
    return "\n".join(f"[[{index}]] {text}" for index, text in enumerate(batch))


def parse_batch_payload(translated: str, expected_size: int) -> list[str]:
    marker_pattern = re.compile(r"\[\[(\d+)]]\s*")
    matches = list(marker_pattern.finditer(translated))
    if len(matches) != expected_size:
        return []

    items: list[str] = []
    for idx, match in enumerate(matches):
        if int(match.group(1)) != idx:
            return []
        start = match.end()
        end = matches[idx + 1].start() if idx + 1 < len(matches) else len(translated)
        items.append(" ".join(translated[start:end].split()))
    return items


def looks_like_infinitive(word: str) -> bool:
    return bool(INFINITIVE_PATTERN.search(word.strip().lower()))


def strip_grammar_suffix(word: str) -> str:
    return GRAMMAR_SUFFIX_PATTERN.sub("", word.strip())


def normalize_english_translation(russian: str, english: str) -> str:
    normalized = " ".join(english.split()).strip()
    if looks_like_infinitive(strip_grammar_suffix(russian)) and normalized and not normalized.lower().startswith("to "):
        normalized = f"to {normalized}"
    return normalized


def translate_russian_words(words: list[str]) -> dict[str, str]:
    cache = load_cache()
    source_terms = {word: strip_grammar_suffix(word) for word in words}
    missing = [source for source in source_terms.values() if source not in cache]
    if not missing:
        return cache

    translator = GoogleTranslator(source="ru", target="en")
    all_batches = batches(missing)
    print(f"Translating {len(missing)} missing Russian headword(s) to English in {len(all_batches)} batch(es)")
    for index, batch in enumerate(all_batches, start=1):
        translated = translator.translate(build_batch_payload(batch))
        parts = parse_batch_payload(translated, len(batch))
        if len(parts) == len(batch):
            for source, target in zip(batch, parts):
                cache[source] = normalize_english_translation(source, target)
        else:
            print(f"Batch {index}/{len(all_batches)} fallback to single-item translation")
            for source in batch:
                cache[source] = normalize_english_translation(source, translator.translate(source))
        if index % 10 == 0 or index == len(all_batches):
            save_cache(cache)
            print(f"Saved progress {index}/{len(all_batches)} batches")
        time.sleep(0.2)
    return cache


def main() -> int:
    rows = load_rows()
    source_words = sorted({row[1].strip() for row in rows if row[0].strip() in TARGET_BLOCKS and row[1].strip()})
    cache = translate_russian_words(source_words)

    updated_rows = []
    for block_id, russian, english, examples in rows:
        if block_id in TARGET_BLOCKS:
            english = cache.get(strip_grammar_suffix(russian), english).strip()
        updated_rows.append([block_id, russian, english, examples])

    with WORDS_SEED_PATH.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter=";", lineterminator="\n")
        writer.writerow(["block", "russian", "english", "examples"])
        writer.writerows(updated_rows)

    print(f"Updated English translations for frequency blocks in {WORDS_SEED_PATH}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
