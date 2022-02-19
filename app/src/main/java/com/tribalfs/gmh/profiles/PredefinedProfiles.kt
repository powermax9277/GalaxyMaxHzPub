package com.tribalfs.gmh.profiles

import android.content.Context
import androidx.annotation.Keep
import com.tribalfs.gmh.R
import com.tribalfs.gmh.profiles.ModelNumbers.A525G
import com.tribalfs.gmh.profiles.ModelNumbers.A52S5G
import com.tribalfs.gmh.profiles.ModelNumbers.A725G
import com.tribalfs.gmh.profiles.ModelNumbers.M25G
import com.tribalfs.gmh.profiles.ModelNumbers.N20U
import com.tribalfs.gmh.profiles.ModelNumbers.N20U5G
import com.tribalfs.gmh.profiles.ModelNumbers.N20U_JP
import com.tribalfs.gmh.profiles.ModelNumbers.S20
import com.tribalfs.gmh.profiles.ModelNumbers.S205G
import com.tribalfs.gmh.profiles.ModelNumbers.S20FE
import com.tribalfs.gmh.profiles.ModelNumbers.S20FE5G
import com.tribalfs.gmh.profiles.ModelNumbers.S20P_E
import com.tribalfs.gmh.profiles.ModelNumbers.S20P_S
import com.tribalfs.gmh.profiles.ModelNumbers.S20U
import com.tribalfs.gmh.profiles.ModelNumbers.S21
import com.tribalfs.gmh.profiles.ModelNumbers.S21FE
import com.tribalfs.gmh.profiles.ModelNumbers.S21_JP
import com.tribalfs.gmh.profiles.ModelNumbers.S21_P
import com.tribalfs.gmh.profiles.ModelNumbers.S21_P_JP
import com.tribalfs.gmh.profiles.ModelNumbers.S21_U
import com.tribalfs.gmh.profiles.ModelNumbers.S22
import com.tribalfs.gmh.profiles.ModelNumbers.S22P
import com.tribalfs.gmh.profiles.ModelNumbers.S22U
import com.tribalfs.gmh.profiles.ModelNumbers.TS7L
import com.tribalfs.gmh.profiles.ModelNumbers.TS7LW
import com.tribalfs.gmh.profiles.ModelNumbers.TS7P
import com.tribalfs.gmh.profiles.ModelNumbers.TS7PL
import com.tribalfs.gmh.profiles.ModelNumbers.TS7PL_TM
import com.tribalfs.gmh.profiles.ModelNumbers.TS7PW
import com.tribalfs.gmh.profiles.ModelNumbers.TS7W
import com.tribalfs.gmh.profiles.ModelNumbers.ZF2
import com.tribalfs.gmh.profiles.ModelNumbers.ZF2_JP
import com.tribalfs.gmh.profiles.ModelNumbers.ZF2_TW
import com.tribalfs.gmh.profiles.ModelNumbers.ZF3
import com.tribalfs.gmh.profiles.ModelNumbers.ZF3_JP
import com.tribalfs.gmh.profiles.ModelNumbers.ZFp3
import com.tribalfs.gmh.profiles.ModelNumbers.ZFp3_JP
import org.json.JSONObject
import java.io.InputStream

@Keep
internal object PredefinedProfiles {
    fun get(context: Context, sevenCharModel: String): JSONObject? {
        val raw: InputStream
        when (sevenCharModel){
            S22, S22P  -> {
                raw = context.resources.openRawResource(R.raw.x2340_1080_10_24_30_48_60_96_120)
            }

            S22U  -> {
                raw = context.resources.openRawResource(R.raw.x3088_1440_10_24_30_48_60_96_120)
            }

            TS7PW, TS7PL, TS7P,TS7PL_TM -> {
                raw = context.resources.openRawResource(R.raw.x2800_1752_60_120)
            }

            TS7W, TS7L, TS7LW -> {
                raw = context.resources.openRawResource(R.raw.x2560_1600_48_60_96_120)
            }


            S20FE5G, S20FE, A725G, A525G, A52S5G, S21FE, M25G -> {
                raw = context.resources.openRawResource(R.raw.x2400_1080_60_120)
            }

            S20, S205G, S20P_S, S20P_E, S20U -> {
                raw = context.resources.openRawResource(R.raw.x3200_1440_60_96_120)
            }

            S21, S21_P, S21_JP, S21_P_JP -> {
                raw = context.resources.openRawResource(R.raw.x2400_1080_48_60_96_120)
            }

            S21_U -> {
                raw = context.resources.openRawResource(R.raw.x3200_1440_48_60_96_120)
            }

            ZFp3, ZFp3_JP -> {
                raw = context.resources.openRawResource(R.raw.x2640_1080_48_60_96_120)
            }

            N20U, N20U5G, N20U_JP -> {
                raw = context.resources.openRawResource(R.raw.x3088_1440_48_60_96_120)
            }

            ZF2, ZF2_JP, ZF3, ZF3_JP, ZF2_TW -> {
                raw = context.resources.openRawResource(R.raw.x2208_1768_48_60_96_120)
            }

            else -> return null
        }

        return JSONObject(raw.bufferedReader().use { it.readText() })
    }
}