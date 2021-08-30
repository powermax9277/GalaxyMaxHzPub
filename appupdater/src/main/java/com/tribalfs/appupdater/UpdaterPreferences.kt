package com.tribalfs.appupdater

import android.content.Context
import android.content.SharedPreferences

internal class UpdaterPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("app_updater_pref", Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    fun checkAllowUpdater(): Boolean {
        val checkCount = sharedPreferences.getInt(UPDATER_CHECK_COUNT, CHECK_EVERY_COUNT)
        return if (checkCount >= CHECK_EVERY_COUNT) {
            editor.putInt(UPDATER_CHECK_COUNT,0).apply()
            true
        } else {
            editor.putInt(UPDATER_CHECK_COUNT,checkCount + 1).apply()
            false
        }
    }

    companion object {
        private const val UPDATER_CHECK_COUNT = "key_updater_check_count"
        private const val CHECK_EVERY_COUNT = 10
    }

}