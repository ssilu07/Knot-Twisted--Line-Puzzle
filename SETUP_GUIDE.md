# 🪢 Tangled Line - Android Studio Setup Guide

## Step 1: Extract the Project
Unzip `TangledLine.zip` to any folder on your computer.

## Step 2: Open in Android Studio
1. Open Android Studio
2. Click **File → Open**
3. Navigate to the `TangledLine` folder and select it
4. Click **OK** and wait for Gradle sync to complete

## Step 3: Run the App
1. Connect your Android phone via USB (or use an emulator)
2. Enable **USB Debugging** on your phone
3. Click the green **▶ Run** button in Android Studio
4. Select your device and click **OK**

## Project Structure
```
TangledLine/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── game.html          ← The full game
│   │   ├── java/com/tangledline/game/
│   │   │   └── MainActivity.kt    ← WebView loader
│   │   ├── res/
│   │   │   ├── drawable/           ← App icon vectors
│   │   │   ├── mipmap-*/           ← Adaptive icons
│   │   │   └── values/             ← Theme, colors, strings
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/
    └── gradle-wrapper.properties
```

## Features
- Fullscreen immersive mode (no status/nav bars)
- Hardware accelerated WebView
- Screen stays on while playing
- Portrait locked
- Back button disabled (no accidental exits)
- Adaptive app icon with knot design
- ProGuard rules for release builds

## Build Release APK
1. Go to **Build → Generate Signed Bundle / APK**
2. Select **APK**
3. Create/select your keystore
4. Choose **release** build type
5. APK will be in `app/build/outputs/apk/release/`

## Troubleshooting
- **Gradle sync fails?** → Make sure you have Android SDK 34 installed
- **WebView blank?** → Check that `game.html` is in `app/src/main/assets/`
- **Sound not working?** → Tap the 🔊 button in-game (requires user interaction first)
