# Validation Record

## Completed

- Verified that the Executive PDF, Complete PDF, and publication ZIP exist and are readable as files.
- Recalculated SHA-256 digests for all three prepared release artifacts.
- Recorded the verified digests in `RELEASE_SHA256SUMS`.
- Kept large binary assets outside Git history and prepared a GitHub Release workflow.
- Added path-traversal protection to the integrity verification script.
- Added exact-name and single-match requirements to the release publisher.
- Added `SupportsShouldProcess`, enabling `-WhatIf` for publication dry runs.

## Required before merge or publication

- Parse and execute both PowerShell scripts with PowerShell 7 on Windows or Linux.
- Run the publisher with `-WhatIf` against the prepared artifact directory.
- Confirm `gh auth status` uses the intended GitHub account.
- Confirm the release target branch and tag are correct.
- Review every external-facing claim against repository evidence.
- Do not publish secrets, destination keys, private keys, router configuration, or identifying operational logs.

## Current artifact digests

| Artifact | SHA-256 |
|---|---|
| Executive PDF | `0bd89e2b6c56361e8fd90aa634db849fe3965ebefeb301c8f38b954d740cd7a5` |
| Complete PDF | `49e9a4bf0c18ae3ac5cd2387f696f2082e7c2dec00129c9e0966ef398f7299ca` |
| Publication ZIP | `e1fca3f2502a1dc17c2f7e5693b2c540e123bbf8b06023e94a3af5bafd98b26d` |
