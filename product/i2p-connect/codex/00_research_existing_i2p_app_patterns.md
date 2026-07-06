# Codex Task 00 - Research Existing I2P App Patterns

## Prompt

Read the current repository before implementation.

Start with:

- `AGENTS.md`
- `DIGITAL_AUTONOMY_DOCTRINE.md`
- `product/03_UX_ONBOARDING_SPEC.md`
- `README.md`
- `docs/ARCHITECTURE.md`
- `docs/SECURITY_BOUNDARIES.md`
- `docs/ROADMAP.md`
- `docs/ANDROID_PERMISSIONS.md`
- `app/src/main/java/no/knoksen/i2pbrowser/AppExperienceMode.kt`
- `app/src/main/java/no/knoksen/i2pbrowser/i2p`
- `app/src/main/java/no/knoksen/i2pbrowser/data`
- `app/src/main/java/no/knoksen/i2pbrowser/ui/I2PScreens.kt`

If any required governance file is missing, state that in the final summary and do not invent replacement doctrine.

## Goal

Produce a short implementation note for I2P Connect that identifies:

- existing real-alpha status patterns to reuse
- lab/demo messaging code that must not be promoted as production
- Room tables that can be reused versus replaced
- test patterns for diagnostics, SAM, migrations, and log sanitizing
- UI navigation rules for `RELEASE_REAL` and `LAB_SIMULATION`

## Constraints

- Do not make product claims beyond implemented behavior.
- Do not change source code in this research task unless needed to fix broken docs links.
- Do not rewrite unrelated docs.

## Validation

Run:

```powershell
.\scripts\check-release-claims.ps1
.\scripts\local-release-verify.ps1
```

Summarize findings and any recommended next task.
