package com.tribalfs.gmh.resochanger

import android.annotation.SuppressLint
import android.content.Context
import com.tribalfs.gmh.helpers.CheckBlacklistApiSt
import com.tribalfs.gmh.helpers.SingletonHolder


@SuppressLint("PrivateApi")
class ResolutionChangeApiSt private constructor (val context: Context) {

    companion object : SingletonHolder<ResolutionChangeApiSt, Context>(::ResolutionChangeApiSt) {
        private const val USER_ID = -3
    }


    fun setDisplayResolution(displayId: Int, res: String, density: Int?): Boolean {

        if (!CheckBlacklistApiSt.instance(context).isAllowed()
            && !CheckBlacklistApiSt.instance(context).setAllowed()) return false

        var arrRes = res.split("x")
        if (arrRes.size != 2) arrRes = res.split(",")
        if (arrRes.size != 2) return false
        return setDisplayResolution(displayId, arrRes[1].toInt(), arrRes[0].toInt(), density)
    }

    @SuppressLint("PrivateApi")
    private fun setDisplayResolution(displayId: Int, w: Int, h: Int, d: Int?): Boolean {
        /*try {
            val serviceManager  = Class.forName("android.os.ServiceManager")
            val service : Method = serviceManager.getDeclaredMethod("getService", String::class.java)
            val binder = service.invoke(null, "window") as IBinder
            val windowManagerStub = Class.forName("android.view.IWindowManager").classes[0]
            val serviceObj = windowManagerStub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
            windowManagerStub.methods.first { it.name == "setForcedDisplaySizeDensity" }
                .invoke(serviceObj, displayId, w, h, d, true, -1)
           // windowManagerStub.methods.first { it.name == "setOverscan" }.invoke(serviceObj, displayId, left, top, right, bottom)

            return true
        } catch (_: Exception) {*/
            try{
                val wmService = Class.forName("android.view.WindowManagerGlobal")
                    .getDeclaredMethod("getWindowManagerService")
                    .invoke(null) ?: return false


                with(Class.forName("android.view.IWindowManager")) {
                    d?.let {d->
                        getDeclaredMethod(
                            "setForcedDisplayDensityForUser",
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType
                        )
                            .invoke(wmService, displayId, d, USER_ID)
                    }

                    getDeclaredMethod(
                        "setForcedDisplaySize",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType
                    )
                        .invoke(wmService, displayId, w, h)

                }
                return true
            }catch(_:Exception) {
                return false
            }
        //}
    }


/*    *//* Clear display resolution *//*
    @SuppressLint("PrivateApi")
    fun clearDisplayResolution(displayId: Int) {
            if (!CheckBlacklistApiSt.get(context).isAllowed()
            && !CheckBlacklistApiSt.get(context).setAllowed()) return false

        *//* Return if we can't get WindowManager service *//*
        val wmService = windowManagerService ?: return

        try {
            Class.forName(WINDOW_MANAGER)
                .getDeclaredMethod("clearForcedDisplaySize", Int::class.javaPrimitiveType)
                .invoke(wmService, displayId)
        } catch (_: java.lang.Exception) {
        }
    }

    *//* Clear display density *//*
    @SuppressLint("PrivateApi")
    fun clearDisplayDensity(displayId: Int) {
        *//* Return if we can't get WindowManager service *//*
        val wmService = windowManagerService ?: return

        *//* Try the old API for some devices *//*
        try {
            Class.forName(WINDOW_MANAGER)
                .getDeclaredMethod("clearForcedDisplayDensity", Int::class.javaPrimitiveType)
                .invoke(wmService, displayId)
        } catch (_: java.lang.Exception) {
        }


        try {
            Class.forName(WINDOW_MANAGER)
                .getDeclaredMethod(
                    "clearForcedDisplayDensityForUser",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                .invoke(wmService, displayId, USER_ID)
        } catch (_: java.lang.Exception) {
        }
    }*/

}



