package com.tribalfs.gmh.helpers

import android.content.Context
import android.net.Uri


object UtilsSettings {

    internal const val SYSTEM = "system"
    internal const val SECURE = "secure"
    internal const val GLOBAL = "global"


    fun getList(settings: String, appCtx: Context):List<String>{
        val list = mutableListOf<String>()
        val columns = arrayOf("_id", "name", "value")
        val cursor = appCtx.contentResolver.query(Uri.parse("content://settings/$settings"), columns, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                list.add("${it.getString(1)}=${it.getString(2)}")
            }
        }

        return list
    }

}