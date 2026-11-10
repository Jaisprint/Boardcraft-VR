# Boardcraft VR – Beta Tester Guide

*Version 0.1.0 (Stage 3: floating menu + controller modes) · Thanks for helping test!*

## 1. What is Boardcraft VR?

Boardcraft VR is a Fabric mod for Minecraft Java that turns a phone and a Google Cardboard-style headset into a VR setup. A small Android gyro app sends your head movement to the mod, the mod turns the camera, renders side-by-side stereo and applies lens distortion. It is made to run through mobile Java Minecraft launchers.

This is an early beta. Things will break, and that is exactly why your feedback matters.

## 2. What you need

- Minecraft Java **26.3** with **Fabric Loader 0.19.3 or newer** and **Fabric API**
- **Java 25** or newer
- The **Boardcraft VR** mod jar
- The **StereoCraft** mod (needed for the stereo side-by-side view; the settings screen shows "StereoCraft: found" or "MISSING")
- The **Boardcraft Gyroscope app** (Android) running on the same device as Minecraft
- A Cardboard-style viewer and a way to press buttons (launcher touch controls, volume keys or a gamepad)

*Download links: \[add links here\]*

## 3. Setup

1. Put the Boardcraft VR jar, StereoCraft and Fabric API in your `mods` folder.
2. Start Minecraft once so the mod creates its config file (`config/cardboardvr.json`).
3. Open the Gyroscope app and leave it running in the background.
4. In Minecraft, open **VR Settings**: click the cardboard icon on the title screen (or the pause menu), or press **F9**.
5. Set **VR mode** to ON, World only, or OFF (see below).
6. Check the status line at the bottom of the settings screen. It should read **Gyro OK** with a packet rate. If it says "No gyro", make sure the port in the app and in the mod match (default **9876**).
7. In the gyro app, scan the QR code on your viewer. The settings screen will then show your viewer name and lens values, which fixes double vision and sets lens distortion.
8. Use **Recenter view** whenever the picture is not facing forward.

### VR modes

- **ON**: VR everywhere, including menus and the pause screen
- **World only**: VR only while inside a world
- **OFF**: normal Minecraft

## 4. What's new in this build

### Floating main menu

With VR mode **ON**, the title screen panorama becomes a 3D background that follows your head. The menu floats in front of it: a panel behind the main buttons, the buttons on top and the small icon buttons nearest to you. When you turn your head the layers slide by different amounts, and they bob slowly.

In VR Settings there is a **Floating Menu** section with an ON/OFF toggle, a status line, and two buttons to invert the panorama direction.

### Controller modes

VR Settings now has a **Controller** section. The available modes are:

| Mode | Status |
| --- | --- |
| Locked Crosshair | Working (crosshair in the middle, tap the screen to touch) |
| Custom ESP32 Controller | Dummy, coming soon |
| GearVR Controller | Dummy, coming soon |
| Daydream Controller | Dummy, coming soon |
| Samsung Galaxy XR Controller | Dummy, coming soon |
| Nintendo Joy-Cons | Dummy, coming soon |

The dummy modes can be selected but do nothing yet. Input stays on Locked Crosshair. You do not need to test them beyond checking that they can be selected without problems.

## 5. Controls

- **Look**: move your head
- **Walk**: move the mouse (or use the gamepad option in Movement settings)
- **Tap, Back, Vol Up, Vol Down**: rebindable in VR Settings, each can trigger any Minecraft action
- **Vol Up + Vol Down together**: opens or closes the pause menu
- **F9**: opens VR Settings from anywhere

## 6. Test checklist

Please try each item and note what happened.

### Floating menu (main focus of this build)

- [ ] Panorama follows my head when VR mode is ON
- [ ] Panorama turns in the **correct direction** (if it feels backwards, flip *Pano yaw* or *Pano pitch*, and tell us which one you needed)
- [ ] The settings status says **Panorama: tracked** while the gyro is running
- [ ] Menu panel and buttons float and slide as I turn my head
- [ ] Tapping or clicking buttons works while they are moving
- [ ] Buttons return to their normal place when I leave the title screen
- [ ] With *Floating menus* OFF, or VR mode OFF or World only, the title screen looks like vanilla
- [ ] Returning to the title screen after leaving a world re-centers the view

### Controller modes

- [ ] The Controller section appears in VR Settings
- [ ] I can open the picker and choose every mode
- [ ] Locked Crosshair works in game (crosshair in the middle, tap to click)
- [ ] Dummy modes show "coming soon" and the game still plays like Locked Crosshair
- [ ] My choice is still there after restarting Minecraft

### Existing features (regression check)

- [ ] Head tracking works in a world and Recenter view works
- [ ] Stereo view and lens distortion look right after scanning the viewer QR
- [ ] Walking with the mouse works
- [ ] Button rebinding works, and Vol Up + Vol Down opens the pause menu
- [ ] Pause menu and settings screens open and close normally

## 7. Known limitations

- Both eyes show the same menu image, so the floating menu has parallax but no real per-eye depth yet.
- The panorama hook depends on Minecraft internals. If the status says **Panorama: waiting** while the gyro is working, the panorama will keep its normal spin. Please report this.
- 6DoF / tracked controllers are not implemented yet; controller modes other than Locked Crosshair are placeholders.
- This is a beta: expect crashes, odd layouts on small screens and rough edges.

## 8. How to report a bug

Please include:

1. **Device and phone model**, Android version, and which launcher you use
2. **Minecraft, Fabric Loader, Fabric API and StereoCraft versions**
3. **Your viewer** (name from the settings screen, or "not scanned")
4. What you **expected**, what **actually happened**, and how to **repeat it**
5. The **status line** text from the bottom of VR Settings (for example "Gyro OK 60/s | blk … | dx …")
6. A screenshot or short screen recording if you can
7. Files: `logs/latest.log` and `config/cardboardvr.json`

*Send reports to: \[add contact / form / issue tracker link here\]*

## 9. Feedback questions

After testing, tell us:

- Did the floating menu feel comfortable, or did it make you dizzy?
- Was the menu movement too much, too little, or just right?
- Was anything hard to read or tap while the menu moved?
- Which controller would you want first?
- What would you most like to see in the next build?

Thank you for testing Boardcraft VR!
