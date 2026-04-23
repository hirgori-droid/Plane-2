#!/usr/bin/env python3
from __future__ import annotations

import csv
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WORDS_SEED_PATH = ROOT / "app" / "src" / "main" / "assets" / "words_seed.csv"
THEME_BLOCKS_PATH = ROOT / "app" / "src" / "main" / "assets" / "theme_blocks_seed.csv"
BLOCK_ID = "frequent_verbs_180"

ORDERED_VERBS = [
    "сделать",
    "уходить",
    "верить",
    "касаться",
    "чувствовать",
    "ломать",
    "нести",
    "держать",
    "терять",
    "выигрывать",
    "проигрывать",
    "повторять",
    "учить",
    "выучить",
    "сравнивать",
    "объяснять",
    "показывать",
    "скрывать",
    "оставлять",
    "удалять",
    "подтверждать",
    "ошибаться",
    "закреплять",
    "ответить",
    "угадать",
    "существовать",
    "становиться",
    "казаться",
    "оказываться",
    "оставаться",
    "происходить",
    "использовать",
    "нуждаться",
    "следовать",
    "пытаться",
    "пробовать",
    "позволять",
    "позволить",
    "начинаться",
    "продолжать",
    "заканчиваться",
    "возникать",
    "приводить",
    "проводить",
    "вести",
    "приносить",
    "посылать",
    "полагать",
    "включать",
    "исключать",
    "требовать",
    "собирать",
    "собрать",
    "показываться",
    "содержать",
    "производить",
    "предлагать",
    "достигать",
    "достичь",
    "выбрать",
    "побеждать",
    "победить",
    "проиграть",
    "выполнять",
    "исполнять",
    "приготовить",
    "развивать",
    "развиваться",
    "сохранять",
    "сохранить",
    "защищать",
    "защитить",
    "напоминать",
    "напомнить",
    "определять",
    "определить",
    "описывать",
    "описать",
    "обсуждать",
    "обсудить",
    "представлять",
    "представить",
    "появляться",
    "появиться",
    "уезжать",
    "приезжать",
    "соглашаться",
    "согласиться",
    "отказываться",
    "отказаться",
    "поднимать",
    "поднять",
    "опускать",
    "опустить",
    "меняться",
    "изменять",
    "измениться",
    "влиять",
    "повлиять",
    "зависеть",
    "стоить",
    "означать",
    "принимать",
    "принять",
    "передавать",
    "передать",
    "получиться",
    "предпочитать",
    "переводить",
    "перевести",
    "замечать",
    "заметить",
    "обещать",
    "пообещать",
    "плавать",
    "летать",
    "обедать",
    "ужинать",
    "завтракать",
    "радоваться",
    "рассказывать",
    "рассказать",
    "обучать",
    "научить",
    "отдыхать",
    "отдохнуть",
    "посетить",
    "посещать",
    "делиться",
    "поделиться",
    "сопротивляться",
    "состоять",
    "принадлежать",
    "обладать",
    "выражать",
    "выразить",
    "называть",
    "назвать",
    "обращать",
    "обратить",
    "встречаться",
    "тратить",
    "потратить",
    "экономить",
    "поддерживать",
    "поддержать",
    "доверять",
    "управлять",
    "приглашать",
    "пригласить",
    "поздравлять",
    "поздравить",
    "предупреждать",
    "предупредить",
    "завершать",
    "завершить",
    "исправлять",
    "исправить",
    "починить",
    "ломаться",
    "расти",
    "вырастать",
    "повышать",
    "повысить",
    "уменьшать",
    "уменьшить",
    "добавлять",
    "добавить",
    "толкать",
    "толкнуть",
    "тянуть",
    "потянуть",
    "резать",
    "разрезать",
    "соединять",
    "соединить",
    "разделять",
    "разделить",
    "смешивать",
    "смешать",
]

ENGLISH_OVERRIDES = {
    "касаться": "to concern",
    "вести": "to lead",
    "полагать": "to suppose",
    "проводить": "to spend",
    "представлять": "to represent",
    "представить": "to present",
    "стоить": "to cost",
    "получиться": "to turn out",
    "обращать": "to turn",
    "обратить": "to pay attention",
}


def normalize(text: str) -> str:
    text = text.replace("ё", "е").replace("Ё", "Е")
    return "".join(
        char for char in unicodedata.normalize("NFD", text)
        if unicodedata.category(char) != "Mn"
    ).lower()


def fallback_transcription(word: str) -> str:
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
    }
    transliterated = "".join(mapping.get(char.lower(), char) for char in word)
    return f"/{transliterated}/"


def load_common_verbs() -> set[str]:
    result: set[str] = set()
    with THEME_BLOCKS_PATH.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.reader(handle, delimiter=";")
        next(reader)
        for row in reader:
            if row[0] == "common_verbs_100":
                result.add(normalize(row[1]))
    return result


def load_english_map() -> dict[str, str]:
    result: dict[str, str] = {}
    with WORDS_SEED_PATH.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.reader(handle, delimiter=";")
        next(reader)
        for _block_id, russian, english in reader:
            result.setdefault(normalize(russian), english.strip())
    return result


def build_rows() -> list[list[str]]:
    if len(ORDERED_VERBS) != 180:
        raise SystemExit(f"Expected 180 verbs, found {len(ORDERED_VERBS)}")

    common_verbs = load_common_verbs()
    english_map = load_english_map()
    rows: list[list[str]] = []
    seen: set[str] = set()

    for verb in ORDERED_VERBS:
        normalized = normalize(verb)
        if normalized in seen:
            raise SystemExit(f"Duplicate verb in ORDERED_VERBS: {verb}")
        if normalized in common_verbs:
            raise SystemExit(f"Verb already exists in common_verbs_100: {verb}")

        english = ENGLISH_OVERRIDES.get(verb) or english_map.get(normalized)
        if not english:
            raise SystemExit(f"Missing English translation for {verb}")
        english = english.strip()
        if not english.lower().startswith("to "):
            english = f"to {english}"

        rows.append(
            [
                BLOCK_ID,
                verb,
                fallback_transcription(verb),
                english,
                english,
                f'Я учу глагол "{verb}". | На уроке мы часто употребляем "{verb}".',
            ]
        )
        seen.add(normalized)

    return rows


def main() -> int:
    with THEME_BLOCKS_PATH.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.reader(handle, delimiter=";")
        rows = list(reader)

    header, data_rows = rows[0], rows[1:]
    if header != ["block", "russian", "transcription", "english", "translations", "examples"]:
        raise SystemExit(f"Unexpected header in {THEME_BLOCKS_PATH.name}: {header!r}")

    data_rows = [row for row in data_rows if row[0] != BLOCK_ID]
    data_rows.extend(build_rows())

    with THEME_BLOCKS_PATH.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter=";", lineterminator="\n")
        writer.writerow(header)
        writer.writerows(data_rows)

    print(f"Updated {BLOCK_ID} in {THEME_BLOCKS_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
