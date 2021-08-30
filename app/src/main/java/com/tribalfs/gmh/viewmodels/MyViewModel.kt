package com.tribalfs.gmh.viewmodels

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.tribalfs.gmh.helpers.Certificate
import com.tribalfs.gmh.sharedprefs.UtilsPrefsAct

@RequiresApi(Build.VERSION_CODES.M)
class MyViewModel(val app: Application) : AndroidViewModel(app) {

    val hideBuyActMenu = MutableLiveData<Int>()
    private val isAdFree = MutableLiveData<Boolean>()
    private val currentSig = Certificate.getEncSig(app.applicationContext)!!
    private val isValidSig = MutableLiveData<Boolean>()

    fun setServerSign(serverSign: String){
        isValidSig.value = serverSign == currentSig
    }

    fun setAdFreeCode(adFreeCode: Int){
        isAdFree.value = (adFreeCode == UtilsPrefsAct.LIC_TYPE_ADFREE || adFreeCode == UtilsPrefsAct.LIC_TYPE_TRIAL_ACTIVE)
        hideBuyActMenu.value = adFreeCode/0x2
    }

    val isValidAdFree = MediatorLiveData<Boolean>().apply {
        addSource(isAdFree) { value = (it == true && isValidSig.value == true) }
        addSource(isValidSig) { value = (it == true && isAdFree.value == true) }
    }.distinctUntilChanged()

}