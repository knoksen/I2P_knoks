# Responsible Use

## Intent

I2P Connect is for lawful, consent-based private communication, community coordination, and education about decentralized networks.

The product should help people communicate with dignity while avoiding claims that could make users overestimate their safety.

## User-Facing Boundaries

The app should explain:

- I2P Connect depends on a working I2P or i2pd router.
- The app does not provide anonymity by itself.
- The MVP is not externally audited.
- Audio notes can reveal identity through voice.
- Delivery may be delayed or fail when contacts are offline.
- The app is not for emergencies.

## Abuse-Resistant Product Choices

MVP design choices:

- no public contact discovery
- no global directory
- no phone contact scraping
- no hidden background recording
- no mass messaging tools
- no automated invite harvesting
- no false online presence

## Consent And Contact Trust

The invite flow should make trust explicit:

- import starts as unverified
- user must review fingerprint
- verified contacts are visually distinct
- key changes require renewed attention
- deleting a contact should be simple

## Safety UX

Keep safety copy short and placed near decisions:

- before exporting identity
- before trusting an invite
- before first message
- before first audio note
- before backup/export of private material

Avoid long generic warnings that users will skip.

## Community Guidelines For Alpha Testers

Alpha testers should:

- test with consenting contacts
- avoid sharing sensitive personal data
- report confusing safety copy
- report delivery and decrypt failures
- avoid using the app for emergencies
- avoid presenting the product as reviewed or production-ready

## Documentation Tone

Good:

- "This message is encrypted by the app before send."
- "Your I2P router is reachable."
- "The recipient may need to be online."
- "This audio note can reveal your voice."

Bad:

- "You are fully protected."
- "Use this for any sensitive situation."
- "Video calls work like mainstream conferencing."
- "No one can identify you."

## Maintainer Checklist

- [ ] Do new features increase abuse potential?
- [ ] Does the UI explain limits before risky actions?
- [ ] Are permissions minimal?
- [ ] Are logs sanitized?
- [ ] Are lab features clearly separated from release behavior?
- [ ] Are user claims backed by tests or measured state?
