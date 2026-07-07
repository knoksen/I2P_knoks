# I2P Network Console Simulator

> [!IMPORTANT]
> This repository currently contains an **educational Android prototype and interface simulator** with an **experimental local SAM handshake/session path**. It is not a complete I2P router, anonymity tool, VPN, production browser, or secure messenger. A successful SAM control connection does not by itself prove that application traffic is routed safely through I2P.

The project explores user-interface patterns for privacy technology using Kotlin, Jetpack Compose, Room, local simulated state, and an early SAM v3 control-session experiment. Its purpose is to make concepts such as destinations, tunnels, key continuity, telemetry, and operational warnings easier to understand while the real network boundary is hardened.

## Current maturity

| Area | Current state |
|---|---|
| Android UI | Prototype implemented with Jetpack Compose |
| Local persistence | Room entities and DAOs |
| Tunnel management | Primarily simulated local records and status changes |
| Network telemetry | Illustrative/generated values, not authoritative I2P measurements |
| SAM integration | Experimental local handshake, transient session creation, and name lookup |
| SAM data transport | Not implemented as a reviewed application traffic path |
| Message transport | Local simulation only |
| Payload protection | Placeholder behavior; Base64 is used in parts of the prototype and is **not encryption** |
| Identity handling | Demonstrative local key generation and storage; not production-safe |
| VPN functionality | Not implemented |
| Production security review | Not completed |

## Implemented prototype modules

### Console and tunnel visualization

- Creates and stores local tunnel-like configuration records.
- Displays simulated state, traffic counters, logs, and topology concepts.
- Allows UI-level activation and deactivation of simulated tunnel entries.
- Can probe a configured SAM endpoint and establish an experimental transient control session.

Most displayed tunnel, peer, latency, packet-loss, and bandwidth values are simulated. The application does not currently verify that these values correspond to router state or routed application traffic.

### Local browser-style interface

- Stores bookmarks and safety labels.
- Demonstrates how `.i2p` destinations might be presented to users.
- Uses seeded fictional or illustrative entries for interface development.
- Contains early SAM name-lookup behavior when a control session is available.

The current application is not a hardened browser. A name lookup or proxy setting does not establish that every browser request is isolated from clearnet access.

### Messaging demonstration

- Stores locally generated message records.
- Visualizes routing and cryptographic stages through simulated logs.
- Generates automatic local responses for selected demonstration destinations.

The existing message path does not provide a reviewed end-to-end encryption protocol or verified I2P network delivery.

### Identity and trust interface

- Demonstrates public/private key fields, aliases, contacts, and trust status.
- Supports local import and deletion workflows.
- Models key verification as a user-interface concept.

Current key persistence and verification behavior are not suitable for real identities or private key material. A transient destination returned by SAM must not be represented as locally stored production key material.

## Security boundary

Do **not** use this prototype to protect sensitive communications, conceal a network address, store production destination keys, manage real infrastructure credentials, or make anonymity claims.

The following are specifically out of scope for the current build:

- verified application traffic routing through I2P;
- authoritative LeaseSet, peer, tunnel, bandwidth, latency, or packet-loss telemetry;
- production browser isolation and clearnet-fallback prevention;
- VPN protection;
- production-grade key storage;
- cryptographically verified peer handshakes in the local messaging model;
- reviewed end-to-end message encryption;
- quantum-resistant security;
- a completed external security assessment.

A SAM handshake verifies only that a compatible endpoint responded to the exchanged control messages. It does not prove anonymity, proxy enforcement, tunnel health, remote destination authenticity, or safe application-layer behavior.

A hash or checksum can verify that an artifact did not change after publication. It does not prove that the software is secure or anonymous.

## Architecture

```text
Jetpack Compose UI
        |
        v
ViewModel / repository layer
        |
        +------> Room database and simulation state
        |
        +------> Experimental SAM control socket
                     |
                     v
              Separately operated I2P router
```

The router process, Android application, proxy path, local database, and release pipeline are separate trust domains.

Router management interfaces should remain loopback-bound or isolated, authenticated where supported, minimally privileged, and inaccessible from untrusted networks. User-supplied SAM hosts must be validated against an explicit deployment policy before production use.

## Build from source

### Prerequisites

- JDK 17
- Android SDK compatible with the project Gradle configuration
- Android Studio or command-line Gradle tooling

### Build

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
./gradlew.bat assembleDebug
```

Run tests and static analysis available in the repository before installing any build.

## Development rules

1. Label simulated data and behavior explicitly in the UI and documentation.
2. Distinguish SAM control-session state from verified application traffic routing.
3. Do not describe encoding as encryption.
4. Do not introduce custom cryptographic protocols.
5. Keep private keys, router credentials, and operational logs out of source control.
6. Bind router APIs to loopback or an isolated container network by default.
7. Use least-privilege permissions in GitHub Actions, Android manifests, containers, and host scripts.
8. Map every external security claim to code, tests, a threat model, or a verifiable release artifact.
9. Fail closed when proxy, router, or destination state is unknown.

## Roadmap toward a defensible I2P client

- Isolate the experimental SAM code behind a typed adapter.
- Validate SAM host and port policy; default to loopback only.
- Add a reviewed stream/datagram transport path instead of equating session creation with routed traffic.
- Remove placeholder cryptographic behavior.
- Keep the Android client separate from router administration.
- Add destination-key protection using platform-backed secure storage where protocol-compatible.
- Add integration tests against an isolated local router fixture.
- Add IP-leak, DNS-leak, and clearnet-fallback test cases.
- Add WebView and URL-scheme security tests before browsing untrusted content.
- Add privacy-preserving telemetry with data minimization and explicit consent.
- Complete independent security review before claiming production readiness.

## Responsible use

This project supports lawful privacy engineering, education, defensive security, and resilient open infrastructure. It must not be represented as providing protections that have not been implemented and independently validated.

See [`SECURITY.md`](./SECURITY.md) for vulnerability reporting and [`THREAT_MODEL.md`](./THREAT_MODEL.md) for the current security analysis.