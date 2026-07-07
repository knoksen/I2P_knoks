# Security Policy

## Project status

I2P_knoks is currently an educational prototype and interface simulator. It is not a production I2P router, anonymity service, VPN, or secure messaging system.

Security reports are still important, especially when they concern unsafe key storage, unintended network access, WebView behavior, exported Android components, dependency vulnerabilities, workflow permissions, secret exposure, or misleading security claims.

## Supported versions

No stable production release is currently supported.

| Version or branch | Security support |
|---|---|
| Latest commit on `main` | Best-effort fixes |
| Development branches | No guaranteed support |
| Older APKs, forks, and third-party builds | Not supported |

## Reporting a vulnerability

### Preferred method

Use GitHub's private vulnerability reporting flow from the repository **Security** tab when the **Report a vulnerability** option is available.

Include:

- affected commit, branch, or artifact;
- concise vulnerability description;
- reproduction steps or proof of concept;
- expected and actual behavior;
- realistic impact and threat actor assumptions;
- relevant logs with secrets and personal data removed;
- suggested remediation, when available.

### When private reporting is unavailable

Open a minimal public issue titled `Security contact requested` and include **no exploit details, secrets, private keys, personal information, or identifying operational data**. A maintainer can then establish an appropriate private channel.

Do not publish a working exploit before remediation or coordinated disclosure.

## Response targets

These are best-effort targets, not contractual service levels:

- acknowledgement within 5 business days;
- initial triage within 10 business days;
- status updates when the risk or remediation timeline changes;
- coordinated disclosure after a fix or documented mitigation is available.

A report may be closed when it is not reproducible, is outside project scope, describes an explicitly documented simulation limitation, or requires unsupported third-party infrastructure.

## Security scope

Reports are especially useful for:

- accidental external exposure of router or management interfaces;
- Android components exported without a justified need;
- insecure WebView configuration or unsafe URL handling;
- path traversal, command injection, or unsafe process execution;
- private keys, credentials, tokens, or sensitive logs committed to the repository;
- insecure persistence or backup of identity material;
- dependency or supply-chain vulnerabilities;
- excessive GitHub Actions permissions;
- artifact substitution or release-integrity failures;
- documentation or UI text that falsely represents simulated behavior as real protection.

## Out of scope

Unless they reveal an additional vulnerability, the following are known project limitations rather than reportable security defects:

- the prototype does not currently route traffic through I2P;
- simulated tunnel and telemetry values are not network measurements;
- placeholder message encoding is not end-to-end encryption;
- the application is not approved for sensitive or anonymous communications;
- no production anonymity guarantee is made.

## Research and safe harbor

Good-faith research should:

- remain within systems and accounts the researcher owns or is explicitly authorized to test;
- avoid privacy violations, service disruption, persistence, social engineering, and data destruction;
- use the minimum access needed to demonstrate the issue;
- stop when sensitive information is encountered;
- provide reasonable time for remediation before disclosure.

The maintainer intends not to pursue action against good-faith research that follows this policy, but cannot authorize testing against third-party systems, I2P peers, hosted services, or infrastructure outside the repository maintainer's control.

## Operational-security warning

Never include real I2P destination private keys, router credentials, API tokens, SSH keys, recovery material, unredacted IP addresses, or identifying traffic logs in a report or issue.