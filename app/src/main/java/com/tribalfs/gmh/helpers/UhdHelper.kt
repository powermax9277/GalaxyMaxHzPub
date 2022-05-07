package com.tribalfs.gmh.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Display
import android.view.Window
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.helpers.UhdHelper
import java.lang.reflect.Field
import java.util.concurrent.atomic.AtomicBoolean

/**
 * UhdHelper is a convenience class to provide interfaces to query
 * 1) Supported Display modes.
 * 2) Get current display mode
 * 3) Set preferred mode.
 */
internal class UhdHelper(var mContext: Context?) {
    private val sDisplayClassName = "android.view.Display"
    private val sPreferredDisplayModeIdFieldName = "preferredDisplayModeId"
    private val mIsSetModeInProgress: AtomicBoolean
    private val mWorkHandler: WorkHandler
    private val overlayStateChangeReceiver: OverlayStateChangeReceiver
    var isReceiversRegistered: Boolean
    private var isInterstitialFadeReceived = false
    private var mTargetWindow: Window? = null
    private var currentOverlayStatus = 0
    var mDisplayManager: DisplayManager
    var mDisplayListener: DisplayManager.DisplayListener? = null

    /**
     * Handler that handles the broadcast or timeout
     * prcoessing and issues callbacks accordingly.
     */
    private inner class WorkHandler  // private UhdHelperListener mCallbackListener;
        (looper: Looper?) : Handler(looper!!) {
        private var mRequestedModeId = 0
        fun setExpectedMode(modeId: Int) {
            mRequestedModeId = modeId
        }

        /* private void setCallbackListener(UhdHelperListener listener) {
            this.mCallbackListener = listener;
        }*/
        @RequiresApi(api = Build.VERSION_CODES.M)
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MODE_CHANGED_MSG -> {
                    val mode = mode
                    if (mode == null) {
                        Log.w(TAG, "Mode query returned null after onDisplayChanged callback")
                        return
                    }
                    if (mode.modeId == mRequestedModeId) {
                        Log.i(TAG, "Callback for expected change Id= $mRequestedModeId")
                        maybeDoACallback(mode)
                        doPostModeSetCleanup()
                    } else {
                        Log.w(
                            TAG,
                            "Callback received but not expected mode. Mode= $mode expected= $mRequestedModeId"
                        )
                    }
                }
                MODE_CHANGE_TIMEOUT_MSG -> {
                    Log.i(TAG, "Time out without mode change")
                    maybeDoACallback(null)
                    doPostModeSetCleanup()
                }
                INTERSTITIAL_FADED_BROADCAST_MSG -> if (!isInterstitialFadeReceived) {
                    Log.i(TAG, "Broadcast for text fade received, Initializing the mode change.")
                    isInterstitialFadeReceived = true
                    initModeChange(mRequestedModeId, null)
                }
                INTERSTITIAL_TIMEOUT_MSG -> if (!isInterstitialFadeReceived) {
                    Log.w(
                        TAG,
                        "Didn't received any broadcast for interstitial text fade till time out, starting the mode change."
                    )
                    isInterstitialFadeReceived = true //So we don't do another.
                    initModeChange(mRequestedModeId, null)
                }
                else -> {}
            }
        }

        private fun maybeDoACallback(mode: Display.Mode?) {
            /*  if(this.mCallbackListener !=null) {
                Log.d(TAG, "Sending callback to listener");
                this.mCallbackListener.onModeChanged(mode);
            } else {
                Log.d(TAG, "Can't issue callback as no listener registered");
            }*/
        }

        /**
         * Removal of message and unregistering receiver after mode set is done.
         */
        private fun doPostModeSetCleanup() {
            if (currentOverlayStatus != OVERLAY_STATE_DISMISSED) {
                Log.i(TAG, "Tearing down the overlay Post mode switch attempt.")
                currentOverlayStatus = OVERLAY_STATE_DISMISSED
            }
            synchronized(mIsSetModeInProgress) {

                //need these to be run in order, tell compiler
                // not to reoder the instructions.
                this.removeMessages(MODE_CHANGE_TIMEOUT_MSG)
                if (isReceiversRegistered) {
                    mDisplayManager.unregisterDisplayListener(mDisplayListener)
                    mContext!!.unregisterReceiver(overlayStateChangeReceiver)
                    isReceiversRegistered = false
                }
                this.removeMessages(MODE_CHANGED_MSG)
                //  mCallbackListener = null;
                mIsSetModeInProgress.set(false)
            }
        }
    }

    /**
     * Private class for receiving the
     * [STATE][com.amazon.tv.notification.modeswitch_overlay.extra.STATE] events.
     */
    private inner class OverlayStateChangeReceiver : BroadcastReceiver() {
        private val OVERLAY_FADE_COMPLETE_EXTRA = 3
        override fun onReceive(context: Context, intent: Intent) {
            currentOverlayStatus = intent.getIntExtra(MODESWITCH_OVERLAY_EXTRA_STATE, -1)
            if (currentOverlayStatus == OVERLAY_FADE_COMPLETE_EXTRA && !isInterstitialFadeReceived) {
                mWorkHandler.removeMessages(INTERSTITIAL_TIMEOUT_MSG)
                mWorkHandler.sendMessage(mWorkHandler.obtainMessage(INTERSTITIAL_FADED_BROADCAST_MSG))
                Log.i(TAG, "Got the Interstitial text fade broadcast, Starting the mode change")
            }
        }
    }

    /**
     * Utility method to check if device is Amazon Fire TV device
     * @return `true` true if device is Amazon Fire TV device.
     */
    private val isAmazonFireTVDevice: Boolean
        private get() {
            val deviceName = Build.MODEL
            val manufacturerName = Build.MANUFACTURER
            return (deviceName.startsWith("AFT")
                    && "Amazon".equals(manufacturerName, ignoreCase = true))
        }

    /**
     * Returns the current Display mode.
     *
     * @return [Mode][Display.Mode]
     * that is currently set on the system or NULL if an error occurred.
     */
    val mode: Display.Mode?
        get() {
            val currentDisplay = currentDisplay ?: return null
            try {
                val classToInvestigate = Class.forName(sDisplayClassName)
                val sGetModeMethodName = "getMode"
                val getModeMethod = classToInvestigate.getDeclaredMethod(sGetModeMethodName, null)
                val currentMode = getModeMethod.invoke(currentDisplay, null)
                return convertReturnedModeToInternalMode(currentMode)
            } catch (e: Exception) {
                Log.e(TAG, e.localizedMessage)
            }
            Log.e(TAG, "Current Mode is not present in supported Modes")
            return null
        }

    /**
     * Utility function to parse android.view.Display,Mode to
     * [mode][Display.Mode]
     * @param systemMode
     * @return [Mode][Display.Mode] object
     * or NULL if an error occurred.
     */
    private fun convertReturnedModeToInternalMode(systemMode: Any): Display.Mode? {
        val returnedInstance: Display.Mode? = null
        try {
            val modeClass: Class<*> = systemMode.javaClass
            val sGetModeIdMethodName = "getModeId"
            val modeId = modeClass.getDeclaredMethod(sGetModeIdMethodName).invoke(systemMode) as Int
            val sGetPhysicalWidthMethodName = "getPhysicalWidth"
            val width =
                modeClass.getDeclaredMethod(sGetPhysicalWidthMethodName).invoke(systemMode) as Int
            val sGetPhysicalHeightMethodName = "getPhysicalHeight"
            val height =
                modeClass.getDeclaredMethod(sGetPhysicalHeightMethodName).invoke(systemMode) as Int
            val sGetRefreshRateMethodName = "getRefreshRate"
            val refreshRate =
                modeClass.getDeclaredMethod(sGetRefreshRateMethodName).invoke(systemMode) as Float
            // returnedInstance =  mInternalDisplay.getModeInstance(modeId, width, height, refreshRate);
        } catch (e: Exception) {
            Log.e(TAG, "error converting", e)
        }
        return returnedInstance
    }

    /**
     * Returns all the supported modes.
     *
     * @return An array of
     * [Mode][Display.Mode] objects
     * or NULL if an error occurred.
     */
    val supportedModes: Array<Display.Mode?>?
        get() {
            var returnedSupportedModes: Array<Display.Mode?>? = null
            try {
                val classToInvestigate = Class.forName(sDisplayClassName)
                val sSupportedModesMethodName = "getSupportedModes"
                val getSupportedMethod =
                    classToInvestigate.getDeclaredMethod(sSupportedModesMethodName, null)
                val SupportedModes = getSupportedMethod.invoke(currentDisplay, null) as Array<Any>
                returnedSupportedModes = arrayOfNulls(SupportedModes.size)
                var i = 0
                for (mode in SupportedModes) {
                    returnedSupportedModes[i++] = convertReturnedModeToInternalMode(mode)
                }
            } catch (e: Exception) {
                Log.e(TAG, e.message!!)
            }
            return returnedSupportedModes
        }//assuming the 1st display is the actual display.

    /**
     * Returns current [Display][android.view.Display] object.
     * Assumes that the 1st display is the actual display.
     *
     * @return [Display][android.view.Display]
     */
    private val currentDisplay: Display?
        private get() {
            if (mContext == null) return null
            val displays = mDisplayManager.displays
            if (displays == null || displays.size == 0) {
                Log.e(TAG, "ERROR on device to get the display")
                return null
            }
            //assuming the 1st display is the actual display.
            return displays[0]
        }

    /**
     * Change the display mode to the supplied mode.
     *
     * @param targetWindow [Window] to use for setting the display
     * and call parameters
     * @param modeId The desired mode to switch to. Must be a valid mode supported
     * by the platform.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    fun setPreferredDisplayModeId(targetWindow: Window?, modeId: Int) {
        /*
         * The Android M preview adds a preferredDisplayModeId to
         * WindowManager.LayoutParams.preferredDisplayModeId API. A PreferredDisplayModeId can be
         * set in the LayoutParams of any Window.
         */
        val deviceName = Build.MODEL
        var supportedDevice = true
        when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.LOLLIPOP, Build.VERSION_CODES.LOLLIPOP_MR1 -> if (!isAmazonFireTVDevice) {
                supportedDevice = false
            }
        }

        //Some basic failure conditions that need handling
        if (!supportedDevice) {
            Log.i(
                TAG,
                "Attempt to set preferred Display mode on an unsupported device: $deviceName"
            )
            mWorkHandler.sendMessage(
                mWorkHandler.obtainMessage(
                    SEND_CALLBACK_WITH_SUPPLIED_RESULT,
                    1,
                    1,
                    null
                )
            )
            return
        }
        if (mIsSetModeInProgress.get()) {
            Log.e(
                TAG, "setPreferredDisplayModeId is already in progress! " +
                        "Cannot set another while it is in progress"
            )
            //Send but don't cleanup as further processing is expected.
            mWorkHandler.sendMessage(
                mWorkHandler.obtainMessage(
                    SEND_CALLBACK_WITH_SUPPLIED_RESULT,
                    null
                )
            )
            return
        }
        val currentMode = mode
        if (currentMode == null || currentMode.modeId == modeId) {
            Log.i(TAG, "Current mode id same as mode id requested or is Null. Aborting.")
            //send and cleanup receivers/callback listeners
            mWorkHandler.sendMessage(
                mWorkHandler.obtainMessage(
                    SEND_CALLBACK_WITH_SUPPLIED_RESULT,
                    1,
                    1,
                    currentMode
                )
            )
            return
        }
        //Check if the modeId given is even supported by the system.
        val supportedModes = supportedModes
        var isRequestedModeSupported = false
        for (mode in supportedModes!!) {
            if (mode!!.modeId == modeId) {
                isRequestedModeSupported = true
                break
            }
        }
        if (!isRequestedModeSupported) {
            Log.e(TAG, "Requested mode id not among the supported Mode Id.")
            //send and cleanup receivers/callback listeners
            mWorkHandler.sendMessage(
                mWorkHandler.obtainMessage(
                    SEND_CALLBACK_WITH_SUPPLIED_RESULT,
                    1,
                    1,
                    null
                )
            )
            return
        }

        //We are now going to do setMode call and will do callback for it.
        mIsSetModeInProgress.set(true)
        //Let the handler know what modeId onDisplayChanged callback event to look for
        mWorkHandler.setExpectedMode(modeId)
        mContext!!.registerReceiver(
            overlayStateChangeReceiver, IntentFilter(
                MODESWITCH_OVERLAY_STATE_CHANGED
            )
        )
        mDisplayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}
            override fun onDisplayRemoved(displayId: Int) {}
            override fun onDisplayChanged(displayId: Int) {
                Log.i(
                    TAG, "onDisplayChanged. id= " + displayId + " " +
                            mDisplayManager.getDisplay(displayId).toString()
                )
                mWorkHandler.obtainMessage(MODE_CHANGED_MSG).sendToTarget()
            }
        }
        mDisplayManager.registerDisplayListener(mDisplayListener, mWorkHandler)
        isReceiversRegistered = true
        mTargetWindow = targetWindow

        //Also check if flag is available, otherwise fail and return
        val mWindowAttributes = mTargetWindow!!.attributes
        //Check if the field is available or not. This is for early failure.
        val cLayoutParams: Class<*> = mWindowAttributes.javaClass
        val attributeFlags: Field
        attributeFlags = try {
            cLayoutParams.getDeclaredField(sPreferredDisplayModeIdFieldName)
        } catch (e: Exception) {
            Log.e(TAG, e.localizedMessage)
            //send and cleanup receivers/callback listeners
            mWorkHandler.sendMessage(
                mWorkHandler.obtainMessage(
                    SEND_CALLBACK_WITH_SUPPLIED_RESULT,
                    1,
                    1,
                    null
                )
            )
            return
        }
        initModeChange(modeId, attributeFlags)
    }

    /**
     * Start the mode change by setting the preferredDisplayModeId field of [WindowManager.LayoutParams]
     */
    private fun initModeChange(modeId: Int, attributeFlagField: Field?) {
        var attributeFlagField = attributeFlagField
        val mWindowAttributes = mTargetWindow!!.attributes
        try {
            if (attributeFlagField == null) {
                val cLayoutParams: Class<*> = mWindowAttributes.javaClass
                attributeFlagField =
                    cLayoutParams.getDeclaredField(sPreferredDisplayModeIdFieldName)
            }
            //attempt mode switch
            attributeFlagField!!.setInt(mWindowAttributes, modeId)
            mTargetWindow!!.attributes = mWindowAttributes
        } catch (e: Exception) {
            Log.e(TAG, e.localizedMessage)
            //send and cleanup receivers/callback listeners
            mWorkHandler.sendMessage(
                mWorkHandler.obtainMessage(
                    SEND_CALLBACK_WITH_SUPPLIED_RESULT,
                    1,
                    1,
                    null
                )
            )
            return
        }
        //We assume that the mode change is not instantaneous and will send the onDisplayChanged callback.
        // Start the clock on the mode change timeout
        mWorkHandler.sendMessageDelayed(
            mWorkHandler.obtainMessage(MODE_CHANGE_TIMEOUT_MSG),
            SET_MODE_TIMEOUT_DELAY_MS.toLong()
        )
    }

    companion object {
        //private UhdHelperListener mListener;
        const val version = "v1.1"

        // public final static String MODESWITCH_OVERLAY_ENABLE = "com.amazon.tv.notification.modeswitch_overlay.action.ENABLE";
        const val MODESWITCH_OVERLAY_DISABLE =
            "com.amazon.tv.notification.modeswitch_overlay.action.DISABLE"
        const val MODESWITCH_OVERLAY_EXTRA_STATE =
            "com.amazon.tv.notification.modeswitch_overlay.extra.STATE"
        const val MODESWITCH_OVERLAY_STATE_CHANGED =
            "com.amazon.tv.notification.modeswitch_overlay.action.STATE_CHANGED"
        const val OVERLAY_STATE_DISMISSED = 0

        /**
         * Physical height of UHD in pixels ( {@value} )
         */
        const val HEIGHT_UHD = 2160

        /**
         * {@value} ms to wait for broadcast before declaring timeout.
         */
        const val SET_MODE_TIMEOUT_DELAY_MS = 15 * 1000

        /**
         * {@value} ms to wait for Interstitial broadcast before declaring timeout.
         */
        const val SHOW_INTERSTITIAL_TIMEOUT_DELAY_MS = 2 * 1000
        private val TAG = UhdHelper::class.java.simpleName
        private const val MODE_CHANGED_MSG = 1
        private const val MODE_CHANGE_TIMEOUT_MSG = 2
        private const val SEND_CALLBACK_WITH_SUPPLIED_RESULT = 3
        private const val INTERSTITIAL_FADED_BROADCAST_MSG = 4
        private const val INTERSTITIAL_TIMEOUT_MSG = 5
    }

    /**
     * Construct a UhdHelper object.
     * @param context Activity context.
     */
    init {
        mIsSetModeInProgress = AtomicBoolean(false)
        mWorkHandler = WorkHandler(Looper.getMainLooper())
        overlayStateChangeReceiver = OverlayStateChangeReceiver()
        mDisplayManager = mContext!!.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        isReceiversRegistered = false
    }
}