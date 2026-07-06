# Roadmap

## Phase 0 - Product Package

Goal: define the I2P Connect direction, MVP boundaries, and implementation prompts.

Deliverables:

- product spec
- architecture sketch
- security model
- responsible-use guidance
- phased implementation prompts

## Phase 1 - Private Messages And Audio Notes

Goal: make the smallest useful I2P-native communication loop.

Deliverables:

- communication readiness status
- local identity model
- contact invite create/import
- verified contact list
- private text message envelope
- direct send/receive prototype through measured I2P transport
- short encrypted audio note prototype
- beginner onboarding missions

Release stance:

- private alpha only
- no audited security claim
- no live calling claim

## Phase 2 - PBX-Light And Team Rooms

Goal: adapt cloud phone system concepts without pretending to be phone infrastructure.

Deliverables:

- shared team rooms
- group-owned identity concept
- routing labels
- message queues by role
- office-hours or availability notes
- admin-lite controls for small communities

Release stance:

- async-first
- no PSTN or SIP claim
- no enterprise compliance claim

## Phase 3 - Push-To-Talk Voice

Goal: test low-latency voice snippets or half-duplex rooms only where I2P measurements support it.

Deliverables:

- latency measurement harness
- push-to-talk prototype
- jitter and retry handling
- failure-first UI copy
- battery and permission review

Release stance:

- experimental
- not a phone-call replacement
- disabled unless network conditions are suitable

## Phase 4 - Experimental Video

Goal: explore whether tiny, low-frame-rate video clips or constrained live previews are usable on selected paths.

Deliverables:

- bandwidth and latency study
- video permission model
- local-only prototype
- measured I2P prototype if results justify it
- explicit UX limits

Release stance:

- no Zoom-like quality claim
- no mainstream video meeting claim
- research preview only

## Phase 5 - Community Adoption And Education Kit

Goal: help communities understand I2P-native communication safely.

Deliverables:

- guided setup docs
- tester scripts
- community safety guide
- facilitator checklist
- onboarding screenshots
- common failure recovery guide

Release stance:

- education-first
- honest limits
- no high-risk guarantee

## Cross-Phase Gates

Every phase must keep:

- release wording grounded in implemented behavior
- security boundaries current
- Android permission docs current
- tests for new state transitions
- no private material in logs
- lab features separated from release behavior
