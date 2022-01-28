package com.tribalfs.gmh.resochanger

import android.annotation.SuppressLint
import android.content.Context
import android.util.Size
import com.tribalfs.gmh.helpers.CheckBlacklistApiSt


@SuppressLint("PrivateApi")
class ResolutionChangeApi (val context: Context) {

    private val userId = -3

    @SuppressLint("PrivateApi")
    fun setDisplaySizeDensity(displayId: Int, reso: Size, d: Int?): Boolean {
        if (!CheckBlacklistApiSt.instance(context).isAllowed()
            && !CheckBlacklistApiSt.instance(context).setAllowed()) return false

       /* if (d != null) {
            try {
                val serviceManager = Class.forName("android.os.ServiceManager")
                val service: Method =
                    serviceManager.getDeclaredMethod("getService", String::class.java)
                val binder = service.invoke(null, "window") as IBinder
                val windowManagerStub = Class.forName("android.view.IWindowManager").classes[0]
                val serviceObj = windowManagerStub.getMethod("asInterface", IBinder::class.java)
                    .invoke(null, binder)
                windowManagerStub.methods.first { it.name == "setForcedDisplaySizeDensity" }
                    .invoke(serviceObj, displayId, reso.width, reso.height, d, true, -1)
                // windowManagerStub.methods.first { it.name == "setOverscan" }.invoke(serviceObj, displayId, left, top, right, bottom)

                return true
            } catch (_: Exception) {
            }
        }*/

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
                        .invoke(wmService, displayId, d, userId)
                }

                getDeclaredMethod(
                    "setForcedDisplaySize",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                    .invoke(wmService, displayId, reso.width, reso.height)


            }
            return true
        }catch(_:Exception) {
            return false
        }

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



