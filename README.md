# I2P Network Console Simulator

> [!IMPORTANT]
> This repository currently contains an **educational Android prototype and interface simulator**. It is **not an I2P router, anonymity tool, VPN, production browser, or secure messenger**. It does not presently establish real I2P tunnels or connect through SAM/I2CP.

The project explores user-interface patterns for privacy technology using Kotlin, Jetpack Compose, Room, and local simulated state. Its purpose is to make concepts such as destinations, tunnels, key continuity, telemetry, and operational warnings easier to understand before real router integration is attempted.

## Current maturity

| Area | Current state |
|---|---|
| Android UI | Prototype implemented with Jetpack Compose |
| Local persistence | Room entities and DAOs |
| Tunnel management | Simulated local records and status changes |
| Network telemetry | Illustrative/generated values, not measurements from I2P |
| Message transport | Local simulation only |
| Payload protection | Placeholder behavior; Base64 is used in parts of the prototype and is **not encryption** |
| Identity handling | Demonstrative local key generation and storage; not production-safe |
| I2P router integration | Not implemented |
| VPN functionality | Not implemented |
| Production security review | Not completed |

## Implemented prototype modules

### Console and tunnel visualization

- Creates and stores local tunnel-like configuration records.
- Displays simulated state, traffic counters, logs, and topology concepts.
- Allows UI-level activation and deactivation of simulated tunnel entries.

No real inbound or outbound I2P tunnel is created by these controls.

### Local browser-style interface

- Stores bookmarks and safety labels.
- Demonstrates how `.i2p` destinations might be presented to users.
- Uses seeded fictional or illustrative entries for interface development.

The current application is not a hardened browser and must not be used to access untrusted content.

### Messaging demonstration

- Stores locally generated message records.
- Visualizes routing and cryptographic stages through simulated logs.
- Generates automatic local responses for selected demonstration destinations.

The existing message path does not provide a reviewed end-to-end encryption protocol or network delivery.

### Identity and trust interface

- Demonstrates public/private key fields, aliases, contacts, and trust status.
- Supports local import and deletion workflows.
- Models key verification as a user-interface concept.

Current key persistence and verification behavior are not suitable for real identities or private key material.

## Security boundary

Do **not** use this prototype to protect sensitive communications, conceal a network address, store production destination keys, manage real infrastructure credentials, or make anonymity claims.

The following are specifically out of scope for the current build:

- verified Garlic Routing;
- real LeaseSet publication or lookup;
- SAM, BOB, or I2CP router communication;
- network-level tunnel construction;
- VPN or proxy protection;
- production-grade key storage;
- cryptographically verified peer handshakes;
- trustworthy uptime, latency, node, or traffic measurements;
- quantum-resistant security.

A hash or checksum can verify that an artifact did not change after publication. It does not prove that the software is secure or anonymous.

## Architecture

```text
Jetpack Compose UI
        |
        v
ViewModel / repository layer
        |
        v
Room database and local simulation state
```

A future real-network architecture must add a narrowly scoped adapter to a separately operated I2P router. Router management interfaces should remain loopback-bound or isolated, authenticated, minimally privileged, and inaccessible from untrusted networks.

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
2. Do not describe encoding as encryption.
3. Do not introduce custom cryptographic protocols.
4. Keep private keys, router credentials, and operational logs out of source control.
5. Bind router APIs to loopback or an isolated container network by default.
6. Use least-privilege permissions in GitHub Actions, Android manifests, containers, and host scripts.
7. Map every external security claim to code, tests, a threat model, or a verifiable release artifact.

## Roadmap toward real I2P integration

- Define a threat model and trust boundaries.
- Remove placeholder cryptographic behavior.
- Introduce a typed SAM or I2CP adapter behind an interface.
- Keep the Android client separate from router administration.
- Add destination-key protection using platform-backed secure storage.
- Add integration tests against an isolated local router fixture.
- Add IP-leak and DNS-leak test cases.
- Add privacy-preserving telemetry with data minimization and explicit consent.
- Complete independent security review before claiming production readiness.

## Responsible use

This project supports lawful privacy engineering, education, defensive security, and resilient open infrastructure. It must not be represented as providing protections that have not been implemented and independently validated.

See [`SECURITY.md`](./SECURITY.md) for vulnerability reporting and [`THREAT_MODEL.md`](./THREAT_MODEL.md) for the current security analysis.