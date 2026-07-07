# Security Policy

## Supported Scope And Current Alpha

This repository is a real-alpha Android and product-planning project for I2P Knoks Browser and I2P Connect work.

Security review and maintenance apply to the current `main` branch, release-candidate review work, and tagged real-alpha artifacts published by the project maintainer.

The current Android app is a real-alpha build. It contains measured local I2P endpoint diagnostics, SAM session work, configured HTTP proxy page inspection for `.i2p` URLs, local preview/lab surfaces, and local persistence. It is not a production candidate and is not independently audited.

## Current Boundaries

- The app depends on an external I2P or i2pd router for real I2P behavior.
- The app requests `android.permission.INTERNET` for configured endpoint diagnostics, SAM work, router console launch intents, and `.i2p` page inspection through the configured HTTP proxy.
- The app does not provide anonymity by itself.
- The app does not provide an OS-level VPN tunnel.
- The app does not provide Tor routing.
- The app does not provide audited encrypted chat.
- The app does not provide an embedded I2P router in the current alpha.
- Lab and simulation features must remain labeled and must not be described as active protection.

For fuller boundaries, see:

- `docs/SECURITY_BOUNDARIES.md`
- `docs/ANDROID_PERMISSIONS.md`
- `docs/RESPONSIBLE_USE.md`
- `docs/CLAIMS_REGISTER.md`
- `docs/CLAIM_SAFE_WRITING.md`

## Secret Handling

Treat the following as secrets:

- private I2P destination material
- app-layer private keys
- router credentials
- tunnel keys
- signing keys
- service role keys
- database credentials
- API keys

Never include secrets in commits, screenshots, issue comments, logs, release notes, sample configs, or external services.

Use `.env.example` only for variable names and safe placeholders.

## Reporting A Vulnerability

Report security issues privately to the project maintainer before creating public issue details. Do not open a public issue that contains exploit details, private material, credentials, message bodies, raw router logs, or sensitive headers.

Include:

- affected commit, tag, or artifact
- short reproduction steps
- expected impact
- whether private material, messages, logs, router credentials, or external services are involved
- any safe proof that avoids exposing secrets

Do not include private keys, private destinations, plaintext message bodies, credentials, raw router logs, sensitive headers, or API keys in the report body. If a finding requires sensitive evidence, first describe the issue at a high level and wait for maintainer guidance.

## Maintainer Response

The maintainer should triage reports by:

- confirming the affected boundary
- deciding whether the report belongs in this repository's current alpha scope
- checking whether release-facing claims must be corrected
- identifying whether secrets, private identity material, message bodies, router credentials, or external services are involved
- adding or updating tests where possible
- updating docs when the security boundary changes
- running local validation before publishing a fix

No fixed response-time promise is made in this real-alpha policy. The expected response is careful triage, reduced public exposure of sensitive details, and claim-safe documentation of any fix or limitation.

## Responsible Disclosure Notes

- Keep public reports limited to non-sensitive reproduction summaries until the maintainer has reviewed the issue.
- Do not post working exploit details, private destinations, private keys, service credentials, raw logs, or message contents publicly.
- If a report shows that documentation overstates implemented behavior, correct the claim as part of the fix.
- If a report affects a released artifact, update release notes or known-limit documentation as needed.
- If a report falls outside the current alpha, record the limitation without implying that unsupported functionality exists.

## Validation

Before security-relevant changes are considered complete, run:

```powershell
.\scripts\check-release-claims.ps1
.\scripts\local-release-verify.ps1
```

If validation cannot run, document the exact blocker and the last successful narrower command.
