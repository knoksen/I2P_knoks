# Secure Systems & I2P Portfolio

The integrated security and I2P portfolio is documented under [`docs/portfolio`](./docs/portfolio/README.md).

## Publication artifacts

The prepared publication set contains:

- Executive portfolio PDF
- Complete portfolio PDF
- Integrity-verifiable publication ZIP
- SHA-256 checksum manifest

Large binary publications are distributed as GitHub Release assets rather than stored in the source history.

## Engineering boundary

Repository behavior, simulations, visualizations, planned architecture, and production security properties must remain explicitly distinguished. See the [`Portfolio Manifest`](./docs/portfolio/PORTFOLIO_MANIFEST.md) for the evidence model and publication rules.

## Release workflow

```powershell
pwsh ./scripts/Publish-PortfolioRelease.ps1 \
  -Repository knoksen/I2P_knoks \
  -Tag portfolio-v1.0.0 \
  -PackageRoot /path/to/prepared/artifacts
```

The publishing script verifies the canonical SHA-256 hashes before creating or updating the release.