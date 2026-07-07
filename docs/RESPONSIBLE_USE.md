# Responsible Use

## Intent

I2P Knoks Browser and I2P Connect work should support lawful, consent-based private communication, local diagnostics, education, and community coordination.

The project should help users understand I2P-related state without making them overestimate their safety.

Public-facing responsible-use wording should follow [Claim-Safe Writing](CLAIM_SAFE_WRITING.md) and the classifications in [Claims Register](CLAIMS_REGISTER.md).

## User-Facing Boundaries

- The app depends on a working I2P or i2pd router for real I2P behavior.
- The app does not provide anonymity by itself.
- The current alpha is not externally audited.
- Delivery may be delayed or fail when local services or contacts are unavailable.
- Audio notes can reveal identity through voice if that feature is implemented later.
- The app is not for emergencies.

## Abuse-Resistant Defaults

- No public contact discovery by default.
- No global directory by default.
- No phone contact scraping.
- No hidden background recording.
- No mass messaging tools.
- No automated invite harvesting.
- No false online presence.

## Consent And Contact Trust

Future invite and contact flows should make trust explicit:

- imported contacts start unverified
- users review fingerprints before trust is granted
- verified contacts are visually distinct
- key changes require renewed attention
- deleting a contact is simple and local-first

## Safety UX

Show short responsible-use copy near risky actions, including:

- exporting identity material
- trusting an invite
- sending a first message
- recording a first audio note if audio exists
- enabling logs
- exporting diagnostics
- using any cloud-connected feature

Avoid long generic warnings that users will skip.

## Alpha Tester Guidance

Alpha testers should:

- test with consenting contacts
- avoid sharing sensitive personal data
- report confusing safety copy
- report delivery and decrypt failures
- avoid using the app for emergencies
- avoid presenting the product as reviewed or broadly protective

## Maintainer Checklist

- [ ] Do new features increase abuse potential?
- [ ] Does the UI explain limits before risky actions?
- [ ] Are permissions minimal?
- [ ] Are logs sanitized?
- [ ] Are lab features clearly separated from release behavior?
- [ ] Are user claims backed by tests or measured state?
