# Product Spec

## Product Name

I2P Connect

## Product Thesis

Open communities need private communication tools that are understandable, local-first, and honest about network readiness. I2P Connect should feel as approachable as a cloud phone/contact system while using I2P-native identity and asynchronous communication patterns.

The phase 1 product target is:

> A future I2P Connect release path should let a user create an I2P contact identity, verify readiness, invite trusted contacts, exchange private messages, and send short encrypted audio notes with clear status and beginner guidance.

The product must not promise Zoom-like real-time video quality, enterprise PBX completeness, PSTN calling, emergency calling, or anonymity by itself.

## Users

- New I2P users who need a guided communication flow instead of raw router settings.
- Small open-source communities that want private contact lists and message channels.
- Mutual-aid, education, or civic groups that need slower but resilient asynchronous communication.
- Developers testing I2P identity, SAM, and message transport patterns on Android.

## Product Principles

- Measured status over hopeful labels.
- Async-first communication over real-time promises.
- Contact trust before message exchange.
- Plain explanations for beginners.
- Small, inspectable features before broad platform claims.
- Local control of identity, contacts, message storage, and exports.

## Core MVP Capabilities

### 1. I2P Status Checks

The app shows a readiness panel before communication actions:

- endpoint host and ports
- SAM reachability
- router console reachability
- app identity availability
- last successful check timestamp
- recommended next action when not ready

Messaging actions must remain disabled or clearly queued when I2P readiness is incomplete.

### 2. Contact Identity

Each user can create or import a local communication identity backed by I2P destination material.

The product should show:

- display name
- public destination or shareable contact code
- short fingerprint for human verification
- local verification state
- created/updated timestamps

The product must not show private destination material except inside deliberate backup/export flows with strong warnings.

### 3. Contact Invites

A contact invite is a shareable bundle that can be copied, shown as QR, imported from text, or opened from a local file.

Minimum invite fields:

- product marker and version
- display name
- public destination
- public app-layer key
- fingerprint
- optional profile note
- created timestamp

The import flow should make verification visible before trust is granted.

### 4. Private Messages

Phase 1 messaging should be asynchronous and queue-aware:

- compose text message
- encrypt payload for a verified contact
- store outbound envelope locally
- send through I2P transport when ready
- show clear states: drafted, queued, sending, sent, failed, received, decrypted
- retry failed sends without duplicating user-visible messages

Message bodies should not appear in logs.

### 5. Encrypted Audio Messages

Audio is a short message attachment, not a live call.

MVP behavior:

- record short voice note after explicit user action
- encode to a compact format selected for Android support and size limits
- encrypt as an attachment envelope
- show duration, size, and delivery state
- allow playback only after successful decrypt

The MVP should not request microphone permission until the audio prototype is actually implemented.

### 6. Beginner Onboarding Missions

The onboarding should be mission-based:

1. Check I2P router readiness.
2. Create a local identity.
3. Copy or import an invite.
4. Verify a contact fingerprint.
5. Send a first private text message.
6. Send a short audio note.
7. Review safety and responsible-use boundaries.

Each mission should have a measured completion condition where possible.

## Future PBX-Light Concepts

After phase 1, the product can borrow cloud phone system language carefully:

- team rooms as shared conversation spaces
- routing labels for who should receive a message
- shared inboxes for group-owned identities
- office hours and availability notes
- audio note queues as voicemail-like workflows
- push-to-talk rooms only after network measurement supports the UX

These concepts must remain I2P-native and should not imply phone network interoperability.

## Non-Goals

- No Zoom-like real-time video claim.
- No PSTN/SMS replacement.
- No emergency calling.
- No enterprise compliance claim.
- No audited cryptography claim until an external review exists.
- No claim that I2P Connect provides anonymity by itself.
- No centralized account system.
- No contact upload or phone address-book sync in the MVP.

## Success Criteria

The MVP is successful when a new user can:

- understand whether their local I2P setup is ready
- create a local identity without seeing private key material
- exchange invites with a trusted contact
- send and receive private text messages through a measured I2P path
- send and receive a short encrypted audio note
- understand the product's limits before using it for sensitive communication

## Release Wording Rule

Release-facing copy must be grounded in implemented behavior. Use language such as:

- "I2P router reachable"
- "message queued"
- "payload encrypted with the configured app-layer key"
- "audio note stored locally"
- "not externally audited"

Avoid language such as:

- "anonymous by default"
- "live video ready"
- "enterprise phone replacement"
- "audited encrypted chat"
- "safe for high-risk use"
