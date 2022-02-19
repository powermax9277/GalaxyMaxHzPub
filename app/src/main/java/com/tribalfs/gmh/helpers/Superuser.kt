package com.tribalfs.gmh.helpers

import com.tribalfs.gmh.BuildConfig
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

object Superuser {
    private fun executeCommandSU(command: String?): Boolean {
        val stdout: List<String> = ArrayList()
        val stderr: List<String> = ArrayList()
        try {
            Shell.Pool.SU.run(command!!, stdout, stderr, true)
        } catch (_: Shell.ShellDiedException) {
            //e.printStackTrace()
            return false
        }
        if (stdout.isEmpty()) return false
        val stringBuilder = StringBuilder()
        for (line in stdout) {
            stringBuilder.append(line).append("\n")
        }
        return true
    }

    internal suspend fun grantSecPerm(): Boolean = withContext(Dispatchers.IO) {
        try {
            return@withContext executeCommandSU("pm grant ${BuildConfig.APPLICATION_ID} android.permission.WRITE_SECURE_SETTINGS")
        }catch(_: Exception){
            return@withContext false
        }
    }
}