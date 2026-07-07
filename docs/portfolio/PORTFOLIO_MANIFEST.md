# Portfolio Manifest

## Purpose

This publication unifies two complementary bodies of work:

1. A security-engineering portfolio centered on I2P, DevSecOps, secure build pipelines, monitoring, containerization, and full-stack infrastructure.
2. A systems-resilience study examining portable identity, reputation continuity, decentralized trust, and adaptation after centralized platform failure.

## Editorial boundary

The material is retained for lawful privacy engineering, defensive security, resilience research, and professional portfolio use. Descriptions of illicit-market behavior are analytical case-study material only and are not implementation guidance.

## Evidence classes

| Class | Definition | Publication rule |
|---|---|---|
| Verified implementation | Supported by repository artifacts, commits, workflows, test output, or signed release artifacts | May be stated as implemented when the evidence remains current |
| Prototype or simulation | Implemented in a limited, demonstrative, or non-production form | Must be labeled as prototype, demo, visualization, or simulation |
| Portfolio claim | A capability or result requiring direct primary evidence | Must not be presented as verified until evidence is linked |
| Research synthesis | Analytical interpretation of supplied or cited material | Must be distinguished from implementation evidence |
| Future target | Planned architecture, control, or outcome | Must be written as a roadmap item, not a current capability |

## Required claim controls

Before publication, verify the following against primary evidence:

- repository and release status;
- supported platform versions;
- contribution counts and dates;
- test results and workflow state;
- cryptographic algorithms actually used by the implementation;
- whether network statistics are measured, simulated, or illustrative;
- whether external URLs and hosted applications are controlled and current;
- project maturity labels such as alpha, beta, stable, or production-ready.

## Prohibited unsupported guarantees

Do not describe the system as any of the following without specific, reproducible evidence:

- anonymous or untraceable;
- quantum-safe or quantum-resistant;
- military-grade;
- zero-trust merely because encryption is present;
- end-to-end encrypted without a verified protocol boundary;
- production-ready based only on a successful build;
- secure because an artifact has a SHA-256 checksum.

## Operational-security principle

The portfolio must never encourage users to expose I2P router consoles, SAM/I2CP interfaces, SSH endpoints, private keys, destination keys, or management services to untrusted networks. Any deployment material must preserve least privilege, loopback binding where appropriate, explicit firewalling, secret separation, and minimal telemetry.