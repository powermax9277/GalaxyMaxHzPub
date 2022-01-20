package com.tribalfs.gmh.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import java.security.MessageDigest


object PackageInfo {

    fun getSignatureString(ctx: Context): String? {
        return try {
            val signatures =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ctx.packageManager.getPackageInfo(
                    ctx.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                @SuppressLint("PackageManagerGetSignatures")
                val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES)
                    @Suppress("DEPRECATION")
                if (packageInfo?.signatures.isNullOrEmpty() || packageInfo.signatures[0] == null
                ) {
                    null
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.signatures
                }
            }
            if (signatures != null) {
                val md = MessageDigest.getInstance("SHA")
                for (signature in signatures) {
                    md.update(signature.toByteArray())
                }
                Base64.encodeToString(md.digest(), Base64.DEFAULT).trim()
            }else{
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /*private fun getSignatures(pm: PackageManager, packageName: String): List<String?>? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo =
                    pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                if (packageInfo == null
                    || packageInfo.signingInfo == null
                ) {
                    return null
                }
                if (packageInfo.signingInfo.hasMultipleSigners()) {
                    signatureDigest(packageInfo.signingInfo.apkContentsSigners)
                } else {
                    signatureDigest(packageInfo.signingInfo.signingCertificateHistory)
                }
            } else {
                @SuppressLint("PackageManagerGetSignatures") val packageInfo =
                    pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                if (packageInfo == null || packageInfo.signatures == null || packageInfo.signatures.size == 0 || packageInfo.signatures[0] == null
                ) {
                    null
                } else signatureDigest(packageInfo.signatures)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }*/
}