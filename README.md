# DJI-FCC-HACK

A simple Android app that forces DJI N1 remotes to FCC

<img src=".github/light.webp" alt="app" width="200"/><img src=".github/dark.webp" alt="app" width="200"/>

> [!WARNING]
> This only works for drones with DJI N1 remotes. If you have a different remote, this app will not work for you.

## How to use

Download the latest release from the [releases page](https://github.com/M4TH1EU/DJI-FCC-HACK/releases) and install it on your Android device.

> [!IMPORTANT]
> You need to repeat the following steps every time you turn on the drone and/or remote.

Then follow these steps:

1. Turn on the drone and remote and wait a few seconds for them to connect.
2. Connect your phone to the **bottom** USB port of the remote.
3. Click on 'Send FCC Patch'.
4. Disconnect your phone from the bottom USB port of the remote and connect it to the **top** USB port.
5. Enjoy your drone with FCC mode.

## Compatibility

This app should work on any Android device running Android 8 and above.

**Tested on the following drones:**

* DJI Mavic Air 2
* DJI Mini 4K
* DJI Mini 2
* DJI Air 2S

> [!WARNING]
> Many people reported that this hack doesn't work with the Mini 3, if anyone finds a working hack I could take a look at reverse-engineering it. 

> [!NOTE]
> Please let me know if you have tested this app on another drone so I can update this README.

## How do I know if it worked?

Open the DJI Fly app and go to the Transmission tab. Look at the horizontal bar around -90 dBm:

* If it lines up with the 1km mark, your drone is in CE mode.
* If it falls below the 1km mark, your drone is in FCC mode.

*Check the images below for reference.*

| FCC                           | CE                          |
| ----------------------------- | --------------------------- |
| ![fcc.webp](.github/fcc.webp) | ![ce.webp](.github/ce.webp) |

## FAQ

### Does this work on iOS?

No, this app is only available for Android.

### Does this work on DJI Smart Controller?

No, this app only works with N1 remotes (the ones without a screen).

### Does this work on DJI XYZ drone?

Maybe? Give it a try and let me know so I can update this README.

### How does this work?

This app simply sends a command to the remote to switch to FCC mode over the USB port.

### Can I use this app to switch back to CE mode?

No, this app only switches to FCC mode. To switch back to CE, turn off the drone and remote, then power them back on.

## Goggles Support
>[!WARNING]
> This app is not related to the following FCC file-based hacks for goggles; they are included here for reference only.

Steps to enable higher power output for DJI Goggles:

**DJI Goggles V1/V2**

* Create a text file named `naco_pwr.txt` with content: `pwr_2`
* Copy it to a microSD card
* Power on Goggles and Air Unit, wait for camera image
* Insert SD card into Goggles and restart

**DJI Goggles 2 / Goggles 3**

* Create an empty file named `ham_cfg_support` (no extension)
* Copy it to a microSD card
* Insert SD card into Goggles
* Power on Goggles

## Air Units
>[!WARNING]
> This app is not related to the following FCC file-based hacks for air units; they are included here for reference only.

Steps to enable FCC mode on DJI video transmitters:

**Air Unit V1**

* Create `naco_pwr.txt` with `pwr_2` inside
* Copy to microSD card, insert into Air Unit
* Power on

**Vista**

* Create `naco_pwr.txt` with `pwr_2` inside
* Power on Vista and connect via USB
* Copy file to Vista storage when it appears
* Power cycle the unit

**Air Unit O3**

* Create empty file `ham_cfg_support`
* Connect O3 via USB
* Copy to O3 storage
* Power cycle

## Credits

This app is based on the work of [galbb](https://mavicpilots.com/members/galbb.148459/) on the [MavicPilots forum](https://mavicpilots.com/threads/mavic-air-2-switch-to-fcc-mode-using-an-android-app.115027/).
