## Documentation

- Project documentation lives under `@docs/`, with the table of contents (`Documentation Map`) in `@docs/README.md`.
- Every time you change something, make sure that the documentation under `@docs/` is up to date.

## Installing on the Paired Phone (Wireless Debugging)

- **Tooling & Sandbox**: `adb.exe` is at `C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe` (not on `PATH`). Both `.\gradlew.bat` (which reads `sdk.dir` outside the workspace) and `adb.exe` must be run with `BypassSandbox: true`.
- **Phone Serial**: `adb-48201FDAS0004G-PnRogK._adb-tls-connect._tcp` (already paired via Android Wireless Debugging; its IP/port is discovered dynamically over mDNS).
- **Personal Profile Only (`--user 0`)**: The paired phone runs both Personal (`User 0`) and Work (`User 10`) profiles. **ALWAYS** pass `--user 0` to `adb install` (`install --user 0 -r`) and `adb shell am start --user 0` so the app is never installed into or launched from the Work profile.
- **NEVER Run Any Tests on the Paired Phone**: Do **not** run any automated tests against the user's actual paired phone—no `connectedDebugAndroidTest` / `connected*AndroidTest`, no `adb shell am instrument`, and no automated test runners (AGP's connected test runner uninstalls `com.rhythm.habittracker` on teardown, wiping unbacked-up local app data and installing across all profiles). Run `.\gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin` locally on the PC instead; only run Android instrumented tests against a dedicated emulator/AVD, never against the user's personal phone.
- **Important mDNS Wait**: Do **not** run `adb.exe devices` or `adb.exe install` immediately after the daemon starts—mDNS takes a few seconds to discover `_adb-tls-connect._tcp`, and separate `run_command` calls may not keep the `adb` daemon alive. Always start the server, sleep ~4 seconds, and run the `adb` command in the **same** PowerShell invocation:

```powershell
# 1. Build the debug APK
.\gradlew.bat assembleDebug

# 2. Discover the paired phone over mDNS and verify it is attached
C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe start-server; Start-Sleep -Seconds 4; C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe devices

# 3. Install the debug APK onto the personal profile (User 0) of the paired phone
C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe start-server; Start-Sleep -Seconds 4; C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe -s adb-48201FDAS0004G-PnRogK._adb-tls-connect._tcp install --user 0 -r app\build\outputs\apk\debug\app-debug.apk
```
