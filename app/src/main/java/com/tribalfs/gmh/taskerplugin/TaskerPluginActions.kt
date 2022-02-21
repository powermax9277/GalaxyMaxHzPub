package com.tribalfs.gmh.taskerplugin

import android.accessibilityservice.AccessibilityService.*
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.util.Size
import android.view.Display.STATE_ON
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputInfo
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputInfos
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import com.tribalfs.gmh.ACTION_HIDE_MAIN_ACTIVITY
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.gmhAccessInstance
import com.tribalfs.gmh.MyApplication.Companion.applicationName
import com.tribalfs.gmh.helpers.*
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.highestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveMode
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.keepModeOnPowerSaving
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzCurMode
import com.tribalfs.gmh.helpers.CacheSettings.minHzListForAdp
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.CacheSettings.supportedHzIntAllMod
import com.tribalfs.gmh.helpers.CacheSettings.supportedHzIntCurMod
import com.tribalfs.gmh.helpers.CacheSettings.turnOffAutoSensorsOff
import com.tribalfs.gmh.helpers.DozeUpdater.mwInterval
import com.tribalfs.gmh.helpers.DozeUpdater.updateDozValues
import com.tribalfs.gmh.helpers.UtilCommon.closestValue
import com.tribalfs.gmh.profiles.ProfilesObj
import com.tribalfs.gmh.resochanger.ResolutionChangeUtil
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import com.tribalfs.gmh.taskerplugin.TaskerKeys.auto_sensors_off
import com.tribalfs.gmh.taskerplugin.TaskerKeys.change_res
import com.tribalfs.gmh.taskerplugin.TaskerKeys.keep_motion_smoothness_on_psm
import com.tribalfs.gmh.taskerplugin.TaskerKeys.max_hertz
import com.tribalfs.gmh.taskerplugin.TaskerKeys.min_hertz
import com.tribalfs.gmh.taskerplugin.TaskerKeys.motion_smoothness_mode
import com.tribalfs.gmh.taskerplugin.TaskerKeys.protect_battery
import com.tribalfs.gmh.taskerplugin.TaskerKeys.quick_doze_mod
import kotlinx.coroutines.*


@ExperimentalCoroutinesApi
class DynamicInputHelper(config: TaskerPluginConfig<Unit>) : TaskerPluginConfigHelperNoOutputOrInput<DynamicInputRunner>(config) {
    override val runnerClass = DynamicInputRunner::class.java
    override fun addToStringBlurb(input: TaskerInput<Unit>, blurbBuilder: StringBuilder) {
        var helperStr = "Configure $applicationName by setting one or more of the following input variables:"
        infosForTasker.forEach {
            helperStr += "\n\n%${it.key}\n${it.name}\n${it.desc}"
        }
        blurbBuilder.insert(
            0,"$helperStr\n" +
                    "\nOnly use the variable(s) you need. If two or more variables are used, order of execution is based on the above order." +
                    "\n\n********************************")
    }
}


@ExperimentalCoroutinesApi
class DynamicInputActivity : ActivityConfigTaskerNoOutputOrInput<DynamicInputRunner, DynamicInputHelper>() {
    override fun getNewHelper(config: TaskerPluginConfig<Unit>) = DynamicInputHelper(config)
    override val inputForTasker = TaskerInput(Unit, TaskerInputInfos().apply {
        infosForTasker.forEach { add(TaskerInputInfo(it.key,it.name,it.desc,true, "%${it.key}")) }
    })
}

data class InfoFromMainApp(val name: String, val key: String, val desc: String)

class InfosFromMainApp : ArrayList<InfoFromMainApp>()


