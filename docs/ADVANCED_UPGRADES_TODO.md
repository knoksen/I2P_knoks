# Advanced Upgrades TODO

This checklist tracks the next development phase for I2P Knoks Browser and the I2P Connect product path.

Unchecked items are planned work only. A feature should become user-facing in `RELEASE_REAL` only after implementation, tests, documentation, and release-claim validation support the wording.

## Operating Rules

- [ ] Ground every upgrade in measured runtime state, persisted local configuration, tested implementation, or clearly labeled lab/demo behavior.
- [ ] Keep private destination material, app-layer private keys, plaintext messages, credentials, sensitive headers, and raw router logs out of UI state, logs, screenshots, exports, and external services.
- [ ] Keep Android permissions minimal; update `docs/ANDROID_PERMISSIONS.md` before any new permission is exposed in a release path.
- [ ] Keep lab/demo surfaces hidden from `RELEASE_REAL` unless the behavior is implemented, tested, documented, and intentionally exposed.
- [ ] Run `.\scripts\check-release-claims.ps1` for every docs, product, app, security, onboarding, or release-facing change.
- [ ] Run `.\scripts\local-release-verify.ps1` before marking release-facing, app, security, onboarding, or product-pack work complete.

## P0 - Repository Foundation Cleanup

These are the next small, clean PR-sized tasks before broader feature work.

### Reconcile Security Files

- [x] Remove the legacy duplicate security-policy placeholder after confirming it contains no unique project guidance.
- [x] Keep `SECURITY.md` as the single authoritative root security policy.
- [x] Remove placeholder disclosure language and example version-support tables.
- [x] Confirm security-reporting instructions, supported scope, current boundaries, and validation commands are consistent.

### Align README With Real-Alpha State

- [x] Reconcile `README.md` with the current Android real-alpha source tree.
- [x] Separate what is implemented, prototype/lab-only, planned, and not yet verified.
- [x] Keep status wording aligned with `docs/SECURITY_BOUNDARIES.md`, `docs/ANDROID_PERMISSIONS.md`, and `docs/CLAIMS_REGISTER.md`.
- [x] Avoid foundation-only wording when describing the repository as a whole.

### Complete Public-Facing Claims Audit

- [x] Review `README.md`, onboarding docs, doctrine docs, MVP docs, demo notes, release docs, metadata, templates, and major Android UI text against `docs/CLAIMS_REGISTER.md`.
- [x] Mark claims as supported, partially supported, future/gated, or unsupported.
- [x] Replace broad privacy/security wording with measured phrases such as `router reachable`, `SAM session ready`, `local preview`, `not externally audited`, and `experimental`.
- [x] Confirm release-facing copy does not imply unsupported anonymity, VPN, Tor, audited chat, embedded-router, browser-isolation, compliance, or mainstream conferencing behavior.

### Lock Claim-Safe Writing Policy

- [x] Add an internal writing policy for product, UI, release, demo, and community copy.
- [x] Define what the project can say about privacy, safety, security, I2P readiness, and app-layer encryption at each maturity level.
- [x] Define wording that must remain blocked until implementation, tests, docs, and validation exist.
- [x] Link the policy from contributor docs, release docs, or the claims register.

## P1 - Validation And Release Quality

### Make Validation Less Person-Dependent

- [ ] Keep `Release Claim Check` as a PR gate.
- [ ] Ensure CI covers formatting or `git diff --check`, claim checks, Android unit tests, debug APK build, artifact upload, checksum generation, and useful failure reports.
- [ ] Decide whether `.\scripts\local-release-verify.ps1` needs a CI-equivalent wrapper or whether CI should continue composing the same checks directly.
- [x] Add or update CI summaries so reviewers can see pass/fail status, artifact names, checksum status, and non-goals.

### Handle Windows Build Locks Systematically

