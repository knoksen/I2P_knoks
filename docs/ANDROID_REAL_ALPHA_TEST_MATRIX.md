# Android Real-Alpha Test Matrix

This matrix records repeatable evidence for the Android real-alpha flows that are currently present in the source tree.

It does not prove anonymity, privacy, production readiness, independent audit status, audited encrypted chat, VPN behavior, Tor routing, embedded-router behavior, or full browser isolation. Automated tests use deterministic local substitutes and must not depend on public I2P availability, live SAM bridges, public proxies, or third-party services.

## Status Vocabulary

| Status | Meaning |
| :--- | :--- |
| `AUTOMATED_PASS` | Covered by deterministic tests that run in the default JVM or scripted validation path. |
| `AUTOMATED_PARTIAL` | Some meaningful behavior is covered, but the full externally meaningful flow is not yet automated. |
| `MANUAL_PASS` | Covered only by documented manual validation. |
| `NOT_IMPLEMENTED` | The flow is not currently present as release-path behavior. |
| `BLOCKED` | The flow exists but needs a new seam, fixture, or environment before it can be automated safely. |
| `NOT_APPLICABLE` | The condition does not apply to the current real-alpha source tree. |

## Test Layers

| Layer | Use |
| :--- | :--- |
| Pure logic test | Kotlin/JVM test for validation, mapping, parser, sanitizer, or state logic. |
| Repository and persistence test | Room, DAO, repository, entity mapping, or migration-path evidence using isolated storage. |
| Protocol and parser contract test | SAM or HTTP proxy command/response behavior through parser contracts or deterministic fakes. |
| Android UI test | Robolectric/Compose or instrumentation test for principal UI representation. |
| Simulated integration test | Local fake, stub, or scripted substitute for a protocol or app dependency. |
| External-router test | Optional manual or non-default test requiring a running I2P or i2pd router. |
| Manual exploratory validation | Human checklist for behavior not yet stable or safe enough for default automation. |

## Summary Matrix

