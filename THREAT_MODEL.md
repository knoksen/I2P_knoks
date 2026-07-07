# Threat Model

## 1. Scope

This threat model covers the current Android prototype and the planned transition toward a client that may communicate with a separately operated I2P router.

The current build is a local simulator. It does not provide network anonymity, real I2P tunnels, VPN protection, or a reviewed secure-messaging protocol.

## 2. Security objectives

1. Prevent users from mistaking simulated controls for real privacy protection.
2. Prevent accidental disclosure of identity material, credentials, logs, and network metadata.
3. Prevent untrusted content from escaping browser or application boundaries.
4. Keep future router management interfaces isolated and least-privileged.
5. Preserve release and dependency integrity.
6. Minimize telemetry and avoid creating new correlation identifiers.

## 3. Assets

- Android application package and source code.
- Local Room database.
- Prototype identity records and imported key material.
- Bookmarks, contacts, messages, and logs.
- Future I2P destination keys.
- Future SAM/I2CP credentials and sessions.
- GitHub Actions workflows, repository tokens, signing material, and release artifacts.
- User expectations about anonymity and security state.

## 4. Trust boundaries

```text
Untrusted URL/content
        |
        v
Android UI / optional WebView boundary
        |
        v
ViewModel and repository boundary
        |
        v
Room database / platform storage

Future architecture:

Android client
        |
        | authenticated, narrow protocol adapter
        v
Loopback-bound or isolated SAM/I2CP interface
        |
        v
Separately operated I2P router
        |
        v
I2P network
```

The application process, router process, host operating system, CI system, and release distribution channel are separate trust domains.

## 5. Adversaries

- A malicious website or crafted destination string.
- A local application with access to exported Android components, backups, clipboard data, or shared storage.
- A compromised dependency or build action.
- A malicious or compromised I2P peer after real integration.
- A remote attacker reaching an incorrectly exposed router API.
- A repository contributor attempting to obtain workflow tokens or publish substituted artifacts.
- A passive observer correlating timing, telemetry, identifiers, or network egress.
- A user relying on misleading UI language and taking unsafe actions.

## 6. Primary threats and required controls

### T1 — False security state

**Threat:** Simulated tunnel, encryption, VPN, or telemetry states are presented as operational facts.

**Impact:** Users disclose sensitive data while believing they are protected.

**Required controls:**

- persistent `SIMULATION` labeling in all relevant views;
- no green “secure” state without measured evidence;
- separate types for simulated and measured telemetry;
- documentation checks for prohibited unsupported claims;
- fail-closed UI when router state is unknown.

### T2 — Private key exposure

**Threat:** Private keys are stored as ordinary database strings, included in backups, logs, crash reports, screenshots, or exports.

**Impact:** Identity takeover and long-term correlation.

**Required controls before real key use:**

- Android Keystore-backed protection where compatible;
- non-exportable keys where protocol design allows;
- encrypted database fields with keys outside the database;
- backup exclusion;
- redaction in logs and UI;
- explicit destruction and rotation workflows;
- no real key material in tests or seed data.

### T3 — Router API exposure

**Threat:** SAM, I2CP, router console, SSH, or management ports bind to public interfaces or broad container networks.

**Impact:** Router control, destination compromise, traffic observation, or host compromise.

**Required controls:**

- loopback binding by default;
- dedicated internal container network when containerized;
- no host port publication unless explicitly required;
- firewall allowlists;
- authentication and session isolation;
- least-privilege service accounts;
- startup refusal when unsafe bindings are detected.

### T4 — URL and WebView escape

**Threat:** Crafted URLs invoke external schemes, local files, JavaScript bridges, cleartext endpoints, or system applications.

**Impact:** Code execution, data exfiltration, browser fingerprinting, or IP leakage.

**Required controls:**

- strict destination parser and scheme allowlist;
- no `file://`, `content://`, `intent://`, or arbitrary custom schemes;
- JavaScript disabled unless a reviewed use case requires it;
- no unrestricted JavaScript bridge;
- downloads disabled or isolated;
- external browser handoff made explicit;
- cleartext traffic policy aligned with a real I2P proxy architecture;
- tests for malformed and mixed-encoding destinations.

### T5 — Metadata and telemetry leakage

**Threat:** Logs or analytics expose IP addresses, destinations, contacts, timing, identifiers, or user behavior.

**Impact:** Correlation and deanonymization.

**Required controls:**

- telemetry off by default;
- local-only, bounded, short-retention logs;
- structured redaction;
- no third-party analytics in privacy-sensitive paths;
- user-visible deletion controls;
- no stable cross-session identifier unless strictly necessary;
- privacy review for every new metric.

### T6 — Dependency and CI compromise

**Threat:** A compromised dependency, action, build plugin, or excessive workflow token modifies source or artifacts.

**Impact:** Supply-chain compromise.

**Required controls:**

- pin GitHub Actions to immutable commit SHAs;
- explicit workflow permissions;
- dependency review and lockfiles/version catalogs;
- protected branches and review gates;
- reproducible or independently verifiable release builds where practical;
- signed artifacts and published checksums;
- isolated signing material;
- no secrets in pull-request workflows from untrusted forks.

### T7 — Artifact substitution

**Threat:** A release APK or document is replaced after review.

**Impact:** Users receive unreviewed or malicious content.

**Required controls:**

- cryptographic signing for executable artifacts;
- SHA-256 manifests generated from final files;
- release provenance linked to a commit;
- verification instructions;
- independent post-upload download and hash verification.

### T8 — Traffic correlation after real integration

**Threat:** Timing, tunnel lifetime, bandwidth patterns, direct clearnet fallbacks, DNS requests, or mixed identities reveal relationships.

**Impact:** Reduced anonymity or deanonymization.

**Required controls:**

- no clearnet fallback;
- explicit proxy-only networking boundary;
- DNS-leak tests;
- separation of identities and sessions;
- conservative retry behavior;
- avoid unique client fingerprints;
- do not claim resistance beyond what the upstream I2P design and deployment support.

## 7. Current known gaps

- Message payloads are not protected by a reviewed encryption protocol.
- Tunnel and telemetry behavior is simulated.
- Identity persistence is not suitable for production secrets.
- Peer verification is simulated.
- No real SAM/I2CP adapter exists.
- No IP-leak, DNS-leak, or WebView security test suite exists.
- No independent security review has been completed.

## 8. Release gates for any real-network build

A build must not be described as a real I2P client until all of the following are satisfied:

- [ ] a documented SAM/I2CP integration boundary exists;
- [ ] router APIs are loopback-bound or isolated by default;
- [ ] no clearnet fallback path exists;
- [ ] destination keys use reviewed secure storage;
- [ ] IP- and DNS-leak tests pass;
- [ ] WebView and URL handling have dedicated security tests;
- [ ] telemetry and logs pass a privacy review;
- [ ] dependency and workflow permissions are reviewed;
- [ ] executable artifacts are signed and integrity metadata is published;
- [ ] user-facing copy accurately reflects measured capabilities;
- [ ] an independent security review or equivalent documented assessment is complete.

## 9. Review cadence

Update this threat model whenever the project adds:

- real network access;
- a WebView or download capability;
- router API communication;
- key import/export;
- background services;
- remote telemetry;
- container or VPS deployment;
- release signing or automated publication.