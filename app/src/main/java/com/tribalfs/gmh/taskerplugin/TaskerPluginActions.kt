package com.tribalfs.gmh.taskerplugin

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputInfo
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputInfos
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import com.tribalfs.gmh.GalaxyMaxHzAccess
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.SCREEN_OFF_ONLY
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.SETUP_ADAPTIVE
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.SWITCH_AUTO_SENSORS
import com.tribalfs.gmh.MyApplication.Companion.applicationName
import com.tribalfs.gmh.helpers.*
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.highestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveModeOn
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.CacheSettings.keepModeOnPowerSaving
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzCurMode
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.lrrPref
import com.tribalfs.gmh.helpers.CacheSettings.minHzListForAdp
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.CacheSettings.supportedHzIntAllMod
import com.tribalfs.gmh.helpers.CacheSettings.supportedHzIntCurMod
import com.tribalfs.gmh.helpers.CacheSettings.turnOffAutoSensorsOff
import com.tribalfs.gmh.helpers.DozeUpdater.mwInterval
import com.tribalfs.gmh.helpers.DozeUpdater.updateDozValues
import com.tribalfs.gmh.helpers.UtilsCommon.closestValue
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_ALWAYS
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_SEAMLESS
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_STANDARD
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.STANDARD_REFRESH_RATE_HZ
import com.tribalfs.gmh.profiles.ProfilesObj
import com.tribalfs.gmh.resochanger.ResolutionChangeUtilSt
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import com.tribalfs.gmh.taskerplugin.TaskerKeys.auto_sensors_off
import com.tribalfs.gmh.taskerplugin.TaskerKeys.change_res
import com.tribalfs.gmh.taskerplugin.TaskerKeys.keep_motion_smoothness_on_psm
import com.tribalfs.gmh.taskerplugin.TaskerKeys.max_hertz
import com.tribalfs.gmh.taskerplugin.TaskerKeys.min_hertz
import com.tribalfs.gmh.taskerplugin.TaskerKeys.motion_smoothness_mode
import com.tribalfs.gmh.taskerplugin.TaskerKeys.quick_doze_mod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlin.math.max


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

private var resoList:List<String>? = null

private val infosForTasker = InfosFromMainApp().apply {

    fun <T> List<T>.joinToStringWithOr(delimiter: String): String{
        return "${dropLast(1).joinToString(delimiter)} or ${last()}"
    }

    if (resoList == null) {
        val tempList = mutableListOf<String>()
        ProfilesObj.refreshRateModeMap["$displayId-0"]?.forEach {
            tempList.add(it.keys.first())
        }
        resoList = tempList
    }

    if (resoList?.size?:0 > 1) {
        add(
            InfoFromMainApp(
                "Change Screen Resolution",
                change_res,
                "Valid value: Any of the following: ${resoList?.joinToStringWithOr(", ")}"
            )
        )
    }


    if (highestHzForAllMode > STANDARD_REFRESH_RATE_HZ) {
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
                    "Minimum Refresh Rate for Adaptive Mod",
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
                    "Valid value: 1-120, 0 [no maintenance window] or -1 [to disable quick-doze]"
                ),
                InfoFromMainApp(
                    "Switch auto SENSORS OFF",
                    auto_sensors_off,
                    "Valid value: 0, 1, 2 or 3" +
                            "\n[for disable auto sensors off, enable auto sensors off, turn on tile only or turn off tile only, respectively]"
                )
            )
        )
    }
}



