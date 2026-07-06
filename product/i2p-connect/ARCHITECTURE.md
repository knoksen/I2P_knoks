# Architecture

## Overview

I2P Connect should be implemented as a bounded communication module that reuses the current app's real-alpha rules:

- runtime status comes from measured diagnostics or persisted configuration
- lab previews stay in `LAB_SIMULATION`
- `RELEASE_REAL` surfaces only show implemented, tested behavior
- private destination material and plaintext messages never enter logs

The MVP can live in the existing Android app first, then be split into a dedicated module if the product grows.

## Suggested Package Layout

```text
app/src/main/java/no/knoksen/i2pbrowser/connect/
  status/
  identity/
  contacts/
  messaging/
  audio/
  onboarding/
  ui/
```

## Primary Components

### Connect Status Service

Purpose:

- expose communication readiness as one state object
- reuse endpoint configuration and diagnostics
- include SAM lifecycle readiness when available
- map status to user-facing actions

Inputs:

- `I2pEndpointConfig`
- `I2pDiagnosticsResult`
- `SamSessionStatus`
- local identity availability

Outputs:

- router readiness
- SAM readiness
- identity readiness
- message-send eligibility
- recommended next action

### Identity Store

Purpose:

- hold local user communication identities
- bind identity records to public I2P destination material
- protect private destination material
- expose short fingerprints for verification

MVP storage:

- Room tables for metadata
- Android Keystore-backed encryption for private material where available
- no private key values in UI state, crash reports, or logs
- public-only identity export/import by default
- cloud sync disabled by default

### Contact And Invite Service

Purpose:

- create shareable invites
- parse imported invites
- validate invite schema and fingerprint
- store trusted contacts separately from unverified imports

Invite formats:

- copyable text code for MVP
- QR code in a later UI pass
- local file import/export after schema stability

### Message Envelope Service

Purpose:

- build encrypted message envelopes
- store outbound and inbound message state
- retry sends without duplicating messages
- keep plaintext outside logs

Envelope fields:

- envelope id
- sender identity id
- recipient contact id
- content type
- ciphertext
- nonce
- key id or ratchet metadata
- created timestamp
- delivery state

Implementation note:

The existing `SecureMessage` entity is a lab/demo model. The production-bound MVP should create a new explicit model instead of upgrading the demo table in place unless a migration plan is written and tested.

### I2P Transport Adapter

Purpose:

- isolate SAM STREAM or future transport behavior behind a small interface
- provide send/receive results with measured timing and errors
- avoid leaking protocol details into UI

Initial interface shape:

```kotlin
interface ConnectTransport {
    suspend fun canSend(): ConnectTransportStatus
    suspend fun send(envelope: OutboundEnvelope): SendResult
    suspend fun receiveOnce(identityId: Long): ReceiveResult
}
```

MVP transport can start with direct SAM STREAM behavior and a retry queue. If both parties must be online, the UI must say so plainly.

### Audio Message Service

Purpose:

- record short voice notes
- cap duration and file size
- encode and encrypt attachment payloads
- provide local playback after decrypt

Android permissions:

- request `RECORD_AUDIO` only when the feature is implemented
- document the permission boundary before release
- keep audio disabled in `RELEASE_REAL` until recording, encryption, storage, and playback tests pass

### Onboarding Missions

Purpose:

- guide new users through real setup steps
- make completion measurable
- avoid burying safety guidance in long text

Mission completion should be stored locally so the user can resume setup.

## Data Model Sketch

```text
connect_identities
  id
  displayName
  publicDestination
  publicAppKey
  fingerprint
  privateMaterialRef
  privateMaterialState
  origin
  cloudSyncEnabled
  createdAt
  updatedAt

connect_contacts
  id
  displayName
  publicDestination
  publicAppKey
  fingerprint
  verificationState
  trustNotes
  createdAt
  updatedAt

connect_messages
  id
  conversationId
  senderId
  recipientContactId
  direction
  contentType
  ciphertextRef
  plaintextCacheRef
  deliveryState
  createdAt
  updatedAt

connect_attachments
  id
  messageId
  type
  ciphertextRef
  durationMs
  sizeBytes
  codec
  createdAt

connect_onboarding_missions
  id
  key
  completedAt
  lastSeenAt
```

## UI Surface

Recommended tabs or screens:

- Connect Home: readiness, identity, next mission
- Contacts: verified and unverified contacts
- Conversations: text and audio note threads
- Invites: create, copy, import, verify
- Safety: concise limits and responsible use

`RELEASE_REAL` should only expose the module when the implemented surfaces are backed by real state. Experimental features can remain in `LAB_SIMULATION`.

## Testing Strategy

- Unit tests for invite parsing and fingerprint generation.
- Unit tests for message envelope state transitions.
- Unit tests for status mapping from diagnostics/SAM/identity state.
- Room migration tests for new tables.
- Instrumented tests for permission-gated audio UI when audio lands.
- Log sanitizer tests for private destinations, plaintext, and attachment metadata.

## Observability

Allowed logs:

- status check started/completed
- message state changed without body text
- send failed with sanitized reason
- invite parse failed with schema reason

Disallowed logs:

- private destination material
- app-layer private keys
- message body
- decrypted audio paths that reveal user content
- contact invite strings containing full public keys unless explicitly sanitized