- [x] Document the known generated `app/build` lock mode and the likely Gradle/Java process cause.
- [x] Document a safe cleanup sequence that removes only generated build output after stopping Gradle/Java.
- [x] Add a helper script that verifies the resolved path before cleanup.
- [x] Add the workaround to `docs/VALIDATION.md` without encouraging destructive cleanup.

### Strengthen Release Discipline

- [ ] Use `docs/RELEASE_CANDIDATE_CHECKLIST.md` as the defensive pre-tag gate.
- [ ] Ensure each release review includes claims, docs, changelog, build, permissions, APK checksum, and security-file consistency.
- [ ] Keep release notes structured around what is real, what remains lab-only, known limits, and non-goals.
- [ ] Verify release notes do not introduce unsupported claims.

### Improve Validation Documentation

- [x] Tie `docs/VALIDATION.md` to the actual command set, expected outputs, and common failures.
- [x] Explain the difference between the fast claim check, local release verification, Gradle tests, debug APK build, and real-alpha smoke notes.
- [x] Include failure-reporting rules: exact command, exit code, relevant output, blocker, and last narrower successful command.
- [ ] Keep examples Windows-first while noting CI/Linux equivalents where useful.

## P1 - Product And Architecture

### Define Module Boundaries

- [ ] Split responsibilities between UI, domain logic, transport/networking, local storage, and security-sensitive components.
- [ ] Use the I2P Connect package layout from `product/i2p-connect/ARCHITECTURE.md` as the default direction when runtime work begins.
- [x] Keep router diagnostics behind a small transport contract rather than leaking socket probing directly into UI code.
- [ ] Keep SAM and future transport details behind small interfaces rather than leaking protocol state into UI code.
- [ ] Document any boundary decisions before large implementation work.

### Map Must-Not-Break Flows

- [x] Identify the current real-alpha flows that must keep working: first launch, mode selection, diagnostics, configured endpoint checks, SAM probing, HTTP proxy inspection, onboarding entry points, and error states.
- [x] Define smoke checks for each flow.
- [ ] Keep lab/demo flows separate from `RELEASE_REAL` during regression testing.
- [x] Add recovery expectations for unavailable router, unavailable SAM, unavailable HTTP proxy, invalid endpoint, timeout, and partial-ready states.

### Introduce Feature Flags For Experiments

- [ ] Gate audio notes, advanced integrations, experimental UX flows, and research features behind explicit flags or lab-only modes.
- [ ] Default risky or incomplete features off in `RELEASE_REAL`.
- [ ] Document each flag's purpose, owner, validation gate, and removal criteria.
- [ ] Add tests for release-mode hiding when a feature is not ready.

### Complete Dependency Review

- [ ] Review Android, Kotlin, Gradle, Room, Compose, networking, and test dependencies for maintenance status, license, and security posture.
- [ ] Record findings in `docs/DEPENDENCY_REPORTS.md` or a follow-up dependency-review note.
- [ ] Prefer maintained libraries for cryptography, audio, parsing, and QR code work.
- [ ] Do not add a new dependency until the use case, alternatives, and security impact are clear.

## P1 - UX And Onboarding

### Tighten First Experience

- [ ] Explain what the app is, what it is not, what the current alpha can actually do, and what the next safe action is.
- [ ] Keep first-run copy plain, calm, and specific.
- [ ] Avoid a wall of text; prioritize the next useful action and link to deeper safety details.
- [ ] Make alpha limits visible before sensitive actions.

### Add Just-In-Time Explanations

- [ ] Place short explanations near permissions, identity creation, invite trust, audio recording, local storage, logs, and network status.
- [ ] Explain why an action is blocked when readiness, identity, trust, or permissions are incomplete.
- [ ] Store onboarding progress locally and avoid cloud sync by default.
- [ ] Use explicit local confirmation only when measured state is not possible, such as acknowledging safety reading.

### Design Empty And Error States

