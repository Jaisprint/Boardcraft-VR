# Minecraft Cardboard VR — Android app source

Android source project for the Minecraft Cardboard VR phone companion app.

## App screen and viewer setup
- Renamed the main heading to **Minecraft Cardboard VR**.
- Updated the subtitle to **Minecraft VR // Google Cardboard VR**.
- Preserved the existing UDP port, Start/Stop controls, status indicator, and their behavior.
- Added a scrollable Cardboard Viewer panel below the original gyro panel.
- Added a QR scanner for Google Cardboard viewer URLs containing the `p=` device-parameters protobuf.
- Decodes and saves the viewer's vendor/model, screen-to-lens distance, inter-lens distance, tray-to-lens distance, left-eye FOV, distortion coefficients, vertical alignment, and primary button.
- Added a Clear Viewer control.
- Set the supplied pixel-art Cardboard headset drawing as the Android launcher icon.

## Backend preservation
`GyroService.java` is unchanged. Sensor selection, sensor sampling, localhost UDP destination, packet format, service lifecycle, and port handling remain as they were in the supplied project.

## Current integration boundary
The scanned viewer parameters are saved locally in the app's `gyro` SharedPreferences. The supplied Boardcraft mod's current UDP receiver only accepts the gyro vector packet (`fx,fy,fz[,ux,uy,uz]`); it does not yet accept viewer-configuration data. Therefore this app revision does **not** claim that the mod automatically receives or applies the saved viewer parameters. That requires a compatible receiver/configuration change in Boardcraft.

## Build
Open this folder in Android Studio and build the `app` debug APK. The QR scanner dependency is declared in `app/build.gradle` and will be resolved by Gradle.
