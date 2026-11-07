# Cardboard Gyro Sender

Android source project for the Cardboard gyro sender app.

## What changed
- Reworked the app screen into a dark, pixel-inspired Minecraft-style UI.
- Added square, beveled Start/Stop button styling and a matching port field.
- Added a red stopped indicator and a softly pulsing/glowing green running indicator.
- Expanded the running/stopped status messages.
- Added a short original pixel-style UI click sound for the Start/Stop button.

## What did not change
GyroService.java, the Android manifest, application ID, port handling, sensor selection,
UDP destination, packet format, and service lifecycle are unchanged.

## Build
Open this folder (`gyro-app`) in Android Studio and build the `app` debug APK.
