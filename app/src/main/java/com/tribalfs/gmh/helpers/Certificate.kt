package com.tribalfs.gmh.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import java.security.MessageDigest

object Certificate {
    private var encSig: String? = null
    @SuppressLint("PackageManagerGetSignatures", "NewApi")
    fun getEncSig(ctx: Context): String? {
        return try {
            if (encSig == null){
                val signatures = ctx.packageManager.getPackageInfo(
                    ctx.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo.apkContentsSigners

                val md = MessageDigest.getInstance("SHA")
                for (signature in signatures) {
                    md.update(signature.toByteArray())
                }
                encSig = Base64.encodeToString(md.digest(), Base64.DEFAULT).trim()
            }
            return encSig
        } catch (_: Exception) {
            null
        }
    }
}