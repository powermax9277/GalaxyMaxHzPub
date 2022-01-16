package com.tribalfs.gmh.profiles

import androidx.annotation.Keep
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
import com.tribalfs.gmh.profiles.ModelNumbers.S21_JP
import com.tribalfs.gmh.profiles.ModelNumbers.S21_P
import com.tribalfs.gmh.profiles.ModelNumbers.S21_P_JP
import com.tribalfs.gmh.profiles.ModelNumbers.S21_U
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

@Keep
internal object PredefinedProfiles {
    fun get(sevenCharModel: String): JSONObject? {
        when (sevenCharModel){
            TS7PW, TS7PL, TS7P,TS7PL_TM -> {
                return JSONObject(
                    "{\"0-2\":{\"2800x1752\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[120.0,60],\\\"resHeight\\\":2800,\\\"resName\\\":\\\"WQXGA+\\\",\\\"resStrLxw\\\":\\\"2800x1752\\\",\\\"resWidth\\\":1752}\"}," +
                            "\"0-1\":{\"2800x1752\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[120.0,60.0],\\\"resHeight\\\":2800,\\\"resName\\\":\\\"WQXGA+\\\",\\\"resStrLxw\\\":\\\"2800x1752\\\",\\\"resWidth\\\":1752}\"}," +
                            "\"0-0\":{\"2800x1752\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[60.0],\\\"resHeight\\\":2800,\\\"resName\\\":\\\"WQXGA+\\\",\\\"resStrLxw\\\":\\\"2800x1752\\\",\\\"resWidth\\\":1752}\"}}"
                )
            }
            TS7W, TS7L, TS7LW -> {
                return JSONObject(
                    "{\"0-2\":{\"2560x1600\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":2560,\\\"resName\\\":\\\"WQXGA+\\\",\\\"resStrLxw\\\":\\\"2560x1600\\\",\\\"resWidth\\\":1600}\"}," +
                            "\"0-1\":{\"2560x1600\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":2560,\\\"resName\\\":\\\"WQXGA+\\\",\\\"resStrLxw\\\":\\\"2560x1600\\\",\\\"resWidth\\\":1600}\"}," +
                            "\"0-0\":{\"2560x1600\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[60.0,48.0],\\\"resHeight\\\":2560,\\\"resName\\\":\\\"WQXGA+\\\",\\\"resStrLxw\\\":\\\"2560x1600\\\",\\\"resWidth\\\":1600}\"}}"
                )
            }
            S20FE5G, S20FE -> {
                return JSONObject(
                    "{\"0-2\":{\"2400x1080\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[120.0,60.0],\\\"resHeight\\\":2400,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2400x1080\\\",\\\"resWidth\\\":1080}\"}," +
                            "\"0-1\":{\"2400x1080\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[120.0,60.0],\\\"resHeight\\\":2400,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2400x1080\\\",\\\"resWidth\\\":1080}\"}," +
                            "\"0-0\":{\"2400x1080\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[60.0],\\\"resHeight\\\":2400,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2400x1080\\\",\\\"resWidth\\\":1080}\"}}")
            }
            S20, S205G, S20P_S, S20P_E, S20U -> {
                return JSONObject(
                    "{\"0-2\":{\"3200x1440\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[60.0],\\\"resHeight\\\":3200,\\\"resName\\\":\\\"WQHD+\\\",\\\"resStrLxw\\\":\\\"3200x1440\\\",\\\"resWidth\\\":1440}\",\"3180x1431\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[120.0,96.0,60.0],\\\"resHeight\\\":3180,\\\"resName\\\":\\\"CQHD+\\\",\\\"resStrLxw\\\":\\\"3180x1431\\\",\\\"resWidth\\\":1431}\",\"2400x1080\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[120.0,96.0,60.0],\\\"resHeight\\\":2400,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2400x1080\\\",\\\"resWidth\\\":1080}\",\"1600x720\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[120.0,96.0,60.0],\\\"resHeight\\\":1600,\\\"resName\\\":\\\"HD+\\\",\\\"resStrLxw\\\":\\\"1600x720\\\",\\\"resWidth\\\":720}\"}," +
                            "\"0-1\":{\"3200x1440\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[60.0],\\\"resHeight\\\":3200,\\\"resName\\\":\\\"WQHD+\\\",\\\"resStrLxw\\\":\\\"3200x1440\\\",\\\"resWidth\\\":1440}\",\"3180x1431\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[120.0,96.0,60.0],\\\"resHeight\\\":3180,\\\"resName\\\":\\\"CQHD+\\\",\\\"resStrLxw\\\":\\\"3180x1431\\\",\\\"resWidth\\\":1431}\",\"2400x1080\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[120.0,96.0,60.0],\\\"resHeight\\\":2400,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2400x1080\\\",\\\"resWidth\\\":1080}\",\"1600x720\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[120.0,96.0,60.0],\\\"resHeight\\\":1600,\\\"resName\\\":\\\"HD+\\\",\\\"resStrLxw\\\":\\\"1600x720\\\",\\\"resWidth\\\":720}\"}," +
                            "\"0-0\":{\"3200x1440\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[60.0],\\\"resHeight\\\":3200,\\\"resName\\\":\\\"WQHD+\\\",\\\"resStrLxw\\\":\\\"3200x1440\\\",\\\"resWidth\\\":1440}\",\"3180x1431\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[60.0],\\\"resHeight\\\":3180,\\\"resName\\\":\\\"CQHD+\\\",\\\"resStrLxw\\\":\\\"3180x1431\\\",\\\"resWidth\\\":1431}\",\"2400x1080\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[60.0],\\\"resHeight\\\":2400,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2400x1080\\\",\\\"resWidth\\\":1080}\",\"1600x720\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":60.0,\\\"refreshRates\\\":[60.0],\\\"resHeight\\\":1600,\\\"resName\\\":\\\"HD+\\\",\\\"resStrLxw\\\":\\\"1600x720\\\",\\\"resWidth\\\":720}\"}}"
                )
            }
            S21, S21_P, S21_JP, S21_P_JP -> {
                return JSONObject(
                    "{\"0-2\":{\"2400x1080\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":2400,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2400x1080\\\",\\\"resWidth\\\":1080}\"}," +
                            "\"0-1\":{\"2400x1080\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":2400,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2400x1080\\\",\\\"resWidth\\\":1080}\"}," +
                            "\"0-0\":{\"2400x1080\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[60.0,48.0],\\\"resHeight\\\":2400,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2400x1080\\\",\\\"resWidth\\\":1080}\"}}"
                )
            }
            ZFp3, ZFp3_JP -> {
                return JSONObject(
                    "{\"0-2\":{\"2640x1080\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":2640,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2640x1080\\\",\\\"resWidth\\\":1080}\",\"1760x720\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":1760,\\\"resName\\\":\\\"HD+\\\",\\\"resStrLxw\\\":\\\"1760x720\\\",\\\"resWidth\\\":720}\"}," +
                            "\"0-1\":{\"2640x1080\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":2640,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2640x1080\\\",\\\"resWidth\\\":1080}\",\"1760x720\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":1760,\\\"resName\\\":\\\"HD+\\\",\\\"resStrLxw\\\":\\\"1760x720\\\",\\\"resWidth\\\":720}\"}," +
                            "\"0-0\":{\"2640x1080\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[60.0,48.0],\\\"resHeight\\\":2640,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2640x1080\\\",\\\"resWidth\\\":1080}\",\"1760x720\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[60.0,48.0],\\\"resHeight\\\":1760,\\\"resName\\\":\\\"HD+\\\",\\\"resStrLxw\\\":\\\"1760x720\\\",\\\"resWidth\\\":720}\"}}"
                )
            }
            S21_U -> {
                return JSONObject(
                    "{\"0-2\":{\"3200x1440\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":3200,\\\"resName\\\":\\\"WQHD+\\\",\\\"resStrLxw\\\":\\\"3200x1440\\\",\\\"resWidth\\\":1440}\",\"2400x1080\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":2400,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2400x1080\\\",\\\"resWidth\\\":1080}\",\"1600x720\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":1600,\\\"resName\\\":\\\"HD+\\\",\\\"resStrLxw\\\":\\\"1600x720\\\",\\\"resWidth\\\":720}\"}," +
                            "\"0-1\":{\"3200x1440\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":3200,\\\"resName\\\":\\\"WQHD+\\\",\\\"resStrLxw\\\":\\\"3200x1440\\\",\\\"resWidth\\\":1440}\",\"2400x1080\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":2400,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2400x1080\\\",\\\"resWidth\\\":1080}\",\"1600x720\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":1600,\\\"resName\\\":\\\"HD+\\\",\\\"resStrLxw\\\":\\\"1600x720\\\",\\\"resWidth\\\":720}\"}," +
                            "\"0-0\":{\"3200x1440\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[60.0,48.0],\\\"resHeight\\\":3200,\\\"resName\\\":\\\"WQHD+\\\",\\\"resStrLxw\\\":\\\"3200x1440\\\",\\\"resWidth\\\":1440}\",\"2400x1080\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[60.0,48.0],\\\"resHeight\\\":2400,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2400x1080\\\",\\\"resWidth\\\":1080}\",\"1600x720\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[60.0,48.0],\\\"resHeight\\\":1600,\\\"resName\\\":\\\"HD+\\\",\\\"resStrLxw\\\":\\\"1600x720\\\",\\\"resWidth\\\":720}\"}}"
                )
            }

            N20U, N20U5G, N20U_JP -> {
                return JSONObject(
                    "{ \"0-2\": {\"3088x1440\": \"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[60.0,48.0],\\\"resHeight\\\":3088,\\\"resName\\\":\\\"WQHD+\\\",\\\"resStrLxw\\\":\\\"3088x1440\\\",\\\"resWidth\\\":1440}\",\"3087x1439\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":3087,\\\"resName\\\":\\\"CQHD+\\\",\\\"resStrLxw\\\":\\\"3087x1439\\\",\\\"resWidth\\\":1439}\",\"2316x1080\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":2316,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2316x1080\\\",\\\"resWidth\\\":1080}\",\"1544x720\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":1544,\\\"resName\\\":\\\"HD+\\\",\\\"resStrLxw\\\":\\\"1544x720\\\",\\\"resWidth\\\":720}\"}," +
                            "\"0-1\": {\"3088x1440\": \"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[60.0,48.0],\\\"resHeight\\\":3088,\\\"resName\\\":\\\"WQHD+\\\",\\\"resStrLxw\\\":\\\"3088x1440\\\",\\\"resWidth\\\":1440}\",\"3087x1439\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":3087,\\\"resName\\\":\\\"CQHD+\\\",\\\"resStrLxw\\\":\\\"3087x1439\\\",\\\"resWidth\\\":1439}\",\"2316x1080\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":2316,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2316x1080\\\",\\\"resWidth\\\":1080}\",\"1544x720\": \"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":1544,\\\"resName\\\":\\\"HD+\\\",\\\"resStrLxw\\\":\\\"1544x720\\\",\\\"resWidth\\\":720}\"}," +
                            "\"0-0\": {\"3088x1440\": \"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[60.0,48.0],\\\"resHeight\\\":3088,\\\"resName\\\":\\\"WQHD+\\\",\\\"resStrLxw\\\":\\\"3088x1440\\\",\\\"resWidth\\\":1440}\",\"3087x1439\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[60.0,48.0],\\\"resHeight\\\":3087,\\\"resName\\\":\\\"CQHD+\\\",\\\"resStrLxw\\\":\\\"3087x1439\\\",\\\"resWidth\\\":1439}\",\"2316x1080\": \"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[60.0,48.0],\\\"resHeight\\\":2316,\\\"resName\\\":\\\"FHD+\\\",\\\"resStrLxw\\\":\\\"2316x1080\\\",\\\"resWidth\\\":1080}\",\"1544x720\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[60.0,48.0],\\\"resHeight\\\":1544,\\\"resName\\\":\\\"HD+\\\",\\\"resStrLxw\\\":\\\"1544x720\\\",\\\"resWidth\\\":720}\"}}"
                )
            }

            ZF2, ZF2_JP, ZF3, ZF3_JP, ZF2_TW -> {
                return JSONObject(
                    "{\"0-2\":{\"2208x1768\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":2208,\\\"resName\\\":\\\"WQXGA+\\\",\\\"resStrLxw\\\":\\\"2208x1768\\\",\\\"resWidth\\\":1768}\"}," +
                            "\"0-1\":{\"2208x1768\":\"{\\\"highestHz\\\":120.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[120.0,96.0,60.0,48.0],\\\"resHeight\\\":2208,\\\"resName\\\":\\\"WQXGA+\\\",\\\"resStrLxw\\\":\\\"2208x1768\\\",\\\"resWidth\\\":1768}\"}," +
                            "\"0-0\":{\"2208x1768\":\"{\\\"highestHz\\\":60.0,\\\"lowestHz\\\":48.0,\\\"refreshRates\\\":[60.0,48.0],\\\"resHeight\\\":2208,\\\"resName\\\":\\\"WQXGA+\\\",\\\"resStrLxw\\\":\\\"2208x1768\\\",\\\"resWidth\\\":1768}\"}}"
                )
            }
            else -> return null
        }
    }
}