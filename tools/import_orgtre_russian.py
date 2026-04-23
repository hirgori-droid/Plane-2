#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_INPUT = ROOT / 'tmp' / '1grams_russian.csv'
DEFAULT_OUTPUT = ROOT / 'app' / 'src' / 'main' / 'assets' / 'words_seed.csv'
BLOCK_RANGES = {
    'core_3000': (1001, 4000),
    'extended_6000': (4001, 10000),
}


def sanitize(text: str) -> str:
    return ' '.join(text.replace(';', ',').split())


def load_existing_rows(path: Path) -> list[list[str]]:
    rows: list[list[str]] = []
    with path.open(encoding='utf-8', newline='') as handle:
        reader = csv.reader(handle, delimiter=';')
        header = next(reader)
        if header not in (
            ['block', 'russian', 'english'],
            ['block', 'russian', 'english', 'examples'],
        ):
            raise ValueError(f'Unexpected seed header: {header!r}')
        for row in reader:
            if len(row) not in (3, 4):
                raise ValueError(f'Unexpected seed row: {row!r}')
            if len(row) == 3:
                rows.append([row[0], row[1], row[2], ''])
            else:
                rows.append(row)
    return rows


def iter_import_rows(path: Path) -> Iterable[list[str]]:
    with path.open(encoding='utf-8', newline='') as handle:
        reader = csv.DictReader(handle)
        required = {'ngram', 'en'}
        if not required.issubset(reader.fieldnames or set()):
            raise ValueError(
                'Expected orgtre CSV columns including at least '
                f"{sorted(required)!r}, found {reader.fieldnames!r}"
            )
        for rank, row in enumerate(reader, start=1):
            russian = sanitize(row['ngram'])
            english = sanitize(row['en'])
            if not russian or not english:
                continue
            block_id = None
            for candidate, (start, end) in BLOCK_RANGES.items():
                if start <= rank <= end:
                    block_id = candidate
                    break
            if block_id is None:
                continue
            yield [block_id, russian, english, '']


def main() -> int:
    parser = argparse.ArgumentParser(
        description='Append core_3000 and extended_6000 rows from orgtre/google-books-ngram-frequency Russian 1-grams.'
    )
    parser.add_argument('--input', type=Path, default=DEFAULT_INPUT, help='Path to 1grams_russian.csv')
    parser.add_argument('--output', type=Path, default=DEFAULT_OUTPUT, help='Path to words_seed.csv')
    args = parser.parse_args()

    if not args.input.exists():
        raise SystemExit(f'Input file not found: {args.input}')
    if not args.output.exists():
        raise SystemExit(f'Output file not found: {args.output}')

    existing_rows = load_existing_rows(args.output)
    preserved_rows = [row for row in existing_rows if row[0] not in BLOCK_RANGES]
    imported_rows = list(iter_import_rows(args.input))

    counts = {block_id: 0 for block_id in BLOCK_RANGES}
    for block_id, *_ in imported_rows:
        counts[block_id] += 1

    for block_id, (start, end) in BLOCK_RANGES.items():
        expected = end - start + 1
        actual = counts[block_id]
        if actual != expected:
            raise SystemExit(
                f'Imported block {block_id} expected {expected} rows from ranks {start}-{end}, found {actual}'
            )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open('w', encoding='utf-8', newline='') as handle:
        writer = csv.writer(handle, delimiter=';')
        writer.writerow(['block', 'russian', 'english', 'examples'])
        writer.writerows(preserved_rows)
        writer.writerows(imported_rows)

    print(f'Preserved {len(preserved_rows)} existing row(s) outside imported blocks.')
    for block_id in BLOCK_RANGES:
        print(f'Imported {counts[block_id]} row(s) into {block_id}.')
    print(f'Wrote {1 + len(preserved_rows) + len(imported_rows)} CSV line(s) to {args.output}.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