| Test ID | Core flow | Layer | Environment | Automated | Evidence | Current status | Release relevance |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `CORE-START-001` | Startup default ViewModel state | Android UI/state | Robolectric JVM | Yes | `I2PViewModelCoreFlowTest` | `AUTOMATED_PASS` | Confirms default release mode and unchecked readiness without implying connectivity. |
| `CORE-START-002` | Supported endpoint config restoration | Repository/persistence | JVM | Yes | `I2pEndpointConfigTest`, repository flow evidence | `AUTOMATED_PARTIAL` | Confirms entity mapping; full process recreation remains manual. |
| `CORE-START-003` | Malformed persisted values | Repository/persistence | JVM | Yes | `I2pEndpointConfigTest` | `AUTOMATED_PASS` | Malformed persisted endpoint values fall back to a documented correction state instead of being reported as a successful configuration. |
| `CORE-START-004` | Repository initialization failure | Android lifecycle/state | Robolectric JVM fake repository | Yes | `I2PViewModelCoreFlowTest` | `AUTOMATED_PASS` | Repository construction failure is represented as bounded state without raw database details. |
| `CORE-MODE-001` | Default mode and release tab boundary | Pure logic | JVM/Robolectric | Yes | `AppExperienceModeTest`, `I2PViewModelCoreFlowTest` | `AUTOMATED_PASS` | Confirms `RELEASE_REAL` does not expose lab tabs by default. |
| `CORE-MODE-002` | Lab labels stay explicit | Android resources | Robolectric JVM | Yes | `AppExperienceModeTest` | `AUTOMATED_PASS` | Confirms lab-only tab labels start with `LAB`. |
| `CORE-MODE-003` | Mode transition controls | UI/manual | N/A | No | Source inspection | `NOT_IMPLEMENTED` | No user-facing mode switch is currently implemented. |
| `CORE-ENDPOINT-001` | Endpoint validation policy | Pure logic | JVM | Yes | `I2pEndpointConfigTest` | `AUTOMATED_PASS` | Validates host and port policy without broadening accepted endpoints. |
| `CORE-ENDPOINT-002` | Endpoint persistence mapping | Repository/persistence | JVM | Yes | `I2pEndpointConfigTest` | `AUTOMATED_PASS` | Confirms saved endpoint fields round-trip through the entity mapping. |
| `CORE-ENDPOINT-003` | Endpoint form validation UI | Android UI | Robolectric Compose JVM | Yes | `AndroidRealAlphaUiCoreFlowTest` | `AUTOMATED_PASS` | Confirms invalid host is rejected visibly without saving an unintended endpoint. |
| `CORE-DIAG-001` | Diagnostic summary mapping | Pure logic | JVM | Yes | `I2pDiagnosticsClientTest` | `AUTOMATED_PASS` | Confirms ready, offline, SAM-disabled, HTTP-disabled, and partial mappings. |
| `CORE-DIAG-002` | Diagnostic probe injection | Simulated integration | JVM fake probe | Yes | `I2pDiagnosticsClientTest` | `AUTOMATED_PASS` | Confirms default CI can test diagnostics without a live router. |
| `CORE-DIAG-003` | Repeated diagnostics replace stale result | Android state | Robolectric JVM fake diagnostics | Yes | `I2PViewModelCoreFlowTest` | `AUTOMATED_PASS` | Confirms stale diagnostic state is replaced and running state resets. |
| `CORE-DIAG-004` | Diagnostic transport contract and connection timeout | Protocol fixture | JVM fake transport | Yes | `I2pDiagnosticsClientTest` | `AUTOMATED_PASS` | Distinguishes connection timeout from general router unavailability without requiring a live router. |
| `CORE-DIAG-005` | Response timeout category | Protocol fixture | JVM fake transport | Yes | `I2pDiagnosticsClientTest` | `AUTOMATED_PASS` | Confirms response timeout maps to a controlled diagnostic category and message. |
| `CORE-DIAG-006` | Cooperative cancellation and cleanup | Protocol fixture | JVM fake transport | Yes | `I2pDiagnosticsClientTest` | `AUTOMATED_PASS` | Confirms cancellation propagates and fixture resources close. |
| `CORE-DIAG-007` | Latest diagnostic request wins | Android state | Robolectric JVM fake diagnostics | Yes | `I2PViewModelCoreFlowTest` | `AUTOMATED_PASS` | Confirms older requests cannot replace newer diagnostic state. |
| `CORE-SAM-001` | SAM command generation and reply parsing | Protocol/parser | JVM fake connection | Yes | `SamBridgeClientTest` | `AUTOMATED_PASS` | Confirms HELLO, DEST GENERATE, SESSION CREATE, and parser behavior. |
| `CORE-SAM-002` | SAM session lifecycle | Protocol/state | JVM fake connection | Yes | `SamSessionManagerTest` | `AUTOMATED_PASS` | Confirms success, error, timeout, cancellation, retry, close, and repeated attempts. |
| `CORE-SAM-003` | SAM socket-level behavior | Protocol fixture | Optional local loopback/manual | Partial | Parser/session fakes | `AUTOMATED_PARTIAL` | Automated tests do not prove live router or anonymous communication behavior. |
| `CORE-PROXY-001` | `.i2p` URL handling and rejection of non-I2P URLs | Protocol/parser | JVM fake transport | Yes | `I2pHttpClientTest` | `AUTOMATED_PASS` | Confirms URL classification without contacting live `.i2p` sites. |
| `CORE-PROXY-002` | HTTP proxy result mapping | Protocol fixture | JVM fake transport | Yes | `I2pHttpClientTest` | `AUTOMATED_PASS` | Confirms success, redirect, HTTP error, unsupported type, unavailable proxy, timeout, and host lookup failure. |
| `CORE-PROXY-003` | Bounded and sanitized preview | Parser/sanitizer | JVM | Yes | `I2pHttpClientTest` | `AUTOMATED_PASS` | Confirms preview cap and script/style removal for supported preview content. |
| `CORE-PROXY-004` | Real external HTTP proxy behavior | External-router test | Optional local I2P/i2pd | No | Manual smoke docs | `MANUAL_PASS` when performed | Not required in default CI; does not prove anonymity. |
| `CORE-IDENTITY-001` | Public-only identity export/import | Pure logic | JVM | Yes | `ConnectIdentityModelTest` | `AUTOMATED_PASS` | Confirms public material export/import and private-material rejection. |
| `CORE-IDENTITY-002` | Duplicate public identity import through DAO | Repository/persistence | JVM/Room needed | No | Matrix gap | `BLOCKED` | Needs a focused repository/DAO duplicate-import test before stronger claim wording. |
| `CORE-LOG-001` | Sensitive log redaction fixtures | Pure logic | JVM | Yes | `LogSanitizerTest` | `AUTOMATED_PASS` | Confirms known sensitive key-value fields are redacted. |
| `CORE-LOG-002` | Repository log insertion uses sanitizer | Repository/persistence | JVM/manual | Partial | `I2PRepository.addLog` source, sanitizer tests | `AUTOMATED_PARTIAL` | Full DAO-backed repository log test remains a follow-up. |
| `CORE-MIGRATION-001` | Room schema export and migration graph guard | Repository/persistence | JVM | Yes | `AppDatabaseMigrationTest`, `app/schemas/no.knoksen.i2pbrowser.data.AppDatabase/` | `AUTOMATED_PASS` | Confirms current version 6, supported graph 4->5->6, committed schemas, SQL shape, and no current destructive fallback. |
| `CORE-MIGRATION-002` | Adjacent Room migration execution | Repository/persistence | Android instrumentation | Partial | `AppDatabaseMigrationInstrumentedTest`, CI emulator job | `AUTOMATED_PARTIAL` | Executes 4->5 and 5->6 with synthetic fixtures when emulator/device instrumentation runs. |
| `CORE-MIGRATION-003` | Historical origins migrate to current | Repository/persistence | Android instrumentation | Partial | `AppDatabaseMigrationInstrumentedTest`, CI emulator job | `AUTOMATED_PARTIAL` | Executes 4->6 and 5->6 through the registered migration chain when emulator/device instrumentation runs. |
| `CORE-MIGRATION-004` | Migration data-preservation assertions | Repository/persistence | Android instrumentation | Partial | `AppDatabaseMigrationInstrumentedTest`, synthetic fixture records | `AUTOMATED_PARTIAL` | Checks representative bookmarks, identities, lab messages, logs, trusted keys, contacts, endpoint defaults, and Connect identity defaults. |
| `CORE-MIGRATION-005` | Repository open after migration | Repository/persistence | Android instrumentation | Partial | `AppDatabaseMigrationInstrumentedTest`, `I2PRepository` | `AUTOMATED_PARTIAL` | Opens migrated DB with current DAOs and checks malformed migrated endpoint values enter a bounded fallback state. |
| `CORE-MIGRATION-006` | Unsupported downgrade/newer database behavior | Repository/persistence | Android instrumentation | Partial | `AppDatabaseMigrationInstrumentedTest` | `AUTOMATED_PARTIAL` | Verifies newer database versions are not opened through destructive downgrade fallback when instrumentation runs. |
| `CORE-UI-001` | Principal real-alpha UI boundaries | Android UI | Robolectric Compose JVM | Yes | `AndroidRealAlphaUiCoreFlowTest` | `AUTOMATED_PASS` | Confirms visible real-alpha status and limitations remain present. |
| `CORE-UI-002` | Full application launch and screen recreation | Android instrumentation | Emulator/manual | No | Existing example instrumentation only | `AUTOMATED_PARTIAL` | Default CI does not run emulator tests yet. |

