# Boardcraft-VR🥽
A Minecraft Java Fabric VR Mod to run Minecraft Java on Google Cardboard devices through Mobile Minecraft Java Edition launchers!

## Disclaimer⚠️
Boardcraft VR is an alpha and comes with no warranty (see the GPL-3.0).
I'm not responsible for broken phones, dead SD cards, overheated batteries,
Thermal Runaway, motion sickness, or you walking into a wall with a Cardboard on your face.
Please do some research before using it. YOU are choosing to install it,
and if you blame me for messing up your device, I will point and laugh at you!

## What it does🪛
- Turns Flat screen Minecraft to VR SBS Mode
- Tracks Head motion using Minecraft Cardboard VR App
- Lens Distortion correction
- Automatic Screen Size
- Walking with Mouse or Gamepad
- VR Mode Pop-up (OFF ON World Only) In-Game VR Settings

##  What you need📋
- Minecraft 26.3 Fabric       -   Game and Mod Loader
-  Boardcraft VR Mod          -   Main Mod
- Minecraft Cardboard VR App  -   Required for Head Tracking Viewer parameter QR Code Scanning and 6DoF Controller tracking (Coming Soon)
- StereoCraft Mod (Required)  -   Required for SBS Rendering
- Sodium
- Fabric API                   -  Required for mod to function
- Mobile Java Launcher         -  See #Launchers 
If StereoCraft is missing, the game won't crash, but you won't get side-by-side 3D (the Stereo View section will tell you it's missing).

## Coming Soon:⌛

 [-] 6DoF IMU Only Tracked Controller
 
 [-]Floating Menus and Panels
 
 [-] Overlay Editor
 
 [-] Hand based interaction

 [-] Add support for various controllers Such as Custom ESP32 Controller (Code coming soon), Daydream Controller, GearVR Controller, GalaxyXR Controllers and Nintendo Switch JoyCons
 
 [-] Custom Menus for Intractable Blocks (Example: Chest, Crafting Table, Furnace, Anvil, etc)

## Recommended launchers:✅

 [✓] Cubiq Launcher (Recommended with LTW Renderer)
 
 [✓] Zalith Launcher (Not Tested)
 
 [✓] Zalith Launcher 2 (Not Tested)

## Not Recommended Launchers:❌

 [✗] Pojavlauncher
 
 [✗] Mojo Launcher


## Installation⬇️

1. Install the Minecraft Cardboard VR APK on your phone.
2. Open your launcher and create a new Fabric 26.3 instance (or use a modpack).
3. Put these in the instance's `mods` folder (the exact path depends on your launcher) or use Install Mods feature (If available): Boardcraft VR, Fabric API, Sodium, and StereoCraft.
4. Open the Minecraft Cardboard VR app and scroll down to the Viewer section. Tap Scan viewer QR (or Paste link if you have the link from the QR code) to save your viewer's lens settings.
5. This isn't required, but it usually fixes double vision and turns on lens distortion, so it's recommended.
5. Press Start before launching hing the game and leave the app running while you play.
6. Launch the game.
7. Go to VR Settings (The Little Viewer icon in the Main Menu
8. Click OFF at the top and press ON or World Only
9. Enjoy!
> \*\*Tip:\*\* start the app \*before\* launching the game. Boardcraft listens for head-tracking data from the app, and the alpha is happiest when the app is already running.

## Gamepad Mode🎮
1. Set the launcher's gamepad input method to SDL Direct (or Direct)
2. Go to VR Settings (The viewer icon in the pause screen or Main Menu)
3. Find Movement: Mouse
4. Click to change it to Movement: Gamepad
5. Exit VR Settings
6. Join any Singleplayer world, LAN World or Server
7. Use Controller Movement Joystick to move around

## Mouse Mode🖱️ (Not Recommended Right Now)
Warning: ⚠️ Mouse mode has a known bug where such as going forward would go in random directions

1. Go to VR Settings (The viewer icon in the pause screen or Main Menu)
2. Find Movement: ####
3. It should be set to Mouse
4. If not Change to Movement: Mouse but clicking the button
5. Exit VR Settings
6. Join any Singleplayer world, LAN World or Server
7. Use Mouse to Move around (Buggy)

## VR Settings Guide
1. Open VR Settings in the game. The Viewer section has the lens options:
2. Use viewer IPD	   -     Uses your viewer's lens gap as the eye separation
3. Lens distortion	 -     Turns the lens correction on or off
4. Strength	         -     Scales the distortion (0 to 125%). Lower it if the image looks over-warped
5. Lens height	     -     Flip it if the image sits too high or too low in the lenses
6. Forget viewer	   -     Clears the saved viewer settings


## Troubleshooting🪛
**Double vision**: scan your viewer's QR code in the app. Without viewer settings the mod doesn't know your lens spacing.
**No head tracking**: make sure the app is on Start, and that the port matches in the app and in the mod's VR Settings.
**Image looks too warped or stretched**: check that Screen is on `Auto` (or set to your phone's real size), and try lowering Strength. Raising Minecraft's FOV (around 90 to 100) can also help.
**Image is shifted up or down**: toggle Lens height.
**Auto screen size** shows "no data": start the app first and wait a couple of seconds. The size is sent every ~2 seconds while the app is running.
**Game crashes**: Make Sure the app says Running with a Green dot and keep open in the background if issue persist, Please open an issue and attach the newest file from `crash-reports/` (or the end of `logs/latest.log`). Say which phone and launcher you used (Unsupported Launcher not considered).

## Known Bugs🐛
- The hitbox outline only shows in the left eye (comes from StereoCraft).
- Double vision until a viewer is configured.
- The first-person hand can get cut off.
- Distortion assumes a centered per-eye view; StereoCraft's small convergence shift isn't accounted for (about 1% error).
- Only tested on the Cubiq Launcher with LTW, on two phones.

  
## Roadmap🗺️
[✓] VR head tracking
[✓] Cardboard QR scanning
[✓] Lens distortion correction
[ ] HUD fit, gaze reticle, floating menus and panels
[ ] Overlay editor
[ ] 6DoF IMU-only tracked controller (DIY, gyro + accelerometer)
[ ] Hand-based interaction
[ ] Custom menus for intractable blocks (chest, crafting table, furnace, anvil, and more)


## Building from source🛠️
Both parts are built with GitHub Actions (see `.github/workflows`).
To build the mod locally you need Java 25 and Gradle 9.5:
1. Download and Extract the Boardcraft Mod Zip/Tar.gz`
2. find the Mod and its build.gradle
3. Use its Build.gradle to build the mod
The jar ends up in `build/libs/`. The companion app is a standard Android Gradle project.

## Credits📋
 - StereoCraft by MitchellMarx (LGPL-3.0) does the side-by-side stereo rendering. Boardcraft drives it through reflection.
 - Sodium and Fabric are what the mod runs on.
 - Viewer parameters use the official Google Cardboard device-parameters format.
 - Lens distortion uses the standard Cardboard model (Brown's distortion model/Radial distortion model), `p' = p(1 + k1·r² + k2·r⁴)`.
Made by Jaisprint (Crafty Reality Labs).

## License🪪
Boardcraft VR is licensed under the GNU GPL v3.0. See LICENSE.