private val infosForTasker = InfosFromMainApp().apply {
    val resoList:MutableList<String> = mutableListOf()

    fun <T> List<T>.joinToStringWithOr(delimiter: String): String{
        return "${dropLast(1).joinToString(delimiter)} or ${last()}"
    }

    ProfilesObj.refreshRateModeMap["$displayId-0"]?.forEach {
        resoList.add(it.keys.first())
    }


    if (resoList.size  > 1) {
        add(
            InfoFromMainApp(
                "Change Screen Resolution",
                change_res,
                "Valid value: Any of the following: ${resoList.joinToStringWithOr(", ")}"
            )
        )
    }


    if (highestHzForAllMode > SIXTY_HZ) {
        addAll(
            arrayOf(
                InfoFromMainApp(
                    "Refresh Rate Mode",
                    motion_smoothness_mode,
                    "Valid value: 0, 1 or 2 \n[for Standard, Adaptive or Force High, respectively]"
                ),
                InfoFromMainApp(
                    "Max Refresh Rate",
                    max_hertz,
                    "Valid value: Any of following: ${supportedHzIntAllMod?.joinToStringWithOr(", ")}. " +
                            /*Any supported refresh rate as indicated in GMH (e.g. 96, 120, 144). "*/
                            "\nThis will change the Max Hz either for non-PSM or PSM depending on the current PSM state of the device."
                )
            )
        )
    }



    if (isPremium.get()!!) {
        if (minHzListForAdp?.size ?: 0 > 1) {
            add(
                InfoFromMainApp(
                    "Minimum Refresh Rate for Adaptive",
                    min_hertz,
                    "Valid value: Any of following: ${minHzListForAdp?.joinToStringWithOr(", ")}."
                )
            )
        }

        addAll(
            arrayOf(
                InfoFromMainApp(
                    "Keep Motion Smoothness on PSM",
                    keep_motion_smoothness_on_psm,
                    "Valid value: true or false"
                ),
                InfoFromMainApp(
                    "Quick-doze Mod - Initial Maintenance Window Interval (minutes)",
                    quick_doze_mod,
                    "Valid value: 1-120, 0 [doze all the way] or -1 [disable quick-doze]"
                ),
                InfoFromMainApp(
                    "Switch auto SENSORS OFF",
                    auto_sensors_off,
                    "Valid value: 0, 1, 2 or 3" +
                            "\n[for disable auto sensors off, enable auto sensors off, turn on tile only or turn off tile only, respectively]"
                ),
                InfoFromMainApp(
                    "Protect Battery",
                    protect_battery,
                    "Valid value: true or false" +
                            "\n[limits charging to 85% on Samsung device with OneUI4.0+ to extend its battery lifespan]"
                )
            )
        )
    }
}



@ExperimentalCoroutinesApi
class DynamicInputRunner : TaskerPluginRunnerActionNoOutputOrInput() {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun run(context: Context, input: TaskerInput<Unit>): TaskerPluginResult<Unit> {
        val appCtx = context.applicationContext
        val dm by lazy {appCtx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager}
        val km by lazy {appCtx.getSystemService(KEYGUARD_SERVICE) as KeyguardManager}

        //var success = true
        infosForTasker.forEach {
            input.dynamic.getByKey(it.key)?.let { info ->
                if ("%${info.key}" == info.value) return@let

                when (info.key) {

                    change_res -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                appCtx.sendBroadcast(Intent(ACTION_HIDE_MAIN_ACTIVITY))
                                delay(550)
                                val resStrSplit = (info.value as String).split("x")
                                val reso = Size(resStrSplit[1].toInt(),resStrSplit[0].toInt())
                                ResolutionChangeUtil(appCtx).changeRes(reso)
                            } catch (_: Exception) {
                            }
                        }
                    }

                    motion_smoothness_mode -> {
                        try{
                            when (val msm = info.value as String){
                                REFRESH_RATE_MODE_STANDARD -> UtilRefreshRateSt.instance(appCtx).setRefreshRateMode(msm)
                                REFRESH_RATE_MODE_ALWAYS -> UtilRefreshRateSt.instance(appCtx).tryThisRrm(msm, null)
                                REFRESH_RATE_MODE_SEAMLESS ->{
                                    if (isPremium.get()!! || isOfficialAdaptive) {
                                        UtilRefreshRateSt.instance(appCtx).tryThisRrm(msm, null)
                                    }
                                }
                            }
                        } catch(_: java.lang.Exception){
                            // success = false
                        }
                    }

                    max_hertz -> {
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val mHz = (info.value as String).toInt()
                                if (dm.getDisplay(displayId).state == STATE_ON) {
                                    UtilChangeMaxHz(appCtx).changeMaxHz(mHz)
                                } else {
                                    if (supportedHzIntCurMod?.indexOfFirst { hz -> hz == mHz } != -1) {
                                        prrActive.set(mHz.coerceAtLeast(lowestHzCurMode))
                                        if (isPremium.get()!! && isPowerSaveMode.get() == true){// && keepModeOnPowerSaving) {
                                            UtilsPrefsGmhSt.instance(appCtx).hzPrefMaxRefreshRatePsm = mHz
                                        }else{
                                            UtilsPrefsGmhSt.instance(appCtx).hzPrefMaxRefreshRate = mHz
                                        }
                                    }else{
                                        Toast.makeText(appCtx,"Refresh rate not supported.",Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (_: Exception) {
                                //  success = false
                            }
                        }
                    }


                    keep_motion_smoothness_on_psm -> {
                        try {
                            ((info.value as String).toBoolean()).let { isKeep ->
                                CoroutineScope(Dispatchers.Default).launch {
                                    if (hasWriteSecureSetPerm) {
                                        if (isPremium.get()!! || !isKeep) {
                                            keepModeOnPowerSaving = isKeep

                                            PsmChangeHandler.instance(appCtx).handle()
                                            UtilsPrefsGmhSt.instance(appCtx).gmhPrefKmsOnPsm =
                                                isKeep
                                            gmhAccessInstance?.setupAdaptiveEnhancer()
                                        }

                                    }
                                }
                            }
                        }catch(_: java.lang.Exception){
                            // success = false
                        }
                    }




                    quick_doze_mod -> {
                        if (isPremium.get()!!) {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    (info.value as String).toInt().let { dozInt ->
                                        if (dozInt == -1){
                                            UtilsPrefsGmhSt.instance(appCtx).gmhPrefQuickDozeIsOn = false
                                            appCtx.updateDozValues(false, null)
                                        }else{
                                            UtilsPrefsGmhSt.instance(appCtx).gmhPrefQuickDozeIsOn = true
                                            mwInterval.closestValue(dozInt).let{dozeMw ->
                                                UtilsPrefsGmhSt.instance(appCtx).gmhPrefGDozeModOpt = dozeMw!!
                                                appCtx.updateDozValues(true, dozeMw)
                                            }
                                        }
                                    }
                                } catch (_: java.lang.Exception) {}
                            }
                        }
                    }


