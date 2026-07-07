## Summary

- 

## Validation

- [ ] `git diff --check`
- [ ] `.\scripts\check-release-claims.ps1`
- [ ] `.\scripts\local-release-verify.ps1`
- [ ] Other relevant checks:

## Public Claims Review

- [ ] This PR does not add or strengthen public-facing product claims.
- [ ] New or changed claims are registered in `docs/CLAIMS_REGISTER.md`.
- [ ] Implemented, experimental, prototype, planned, and unsupported behavior are clearly distinguished.
- [ ] Security, privacy, encryption, and anonymity wording has supporting evidence.
- [ ] Relevant limitations appear close to the claim.
- [ ] Wording follows `docs/CLAIM_SAFE_WRITING.md`.

## Security And Privacy Notes

- [ ] No secrets, private destinations, private keys, message bodies, credentials, raw router logs, or sensitive headers are included.
- [ ] Android permissions, endpoints, storage, identity handling, and cryptographic behavior are unchanged or documented.
- [ ] Lab/demo behavior is not presented as `RELEASE_REAL`.