- [ ] Add empty, loading, partial-ready, blocked, failed, and success states for real-alpha screens.
- [ ] Make unavailable features feel intentionally disabled rather than mysterious.
- [ ] Show recovery hints for router unavailable, SAM unavailable, HTTP proxy unavailable, invalid endpoint, storage failure, and disabled feature states.
- [ ] Keep state labels measured and avoid implying privacy or security outcomes from setup success alone.

### Test With Three User Profiles

- [ ] Review onboarding with a technical user, a non-technical user, and a security-aware user.
- [ ] Track where wording is confusing, too broad, too technical, or too reassuring.
- [ ] Convert findings into copy changes, state changes, or docs updates.
- [ ] Keep user-test notes free of sensitive personal data.

## P1 - Security And Responsible Use

### Connect Security Policy To Development Flow

- [ ] Decide who reviews security-relevant PRs.
- [ ] Define the private reporting route, triage steps, expected response behavior, and out-of-scope reports.
- [ ] Make `SECURITY.md` operational rather than a passive policy file.
- [x] Confirm the legacy security placeholder no longer creates a competing disclosure story.

### Operationalize Responsible Use

- [ ] Convert `docs/RESPONSIBLE_USE.md` principles into rules for demos, README wording, issue triage, prototype communication, and community posts.
- [ ] Add concise responsible-use copy near invite trust, diagnostics export, identity export, audio recording, logs, and any cloud-connected feature.
- [ ] Keep abuse-resistant defaults: no public discovery, no contact scraping, no mass messaging, no hidden recording, and no false online presence.
- [ ] Keep alpha-tester guidance short and practical.

### Create A Lightweight Threat Model

- [ ] Describe actors, sensitive data, local artifacts, trust boundaries, attack surfaces, and misconfiguration risks.
- [ ] Include private destination material, app-layer keys, router credentials, contact graph, logs, message bodies, and audio attachments.
- [ ] Tie mitigations to tests, docs, permissions, and UI states.
- [ ] Revisit the threat model before adding messaging, audio, contact import, cloud-connected features, or export flows.

### Add Permission Audit Discipline

- [ ] Before each feature, ask whether a new permission is truly required.
- [ ] Prefer optional, explicit, user-triggered permission requests.
- [ ] Update `docs/ANDROID_PERMISSIONS.md` before a permission enters a release path.
- [ ] Cover permission denial, revoke, and recovery states in tests for permissioned features.

## P1 - Quality, Testing, And Operations

### Build A Minimal Test Matrix

- [x] Add unit tests for domain logic and state mapping.
- [x] Record repository, persistence, and migration coverage in the matrix with automated, partial, manual, and blocked status.
- [x] Add UI smoke tests or snapshot checks for onboarding and mode boundaries.
- [x] Protect `RELEASE_REAL` from lab-only surfaces with explicit tests.
- [x] Commit Room schemas for supported versions 4, 5, and 6 and add a JVM schema/graph guard.
- [x] Add Android instrumentation migration tests for supported 4->5->6 paths with synthetic fixtures.
- [ ] Capture successful migration instrumentation execution from a local emulator/device or GitHub Actions before marking migration execution complete.

### Gate Claims In Pull Requests

- [ ] Keep docs and UI wording under release-claim validation.
- [ ] Fail the pipeline when public-facing docs promise more than code, tests, or the claims register support.
- [ ] Make remediation clear when the gate fails.
- [ ] Avoid weakening blocked-claim patterns to make inconvenient wording pass.

### Define Alpha Exit Criteria

- [ ] Define which core flows must be stable before moving beyond alpha.
- [ ] Require documentation alignment, known crash/failure posture, security-boundary review, and validation pass status.
- [ ] Keep maturity controlled by criteria, not enthusiasm.
- [ ] Document which criteria are manual, automated, or still missing.

### Add Issue Templates

- [ ] Add a bug-report template with environment, version/commit, reproduction steps, expected result, actual result, logs, and security relevance.
- [ ] Add a security-sensitive reporting reminder that tells users not to paste secrets, private destinations, message bodies, credentials, or raw router logs.
- [ ] Add a feature-request template that asks for user value, risk, permissions, docs impact, and claim impact.
- [ ] Link templates to validation and responsible-use expectations where useful.

