#!/usr/bin/env python3
from __future__ import annotations

from collections import Counter
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
CSV_PATHS = [
    ROOT / 'app/src/main/assets/words_seed.csv',
    ROOT / 'app/src/main/assets/theme_blocks_seed.csv',
]
LOCALIZED_TRANSLATIONS_PATH = ROOT / 'app/src/main/assets/localized_translations.csv'
MANIFEST_PATH = ROOT / 'app/src/main/AndroidManifest.xml'
STRINGS_PATHS = [
    ROOT / 'app/src/main/res/values/strings.xml',
    ROOT / 'app/src/main/res/values-ru/strings.xml',
    ROOT / 'app/src/main/res/values-fr/strings.xml',
    ROOT / 'app/src/main/res/values-es/strings.xml',
    ROOT / 'app/src/main/res/values-pt/strings.xml',
    ROOT / 'app/src/main/res/values-ar/strings.xml',
    ROOT / 'app/src/main/res/values-zh/strings.xml',
]
EXPECTED_BLOCK_SIZES = {
    'beginner_1000': 1000,
    'core_3000': 3000,
    'extended_6000': 6000,
    'fruits_vegetables_120': 120,
    'shopping_150': 150,
    'professions_40': 100,
    'clothes_accessories_100': 100,
    'appearance_80': 80,
    'health_100': 100,
    'sports_60': 60,
    'home_apartment_120': 120,
    'confectionery_50': 50,
    'transport_80': 80,
    'outdoor_recreation_60': 60,
    'weather_50': 50,
    'human_body_60': 60,
    'common_verbs_100': 100,
    'frequent_verbs_180': 180,
    'cafe_50': 50,
    'animals_birds_80': 80,
    'time_calendar_80': 80,
    'adverbs_100': 100,
    'numbers_50': 50,
    'prepositions_35': 35,
    'pronouns_60': 60,
    'park_trees_40': 40,
}


def check_csv() -> list[str]:
    errors: list[str] = []
    block_pairs: dict[str, list[tuple[str, str]]] = {}
    found_any_csv = False
    for csv_path in CSV_PATHS:
        if not csv_path.exists():
            continue
        found_any_csv = True
        rows = csv_path.read_text(encoding='utf-8').strip().splitlines()
        if not rows:
            errors.append(f'{csv_path.name} is empty')
            continue
        if rows[0] not in {
            'block;russian;english',
            'block;russian;english;examples',
            'block;russian;transcription;english;translations;examples',
        }:
            errors.append(f'Unexpected CSV header in {csv_path.name}: {rows[0]!r}')
            continue

        is_rich_csv = rows[0] == 'block;russian;transcription;english;translations;examples'
        is_seed_with_examples = rows[0] == 'block;russian;english;examples'

        for index, row in enumerate(rows[1:], start=2):
            parts = row.split(';', 5 if is_rich_csv else 3 if is_seed_with_examples else 2)
            if is_rich_csv:
                if len(parts) != 6:
                    errors.append(f'{csv_path.name} line {index} must contain exactly 6 columns separated by semicolons')
                    continue
                block_id, russian, transcription, english, translations, examples = [part.strip() for part in parts]
                if not all((block_id, russian, transcription, english, translations, examples)):
                    errors.append(f'{csv_path.name} line {index} contains an empty column')
                    continue
            elif is_seed_with_examples:
                if len(parts) != 4:
                    errors.append(f'{csv_path.name} line {index} must contain exactly 4 columns separated by semicolons')
                    continue
                block_id, russian, english, _examples = [part.strip() for part in parts]
                if not all((block_id, russian, english)):
                    errors.append(f'{csv_path.name} line {index} contains an empty required column')
                    continue
            else:
                if len(parts) != 3:
                    errors.append(f'{csv_path.name} line {index} must contain exactly 3 columns separated by semicolons')
                    continue
                block_id, russian, english = [part.strip() for part in parts]
                if not all((block_id, russian, english)):
                    errors.append(f'{csv_path.name} line {index} contains an empty column')
                    continue
            block_pairs.setdefault(block_id, []).append((russian, english))

    if not found_any_csv:
        return ['No seed CSV files were found']

    total_rows = sum(len(items) for items in block_pairs.values())
    print(f'CSV OK: {total_rows} rows across {len(block_pairs)} block(s) from {len([p for p in CSV_PATHS if p.exists()])} file(s)')
    for block_id, pairs in sorted(block_pairs.items()):
        russian_counts = Counter(russian for russian, _ in pairs)
        duplicates = sorted(word for word, count in russian_counts.items() if count > 1)
        if duplicates:
            errors.append(f'Block {block_id} has duplicate Russian headwords: {duplicates[:10]}')
        print(f'Block {block_id}: {len(pairs)} rows, {len(set(pairs))} unique pairs')

    for block_id, expected_size in EXPECTED_BLOCK_SIZES.items():
        if block_id in block_pairs:
            actual_size = len(block_pairs[block_id])
            if actual_size != expected_size:
                errors.append(f'Block {block_id} must contain exactly {expected_size} rows, found {actual_size}')
        else:
            print(f'Block {block_id}: not bundled in CSV yet')
    return errors


