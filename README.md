# Duress

Duress password trigger.

[<img
     src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/me.lucky.duress/)
[<img
      src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
      alt="Get it on Google Play"
      height="80">](https://play.google.com/store/apps/details?id=me.lucky.duress)

<img 
     src="https://raw.githubusercontent.com/x13a/Duress/main/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" 
     width="30%" 
     height="30%">

Tiny app to listen for a duress password on the lockscreen.  
When found, it can send a broadcast message or wipe the device.

## Wasted

You have to set:

* action: `me.lucky.wasted.action.TRIGGER`
* receiver: `me.lucky.wasted/.TriggerReceiver`
* authentication code: the code from Wasted
* password length: your actual password len plus at least two!

Do not forget to activate `Broadcast` trigger in Wasted.

## Permissions

* ACCESSIBILITY - listen for a duress password on the lockscreen
* DEVICE_ADMIN - wipe the device (optional)

## License
[![GNU GPLv3 Image](https://www.gnu.org/graphics/gplv3-127x51.png)](https://www.gnu.org/licenses/gpl-3.0.en.html)

This application is Free Software: You can use, study share and improve it at your will.
Specifically you can redistribute and/or modify it under the terms of the
[GNU General Public License v3](https://www.gnu.org/licenses/gpl.html) as published by the Free
Software Foundation.
