# I2P Knoks Browser

[![Windows Desktop Build](https://img.shields.io/badge/Windows_Desktop-v1.2.0--stable-0078d4?style=for-the-badge&logo=windows&logoColor=white)](https://github.com/)
[![PWA Quick Launch](https://img.shields.io/badge/PWA_Quick_Launch-Active--Online-3178c6?style=for-the-badge&logo=typescript&logoColor=white)](https://ais-pre-lotxsxijwzctq7cadhhava-983598203489.europe-west2.run.app)
[![I2P Mode](https://img.shields.io/badge/I2P-SAM_optional-4caf50?style=for-the-badge&logo=security&logoColor=white)](https://geti2p.net/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_--_Material_3-4285f4?style=for-the-badge&logo=android&logoColor=white)](https://github.com/)

A Jetpack Compose / Room Android network console for exploring I2P-style workflows. The app is primarily a simulator, with an MVP-level real I2P detection path through a local SAM bridge on `127.0.0.1:7656`.

Current truth model:

- `REAL_I2P`: a local I2P or i2pd router with SAM enabled answered the SAM hello/session handshake.
- `SIMULATION`: no SAM bridge was detected; browser pages, router metrics, VPN/VPS controls, and messenger events are local previews only.
- Secure Chat currently stores demo Base64 payloads and local simulated responses. It is not audited cryptography.

---

## 🚀 PWA & DESKTOP QUICK LAUNCH

Access and deploy the application instantly across your devices with these quick launch triggers:

| Platform | Quick Launch Button | Installation Type |
| :--- | :--- | :--- |
| **PWA Web App** | [![Launch PWA](https://img.shields.io/badge/Launch-PROG_WEB_APP-3178c6?style=flat-for-the-badge&logo=googlechrome&logoColor=white)](https://ais-pre-lotxsxijwzctq7cadhhava-983598203489.europe-west2.run.app) | Installs as an app directly from Microsoft Edge or Google Chrome on Windows. |
| **Windows Desktop** | [![Windows Installer](https://img.shields.io/badge/Install-WINDOWS_DESKTOP-0078d4?style=flat-for-the-badge&logo=windows&logoColor=white)](#windows-desktop-native-pwa-installation) | Standalone Chromium wrapper with native system notification integration and offline caching. |
| **Android Emulator** | [![Stream Applet](https://img.shields.io/badge/Stream-AISTUDIO_EMULATOR-ff6f00?style=flat-for-the-badge&logo=android&logoColor=white)](https://ai.studio/build) | Direct high-performance cloud streaming interface via Google AI Studio. |

---

## 🖥️ SCREENSHOT SHOWCASE

### 1. Windows Desktop PWA & Gateway Controller
*A preview of the dashboard interface running inside a dedicated Windows desktop frame, displaying simulated routing nodes, tunnel latency, and network panels.*

<p align="center">
  <img src="assets/desktop_mockup.jpg" width="85%" alt="Windows Desktop PWA Interface" style="border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.5);" />
</p>

### 2. Network Telemetry & VPN/VPS Live Monitor
*High-resolution status visualization representing active tunnel throughput, server CPU/RAM metrics, and public gateway peer handshakes.*

<p align="center">
  <img src="assets/app_stats_banner.jpg" width="85%" alt="Cyber Security Telemetry and Graphs" style="border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.5);" />
</p>

---

## Live Network Simulation Statistics

| Parameter | Value | Standard | Operational Status |
| :--- | :--- | :--- | :--- |
| **SAM Bridge** | `127.0.0.1:7656` | I2P/i2pd local router | Real only when detected |
| **Browser Rendering** | Compose preview content | Not WebView/proxy browsing | Simulated |
| **Messenger Payloads** | Base64 demo storage | Not encryption | Simulated |
| **VPN/VPS Controls** | UI state machines | Do not modify OS networking | Simulated |
| **Router Metrics** | Generated telemetry | Real SAM detection only | Mixed MVP |

---

## 🛡️ KEY FUNCTIONAL MODULES

### 🌐 1. Router Console (Garlic Routing Topology)
- Detects a local SAM bridge and marks the app as `REAL_I2P` when the hello/session handshake succeeds.
- Falls back to `SIMULATION` with setup guidance when SAM is missing or disabled.
- Visualizes generated tunnel metrics and router logs for console prototyping.

### 🧭 2. Secure Web Browser
- Browse local preview pages for `.i2p`-style URLs.
- Features an Address Book with custom bookmark creation.
- Clearly labels content as simulated unless a local SAM bridge is active.

### 🔒 3. VPN & VPS Routing Portal
- Simulated VPN and VPS state controls for UI exploration.
- Does not currently create OS-level VPN tunnels, SSH sessions, or network routes.

### 💬 4. Secure Messenger (P2P Chat)
- Demo messenger with local SQLite storage and Base64 payload previews.
- Requires a real crypto and transport backend before production or sensitive use.

### 🔑 5. Cryptographic Identity Panel
- Generates demo local identity material for previews.
- Imports and displays peer keys for keyring UI workflows.
- Invitation packet obfuscation is demo-only and not secure encryption.

---

## 📦 WINDOWS DESKTOP & NATIVE PWA INSTALLATION

### Method A: Standalone PWA Installation (Recommended)
You can launch and install this app as a standalone Windows applet using any Chromium-based desktop browser:
1. Open **Google Chrome** or **Microsoft Edge** on Windows.
2. Navigate to the **PWA Web App** URL:  
   👉 [https://ais-pre-lotxsxijwzctq7cadhhava-983598203489.europe-west2.run.app](https://ais-pre-lotxsxijwzctq7cadhhava-983598203489.europe-west2.run.app)
3. Look at the address bar for the **Install App** icon (represented by an overlapping screen and an arrow, or tap the top-right menu `...` and click **"Install App"** / **"Apps" > "Install this site as an app"**).
4. Click **Install**. A desktop shortcut is generated, the app runs in a borderless window, handles native offline caching, and runs on Windows Startup if permitted.

---

### Method B: Native Windows Installer (Electron / Nativefier Wrapper)
If you prefer a standalone executable (`.exe`) file running on the Windows Desktop:

1. Ensure [Node.js](https://nodejs.org) is installed on your Windows machine.
2. Run the following command in your PowerShell / Command Prompt to package the PWA into a high-performance Windows Application:
   ```bash
   npx nativefier --name "I2P Browser" --icon "assets/app_icon.png" --width 1280 --height 800 --single-instance "https://ais-pre-lotxsxijwzctq7cadhhava-983598203489.europe-west2.run.app"
   ```
3. A native folder named `I2P Browser-win32-x64` is created containing `I2P Browser.exe`.
4. Double-click **`I2P Browser.exe`** to launch the secure client directly on your Windows Desktop!

---

### Method C: WSA (Windows Subsystem for Android) APK Install
For running the Android build with native system integration inside Windows 11:
1. Enable **Windows Subsystem for Android (WSA)** or install an emulator (like BlueStacks).
2. Download the compiled release APK of **I2P Browser** from your AI Studio repository.
3. Install the APK via ADB command:
   ```powershell
   adb connect 127.0.0.1:58526
   adb install I2P_Browser.apk
   ```
4. Access **I2P Browser** from your Windows Start Menu, complete with full multi-window resizing support and integration with the Windows clipboard.

---

## 🛠️ LOCAL BUILD & COMPILATION GUIDE

This project uses modern Android Gradle toolchains. To build from source:

### Prerequisites
- JDK 17
- Android SDK matching the project `compileSdk`
- Android Studio or the included Gradle wrapper.

### Build Steps
1. Clone this repository to your system.
2. Open the project in **Android Studio (Koala or newer)**.
3. Sync Gradle and run the compilation.
4. Execute via terminal:
   ```bash
   # Compile Debug APK
   ./gradlew assembleDebug

   # Run local JVM/Robolectric tests
   ./gradlew testDebugUnitTest
   ```

---

*Secured by the Escrow Protocol. Powered by Jetpack Compose & SQLite Room database.*
