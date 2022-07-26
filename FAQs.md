## Frequently Asked Questions (FAQs)

### 1. Why is it that we can't download this app in play store and how do we know it's safe?

<details>
 <summary>Click to show</summary>

> This app targets older android sdk in order to set refresh rates without root requirement. Unfortunately, play store doesn't anymore allow us to publish app targeting such older sdk. If you doubt if it's safe, you can scan it with any virus scanner like [virustotal](http://www.virustotal.com/), [metadefender](http://www.metadefender.opswat.com/), with Samsung's built-in threat scanner powered by McAfee or Google Play Protect scanner.
</details>


### 2. I could not find activation code email in my inbox or I forgot to backup my activation code, what should I do?

<details>
 <summary>Click to show</summary>

> If you purchased via Paypal, activation code email is sent immediately after you purchased. If it's not in your inbox, it may went to your spam folder. If you lost it, please send us your paypal email address or paypal receipt to retrieve your activation code.`

> If you purchased via Google Play Store donation app, just use the Google Play Order Number sent to you by Google. It's the same as the activation code.
</details>

### 3. What android permissions are being used by the app?  Why does this app require Accessibility Service permission?  What data does it access?

<details>
 <summary>Click to show</summary>

> Permissions
> * WRITE_SECURE_SETTINGS. Required primarily for switching and overriding motion smoothness mode. Other features requiring it is provided in the app. This needs to be manually granted using ADB.
> * FOREGROUND_SERVICE. Basic android permission to be able to run background service (needed for most of the features)
> * EXPAND_STATUS_BAR. Needed for auto SENSORS OFF feature
> * INTERNET. Basic android permission to access internet - needed for fetching refresh rate profiles from backend, license validation and app update.
> * ACCESS_NETWORK_STATE. Basic android permission needed to check if device is connected to an internet - needed for Net Speed Indicator.
> * READ_SYNC_SETTINGS/WRITE_SYNC_SETTINGS. Needed to enable/disable autosync settings.
> * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS. To show request to allow app to ignore battery optimization to prevent it from being killed by the system
> * RECEIVE_BOOT_COMPLETED. To initialize some features that needed to be applied or run automatically after the device is booted
> * ACCESSIBILITY_SERVICE. Needed primarily to identify apps that is/are currently opened/operated - needed for adaptive refresh rate mod to properly switch refresh rates (see How the Adaptive refresh rate mod works? section below). This is also needed for better stability of features which are requiring background service. This can be manually toggled in the phone settings or is automatically enabled once WRITE_SECURE_SETTINGS is granted to the app. You can turn off the accessibility service when not using features that require it. You will be notified what those features are when turning off the accessibility service.

> Data usage and collection. 
> * No personal data is being collected by the app. Device information being used are only those required for device identification and license validation. When premium license is purchased via paypal, buyer's name and email address is provided by paypal and is solely use for sending activation email and license validation.
</details>

### 4. I am on adaptive or high refresh rate mode, but why my refresh rate stays at 60hz on some apps like google maps, waze, camera and some other apps and games even if I set a higher max refresh rate?

<details>
 <summary>Click to show</summary>

> Some apps are either (1) blacklisted by the android system to not use the high refresh rates due to compatibility reasons, (2) sets its own preferred refresh rate or (2) controlled by Samsung's game optimizing service (game launcher). GMH app can't force these apps to use higher refresh rates.
</details>

### 5. How does Quick-Doze Mod work?

<details>
 <summary>Click to show</summary>
> If enabled, the device will quickly enter into doze mode (except when charging) while screen is OFF. This will make a better standby power consumption. Motion detection will not interrupt this doze mod. While in doze, the device periodically enters a maintenance window (MW), during which apps can complete pending work (syncs, jobs, etc.). The device will continue this cycle, doubling the length MW interval each time. The longer the MW interval, the better the battery saving.

> Doze mode may cause notification delays on some apps. It will not affect SMS, notifications delivered using Google's high Priority Firebase Cloud Messaging (FCM; part of google play services) and apps which battery optimizations are disabled.

> Doze mode will deactivate once the device is unlocked.

>**Caution: Ensure to disable Doze Mods from other apps (if you have it) when enabling this feature in GMH to avoid conflict or clash .**
</details>

### 6. Why certain refresh rate(s) are missing on my device?

<details>
 <summary>Click to show</summary>

> Availability of certain refresh rates is dependent on hardware support and must also be enabled by the manufacturer. The refresh rates show in the Resolutions and Refresh Rate Profiles section of the app are the ones that are enabled.

> In rare circumstances (usually on newly released devices), these values may not be properly read from the device. If you are very certain that it's missing one or more refresh rates, please report it to us so we can override it by using the values from our backend.
</details>

### 7. How does GMH Adaptive Mod work? 

<details>
 <summary>Click to show</summary>

> For those devices that do NOT have the Samsung's stock Adaptive refresh rate mode, GMH's own Adaptive Mod is applied.

> For those devices which support Samsung's stock adaptive refresh rate, the stock adaptive refresh rate is applied 'AS IS' even when the max/peak refresh rate is changed. GMH Adaptive Mod is only applied when overriding the default minimum refresh for adaptive for a lower value.

> GMH Adaptive Mod aims to replicate the stock adaptive mode behavior. It's design not to only detect touches but also the changes on the views being displayed on screen similar to Samsung's stock adaptive implementation. Please note though that GMH adaptive mod has some limitations like it's not being able to detect videos playing (no android API available for 3rd party apps) - it only adjust the refresh rate based on the video app's manifest declaration as being a video app. But not all video apps has this declaration. It may also not able to detect S-Pen continuous writings on some apps surface.  Please use the **Per-App** settings feature of Galaxy MaxHz to adjust refresh rate to desired (v8+).
</details>

### 8. Why sometimes I notice my color shift when refresh rate switches in low brightness? What's the workaround?

<details>
 <summary>Click to show</summary>

> On certain low brightness conditions, some users may notice some slight flickers on the display when the refresh rate switches. This is a display calibration issue - dependent on how the manufacturer has finely tuned the gamma curves across the different refresh rates. Our eyes are more sensitive to it on darker environment. The level of brightness varies on how finely the manufacturer has calibrated the display.

> This is an issue that GMH can't solve directly. However, you can use GMH brightness threshold for adaptive mod as a workaround which will automatically pause the Adaptive mod when screen brightness falls below the set threshold.
</details>

### 9. In Adaptive refresh rate mod, I set minimum refresh rate to 48Hz but while watching videos, refresh rate only goes down to 60Hz

<details>
 <summary>Click to show</summary>

> On video apps like youtube, the stock default minimum refresh rate is used (usually 60Hz) but it will automatically go down to 48Hz when playing compatible videos (i.e 24fps or 48fps videos) or when available to 24Hz when playing 24fps video.
</details>

### 10. Refresh rate goes down while playing a GAME.

<details>
 <summary>Click to show</summary>

> While using Adaptive Mod, Galaxy MaxHz pauses refresh rates when it detects an application being opened is a Game. This is to preserve input responsiveness and smoothness while playing. Being the only option available for 3rd party non-root app, Galaxy MaxHz solely relies on CATEGORY_GAME(https://developer.android.com/reference/android/content/pm/ApplicationInfo#CATEGORY_GAME) declared declared in the app's manifest.  However, there are few games that do not provide this manifest declaration. For this case, you can use the `Per-App `settings of Galaxy MaxHz to adjust its refresh rates when necessary.

> Important Note:
> When you are using "Priority Mode" in Game Launcher, you should add GMH to the excluded list in order to keep it's background service from being killed when opening a game. Not adding GMH to this list can cause abnormal behavior while gaming.
</details>


### 11. Refresh rate monitor in statusbar is split second late than the overlay. Is this normal?

<details>
 <summary>Click to show</summary>

> It's due to the limitation by the android system on the frequency of updating the notification content(~2x per second).
</details>

### 12. ~~Can I add the CQHD+(custom QHD) resolution for Note20 Ultra or S20 series device?~~

<details>
 <summary>Click to show</summary>

>~~Yes, this option is added for free as requested by some users. If it's not shown in the app, just click the "Reload Profile" in the 3-dot menu while connected to the internet to load profiles from my backend containing such resolution. The backend copies of the resolution and refresh profiles have the following additional resolution:~~
> ~~Note 20 Ultra: 3087x1439 @ 48/60/96/120 hz~~
> ~~S20/S20+/S20Ultra: 3180x1431 @ 60/96/120 hz~~

> ~~Note: The CQHD+ resolution (as well as the system pre-defined resolutions) is applied by using the hidden android IWindowManager api (equivalent to "adb shell wm size .." command in adb). How the system or applications handle custom resolutions varies and not guaranteed and beyond control of GMH. Apps that strictly observed pre-defined system resolutions (i.e. using display modes) will not benefit on this.~~

> ~~**Caution!! It's highly discourage to use this tweak on OneUI4.*. CQHD causes displayed UI to shrink after reboot or long sleep that causes difficulty on entering the lockscreen**~~.

> **Starting v8.0.0, CQHD+ will not be provided anymore by the backend on OneUI4.* devices due to some undesirable experiences of shrinking UI reported by some users.**
</details>

### 13. I enabled Power Saving Mode on Screen-off and the Always on Display (AOD) stopped working?

<details>
 <summary>Click to show</summary>

> Disable "Turn-off Always on Display" option in your device's Power Saving Mode settings.
> Note: You can use this setting to confirm that PSM on Screen Off is working. AOD turn off after 6-10 seconds.
</details>

### 14. Refresh Rate is stuck after a long idle while using Adaptive Mod

<details>
 <summary>Click to show</summary>

> In rare cases, GMH background process is being destroyed by the system. To avoid this, grant allow background activities or ignore battery optimization  to GMH. If you are using Priority Mode in Game Launcher, adding GMH to the excluded list is required.

> If the issue still persists, GMH might be crashing on your device. If this is the case, please provide us the crashlog (refer to Questions and Bug Reports section on how)
</details>

### 15. How to check if power saving mode and auto disable sync working on screen off?

<details>
 <summary>Click to show</summary>

> You can execute the following command in adb after ~10 secs. of turning off the screen.
> 
> `adb shell dumpsys content`
> 
> This will return a lengthy output, just scroll back to the first few lines.
> You can see like this:
> ```Data connected: true
> Battery saver: true
> Background network restriction: disabled
> Auto sync: u0=false```

> Alternatively, you can confirm power saving using any of the following:
> Set AOD to show always, and enable turn-off AOD on power saving settings. AOD should be gone after ~10secs. of turning off the screen.
> 
> `adb shell settings global get low_power`

> The output should be `1`.
</details>

### 16. How to setup GMH with [Tasker](https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm)? Can I assign different refresh rate or refresh rate mode on per-app basis?

<details>
 <summary>Click to show</summary>

> Creating the Tasks (the actions to perform) 

> * Tap the (+) button inside Tasker's TASKS tab and give it a name. Inside the Task Edit window, add GMH Action by clicking (+) button > Plugin > Galaxy Max Hz.

> * Inside Action Edit window, expand the configuration section to see the tasker settings/variables used by GMH. Take note of them.

> * Go back to Task Edit window and set a value to each of the variable that you want to use by clicking (+) button > Variables >Variable Set > enter the variable name and a corresponding valid value.

> * After adding all the variable set actions you need, move the GMH action to the last row (i.e. after all the variable set actions). Tip: Tap, hold and drag to re-order.

> * Test the task you created by clicking the ▶️ button at the bottom.

> * Repeat above to create additional GMH tasks for different set of configurations/variables.

>  Create a Profile (what triggers the task) 

> * Tap the (+) button found inside Tasker's PROFILES tab to create a profile. You can select different types of profile including application-opened triggers and event triggers.
> This will not be explained in details here - just explore it.

> * Once you created a profile, it will prompt you to link it to the task that you created in step 1.~~

> ~~How to assign different refresh rate or refresh rate mode to different apps.~~
> ~~If you want to override the default behavior for certain apps, just create an Application-Activity Profile(Trigger) for each of the apps that you want in Tasker's Profile tab~~

> ~~Following step 1 above, create one or more tasks for each set of GMH configurations that you want to apply including a task for your default set of GMH configuration.~~

> ~~After creating the needed tasks, go to the PROFILES tab > Tap the (+) button > Application > Select the application(s) that you want to trigger the task (GMH configurations) that you created*** > Select Activity button at the bottom > Hit the back button.~~

> ~~It will then prompt you to link a task - select the task that you want to apply for the selected application(s). After that, long-pressed the linked task > select Add Exit Task > select the task for your default GMH configuration.~~

> ~~That's it. Repeat the same procedures for a different app or set of apps that you want to apply different GMH configurations.~~

 <img src="https://forum.xda-developers.com/attachments/screenshot_20220424-002948_tasker-jpg.5596341" width=200 height=140>
 <img src="https://forum.xda-developers.com/attachments/screenshot_20220424-002953_tasker-jpg.5596343" width=200 height=140>
<img src="https://forum.xda-developers.com/attachments/screenshot_20220424-003032_tasker-jpg.5596345" width=200 height=140>
<img src="https://forum.xda-developers.com/attachments/screenshot_20220121-195737_tasker-jpg.5519497" width=200 height=140>
<img src="https://forum.xda-developers.com/attachments/screenshot_20220121-195715_tasker-jpg.5519499" width=200 height=140>


>  **Update: `Per-app` settings feature is added to Galaxy MaxHz starting v8.0.0**
</details>

### 17. If I uninstall the app, will it restore the phone to its stock settings. The reboot.

<details>
 <summary>Click to show</summary>

> Yes, just set the max hz and min hz back to the default values and disable all screen off mods before uninstalling. 

</details>

### 18. I have a rooted device and modified the device model number, will it work?

<details>
 <summary>Click to show</summary>

> It can affect premium features on the device and can cause issue as refresh rate profiles are loaded based on the device model number.

</details>


### 19. Why is it that when I enable Auto SENSORS OFF, the notification panels gets expanded very quickly every time I unlock the device ? Can you make Auto SENSORS OFF deactivate when the screen turns on instead of device unlock?


<details>
 <summary>Click to show</summary>

> Since the api behind android's SENSORS OFF feature is highly restricted and inaccessible by 3rd party on stock roms, GMH's Auto SENSORS OFF option is implemented using touch simulations on the Sensors Off tile which require the notification panel to be expanded when switching this tile on and off. Thus, requiring to put the SENSORS OFF tile within the first 4 positions.

> Unfortunately, it is not possible to disable SENSORS OFF while device is locked. This android feature is restricted not to be turned-off with locked screen.

```
@Override
public void setIsEnabled(boolean isEnabled) {
  // Don't allow sensors to be reenabled from the lock screen.    if (mIsEnabled && mKeyguardManager.isKeyguardLocked()) {
     return;
  }
  mMetricsFeatureProvider.action(getApplicationContext(), SettingsEnums.QS_SENSOR_PRIVACY, isEnabled);
  mIsEnabled = isEnabled;
 mSensorPrivacyManager.setSensorPrivacy(isEnabled);
 }
```
</details>
