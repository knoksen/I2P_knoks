# Real Alpha Test Plan

This plan verifies that I2P Knoks Browser reports real local I2P state without inventing success.

## Baseline

- Build: `0.3.1-dev` or later.
- Mode: `RELEASE_REAL`.
- Default endpoint:
  - SAM: `127.0.0.1:7656`
  - HTTP proxy: `127.0.0.1:4444`
  - Router console: `127.0.0.1:7657`

Run before testing:

```powershell
.\scripts\local-release-verify.ps1
```

If Java/JDK preflight fails, see [Build Toolchain](BUILD_TOOLCHAIN.md). Local release verification requires Java 21 or newer plus the Android SDK.

## Test Matrix

| Case | Setup | Expected UI |
| :--- | :--- | :--- |
| No I2P router running | Stop I2P/i2pd. Run diagnostics. | Real Alpha Status shows offline/router not reachable. SAM, HTTP proxy, and console are not reachable. |
| Router console only | Router console reachable, SAM and HTTP proxy disabled. | Router Console reachable. Summary indicates partial setup, with guidance to enable SAM/HTTP proxy. |
| SAM disabled | Router and HTTP proxy may be up, SAM disabled. | SAM Bridge is not reachable. Recommended action points to enabling SAM. |
| HTTP proxy disabled | SAM reachable, HTTP proxy disabled. | SAM reachable, HTTP proxy unavailable. Browser `.i2p` fetch shows proxy unavailable and recovery actions. |
| All services ready | SAM, HTTP proxy, and router console enabled. | Real Alpha Status shows ready. Diagnostics rows are reachable. |
| Invalid endpoint host | Try blank host, host with spaces, or `999.1.1.1`. | Config is rejected with visible validation error. Previous endpoint remains active. |
| Invalid endpoint port | Try `0`, `65536`, or non-numeric input. | Config is rejected with visible validation error. Previous endpoint remains active. |
| Proxy reachable but host lookup fails | Use HTTP proxy with an unknown `.i2p` hostname. | Browser shows host lookup failure, not a fake success. |
| Retry after diagnostics | Trigger a proxy failure, run diagnostics, fix router/proxy, tap Retry. | Browser retries the same URL without retyping. |
| Reset endpoint to local | Change endpoint, then use reset/default. | Active endpoint returns to `127.0.0.1 / 7656 / 4444 / 7657`. |
| SAM reachable but HELLO fails | SAM port accepts a socket but returns `NOVERSION` or `I2P_ERROR`. | SAM lifecycle shows FAILED with the HELLO error reason. |
| HELLO OK but SESSION CREATE fails | Router returns OK to HELLO and DEST GENERATE, then SESSION CREATE fails. | SAM lifecycle shows FAILED; router UI does not mark session ready. |
| SESSION CREATE fallback | Router rejects `i2cp.leaseSetEncType=6,4` but accepts `4`. | SAM lifecycle reaches READY and records compatibility fallback in status/logs. |
| Close/reconnect session | Connect SAM session, close it, then connect again. | Close shows CLOSED, reconnect can reach READY without leaked socket state. |
| Java I2P SAM disabled by default | Run Java I2P without enabling SAM. | Diagnostics/SAM lifecycle indicate SAM unavailable or failed; no fake identity appears. |
| i2pd compatibility path | Test with i2pd and SAM enabled. | HELLO, DEST GENERATE, and SESSION CREATE states are visible; fallback is explicit if used. |

## Screenshot Checklist

- Real Alpha Status card in ready state.
- Real Alpha Status card in offline/partial state.
- Endpoint validation error.
- Browser failure state with Retry, Diagnostics, and Copy Error.
- Security Boundaries card.

## Known Limits

- No full WebView browser is included.
- Preview rendering is local Compose content unless a real proxy response is shown.
- VPN/VPS, chat, and peer discovery remain lab/demo areas unless explicitly wired to real runtime behavior.
