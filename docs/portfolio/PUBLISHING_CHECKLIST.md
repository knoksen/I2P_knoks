# Publishing checklist

- [ ] Run both PowerShell scripts with PowerShell 7.
- [ ] Execute the publisher with `-WhatIf`.
- [ ] Confirm the authenticated GitHub account.
- [ ] Confirm tag, target branch, title, and asset filenames.
- [ ] Recalculate and compare all SHA-256 values.
- [ ] Review claims against current repository evidence.
- [ ] Scan artifacts for secrets, private keys, identifying logs, and sensitive metadata.
- [ ] Publish release assets.
- [ ] Download the published assets and verify them independently.
