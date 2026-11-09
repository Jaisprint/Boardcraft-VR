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

## Viewer QR (2b)
"Scan viewer QR" uses Google's code scanner (play-services-code-scanner), so no camera permission and no zxing.
It decodes the Cardboard `?p=` params and sends them to the mod ("VP1" packet) now and every ~2 s while Started.
GyroService now sends the 6-value packet (look + phone x axis) that the mod's pole-flip fix needs.
