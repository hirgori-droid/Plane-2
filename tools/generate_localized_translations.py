#!/usr/bin/env python3
from __future__ import annotations

import csv
import re
import sys
import time
import unicodedata
from collections import OrderedDict
from pathlib import Path

from deep_translator import GoogleTranslator
from deep_translator.exceptions import TranslationNotFound

ROOT = Path(__file__).resolve().parent.parent
SOURCE_CSVS = [
    ROOT / "app/src/main/assets/words_seed.csv",
    ROOT / "app/src/main/assets/theme_blocks_seed.csv",
]
OUTPUT_CSV = ROOT / "app/src/main/assets/localized_translations.csv"
CACHE_DIR = ROOT / "tmp" / "translation_cache"
LIST_DELIMITER = " | "
LANGUAGES = OrderedDict(
    [
        ("fr", "French"),
        ("es", "Spanish"),
        ("pt", "Portuguese"),
        ("ar", "Arabic"),
        ("zh-CN", "Chinese (Simplified)"),
    ]
)
MAX_BATCH_ITEMS = 40
MAX_BATCH_CHARS = 3000
RUSSIAN_DIRECT_BLOCKS = {"core_3000", "extended_6000"}
INFINITIVE_PATTERN = re.compile(r"(ться|тись|чься|ть|ти|чь)$")
CYRILLIC_PATTERN = re.compile(r"[А-Яа-яЁё]")
GRAMMAR_SUFFIX_PATTERN = re.compile(r"\s+\((м\.р\.|ж\.р\.|с\.р\.)\)$")
MANUAL_LOCALIZED_OVERRIDES: dict[str, dict[str, str]] = {
    "проведения": {
        "fr": "realisation",
        "es": "realizacion",
        "pt": "realizacao",
        "ar": "إجراء",
        "zh-CN": "进行",
    },
    "носить": {
        "fr": "porter",
        "es": "llevar",
        "pt": "usar",
        "ar": "ارتداء",
        "zh-CN": "穿",
    },
    "носят": {
        "fr": "portent",
        "es": "llevan",
        "pt": "usam",
        "ar": "يرتدون",
        "zh-CN": "穿着",
    },
}


def load_rows() -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    for path in SOURCE_CSVS:
        with path.open("r", encoding="utf-8", newline="") as handle:
            reader = csv.reader(handle, delimiter=";")
            header = next(reader, None)
            if header in (["block", "russian", "english"], ["block", "russian", "english", "examples"]):
                for row in reader:
                    if len(row) < 3:
                        continue
                    block, russian, english = row[:3]
                    block = block.strip()
                    russian = russian.strip()
                    english = english.strip()
                    use_russian_source = block in RUSSIAN_DIRECT_BLOCKS or looks_like_infinitive(russian)
                    rows.append(
                        {
                            "block": block,
                            "russian": russian,
                            "translations_en": english,
                            "translation_source_mode": "ru" if use_russian_source else "en",
                        }
                    )
            elif header == ["block", "russian", "transcription", "english", "translations", "examples"]:
                for block, russian, _transcription, english, translations, _examples in reader:
                    primary_translation = split_items(translations.strip()).pop(0) if translations.strip() else english.strip()
                    russian = russian.strip()
                    rows.append(
                        {
                            "block": block.strip(),
                            "russian": russian,
                            "translations_en": primary_translation,
                            "translation_source_mode": "ru" if looks_like_infinitive(russian) else "en",
                        }
                    )
            else:
                raise ValueError(f"Unexpected header in {path.name}: {header!r}")
    return rows


def split_items(value: str) -> list[str]:
    return [item.strip() for item in value.split(LIST_DELIMITER) if item.strip()]


def strip_grammar_suffix(word: str) -> str:
    return GRAMMAR_SUFFIX_PATTERN.sub("", word.strip())


def strip_accents(text: str) -> str:
    return "".join(
        ch for ch in unicodedata.normalize("NFD", text)
        if unicodedata.category(ch) != "Mn"
    )


def looks_like_infinitive(word: str) -> bool:
    return bool(INFINITIVE_PATTERN.search(strip_grammar_suffix(word).lower()))


def build_english_source_items(row: dict[str, str]) -> list[str]:
    items = split_items(row["translations_en"])
    if row["translation_source_mode"] == "ru" and looks_like_infinitive(row["russian"]):
        normalized_items = []
        for item in items:
            normalized = " ".join(item.split()).strip()
            if normalized and not normalized.lower().startswith("to "):
                normalized = f"to {normalized}"
            normalized_items.append(normalized)
        return normalized_items
    return items


