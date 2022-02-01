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
    const val N20U_JP = "SCG06"

    const val S21 = "SM-G991"
    const val S21_JP = "SCG09"
    const val S21_P = "SM-G996"
    const val S21_P_JP = "SCG10"
    const val S21_U = "SM-G998"

    private const val S22 = "SM-S901"
    private const val S22P = "SM-S906"
    private const val S22U = "SM-S908"

    const val ZF2 = "SM-F916"
    const val ZF2_JP = "SCG05"
    const val ZF2_TW = "SM-W202"

    const val TS7W = "SM-T870"
    const val TS7L = "SM-T875"
    const val TS7LW = "SM-T876"

    const val TS7PL = "SM-T975"
    const val TS7P = "SM-T976"
    const val TS7PW = "SM-T970"
    const val TS7PL_TM = "SM-T978"

    const val ZF3 = "SM-F926"
    const val ZF3_JP = "SCG11"

    const val ZFp3 = "SM-F711"
    const val ZFp3_JP = "SCG12"

    val fordableWithHrrExternal = listOf(ZF3,ZF3_JP)

    val adaptiveModelsLocal: List<String> = listOf(
        N20U,
        N20U5G,
        N20U_JP,
        ZF2,
        ZF2_JP,
        ZF2_TW,
        S21,
        S21_JP,
        S21_P,
        S21_P_JP,
        S21_U,
        ZF3,
        ZF3_JP,
        TS7L,
        TS7LW,
        ZFp3,
        ZFp3_JP,
        S22,
        S22P,
        S22U
    )

    val withAllResoAtHigh: List<String> = listOf(
        S21,
        S21_JP,
        S21_P,
        S21_P_JP,
        S21_U,
        S22,
        S22P,
        S22U,
        ZFp3,
        ZFp3_JP,
        N20U5G
    )
}
