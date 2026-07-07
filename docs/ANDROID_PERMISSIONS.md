# Android Permission Boundary

## Purpose

This document defines the Android permission surface for I2P Knoks Browser real-alpha releases. It records what the app currently requests from Android, why each permission exists, what it enables, and what must not be inferred from the permission set.

This file is a release-facing boundary document. It should be updated whenever `app/src/main/AndroidManifest.xml` changes.

## Current Permission Set

| Permission | Required By | Why It Exists | What It Enables | What It Does Not Provide |
| :--- | :--- | :--- | :--- | :--- |
| `android.permission.INTERNET` | I2P endpoint diagnostics, SAM probing/session work, router console launch intents, and `.i2p` page inspection through the configured HTTP proxy. | The app must open network sockets to the configured local, emulator, or LAN I2P/i2pd services. | Outbound app network access to user-configured endpoints such as SAM, HTTP proxy, and router console addresses. | It does not provide anonymity, full-device routing, OS-level VPN behavior, Tor routing, browser isolation, traffic correlation protection, or an embedded I2P router. |

No foreground service, background service, VPN service, storage, location, notification, camera, or microphone permission is requested in the current manifest.

## Current Real-Alpha Boundary

- The app requires an external I2P or i2pd router for real I2P behavior.
- The app can perform local, emulator, or LAN endpoint diagnostics when configured by the user.
- The app can use the configured HTTP proxy for `.i2p` page inspection.
- The app can probe SAM through the configured endpoint.
- The app does not provide OS-level VPN tunneling.
- The app does not provide full browser isolation.
- The app does not provide audited encrypted chat.
- The app does not provide Tor integration.
- The app does not provide an embedded I2P router in the current alpha.

## Claims That Must Not Be Inferred

Android permissions must not be interpreted as guarantees of:

- anonymity
- encrypted messaging
- VPN tunneling
- no Tor routing guarantee
- full-device traffic routing
- browser sandboxing
- cryptographic audit status
- protection from traffic correlation
- protection from endpoint compromise

The permission set only describes what Android capabilities the app may request. Runtime privacy and network behavior depends on the configured external router, reachable local services, app code paths, and documented boundaries.

## RELEASE_REAL Notes

`RELEASE_REAL` means measured diagnostics and real proxy/SAM interactions when the configured endpoint is reachable.

`RELEASE_REAL` does not mean the app provides anonymity by itself.

Lab and simulation UI must remain labeled and must not be presented as active protection.

## Review Checklist

- [ ] Manifest permissions changed?
- [ ] README permission section updated?
- [ ] `docs/ANDROID_PERMISSIONS.md` updated?
- [ ] `docs/SECURITY_BOUNDARIES.md` still accurate?
- [ ] Release claim check passes?
- [ ] No new unsupported privacy/security claims added?
