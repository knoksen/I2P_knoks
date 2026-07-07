# Room Migration Contract

This document is the persistence contract for the Android real-alpha Room database.

Migration testing strengthens local persistence confidence only. It does not prove storage security, anonymity, encryption, or recovery from arbitrary corruption.

## Current Version

Current Room database: `no.knoksen.i2pbrowser.data.AppDatabase`

Current database version: `6`

Schema export path:

```text
app/schemas/no.knoksen.i2pbrowser.data.AppDatabase/
```

Committed schema files:

```text
4.json
5.json
6.json
```

## Schema Provenance

Historical schemas must not be regenerated from current entity definitions. They are committed evidence for old database shapes and should only change when the corresponding historical source is proven wrong.

| Schema | Database version | Provenance | Originating migration edge | Introduced into test baseline | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `4.json` | `4` | Generated from Git commit `5beea81` (`Refactor app into honest SAM-testable I2P MVP`). | `4 -> 5` | 2026-07-07 | Oldest currently supported migration origin. |
| `5.json` | `5` | Generated from Git commit `43cca41` (`Add Room migration for endpoint settings`). | `5 -> 6` | 2026-07-07 | Contains `app_settings` endpoint persistence. |
| `6.json` | `6` | Generated from the current version-6 source tree for this migration-test baseline. | Current schema target | 2026-07-07 | Includes `connect_identities`, unique fingerprint index, and `cloudSyncEnabled` default metadata aligned with `MIGRATION_5_6`. |

## Supported Migration Graph

Supported adjacent migrations:

```text
4 -> 5
5 -> 6
```

Supported historical origins to current:

```text
4 -> 5 -> 6
5 -> 6
6 current schema open, no migration
```

Versions below `4` are not supported migration origins in the current real-alpha source tree because there are no committed schemas or registered migration edges for them.

Downgrades are not supported. A database newer than the app must fail to open rather than fall back to destructive migration.

## Version Classification

| Version | Classification | Notes |
| :--- | :--- | :--- |
| `4` | Supported migration origin | Historical schema from commit `5beea81`; contains bookmarks, identities, secure messages, router logs, trusted keys, and contacts. |
| `5` | Supported migration origin | Adds `app_settings` endpoint persistence through `MIGRATION_4_5`. |
| `6` | Current version | Adds `connect_identities` and the unique fingerprint index through `MIGRATION_5_6`. |
| `<4` | Unsupported legacy or no longer reconstructable | No current migration edge or committed schema support. |
| `>6` | Unsupported downgrade/newer app state | Must not be opened destructively by this app version. |

Older development history briefly used destructive fallback before the supported migration graph was added. The current database builder does not use `fallbackToDestructiveMigration`, `fallbackToDestructiveMigrationFrom`, or `fallbackToDestructiveMigrationOnDowngrade`.

## Preservation Invariants

Migration tests use synthetic, test-only data. They must not use real I2P identities, private destinations, real endpoints owned by users, production keys, copied app databases, or secrets.

For version `4` origins, these records must survive supported migration paths:

- `bookmarks`
- `identities` with synthetic placeholder key material only
- `secure_messages` with synthetic lab payload/body fields only
- `router_logs`
- `trusted_keys`
- `contacts`

For `4 -> 5`, migration must create `app_settings` and insert the default local Android router endpoint:

```text
Local Android Router
127.0.0.1
7656 / 4444 / 7657
```

For version `5` origins, existing `app_settings` rows must survive unchanged at the SQL migration layer. Malformed endpoint values are handled after migration by the repository endpoint-load contract, which falls back to a bounded correction state instead of presenting invalid settings as restored user configuration.

For `5 -> 6`, migration must create `connect_identities`, keep cloud sync disabled by default, and enforce unique fingerprints. The table stores public identity material plus local private-material references; it does not create a private-key export or cloud-sync claim.

## Test Coverage

JVM guard:

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest --tests "*AppDatabaseMigrationTest"
```

Instrumentation migration execution:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=no.knoksen.i2pbrowser.AppDatabaseMigrationInstrumentedTest"
```

The instrumentation suite executes:

- `4 -> 5`
- `5 -> 6`
- `4 -> 6`
- malformed `5 -> 6` endpoint repository-load fallback
- current schema open
- unsupported newer-version/downgrade behavior

Instrumentation tests require an Android emulator or connected device. They do not require a live I2P router, public `.i2p` sites, external endpoints, or secrets.

## Future Schema Changes

A schema-changing PR must:

1. Increment `AppDatabase` version.
2. Add an explicit migration edge from the previous supported version.
3. Preserve data or document intentional transformation/loss.
4. Export only the new current schema.
5. Keep all historical schema files.
6. Add an adjacent migration test.
7. Add supported-origin-to-current coverage.
8. Update synthetic fixture data when new tables or columns matter.
9. Run the JVM schema guard and instrumentation migration suite.
10. Update `docs/ANDROID_REAL_ALPHA_TEST_MATRIX.md`.
11. Update `docs/CLAIMS_REGISTER.md` only when evidence supports the wording.
12. Document any intentional data loss.

Do not rewrite historical schema files to make a migration pass. Historical schemas are review evidence for real old-version databases.
