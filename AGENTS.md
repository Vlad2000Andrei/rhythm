## Documentation

- Project documentation lives under `@docs/`, with the table of contents (`Documentation Map`) in `@docs/README.md`.
- Every time you change something, make sure that the documentation under `@docs/` is up to date.

## Installing on the Paired Phone (Wireless Debugging)

- **Tooling & Sandbox**: `adb.exe` is at `C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe` (not on `PATH`). Both `.\gradlew.bat` (which reads `sdk.dir` outside the workspace) and `adb.exe` must be run with `BypassSandbox: true`.
- **Phone Serial**: `adb-48201FDAS0004G-PnRogK._adb-tls-connect._tcp` (already paired via Android Wireless Debugging; its IP/port is discovered dynamically over mDNS).
- **Important mDNS Wait**: Do **not** run `adb.exe devices` or `adb.exe install` immediately after the daemon starts—mDNS takes a few seconds to discover `_adb-tls-connect._tcp`, and separate `run_command` calls may not keep the `adb` daemon alive. Always start the server, sleep ~4 seconds, and run the `adb` command in the **same** PowerShell invocation:

```powershell
# 1. Build the debug APK
.\gradlew.bat assembleDebug

# 2. Discover the paired phone over mDNS and verify it is attached
C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe start-server; Start-Sleep -Seconds 4; C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe devices

# 3. Install the debug APK onto the paired phone
C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe start-server; Start-Sleep -Seconds 4; C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe -s adb-48201FDAS0004G-PnRogK._adb-tls-connect._tcp install -r app\build\outputs\apk\debug\app-debug.apk
```