## P0 Flow Details

### `CORE-START-001` Startup Default ViewModel State

- Preconditions: clean test process, no explicit endpoint override.
- Action: instantiate `I2PViewModel` under Robolectric.
- Expected result: `RELEASE_REAL`, local endpoint defaults, unchecked readiness, offline router state, no diagnostic result.
- Expected failure behavior: test fails if startup implies connectivity or lab mode by default.
- Test layer: Android state test.
- Fixture: Robolectric application context; no external router.
- Source evidence: `I2PViewModel`, `RealAlphaStatus`.
- Limitations: does not prove activity process recreation.
- CI status: runs through `testDebugUnitTest`.
- Claim relationship: `CLAIM-001`, `CLAIM-007`.

### `CORE-MODE-001` Mode Boundaries

- Preconditions: selected `AppExperienceMode` and SAM identity flag.
- Action: call `visibleAppTabs`.
- Expected result: release mode shows Router and Browser by default; lab-only tabs remain hidden unless lab mode is active; Identity requires a real SAM identity in release mode.
- Expected failure behavior: test fails if VPN/VPS or lab chat appears in release mode.
- Test layer: pure logic plus Android resource label check.
- Fixture: none.
- Source evidence: `MainActivity.visibleAppTabs`, `strings.xml`.
- Limitations: no user-facing mode transition control currently exists.
- CI status: runs through `testDebugUnitTest`.
- Claim relationship: `CLAIM-007`.

### `CORE-ENDPOINT-001` Endpoint Configuration

