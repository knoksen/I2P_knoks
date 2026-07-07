# Change summary

This branch adds the integrated security and I2P portfolio publication structure without committing large binary PDFs to Git history.

## Added

- evidence-aware portfolio documentation;
- canonical release checksums;
- cross-platform PowerShell integrity verification;
- a GitHub CLI release publisher with hash gating and dry-run support;
- validation, publication, and next-step checklists.

## Validation limitation

The artifact hashes were recalculated successfully. PowerShell 7 was not available in the preparation environment, so script parsing and execution remain required before merge or publication.
