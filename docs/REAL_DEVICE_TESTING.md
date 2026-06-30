# Real Device Testing

This document describes repeatable manual/emulator testing for I2P Knoks Browser real alpha builds.

The app does not embed an I2P router. It requires an external Java I2P or i2pd router reachable from the Android device/emulator.

## Purpose

Use this checklist to verify real alpha behavior on an Android emulator or physical Android device. The goal is to confirm that diagnostics, SAM lifecycle, endpoint configuration, and Page Inspector results reflect real router state without inventing success.

## Required Tools

- Android Studio or Android SDK platform tools
- A debug APK built from this repository
- `adb` available on PATH or from the Android SDK
- A Java I2P router or i2pd instance for router-backed tests
- Network access from the Android target to the router host and ports

## Test Environments

| Environment | Router | Expected |
|---|---|---|
| Android emulator + host Java I2P | Java I2P on desktop | Works when emulator can reach host router |
| Android emulator + host i2pd | i2pd on desktop | Works when SAM and HTTP proxy are exposed |
| Physical Android device + LAN Java I2P | Java I2P on desktop/LAN | Works when firewall/router binding allows LAN access |
| Physical Android device + LAN i2pd | i2pd on desktop/LAN | Works when firewall/router binding allows LAN access |
| No router | none | App should show offline/diagnostic failure, not fake success |

Exact host IP depends on the emulator or physical device network.

## Android Emulator Setup

For the standard Android emulator, the host machine is often reachable as:

```text
10.0.2.2
```

Example endpoint:

```text
Host: 10.0.2.2
SAM: 7656
HTTP Proxy: 4444
Router Console: 7657
```

Verify the router is running on the host before testing from the emulator. If diagnostics fail, check that the router service binds to an interface reachable by the emulator.

## Physical Android Device Setup

For a physical Android device, use the LAN address of the desktop or server running Java I2P or i2pd.

Example endpoint:

```text
Host: 192.168.1.x
SAM: 7656
HTTP Proxy: 4444
Router Console: 7657
```

Firewall rules must allow inbound access from the Android device. Router services must bind to a reachable interface. Exposing local router services on a LAN should be intentional and limited to trusted networks.

## Java I2P Setup

Typical local Java I2P ports:

- Router console: `127.0.0.1:7657`
- HTTP proxy: `127.0.0.1:4444`
- SAM bridge: `127.0.0.1:7656`

SAM may need to be enabled manually. LAN or emulator access may require binding and firewall changes depending on the router configuration.

Checklist:

- [ ] Router console reachable
- [ ] HTTP proxy reachable
- [ ] SAM bridge reachable
- [ ] App diagnostics show expected state
- [ ] SAM lifecycle reaches READY
- [ ] `.i2p` inspector fetch works

## i2pd Setup

Start i2pd and confirm the relevant services are enabled. Exact configuration file paths and service names depend on installation method and platform.

Checklist:

- [ ] i2pd running
- [ ] SAM enabled
- [ ] HTTP proxy enabled
- [ ] Router console or status endpoint reachable if configured
- [ ] App diagnostics show expected state
- [ ] SAM lifecycle reaches READY
- [ ] `.i2p` inspector fetch works

Test with local desktop tools first, then test from the emulator or physical Android device.

## Endpoint Configuration

In the app, configure the endpoint to match the router host visible from the Android target.

Common examples:

| Target | Host | SAM | HTTP Proxy | Router Console |
|---|---:|---:|---:|---:|
| Emulator to host router | `10.0.2.2` | `7656` | `4444` | `7657` |
| Device to LAN router | `192.168.1.x` | `7656` | `4444` | `7657` |
| Local Android router testing | `127.0.0.1` | `7656` | `4444` | `7657` |

If validation rejects the endpoint, fix the host or port before continuing. The app should keep the previous endpoint when invalid values are entered.

## ADB Smoke Test Notes

Useful commands:

```powershell
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n no.knoksen.i2pbrowser/.MainActivity
adb logcat | findstr /i "I2P SAM PROXY"
```

You can also print the local command checklist:

```powershell
.\scripts\real-alpha-smoke-notes.ps1
```

## Smoke Test Sequence

1. Install debug APK.
2. Open app.
3. Confirm Security Boundaries are visible.
4. Run diagnostics with no router running.
5. Start Java I2P or i2pd.
6. Configure endpoint.
7. Run diagnostics.
8. Connect SAM session.
9. Confirm SAM lifecycle reaches READY.
10. Inspect a known `.i2p` URL.
11. Confirm metadata panel shows:
    - mode
    - status
    - content type
    - elapsed time
    - fetched timestamp
12. Trigger or inspect redirect case if available.
13. Stop router.
14. Retry fetch.
15. Confirm failure is honest and no fake success is shown.

## Screenshot Checklist

Capture these during real alpha testing:

- [ ] Router offline / no services reachable
- [ ] Router console reachable but SAM disabled
- [ ] SAM reachable but HTTP proxy disabled
- [ ] SAM lifecycle CONNECTING
- [ ] SAM lifecycle READY
- [ ] SAM lifecycle timeout/failure state
- [ ] Page Inspector success response
- [ ] Page Inspector redirect response
- [ ] Page Inspector HTTP error response
- [ ] Page Inspector unsupported content type
- [ ] Page Inspector timeout
- [ ] Security Boundaries card
- [ ] Endpoint validation error

## Known Limits

- The app does not embed an I2P router.
- Full WebView browsing is not implemented.
- HTML and JavaScript are not executed by the Page Inspector.
- VPN/VPS, chat, and peer discovery remain lab/demo areas unless explicitly wired to real runtime behavior.
- Router service exposure on LAN must be configured outside the app.

## Failure Interpretation

| Symptom | Likely Cause | Next Check |
|---|---|---|
| All diagnostics unavailable | Router not running, wrong host, or firewall blocked | Confirm host IP and router process |
| Router console reachable but SAM unavailable | SAM disabled or bound only to localhost | Enable SAM or adjust bind address intentionally |
| SAM READY but HTTP proxy unavailable | HTTP proxy disabled or wrong port | Check proxy tunnel/client configuration |
| Proxy reachable but host lookup fails | Unknown `.i2p` host or router NetDB not ready | Try a known reachable `.i2p` URL |
| Timeout during SAM lifecycle | Router slow, hung, or inaccessible after connect | Retry after router startup completes |
| Endpoint validation error | Invalid host or port | Fix host format and port range |

## Release Signoff Checklist

- [ ] Local release verification passes
- [ ] No-router failure path tested
- [ ] Java I2P or i2pd diagnostics tested
- [ ] SAM lifecycle READY captured
- [ ] `.i2p` Page Inspector success captured
- [ ] Redirect or HTTP error case captured
- [ ] Router stop/retry failure path tested
- [ ] Security Boundaries card captured
- [ ] Release notes mention known limits without expanding claims
