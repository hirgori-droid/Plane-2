# Russian Trainer

Android word trainer built with **Kotlin + Jetpack Compose + Room**.

## Toolchain
- Recommended Java: **21.0.2** (the repo now pins this in `.tool-versions`).
- Recommended Gradle: **8.14.3**.

## Local-only
- No account system.
- No cloud sync.
- No server dependency for gameplay, learned-word filtering, or statistics.
- Round history and progress are stored only in the local Room database on the device.

## Implemented
- 30-word rounds.
- 6 answer choices for every Russian word.
- Automatic per-word streak tracking.
- Words are marked as learned after **5 correct answers in a row**.
- End-of-round statistics with encouragement.
- Review list of newly learned words where the learner can uncheck any words they do not feel confident about yet.
- In-app language switch between **Russian** and **English**.
- Local history of recent rounds and a reset action for wiping all on-device progress.
- A local word-library screen with search, learned/learning filters, and manual return of a word back into active practice.
- Offline local database seeded from `app/src/main/assets/words_seed.csv`, with per-block seeding so future block rows can be added without changing repository logic.
- Block metadata for three frequency ranges: `beginner_1000` (`1-1000`), `core_3000` (`1001-4000`), and `extended_6000` (`4001-10000`).

## Current seed list
The bundled CSV now includes all configured blocks:
- `beginner_1000` → **1000** rows
- `core_3000` → **3000** rows
- `extended_6000` → **6000** rows

Total bundled dictionary size: **10000 Russian-English pairs**.

The repository seeding logic remains block-aware: if a block count differs from the CSV, it is replaced during seeding, and missing blocks are inserted automatically.

## Local validation
Run the local validation script without Android SDK:

```bash
python tools/validate_project.py
```

This checks:
- CSV header / row integrity / duplicate headwords by block.
- Exact size of any recognized bundled block that is present in the CSV.
- String-key parity between `values/strings.xml` and `values-ru/strings.xml`.
- Absence of `android.permission.INTERNET` in the manifest.

## Importing the larger Russian blocks
The repo now includes a helper for the selected source **`orgtre/google-books-ngram-frequency`**. Download `ngrams/1grams_russian.csv` from that repository, place it at `tmp/1grams_russian.csv` (or pass `--input`), then run:

```bash
python tools/import_orgtre_russian.py
python tools/validate_project.py
```

The importer preserves the bundled `beginner_1000` rows, replaces any existing `core_3000` / `extended_6000` rows, and maps source ranks as follows:
- `core_3000` → source rows **1001-4000**
- `extended_6000` → source rows **4001-10000**

## Recommended next steps
1. Fetch `1grams_russian.csv` from `orgtre/google-books-ngram-frequency` and run `python tools/import_orgtre_russian.py`.
2. Open the project in Android Studio with Android SDK installed and run an emulator/device smoke test.
3. Add JVM/Android tests for round generation, per-block filtering, and incremental block seeding once the Android toolchain is available.

## Main files
- `app/src/main/java/com/example/russiantrainer/MainActivity.kt`
- `app/src/main/java/com/example/russiantrainer/ui/TrainerApp.kt`
- `app/src/main/java/com/example/russiantrainer/ui/TrainerViewModel.kt`
- `app/src/main/java/com/example/russiantrainer/ui/VocabularyBlock.kt`
- `app/src/main/java/com/example/russiantrainer/data/WordRepository.kt`
- `app/src/main/assets/words_seed.csv`