def is_suspicious_localized_value(language: str, source_russian: str, translated: str) -> bool:
    normalized = translated.strip()
    if not normalized:
        return True
    if CYRILLIC_PATTERN.search(normalized):
        return True
    if source_russian == "носить" and language in {"es", "pt", "zh-CN"} and normalized in {"desgaste", "磨损"}:
        return True
    if source_russian == "носят" and language in {"es", "pt"} and normalized == "desgaste":
        return True
    return False


def build_unique_terms(rows: list[dict[str, str]]) -> list[tuple[str, str]]:
    seen: OrderedDict[tuple[str, str], None] = OrderedDict()
    for row in rows:
        if row["translation_source_mode"] == "ru":
            seen.setdefault(("ru", strip_accents(strip_grammar_suffix(row["russian"]))), None)
        for item in build_english_source_items(row):
            seen.setdefault(("en", item), None)
    return list(seen.keys())


def load_cache(target_language: str, source_language: str) -> dict[str, str]:
    path = CACHE_DIR / f"{target_language}_{source_language}.tsv"
    cache: dict[str, str] = {}
    if not path.exists():
        return cache
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.reader(handle, delimiter="\t")
        for source, translated in reader:
            cache[source] = translated
    return cache


def save_cache(target_language: str, source_language: str, cache: dict[str, str]) -> None:
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    path = CACHE_DIR / f"{target_language}_{source_language}.tsv"
    with path.open("w", encoding="utf-8", newline="") as handle:
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


def translate_terms(target_language: str, source_language: str, terms: list[str]) -> dict[str, str]:
    cache = load_cache(target_language, source_language)
    missing = [term for term in terms if term not in cache]
    if not missing:
        return cache

    translator = GoogleTranslator(source=source_language, target=target_language)
    all_batches = batches(missing)
    print(
        f"{target_language} from {source_language}: translating {len(missing)} missing phrase(s) "
        f"in {len(all_batches)} batch(es)"
    )
    for index, batch in enumerate(all_batches, start=1):
        try:
            translated = translator.translate(build_batch_payload(batch))
            parts = parse_batch_payload(translated, len(batch))
        except TranslationNotFound:
            parts = []

        if len(parts) == len(batch):
            for source, target in zip(batch, parts):
                cache[source] = " ".join(target.split())
        else:
            print(
                f"{target_language} from {source_language}: batch {index}/{len(all_batches)} returned {len(parts)} part(s), "
                f"falling back to per-item translation for {len(batch)} phrase(s)"
            )
            for source in batch:
                cache[source] = " ".join(translator.translate(source).split())
        if index % 10 == 0 or index == len(all_batches):
            save_cache(target_language, source_language, cache)
            print(f"{target_language} from {source_language}: saved progress {index}/{len(all_batches)} batches")
        time.sleep(0.2)
    return cache


def build_output(
    rows: list[dict[str, str]],
    caches: dict[tuple[str, str], dict[str, str]]
) -> list[list[str]]:
    output: list[list[str]] = []
    for row in rows:
        localized = []
        for language in LANGUAGES:
            english_source_items = build_english_source_items(row)
            russian_source = strip_grammar_suffix(row["russian"])
            russian_translation_source = strip_accents(russian_source)
            if row["translation_source_mode"] == "ru":
                translated_items = [caches[(language, "ru")].get(russian_translation_source) or russian_source]
                if any(is_suspicious_localized_value(language, russian_source, item) for item in translated_items):
                    translated_items = [caches[(language, "en")].get(item) or item for item in english_source_items]
            else:
                translated_items = [caches[(language, "en")].get(item) or item for item in english_source_items]

            manual_override = MANUAL_LOCALIZED_OVERRIDES.get(russian_source, {}).get(language)
            if manual_override:
                translated_items = [manual_override]
            localized.append(LIST_DELIMITER.join(translated_items))
        output.append(
            [
                row["block"],
                row["russian"],
                localized[0],
                localized[1],
                localized[2],
                localized[3],
                localized[4],
            ]
        )
    return output


def main() -> int:
    rows = load_rows()
    unique_terms = build_unique_terms(rows)
    print(f"Loaded {len(rows)} rows with {len(unique_terms)} unique translation source item(s)")

    terms_by_source_language: dict[str, list[str]] = {"en": [], "ru": []}
    for source_language, term in unique_terms:
        terms_by_source_language[source_language].append(term)

    caches = {
        (language, source_language): translate_terms(language, source_language, terms)
        for language in LANGUAGES
        for source_language, terms in terms_by_source_language.items()
        if terms
    }
    output_rows = build_output(rows, caches)

    with OUTPUT_CSV.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter=";", lineterminator="\n")
        writer.writerow(["block", "russian", "fr", "es", "pt", "ar", "zh"])
        writer.writerows(output_rows)

    print(f"Wrote {1 + len(output_rows)} line(s) to {OUTPUT_CSV}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