@ExperimentalCoroutinesApi
class DynamicInputRunner : TaskerPluginRunnerActionNoOutputOrInput() {
    @SuppressLint("NewApi")
    override fun run(context: Context, input: TaskerInput<Unit>): TaskerPluginResult<Unit> {
        val appCtx = context.applicationContext
        val mUtilsPrefsGmh by lazy { UtilsPrefsGmh(appCtx)}
        val mUtilsRefreshRate by lazy { UtilsRefreshRate(appCtx) }
        //var success = true
        infosForTasker.forEach {
            input.dynamic.getByKey(it.key)?.let { info ->
                if ("%${info.key}" == info.value) return@let

                when (info.key) {

                    change_res -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {

                                ResolutionChangeUtilSt.instance(appCtx)
                                    .changeRes(info.value as String)
                            } catch (_: Exception) {
                            }
                        }
                    }

                    motion_smoothness_mode -> {
                        try{
                            when (val msm = info.value as String){
                                REFRESH_RATE_MODE_STANDARD -> mUtilsRefreshRate.setRefreshRateMode(msm)
                                REFRESH_RATE_MODE_ALWAYS -> mUtilsRefreshRate.tryPrefRefreshRateMode(msm, null)
                                REFRESH_RATE_MODE_SEAMLESS ->{
                                    if (isPremium.get()!! || isOfficialAdaptive) {
                                        mUtilsRefreshRate.tryPrefRefreshRateMode(msm, null)
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
                                if (isScreenOn) {
                                    UtilsChangeMaxHzSt.instance(appCtx).changeMaxHz(mHz)
                                } else {
                                    if (supportedHzIntCurMod?.indexOfFirst { hz -> hz == mHz } != -1) {
                                        prrActive.set(mHz.coerceAtLeast(lowestHzCurMode))
                                        if (isPremium.get()!! && isPowerSaveModeOn.get() == true){// && keepModeOnPowerSaving) {
                                            mUtilsPrefsGmh.hzPrefMaxRefreshRatePsm = mHz
                                        }else{
                                            mUtilsPrefsGmh.hzPrefMaxRefreshRate = mHz
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
                            ((info.value as String).toBoolean()).let {isKeep ->
                                if (isPremium.get()!! || !isKeep) {
                                    keepModeOnPowerSaving = isKeep
                                    CoroutineScope(Dispatchers.Default).launch {
                                        PsmChangeHandler.instance(appCtx).handle()
                                        mUtilsPrefsGmh.gmhPrefKmsOnPsm = isKeep
                                        appCtx.startService(Intent(appCtx, GalaxyMaxHzAccess::class.java).apply{
                                            putExtra(SETUP_ADAPTIVE, true)
                                        })
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
                                            mUtilsPrefsGmh.gmhPrefQuickDozeIsOn = false
                                            appCtx.updateDozValues(false, null)
                                        }else{
                                            mUtilsPrefsGmh.gmhPrefQuickDozeIsOn = true
                                            mwInterval.closestValue(dozInt).let{dozeMw ->
                                                mUtilsPrefsGmh.gmhPrefGDozeModOpt = dozeMw!!
                                                appCtx.updateDozValues(true, dozeMw)
                                            }
                                        }
                                    }
                                } catch (_: java.lang.Exception) {

                                    //   success = false
                                }
                            }
                        }
                    }


                    auto_sensors_off ->{
                        if (isPremium.get()!!) {
                            CoroutineScope(Dispatchers.IO).launch{
                                try {
                                    when ((info.value as String).toInt()) {
                                        1 -> {//turn On
                                            mUtilsPrefsGmh.gmhPrefSensorsOff = (CheckBlacklistApiSt.instance(appCtx).isAllowed()
                                                    || CheckBlacklistApiSt.instance(appCtx).setAllowed())

                                            if (!isScreenOn && mUtilsPrefsGmh.gmhPrefSensorsOff){
                                                // turnOnAutoSensorsOff = true
                                                appCtx.startService(
                                                    Intent(appCtx, GalaxyMaxHzAccess::class.java).apply {
                                                        putExtra(SWITCH_AUTO_SENSORS, true)
                                                    }
                                                )
                                            }
                                        }

                                        0 -> {//turn Off
                                            if ((appCtx.getSystemService(AccessibilityService.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked){
                                                turnOffAutoSensorsOff = true
                                            }else{
                                                appCtx.startService(
                                                    Intent(appCtx, GalaxyMaxHzAccess::class.java).apply {
                                                        putExtra(SWITCH_AUTO_SENSORS, false)
                                                    }
                                                )
                                                mUtilsPrefsGmh.gmhPrefSensorsOff = false
                                            }

                                        }

                                        2 -> {//turn On Tile only
                                            appCtx.startService(
                                                Intent(appCtx, GalaxyMaxHzAccess::class.java).apply {
                                                    putExtra(SWITCH_AUTO_SENSORS, true)
                                                    putExtra(SCREEN_OFF_ONLY, false)
                                                }
                                            )

                                        }

                                        3 -> {//turn Off Tile only
                                            if ((appCtx.getSystemService(AccessibilityService.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked) {
                                                if (!mUtilsPrefsGmh.gmhPrefSensorsOff){
                                                    //temporarily turn on to trigger on unlock
                                                    mUtilsPrefsGmh.gmhPrefSensorsOff = true
                                                    turnOffAutoSensorsOff = true//this will switch off mUtilsPrefsGmh.gmhPrefSensorsOff automatically
                                                }//else don't need to touch settings
                                            }else{
                                                appCtx.startService(
                                                    Intent(appCtx, GalaxyMaxHzAccess::class.java).apply {
                                                        putExtra(SWITCH_AUTO_SENSORS, false)
                                                    }
                                                )
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
                        if (isPremium.get()!! && hasWriteSecureSetPerm) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val minHz = (info.value as String).toInt()
                                val idx = minHzListForAdp?.indexOf(minHz)
                                if (idx != -1){
                                    lrrPref.set(max(lowestHzForAllMode, minHz))
                                    mUtilsPrefsGmh.gmhPrefMinHzAdapt = minHz
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

                    else -> {

                    }

                }
            }
        }

        return TaskerPluginResultSucess()
    }

}


