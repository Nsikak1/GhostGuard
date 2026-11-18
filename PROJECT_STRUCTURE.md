# GhostGuard: project structure

Application id: `ng.name.nsikak.ghostguard` · Kotlin package / namespace: `ng.name.version5` · minSdk 21

## What the app does
1. **Ghost Watch** records touches while the phone is left alone and turns them into hotspots.
2. **Profiles** store those hotspots in *natural-orientation fractions*, so they work in portrait and landscape.
3. **Protection service** covers each enabled hotspot with an invisible overlay that swallows touches.
4. **Shake-to-sleep** and **ghost-storm lock** lock the screen without using the touch pipeline.

## Source tree (`app/src/main/java/ng/name/version5`)
```
MainActivity.kt            Compose host: 3 tabs (Protect / Profiles / Settings), notification permission,
                           bumps a "refresh" counter on every resume so status rows re-read permissions
ghost/                     PURE Kotlin (no Android imports) so it is JVM unit-testable
  Rotation.kt              Rotation (portrait<->natural), Zone, ZoneGeometry, ProfileMatcher (is this touch a ghost?)
  Model.kt                 GhostSample, Hotspot, GhostProfile
  HotspotAnalyzer.kt       Greedy clustering of samples into hotspots, measured in mm
  ProfileJson.kt           org.json (de)serialisation of a profile
data/
  Prefs.kt                 SharedPreferences wrapper (all settings + counters)
  ProfileStore.kt          Profiles as JSON files in filesDir/profiles, plus the unsaved "draft"
  Screen.kt                Display helpers: rotation, real size, natural size, xdpi
capture/
  CaptureActivity.kt       Ghost Watch: setup, 5 s get-ready, N s watch, records MotionEvents, saves draft
  TestActivity.kt          Test mode: draws zones, touch dots go green (pass) / red (blocked); rotatable
service/
  ProtectionService.kt     Foreground service: overlay zones, shake sensor, storm lock, notification,
                           display-rotation listener, 4 s watchdog for overlay permission changes
  ShakeLogic.kt            Pure shake detector (N spikes in a window, cooldown)
  ScreenLocker.kt          GhostDeviceAdmin + GhostAccessibilityService + ScreenLocker (live permission checks)
  BootAndTile.kt           BootReceiver (restart after reboot) + Quick Settings tile (API 24+)
ui/
  HomeScreen.kt            Protection switch, pause, active profile, warnings, test button
  ProfilesScreen.kt        Profile list + launches CaptureActivity, hosts ReviewScreen
  ReviewScreen.kt          Hotspots over a phone outline; resize / toggle / delete / tap to add
  SettingsScreen.kt        Permission rows (live), shake sensitivity, storm settings, zone tint
  theme/                   Template Compose theme (unchanged)
```

## Resources and config
```
app/build.gradle.kts                 applicationId, minSdk 21, deps (+ org.json for unit tests)
app/src/main/AndroidManifest.xml     permissions, activities (configChanges for rotation), service (specialUse),
                                     device admin, accessibility service, boot receiver, QS tile
res/xml/device_admin.xml             force-lock policy only
res/xml/accessibility_config.xml     minimal; only used for GLOBAL_ACTION_LOCK_SCREEN (API 28+)
res/drawable/ic_launcher_*.xml       adaptive icon layers (background, foreground, monochrome)
res/mipmap-anydpi-v26/               adaptive icon (API 26+)
res/mipmap-*dpi/ic_launcher*.png     legacy icons for API 21-25
branding/ghostguard_logo.svg         master logo (also _round); branding/make_icons.py regenerates all icons
app/src/test/.../RotationTest.kt     rotation, zones, clustering, shake, JSON tests
```

## Data flow
```
CaptureActivity --samples--> HotspotAnalyzer --> draft.json --> ReviewScreen --> profiles/<id>.json
                                                                   Prefs.activeProfileId
ProtectionService reads active profile --> ZoneGeometry (natural -> current rotation) --> overlay windows
                       \--> accelerometer --> ShakeLogic --> ScreenLocker.lock()
                       \--> blocked taps --> storm counter --> ScreenLocker.lock()
```

## Permissions and how they are checked (all live, nothing is assumed)
| Permission | Check | Notes |
|---|---|---|
| Display over other apps | `Settings.canDrawOverlays` (API 23+; always true on 21-22) | service re-checks every 4 s |
| Device admin (force-lock) | `DevicePolicyManager.isAdminActive` on every lock | works on API 21+; receiver reloads the service on enable/disable |
| Accessibility (optional, API 28+) | `GhostAccessibilityService.instance != null` | alternative lock path |
| Battery optimisation | `isIgnoringBatteryOptimizations` (API 23+) | prevents OEM killing |

The Home and Settings screens re-read these every 2 s and on every resume, so grants made outside the app show up.

### Granting outside the app (adb)
```
adb shell dpm set-active-admin --user 0 ng.name.nsikak.ghostguard/ng.name.version5.service.GhostDeviceAdmin
adb shell appops set ng.name.nsikak.ghostguard SYSTEM_ALERT_WINDOW allow
adb shell settings put secure enabled_accessibility_services ng.name.nsikak.ghostguard/ng.name.version5.service.GhostAccessibilityService
adb shell settings put secure accessibility_enabled 1
```
(`dpm set-active-admin` generally only works on a fresh device or one with no accounts; otherwise use Settings > Security > Device admin apps.)
