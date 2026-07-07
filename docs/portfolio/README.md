# Secure Systems & I2P Portfolio

This portfolio connects defensive security architecture, I2P privacy engineering, DevSecOps, container isolation, artifact integrity, privacy-preserving observability, and systems-resilience research.

## Scope

The portfolio is designed for lawful privacy engineering, defensive security, professional review, research discussion, and project due diligence.

It deliberately separates:

- **implemented repository evidence** from aspirational descriptions;
- **prototype or simulation behavior** from production capability;
- **privacy engineering** from unsupported anonymity guarantees;
- **research synthesis** from operational instructions.

## Portfolio editions

| Edition | Intended use | Distribution |
|---|---|---|
| Executive | External introductions, project pitches, recruitment, and partner briefings | GitHub Release asset |
| Complete | Technical review, research discussion, and due diligence | GitHub Release asset |

Large binary PDFs are distributed as release assets rather than committed to the Git history. This keeps clones lightweight and avoids permanent repository bloat.

## Release assets

Expected asset names:

- `Secure_Systems_I2P_Portfolio_Executive.pdf`
- `Secure_Systems_I2P_Portfolio_Complete.pdf`
- `Secure_Systems_I2P_Portfolio_Publish.zip`
- `SHA256SUMS`

The canonical checksums for the current prepared artifacts are recorded in [`RELEASE_SHA256SUMS`](./RELEASE_SHA256SUMS).

## Integrity verification

Download the release assets into one directory and run:

```powershell
pwsh ./scripts/Verify-PortfolioIntegrity.ps1 -PackageRoot ./downloaded-assets
```

On Linux or macOS:

```bash
cd downloaded-assets
sha256sum -c SHA256SUMS
```

A checksum match verifies file integrity. It does **not** independently validate the factual accuracy, maturity, or security claims inside the documents.

## Publishing

The release publication script validates local artifacts before uploading them:

```powershell
pwsh ./scripts/Publish-PortfolioRelease.ps1 \
  -Repository knoksen/I2P_knoks \
  -Tag portfolio-v1.0.0 \
  -PackageRoot /path/to/Secure_Systems_I2P_Portfolio_Publish
```

The script requires the authenticated GitHub CLI (`gh`) and refuses to publish when an expected artifact or checksum is missing.

## Core architectural themes

1. I2P tunnel separation, Garlic Routing concepts, and leak prevention.
2. Least-privilege Docker and host isolation.
3. CI/CD permissions, static analysis, and release artifact verification.
4. Cryptographic identity continuity and portable trust.
5. Privacy-preserving telemetry with strict data minimization.
6. Recovery from centralized infrastructure failure.
7. Evidence-based communication of security properties.

## Security boundary

Neither this portfolio nor the associated application should claim that a prototype, visual simulator, VPN toggle, cryptographic primitive, or checksum alone provides anonymity, quantum resistance, operational security, or production readiness.

Security properties must be supported by reproducible implementation evidence, threat modeling, tests, dependency review, and deployment-specific validation.