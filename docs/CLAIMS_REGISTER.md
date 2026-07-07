# Claims Register

This is the authoritative registry for public-facing product claims in I2P Knoks Browser and the I2P Connect product path.

Public-facing wording must be grounded in measured runtime state, persisted local configuration, tested implementation, or clearly labeled lab/demo behavior. When wording is stronger than the evidence below, weaken the wording or add the implementation, tests, documentation, and review needed to support it.

For practical writing rules, see [Claim-Safe Writing](CLAIM_SAFE_WRITING.md).

## Classification Vocabulary

| Classification | Meaning |
| :--- | :--- |
| `IMPLEMENTED_SOURCE_VERIFIED` | Source code exists and the behavior is reachable, but test coverage may be limited for the exact claim. |
| `IMPLEMENTED_TEST_VERIFIED` | Source code exists and tests cover the claim at an appropriate level for current risk. |
| `PARTIALLY_VALIDATED` | Some source, tests, or docs exist, but the public wording must stay narrow. |
| `EXPERIMENTAL` | Exploratory behavior may exist, but it is subject to change and must be labeled. |
| `PROTOTYPE_ONLY` | Demo or lab behavior exists and must not be promoted as release behavior. |
| `PLANNED` | Roadmap or implementation prompt exists, but current release-path behavior does not. |
| `ARCHITECTURAL_GOAL` | A desired property or design direction, not a current product claim. |
| `NOT_SUPPORTED` | The repository must not claim this as current behavior. |
| `RELEASE_REAL` | Conservative release-path behavior: reachable implementation, risk-appropriate tests, documented limits, stable user-facing behavior, validation passing, no known blocking security issue, and claim review complete. |

`RELEASE_REAL` does not mean production quality, independent audit, broad anonymity, full privacy protection, or suitability for high-risk use. It only means the specific behavior has enough evidence to be described as real in the current alpha.

## Claim Entries

