package com.tribalfs.gmh.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.tribalfs.gmh.helpers.PackageInfo
import com.tribalfs.gmh.sharedprefs.UtilsPrefsAct
import com.tribalfs.gmh.sharedprefs.UtilsPrefsAct.Companion.LIC_TYPE_TRIAL_ACTIVE


class MyViewModel(val app: Application) : AndroidViewModel(app) {

    val hideBuyActMenu = MutableLiveData<Int>()
    private val isAdFree = MutableLiveData<Boolean>()
    private val currentSig = PackageInfo.getSignatureString(app.applicationContext)!!
    private val isValidSig = MutableLiveData<Boolean>()

    fun setServerSign(serverSign: String){
        isValidSig.value = serverSign == currentSig
    }

    fun setAdFreeCode(adFreeCode: Int){
        isAdFree.value = (adFreeCode == UtilsPrefsAct.LIC_TYPE_ADFREE || adFreeCode == LIC_TYPE_TRIAL_ACTIVE)
        hideBuyActMenu.value = adFreeCode/0x2
    }

    val isValidAdFree = MediatorLiveData<Boolean>().apply {
        addSource(isAdFree) { value = (it == true && isValidSig.value == true) }
        addSource(isValidSig) { value = (it == true && isAdFree.value == true) }
    }.distinctUntilChanged()

}