## P2 - Advanced Feature Tracks

### Audio Notes As A Controlled Experiment

- [ ] Turn `product/i2p-connect/codex/05_add_audio_message_prototype.md` into an implementation plan before code lands.
- [ ] Specify recording, playback, local buffering, file format, maximum duration, maximum size, interruption handling, and delete cleanup.
- [ ] Keep `RECORD_AUDIO` absent until recording, encryption, storage, playback, denial, and revoke flows are implemented and tested.
- [ ] Keep audio disabled in `RELEASE_REAL` until permission docs, tests, security notes, and claim validation pass.

### Media Risk Analysis

- [ ] Document voice-identification risk, metadata exposure, temporary files, local storage, deletion expectations, export behavior, and reuse/sharing risks.
- [ ] Keep decrypted audio paths and samples out of logs.
- [ ] Define what users should see before first recording.
- [ ] Define cleanup behavior for encrypted attachments and decrypted temporary files.

### Break Advanced Work Into Epics

- [ ] Communication readiness and transport.
- [ ] Identity, invites, contacts, and trust.
- [ ] Private message envelope and queueing.
- [ ] Audio and media attachments.
- [ ] Security, permissions, and threat modeling.
- [ ] Testing, CI, validation, and observability.
- [ ] Distribution, release notes, checksums, and alpha-tester operations.

### Later Research Tracks

- [ ] Add a latency and bandwidth measurement harness before exploring push-to-talk voice.
- [ ] Keep push-to-talk experimental until measurements support a useful, honest UX.
- [ ] Explore constrained video only as a research preview after bandwidth and latency studies justify a prototype.
- [ ] Do not describe future media experiments as mainstream conferencing, phone replacement, or emergency-use features.
- [ ] Add a community education kit after private messaging, invites, and onboarding are tested.

## Suggested PR Sequence

### PR 1 - Repository Hygiene And Truthful Documentation

- [x] Remove the legacy security placeholder and keep `SECURITY.md` authoritative.
- [x] Update `README.md` to match the current Android real-alpha state.
- [x] Complete claims audit against `docs/CLAIMS_REGISTER.md`.
- [x] Add claim-safe writing policy.

### PR 2 - Validation And CI

- [x] Ensure validation scripts are represented in CI.
- [x] Document Windows build-lock recovery.
- [ ] Strengthen release checklist usage.
- [x] Tighten `docs/VALIDATION.md`.

### PR 3 - Alpha Stabilization

- [ ] Clarify module responsibilities.
- [x] Define must-not-break flows.
- [ ] Introduce feature flags for experiments.
- [ ] Complete dependency review.

### PR 4 - Onboarding And Responsible Use In Practice

- [ ] Revise onboarding against real alpha limits.
- [ ] Add empty/error states and recovery hints.
- [ ] Connect responsible-use rules to product copy and demo practice.
- [ ] Test onboarding language with three user profiles.

### PR 5 - Advanced Feature Track

- [ ] Scope the audio-note prototype behind a feature flag.
- [ ] Add media risk analysis.
- [ ] Define technical spikes and exit criteria for advanced work.
- [ ] Keep advanced features off the release path until tests, docs, and validation pass.

## Top 10 Next Actions

1. Keep validation automated in CI and visible in PR review.
2. Run the Room migration instrumentation suite in CI/local emulator and promote migration matrix entries only after reports are available.
3. Define alpha exit criteria.
4. Introduce feature flags for experimental functionality.
5. Revise onboarding so it is honest, simple, and operational.
6. Complete dependency review before adding advanced libraries.
7. Add issue templates for bugs and security-sensitive reports if more structure is needed.
8. Turn audio and advanced upgrades into bounded experiment tracks.
9. Implement deterministic loopback socket fixtures for SAM and HTTP-proxy lifecycle validation.
10. Add a lightweight threat model before private messaging or audio work.