| ID | Normalized Claim | Surfaces | Classification | Implementation Evidence | Test / Validation Evidence | Limitations | Allowed Wording | Prohibited Or Premature Wording | Review Status | Stronger Wording Requires |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| CLAIM-001 | The repository contains an Android real-alpha app and I2P Connect planning docs. | README, release notes, docs | `IMPLEMENTED_SOURCE_VERIFIED` | Android source exists under `app/`; product docs exist under `product/i2p-connect/`. | `local-release-verify.ps1` builds/tests the Android app. | Alpha maturity only. | "Android real-alpha source tree"; "I2P Connect planning path". | Prohibited: "production-ready"; "complete product". | Reviewed 2026-07-06 | Stable release criteria, documented signoff, and broader test coverage. |
| CLAIM-002 | Real I2P behavior requires an external I2P or i2pd router. | README, security docs, onboarding, release notes, UI | `RELEASE_REAL` | Endpoint config and diagnostics use configured external/local router endpoints. | Endpoint and diagnostics tests; validation scripts. | App does not embed or manage a router. | "Requires an external I2P or i2pd router." | "Built-in router"; "works without a router"; "embedded router". | Reviewed 2026-07-06 | Embedded-router implementation, docs, permissions, tests, and security review. |
| CLAIM-003 | Users can configure local/emulator/LAN I2P endpoints. | README, UI, docs | `IMPLEMENTED_TEST_VERIFIED` | `I2pEndpointConfig`, `RouterEndpoint`, `app_settings`, endpoint setup UI. | `I2pEndpointConfigTest`, migration tests. | Does not prove router reachability by itself. Malformed persisted settings fall back to a documented correction state. | "configured endpoint"; "endpoint settings are persisted locally"; "endpoint input is validated and normalized." | "traffic is private after setup"; "router protected". | Reviewed 2026-07-07 | Additional security tests for LAN exposure and user warning UX. |
| CLAIM-004 | Diagnostics can measure SAM, HTTP proxy, and router console reachability. | README, UI, release notes | `IMPLEMENTED_TEST_VERIFIED` | `I2pDiagnosticsClient`, `RouterDiagnosticTransport`, diagnostics panel. | `I2pDiagnosticsClientTest`, `I2PViewModelCoreFlowTest`, `RealAlphaStatusTest`. | Port reachability, timeout, and cancellation results are diagnostic evidence only; they are not anonymity, privacy, or router-offline guarantees. | "router reachable"; "SAM reachable"; "HTTP proxy reachable"; "measured local service state"; "diagnostic timeout/cancellation behavior is covered by deterministic tests." | "private connection"; "protected traffic"; "safe to use"; "timeout proves router offline." | Reviewed 2026-07-07 | Device/emulator smoke evidence and release-candidate review for networking changes. |
| CLAIM-005 | SAM session state can be represented from measured SAM interactions. | README, UI, release notes | `IMPLEMENTED_TEST_VERIFIED` | `SamBridgeClient`, `SamSessionManager`. | `SamBridgeClientTest`, `SamSessionManagerTest`. | Transient session state only; not messaging delivery or anonymity. | "SAM session ready"; "SAM session state". | "anonymous session"; "secure tunnel"; "guaranteed route". | Reviewed 2026-07-06 | Transport threat model, delivery tests, and stronger SAM lifecycle UX review. |
| CLAIM-006 | `.i2p` HTTP proxy inspection is available through the configured proxy. | README, UI, release notes | `IMPLEMENTED_TEST_VERIFIED` | `I2pHttpClient`, browser/page inspector UI. | `I2pHttpClientTest`, safe preview tests. | Text preview only; no WebView/full browser isolation; HTML/JS not executed. | "page inspection"; "real proxy response"; "safe text preview". | "full browser"; "browser isolation"; "anonymous browsing". | Reviewed 2026-07-06 | WebView/browser architecture, isolation threat model, tests, and docs. |
| CLAIM-007 | `RELEASE_REAL` and `LAB_SIMULATION` mode boundaries exist. | README, UI, release docs | `IMPLEMENTED_TEST_VERIFIED` | `AppExperienceMode`, `visibleAppTabs`. | `AppExperienceModeTest`. | Some lab UI remains in source and must stay labeled. | "`RELEASE_REAL` hides lab-only tabs"; "lab/demo surfaces are labeled." | "all app surfaces are production behavior"; "lab behavior is active protection". | Reviewed 2026-07-06 | UI audit and screenshots for every public release path. |
| CLAIM-008 | Public-only Connect identity export/import helpers exist. | README, docs, future onboarding | `IMPLEMENTED_TEST_VERIFIED` | `ConnectIdentityModel`, Room entity, export codec. | `ConnectIdentityModelTest`, migration tests. | Does not prove ownership; private backup is not implemented. | "public-only identity export"; "private material excluded". | "secure identity backup"; "private key export supported". | Reviewed 2026-07-06 | Protected private-material storage, backup design, permission/docs review, tests. |
| CLAIM-009 | Logs are sanitized for known sensitive fields before Room storage. | README, security docs | `IMPLEMENTED_TEST_VERIFIED` | `LogSanitizer` in repository layer. | `LogSanitizerTest`, including diagnostic exception fixtures. | Pattern-based sanitizer; not a universal no-logs or metadata-protection guarantee. | "sanitized logs"; "known sensitive fields are redacted." | "no logs"; "zero logs"; "metadata-free logs"; "all sensitive values are guaranteed removed." | Reviewed 2026-07-07 | Broader structured logging policy, coverage for new fields, and audit of all log call sites. |
| CLAIM-010 | App settings and Connect identity migrations are present and have a bounded migration-test contract. | README, release docs | `IMPLEMENTED_TEST_VERIFIED` | Room migrations `MIGRATION_4_5`, `MIGRATION_5_6`; committed schemas for versions 4, 5, and 6; `AppDatabaseMigrationInstrumentedTest`. | `AppDatabaseMigrationTest`; GitHub Actions `Android / migration-instrumentation` run `28834367192` for commit `1dddeb9`; instrumentation XML reported `tests=6`, `failures=0`, `errors=0`, and `skipped=0`. | JVM guard verifies schema files, graph, SQL shape, and no current destructive fallback. Instrumentation coverage executes bounded synthetic 4->5, 5->6, 4->6, repository-open, malformed endpoint fallback, and newer-version rejection cases; it does not prove arbitrary historical database recovery, storage security, anonymity, or encryption. | "Room schema files and migration graph are checked"; "instrumented migration tests pass for supported synthetic 4->5->6 paths." | "all historical databases can be recovered"; "migration is lossless"; "downgrades are supported"; "migration testing proves storage security". | Reviewed 2026-07-07 | Future schema changes require new migration edges, exported schema, JVM guard updates, and instrumented migration coverage. |
| CLAIM-011 | Beginner onboarding direction is documented. | README, onboarding docs | `ARCHITECTURAL_GOAL` | `product/03_UX_ONBOARDING_SPEC.md`, task prompts. | Claim check only; no complete onboarding implementation yet. | Current onboarding is not a complete mission system. | "onboarding direction is documented"; "planned missions". | "onboarding complete"; "guided setup fully implemented". | Reviewed 2026-07-06 | Implemented onboarding flow, state persistence, UI tests, docs. |
| CLAIM-012 | Audio messaging is planned as a controlled experimental track. | README, MVP docs, task prompts | `PLANNED` | Product specs and `05_add_audio_message_prototype.md`. | No runtime audio tests; no microphone permission. | Not in current release path. | "planned audio-note prototype"; "experimental track"; "not currently release-path behavior." | "audio messages are supported"; "encrypted audio messages are available." | Reviewed 2026-07-06 | Recording/playback implementation, permission docs, crypto/storage tests, delete cleanup, validation. |
| CLAIM-013 | Private messaging is a planned I2P Connect capability. | README, product docs | `PLANNED` | Product specs and implementation prompts. | Existing `secure_messages` is lab/demo only. | No release-path private messaging implementation. | "planned private-message MVP"; "demo messaging is lab-only." | "secure chat"; "private messaging is available"; "production messaging". | Reviewed 2026-07-06 | Message envelope, crypto tests, transport tests, storage/migration tests, UI copy review. |
| CLAIM-014 | App-layer encryption is not a current release-path communication guarantee. | README, docs, UI, release notes | `NOT_SUPPORTED` for current release-path messaging | Product docs define future requirements; demo Base64/local RSA surfaces are lab/prototype. | Claim check and unit tests for public-only identity export. | Cryptographic design and tests are not complete for messaging. | "not independently audited"; "app-layer encryption is planned"; "demo encoding". | "full E2EE"; "end-to-end encrypted"; "audited encryption"; "secure chat". | Reviewed 2026-07-06 | Crypto design, maintained library choice, test vectors, review, docs, release gate. |
| CLAIM-015 | The app does not provide anonymity by itself. | README, security docs, UI, release notes | `NOT_SUPPORTED` for guarantee wording | Security docs and UI boundaries. | Claim check. | I2P router behavior and user OpSec affect privacy. | "does not provide anonymity by itself"; "no anonymity guarantee." | "anonymous by default"; "complete anonymity"; "untraceable". | Reviewed 2026-07-06 | Threat model, measured properties, external review, and careful scoped language. |
| CLAIM-016 | The app does not provide OS-level VPN behavior. | README, security docs, UI, release notes | `NOT_SUPPORTED` | Manifest has no VPN service permission; lab UI only. | Android permission docs and tests around mode hiding. | Lab VPN/VPS samples are local UI previews. | "No OS-level VPN tunnel"; "LAB VPN profile sample." | "VPN protects traffic"; "VPN active" without lab qualifier. | Reviewed 2026-07-06 | Android VPN service implementation, permissions, tests, docs, review. |
| CLAIM-017 | The app does not provide Tor routing. | README, security docs, release notes | `NOT_SUPPORTED` | No Tor integration in source. | Claim check. | None. | "No Tor routing"; "Tor is not integrated." | "Tor-enabled"; "routes through Tor". | Reviewed 2026-07-06 | Tor integration design, implementation, tests, docs, and risk review. |
| CLAIM-018 | The app does not provide an embedded I2P router. | README, security docs, release notes | `NOT_SUPPORTED` | External endpoint requirement in source/docs. | Claim check and permission docs. | Router must run separately. | "No embedded I2P router"; "external router required." | "built-in router"; "embedded router". | Reviewed 2026-07-06 | Embedded router integration, lifecycle tests, resource review, docs. |
| CLAIM-019 | The app does not provide full browser isolation. | README, security docs, release notes | `NOT_SUPPORTED` | Page inspector is text-preview oriented; no full WebView isolation claim. | HTTP client/sanitizer tests. | No JS execution or full browser sandbox. | "page inspection"; "safe preview"; "no full browser isolation." | "isolated browser"; "full browser". | Reviewed 2026-07-06 | Browser architecture, sandbox review, tests, docs. |
| CLAIM-020 | The project is not independently security-audited. | README, security docs, release notes | `NOT_SUPPORTED` for audit claims | Docs state no independent audit. | Claim check. | Internal tests exist, but they are not an external audit. | "not independently audited"; "not externally audited." | "audited"; "externally reviewed"; "audited encryption." | Reviewed 2026-07-06 | Published external audit scope, findings, fixes, and claim review. |
| CLAIM-021 | Release validation scripts exist and should pass before release-facing work is complete. | README, validation docs, PR template | `IMPLEMENTED_SOURCE_VERIFIED` | `scripts/check-release-claims.ps1`, `scripts/local-release-verify.ps1`. | Running the scripts; script tests for claim checker. | Pattern checks are guardrails, not semantic proof. | "validated by current automated checks"; "claim check passed." | "release-ready because script passed." | Reviewed 2026-07-06 | CI parity, branch protection, and documented release-candidate signoff. |
| CLAIM-022 | The Android real-alpha core-flow test matrix maps current P0 coverage and limitations. | Validation docs, PR template, release review notes | `IMPLEMENTED_TEST_VERIFIED` | `docs/ANDROID_REAL_ALPHA_TEST_MATRIX.md`; Android test sources under `app/src/test`. | `AndroidRealAlphaTestMatrixDocTest`; JVM/Robolectric tests referenced by the matrix; CI `testDebugUnitTest`. | The matrix is traceability evidence only. It does not prove anonymity, privacy, production readiness, independent audit status, audited encrypted chat, VPN behavior, Tor routing, embedded-router behavior, or full browser isolation. Some entries remain partial, manual, or blocked. | "traceable Android real-alpha core-flow matrix"; "P0 coverage is recorded with automated, partial, manual, and blocked status." | "full coverage"; "all flows proven"; "production-ready because tests pass." | Reviewed 2026-07-07 | Closed partial/blocked gaps, emulator evidence where useful, and release-candidate signoff. |