- Preconditions: synthetic host and port values only.
- Action: validate local, LAN, DNS, malformed, whitespace, and invalid port inputs; map config to diagnostics, SAM, HTTP client, and persistence entity.
- Expected result: valid values pass; malformed and out-of-range values fail with explicit errors; persistence round-trip preserves fields.
- Expected failure behavior: no silent trimming into a different endpoint and no fallback to unintended host.
- Test layer: pure logic and persistence mapping.
- Fixture: synthetic endpoint values such as `10.0.2.2` and `127.0.0.1`.
- Source evidence: `I2pEndpointConfig`, `AppSettingsEntity` mapping.
- Limitations: does not prove external router reachability.
- CI status: runs through `testDebugUnitTest`.
- Claim relationship: `CLAIM-003`.

### `CORE-DIAG-001` Router Diagnostics

- Preconditions: fake port-probe results, fake diagnostic transport, or fake diagnostics client.
- Action: run diagnostic mapping, timeout classification, cancellation, and repeated ViewModel diagnostics.
- Expected result: ready, offline, SAM-disabled, HTTP-proxy-disabled, partial, timeout, and invalid-config states map to documented actions; latest request wins.
- Expected failure behavior: stale result must not remain current after a later diagnostic run, and cancellation must not become a generic failure.
- Test layer: pure logic, simulated integration, and Android state.
- Fixture: injected fake port probe, fake transport, or scripted diagnostics client.
- Source evidence: `I2pDiagnosticsClient`, `RouterDiagnosticTransport`, `DiagnosticPolicy`, `I2PViewModel`.
- Limitations: production socket probes still indicate local service reachability only; timeout does not prove the router is offline.
- CI status: runs through `testDebugUnitTest`.
- Claim relationship: `CLAIM-004`.

### `CORE-SAM-001` SAM Behavior

- Preconditions: deterministic fake SAM connection.
- Action: send HELLO, DEST GENERATE, SESSION CREATE, optional fallback, name lookup, timeout, cancellation, close, repeated connect attempts.
- Expected result: parser extracts expected fields, session reaches `READY` only after required steps, failures close sockets and remain explicit.
- Expected failure behavior: malformed or error replies fail without treating communication as ready.
- Test layer: protocol/parser contract and simulated integration.
- Fixture: in-memory fake `SamConnection`.
- Source evidence: `SamBridgeClient`, `SamSessionManager`.
- Limitations: does not prove live I2P routing, anonymity, or message delivery.
- CI status: runs through `testDebugUnitTest`.
- Claim relationship: `CLAIM-005`.

### `CORE-PROXY-001` `.i2p` HTTP Proxy Inspection

- Preconditions: deterministic fake HTTP transport.
- Action: fetch `.i2p`, non-`.i2p`, malformed, redirect, HTTP error, unsupported content type, timeout, unknown-host, and unavailable-proxy cases.
- Expected result: only `.i2p` HTTP(S) URLs use proxy path; status and preview states are bounded and explicit.
- Expected failure behavior: invalid or non-I2P inputs do not call transport.
- Test layer: protocol/parser contract and simulated integration.
- Fixture: injected `I2pHttpTransport`.
- Source evidence: `I2pHttpClient`, `SafePreviewSanitizer`.
- Limitations: no default CI access to live `.i2p` sites.
- CI status: runs through `testDebugUnitTest`.
- Claim relationship: `CLAIM-006`.

### `CORE-IDENTITY-001` Public-Only Identity Helpers

- Preconditions: synthetic public destination and public app key.
- Action: create local identity, encode public export, decode import, reject private fields, reject tampered fingerprint.
- Expected result: export excludes private material, import creates public-only identity with warnings and cloud sync disabled.
- Expected failure behavior: private-material fields and tampered fingerprints fail.
- Test layer: pure logic.
- Fixture: synthetic values only.
- Source evidence: `ConnectIdentityModel`.
- Limitations: duplicate DAO insertion behavior is not yet covered.
- CI status: runs through `testDebugUnitTest`.
- Claim relationship: `CLAIM-008`.

### `CORE-LOG-001` Log Sanitization

