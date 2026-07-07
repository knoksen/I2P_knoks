# Claim-Safe Writing

This guide turns `docs/CLAIMS_REGISTER.md` into practical writing rules for contributors, reviewers, release notes, screenshots, demos, and community posts.

Use it whenever public-facing text describes privacy, security, anonymity, routing, encryption, release maturity, or product readiness.

## Evidence Hierarchy

Use the weakest wording that matches the evidence.

| Evidence Level | What It Means | Safe Wording |
| :--- | :--- | :--- |
| Design intent | A desired direction with no implementation requirement yet. | "The project aims to..." |
| Roadmap | Planned work is documented. | "Planned"; "not available in the current release path." |
| Prototype | Demo or lab behavior exists. | "Prototype"; "lab-only"; "local preview." |
| Source implementation | Code exists and appears reachable. | "Available in the current source tree." |
| Tested implementation | Tests cover the behavior. | "Covered by current repository tests." |
| Release-path implementation | User-facing behavior is reachable, tested, documented, and claim-reviewed. | "`RELEASE_REAL` for this specific behavior." |
| Independent review or audit | A scoped external review exists and findings are handled. | "Independently reviewed for [scope]" only with a citation and claim-register entry. |

Passing validation is required for release-facing work, but it is not enough by itself to claim broad safety, anonymity, privacy, or production maturity.

## Approved Wording

Use these patterns when they match the evidence:

- "Available in the current source tree."
- "Implemented in the Android real-alpha source tree."
- "Experimental and subject to change."
- "Requires an external I2P or i2pd router."
- "Validated by the repository's current automated checks."
- "Not independently security-audited."
- "This feature does not provide an anonymity guarantee."
- "Planned work; not available in the current release path."
- "Lab-only preview."
- "Measured local service state."
- "Public-only identity export."
- "Private material excluded."

## Restricted Wording

The following words can be accurate in narrow technical contexts, but they require explicit evidence and nearby limits:

- Restricted wording: anonymous
- Restricted wording: secure
- Restricted wording: private
- Restricted wording: encrypted
- Restricted wording: hardened
- Restricted wording: audited
- Restricted wording: metadata-free
- Restricted wording: production-ready
- Restricted wording: untraceable
- Restricted wording: guaranteed
- Restricted wording: censorship-proof
- Restricted wording: end-to-end encrypted
- Restricted wording: E2EE
- Restricted wording: no logs
- Restricted wording: zero logs

Before using restricted wording, check `docs/CLAIMS_REGISTER.md`. If the register does not allow the wording for that surface, weaken it.

## Prohibited Claim Patterns

Do not use these as current product claims unless the claim register is updated with implementation, validation, threat-model, and audit evidence:

- Prohibited example: "Provides complete anonymity."
- Prohibited example: "Guarantees privacy."
- Prohibited example: "Cannot be traced."
- Prohibited example: "Stores no metadata."
- Prohibited example: "No logs" or "zero logs."
- Prohibited example: "Fully secure."
- Prohibited example: "Production-ready."
- Prohibited example: "Release-ready" without the exact validation scope.
- Prohibited example: "Audited encryption."
- Prohibited example: "Private by default."
- Prohibited example: "End-to-end encrypted" or "full E2EE."
- Prohibited example: "Censorship-proof."

## Surface-Specific Rules

### README

- Lead with current maturity and known limits.
- Separate implemented, lab/prototype, planned, and unsupported behavior.
- Link to `docs/CLAIMS_REGISTER.md`, this guide, `docs/RESPONSIBLE_USE.md`, and `docs/VALIDATION.md`.
- Do not use product-vision wording as a current feature claim.

### Android UI Text

- `RELEASE_REAL` text must describe measured state or persisted local configuration.
- Lab screens must use "LAB", "demo", "prototype", or "preview" near risky claims.
- Never say a lab message, route, VPN, invite, or crypto surface is active protection.
- Put limitations near the action, not only in a separate document.

### Onboarding

- Explain what this version can do, what is experimental, and what is not implemented.
- Mark mission completion from measured app state where possible.
- Do not imply that setup success proves privacy, anonymity, or protection.

### Release Notes

- Include `Real`, `Still LAB`, `Known Limits`, `Non-goals`, and `Verification`.
- State exactly which commands passed.
- Do not imply production signing, Play Store readiness, independent audit, anonymous browsing, secure chat, VPN, Tor, embedded router, or full browser behavior.

### Pull Requests

- Use the public claims checklist in `.github/PULL_REQUEST_TEMPLATE.md`.
- Register new or stronger public claims in `docs/CLAIMS_REGISTER.md`.
- Keep docs, UI copy, screenshots, and release notes aligned.

### Screenshots And Demonstrations

- Do not crop away lab labels or limitations.
- Say "demo", "lab", or "local preview" when showing prototype flows.
- Do not describe generated or simulated data as live network evidence.

### Social And Community Posts

- Prefer beginner-friendly limits over hype.
- Do not market the alpha as anonymous, secure, audited, metadata-free, production-ready, or suitable for high-risk use.
- Link to README known limits or release notes when announcing artifacts.

### Technical Architecture Documents

- Label architectural goals as goals.
- Use "future", "planned", or "requires implementation and tests" for unbuilt layers.
- Keep release-path claims in sync with `docs/CLAIMS_REGISTER.md`.

## Claim Review Checklist

Before merging public-facing wording, ask:

1. Is the behavior implemented?
2. Is it reachable through the relevant release path?
3. Is it tested?
4. Is the test meaningful for the claim?
5. Are limitations shown near the claim?
6. Could a non-technical reader interpret it more strongly than intended?
7. Does the claim register permit this wording?
8. Does the statement create a security, anonymity, encryption, or privacy expectation?

If the answer is uncertain, weaken the wording and file a follow-up task for implementation or validation evidence.