                    auto_sensors_off ->{
                        if (isPremium.get()!!) {
                            CoroutineScope(Dispatchers.IO).launch{
                                try {
                                    when ((info.value as String).toInt()) {
                                        1 -> {//turn On
                                            UtilsPrefsGmhSt.instance(appCtx).gmhPrefSensorsOff = true
                                            if (dm.getDisplay(displayId).state != STATE_ON && UtilsPrefsGmhSt.instance(appCtx).gmhPrefSensorsOff){
                                                gmhAccessInstance?.checkAutoSensorsOff(true, screenOffOnly = true)
                                            }
                                        }

                                        0 -> {//turn Off
                                            if (km.isKeyguardLocked){
                                                turnOffAutoSensorsOff = true
                                            }else{
                                                gmhAccessInstance?.checkAutoSensorsOff(false, screenOffOnly = true)
                                                UtilsPrefsGmhSt.instance(appCtx).gmhPrefSensorsOff = false
                                            }

                                        }

                                        2 -> {//turn On Tile only
                                            gmhAccessInstance?.checkAutoSensorsOff(true, screenOffOnly = false)

                                        }

                                        3 -> {//turn Off Tile only
                                            if (km.isKeyguardLocked) {
                                                if (!UtilsPrefsGmhSt.instance(appCtx).gmhPrefSensorsOff){
                                                    //temporarily turn on to trigger on unlock
                                                    UtilsPrefsGmhSt.instance(appCtx).gmhPrefSensorsOff = true
                                                    turnOffAutoSensorsOff = true//this will switch off mUtilsPrefsGmh.gmhPrefSensorsOff automatically
                                                }//else don't need to touch settings
                                            }else{
                                                gmhAccessInstance?.checkAutoSensorsOff(false, screenOffOnly = true)

                                            }
                                        }
                                        else -> {
                                            launch(Dispatchers.Main) {
                                                Toast.makeText(
                                                    appCtx,
                                                    "${info.value} is not a valid value for $auto_sensors_off",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }catch (_: java.lang.Exception) { }
                            }
                        }
                    }

                    min_hertz ->{
                        if (isPremium.get()!!) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val minHz = (info.value as String).toInt()
                                if (minHzListForAdp?.indexOf(minHz) != -1){
                                    if (isOfficialAdaptive && minHz < UtilsDeviceInfoSt.instance(context).regularMinHz) {
                                        if (gmhAccessInstance == null/*!isAccessibilityEnabled(appCtx, GalaxyMaxHzAccess::class.java)*/) {
                                            return@launch
                                        }
                                    }
                                    if (minHz >= prrActive.get()!!) return@launch

                                    UtilsPrefsGmhSt.instance(appCtx).gmhPrefMinHzAdapt = minHz

                                    UtilRefreshRateSt.instance(appCtx).applyMinHz()

                                    gmhAccessInstance?.setupAdaptiveEnhancer()

                                    /*appCtx.startService(
                                        Intent(appCtx,GalaxyMaxHzAccess::class.java).apply {
                                            putExtra(SETUP_ADAPTIVE, true)
                                        }
                                    )*/
                                }else{
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(
                                            appCtx,
                                            "Invalid minimum Hertz for adaptive value.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }

                    }

                    protect_battery ->{
                        try {
                            Settings.Global.putString(
                                appCtx.contentResolver,
                                "protect_battery",
                                if ((info.value as String).toBoolean()) "1" else "0"
                            )
                        }catch(_:Exception){}
                    }

                    else -> {

                    }

                }
            }
        }

        return TaskerPluginResultSucess()
    }

}