- Preconditions: synthetic sensitive key-value fixture set.
- Action: sanitize SAM private material, credentials, headers, message bodies, endpoint fields, destination identifiers, session IDs, query parameters, URL-encoded tokens, multiline input, mixed-case keys, and long input.
- Expected result: known sensitive values are redacted while ordinary diagnostic structure remains useful.
- Expected failure behavior: no known fixture value reappears after repeated sanitization.
- Test layer: pure logic.
- Fixture: clearly fake values such as `example-test-destination.invalid` and `i2p_knoks_test_session`.
- Source evidence: `LogSanitizer`.
- Limitations: pattern-based sanitization does not guarantee every possible sensitive value is removed.
- CI status: runs through `testDebugUnitTest`.
- Claim relationship: `CLAIM-009`.

### `CORE-MIGRATION-001` Persistence And Migrations

- Preconditions: committed Room schemas for versions 4, 5, and 6; explicit migration constants.
- Action: verify current database version, supported migration graph, committed schema files, migration SQL shape, and absence of current destructive migration fallback.
- Expected result: schemas and graph remain stable and reviewable.
- Expected failure behavior: test fails if a schema file disappears, the graph changes without test updates, or destructive fallback returns to the current builder.
- Test layer: repository/persistence guard.
- Fixture: generated Room schema JSON and migration SQL constants.
- Source evidence: `AppDatabase`, `AppDatabaseMigrationTest`, `docs/ROOM_MIGRATIONS.md`.
- Limitations: JVM guard does not execute Android SQLite migrations.
- CI status: runs through `testDebugUnitTest`.
- Claim relationship: `CLAIM-010`.

### `CORE-MIGRATION-002` Through `CORE-MIGRATION-006` Instrumented Migration Execution

- Preconditions: Android emulator or connected device; committed Room schemas for versions 4, 5, and 6.
- Action: create old-version databases with `MigrationTestHelper`, insert synthetic records, execute registered migrations, validate schema, open through current Room DAOs/repository, and check newer-version rejection.
- Expected result: 4->5, 5->6, 4->6, and 5->6 paths preserve documented fixture data or enter bounded endpoint fallback where historical values are malformed.
- Expected failure behavior: test fails if supported paths fall back destructively, lose required synthetic records, miss defaults, violate the unique Connect identity index, fail current DAO reads, or silently open a newer database version.
- Test layer: Android instrumentation.
- Fixture: synthetic test-only endpoint, identity, contact, log, and message records; no real I2P identity material or secrets.
- Source evidence: `AppDatabaseMigrationInstrumentedTest`, `app/schemas/no.knoksen.i2pbrowser.data.AppDatabase/`.
- Limitations: local execution requires emulator/device availability; this suite does not prove storage security, arbitrary corruption recovery, anonymity, or encryption.
- CI status: configured in `.github/workflows/android.yml`; do not treat CI execution as validated until GitHub Actions has parsed and run the workflow.
- Claim relationship: `CLAIM-010`.

### `CORE-UI-001` Principal UI Core Flow

- Preconditions: Robolectric Compose test, no external router.
- Action: render endpoint setup, enter invalid manual host, save, and render real-alpha status plus security boundaries.
- Expected result: endpoint validation error is visible; real-alpha limitations remain visible.
- Expected failure behavior: UI test fails if invalid input is accepted or limitation copy disappears.
- Test layer: Android UI.
- Fixture: Compose rule and fake callback.
- Source evidence: `I2pEndpointSetupCard`, `RealAlphaStatusCard`, `SecurityBoundariesCard`.
- Limitations: not a full emulator launch or process recreation test.
- CI status: runs through `testDebugUnitTest`.
- Claim relationship: `CLAIM-007`, `CLAIM-021`.

## Coverage Snapshot

- P0 matrix entries: 36
- Automated pass entries: 24
- Automated partial entries: 9
- Manual entries: 1
- Blocked entries: 1
- Not implemented entries: 1
- JVM unit and Robolectric tests: run by `testDebugUnitTest`
- Instrumentation tests: Room migration suite is configured for a bounded emulator CI job; local execution requires an emulator/device
- Protocol-fixture coverage: SAM fake connection and HTTP fake transport
- External-router coverage: manual only
- Migration coverage: schema/graph guard passes in JVM; old-version migration execution exists as instrumentation coverage and awaits emulator/CI execution evidence

## CI Policy

Default CI must keep using deterministic local tests. It must not depend on:

- public I2P availability
- arbitrary live `.i2p` destinations
- an external I2P or i2pd router
- public SAM bridges
- public HTTP proxies
- third-party services

Optional external-router validation belongs in manual or separately triggered workflows until the environment is declared and bounded. Room migration instrumentation is bounded to a CI emulator job and does not require external routers.
