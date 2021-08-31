package com.tribalfs.gmh.profiles

internal object ModelNumbers {
    const val S20FE5G = "SM-G781"
    const val S20FE = "SM-G780"
    const val S20 = "SM-G980"
    const val S205G = "SM-G981"
    const val S20P_S = "SM-G986"
    const val S20P_E = "SM-G985"
    const val S20U = "SM-G988"
    const val N20U5G = "SM-N986"
    const val N20U = "SM-N985"
    const val S21 = "SM-G991"
    const val S21_P = "SM-G996"
    const val S21_U = "SM-G998"
    const val ZF2 = "SM-F916"
    const val ZF2_JP = "SCG05"
    const val TS7W = "SM-T870"
    const val TS7L = "SM-T875"
    const val TS7LW = "SM-T876"
    const val TS7PL = "SM-T975"
    const val TS7P = "SM-T976"
    const val TS7PW = "SM-T970"
    const val N20U_JP = "SCG06"
    const val ZF3 = "SM-F926"
    const val ZFp3 = "SM-F711"

    val fordableWithHrrExternal = listOf(ZF3)

    val adaptiveModelsLocal: List<String> = listOf(
        N20U,
        ZF2,
        ZF2_JP,
        N20U5G,
        S21,
        S21_P,
        S21_U,
        N20U_JP,
        ZF3,
        ZFp3,
        TS7L,
        TS7LW
    )
}