## Approved Wording

- "Available in the current source tree."
- "Implemented in the Android real-alpha source tree."
- "Measured local I2P service state."
- "Requires an external I2P or i2pd router."
- "Experimental and subject to change."
- "Prototype only."
- "Planned work; not available in the current release path."
- "Validated by the repository's current automated checks."
- "Not independently security-audited."
- "This feature does not provide an anonymity guarantee."

## Prohibited Or Premature Wording

Do not use these as current product claims unless this register is updated with evidence:

- Prohibited example: "Provides complete anonymity."
- Prohibited example: "Guarantees privacy."
- Prohibited example: "Cannot be traced."
- Prohibited example: "Stores no metadata."
- Prohibited example: "No logs" or "zero logs."
- Prohibited example: "Fully secure."
- Prohibited example: "Production-ready."
- Prohibited example: "Release-ready" without explicit validation scope.
- Prohibited example: "Audited encryption."
- Prohibited example: "Private by default."
- Prohibited example: "End-to-end encrypted" or "full E2EE."
- Prohibited example: "Censorship-proof."

## Review Checklist

- [ ] Is the behavior implemented?
- [ ] Is it reachable through the relevant release path?
- [ ] Is it tested?
- [ ] Is the test meaningful for the claim?
- [ ] Are limitations shown near the claim?
- [ ] Could a non-technical reader interpret the statement more strongly than intended?
- [ ] Does this register permit the wording?
- [ ] Does the statement create a security, anonymity, encryption, or privacy expectation?
- [ ] Did `.\scripts\check-release-claims.ps1` pass?