def check_localized_translations() -> list[str]:
    if not LOCALIZED_TRANSLATIONS_PATH.exists():
        print('localized_translations.csv not found yet, skipping localized data validation')
        return []

    rows = LOCALIZED_TRANSLATIONS_PATH.read_text(encoding='utf-8').strip().splitlines()
    if not rows:
        return ['localized_translations.csv is empty']
    expected_header = 'block;russian;fr;es;pt;ar;zh'
    if rows[0] != expected_header:
        return [f'Unexpected CSV header in {LOCALIZED_TRANSLATIONS_PATH.name}: {rows[0]!r}']

    errors: list[str] = []
    for index, row in enumerate(rows[1:], start=2):
        parts = row.split(';', 6)
        if len(parts) != 7:
            errors.append(f'{LOCALIZED_TRANSLATIONS_PATH.name} line {index} must contain exactly 7 columns separated by semicolons')
            continue
        if not all(part.strip() for part in parts):
            errors.append(f'{LOCALIZED_TRANSLATIONS_PATH.name} line {index} contains an empty column')
    print(f'{LOCALIZED_TRANSLATIONS_PATH.name} OK: {max(len(rows) - 1, 0)} localized rows')
    return errors


def check_strings() -> list[str]:
    errors: list[str] = []
    parsed = []
    for path in STRINGS_PATHS:
        root = ET.parse(path).getroot()
        keys = [node.attrib['name'] for node in root.findall('string')]
        parsed.append((path.name, set(keys), len(keys)))
        print(f'{path.name} OK: {len(keys)} string keys')

    _, base_keys, _ = parsed[0]
    for name, keys, _ in parsed[1:]:
        missing = sorted(base_keys - keys)
        extra = sorted(keys - base_keys)
        if missing:
            errors.append(f'{name} is missing keys: {missing}')
        if extra:
            errors.append(f'{name} contains extra keys: {extra}')
    return errors


def check_manifest() -> list[str]:
    errors: list[str] = []
    text = MANIFEST_PATH.read_text(encoding='utf-8')
    if 'android.permission.INTERNET' in text:
        errors.append('Manifest requests INTERNET permission, but app should remain local-only')
    else:
        print('Manifest OK: INTERNET permission is absent')
    return errors


def main() -> int:
    all_errors: list[str] = []
    for check in (check_csv, check_localized_translations, check_strings, check_manifest):
        all_errors.extend(check())

    if all_errors:
        print('\nVALIDATION FAILED:')
        for error in all_errors:
            print(f'- {error}')
        return 1

    print('\nAll local validations passed.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
