# Security Model

## Security Posture

I2P Connect should be designed as a privacy-preserving communication layer, but the project must be honest about maturity. Until cryptography, storage, transport, and UX have been externally reviewed, the product should not claim audited security or suitability for high-risk use.

## Trust Boundaries

### Local Device

Trusted for:

- storing user identity metadata
- holding encrypted private material references
- rendering decrypted message content
- recording and playing audio notes after explicit user action

Risks:

- compromised device
- screenshots or screen recording
- backups that include sensitive data
- notification previews
- debug logs

### I2P Router

Trusted for:

- providing I2P network connectivity
- exposing SAM and proxy services when configured

Not trusted for:

- app-layer message secrecy
- contact verification
- content safety
- proving user identity

### Remote Contact

Trusted only after user verification.

Risks:

- wrong invite imported
- contact key changed without notice
- impersonation through copied display names
- malicious attachments

## Data Classes

### Highly Sensitive

- private destination material
- app-layer private keys
- local private-material references
- decrypted message bodies
- decrypted audio files
- local backup passphrases

### Sensitive

- public destinations
- contact graph
- message timestamps
- delivery state
- audio duration and file size

### Low Sensitivity

- app version
- onboarding completion state
- generic status errors

## Cryptography Requirements

MVP cryptography must use well-maintained libraries instead of handwritten primitives.

Required properties:

- authenticated encryption for every message payload
- unique nonces
- replay protection or duplicate envelope detection
- contact key fingerprint verification
- key rotation plan before broad release
- private key material protected by Android platform storage where possible

Recommended direction:

- choose one reviewed Android-compatible crypto library
- wrap it behind a small `MessageCrypto` interface
- write deterministic tests with fixed test vectors
- label the result as implementation-tested, not externally audited

## Message Security

Message envelopes should include:

- version
- sender public identity
- recipient public identity or key id
- content type
- nonce
- ciphertext
- created timestamp
- optional attachment metadata

Message envelopes should not include:

- plaintext preview
- private key material
- raw audio samples
- device identifiers
- phone contacts

## Audio Security

Audio notes increase privacy risk because a voice can identify a person.

Controls:

- explicit record button
- visible recording timer
- short duration cap
- local delete
- encrypted attachment storage
- playback only after decrypt
- no background recording
- no microphone permission before the feature exists

## Logging Rules

Never log:

- private destinations
- private keys
- private material references
- full invite strings
- plaintext messages
- decrypted audio paths
- audio transcription
- passwords or backup secrets

Allowed logs should be coarse and sanitized:

- "invite import failed: missing fingerprint"
- "message queued: router not ready"
- "send failed: SAM unavailable"
- "audio note encrypted: duration=12s size=48KB"

## Permissions

Phase 1 text messaging should not require new Android permissions beyond the existing network permission.

Audio recording requires `RECORD_AUDIO`, but only when:

- implementation exists
- UI is permission-gated
- docs explain the boundary
- tests cover denial and revoke flows

The app should not request contacts, SMS, phone, location, camera, or VPN permissions for this MVP.

## Threats And Mitigations

| Threat | Mitigation |
| :--- | :--- |
| Contact impersonation | Fingerprints, verification state, key-change warnings |
| Message disclosure through logs | Log sanitizer tests and no-body logging rule |
| Private key exposure | Protected storage reference and no UI/log display |
| Replay or duplicate messages | Envelope ids and duplicate detection |
| Router unavailable | Queue state and clear readiness panel |
| Audio identity exposure | Warnings, short notes, no auto-recording |
| Overclaiming product safety | Release wording gate and responsible-use screen |

## Release Security Checklist

- [ ] No private material appears in UI screenshots.
- [ ] No message body appears in logs.
- [ ] Invite parser rejects malformed or oversized input.
- [ ] Contact key changes are visible.
- [ ] Message crypto has unit tests and test vectors.
- [ ] Audio files are deleted when messages are deleted.
- [ ] Android permission docs match the manifest.
- [ ] Responsible-use copy appears before first sensitive action.
