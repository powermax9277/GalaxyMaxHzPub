package com.tribalfs.gmh.profiles

internal object ModelNumbers {
    const val S20FE5G = "SM-G781"
    const val S20FE = "SM-G780"
    const val A725G = "SM-A726"
    const val A525G = "SM-A526"
    const val A52S5G = "SM-A528"
    const val A52_D = "SC-53B"
    const val S21FE = "SM-G990"
    const val M25G = "SM-M526"
    const val A53 = "SCG15"

    const val S20 = "SM-G980"
    const val S205G = "SM-G981"
    const val S20P_S = "SM-G986"
    const val S20P_E = "SM-G985"
    const val S20U = "SM-G988"

    const val N20U5G = "SM-N986"
    const val N20U = "SM-N985"
    const val N20U_JP = "SCG06"
    const val N20U5G_D = "SC-53A"

    const val S21 = "SM-G991"
    const val S21_JP = "SCG09"
    const val S21_O = "SC-51B"
    const val S21_P = "SM-G996"
    const val S21_P_JP = "SCG10"
    const val S21_U = "SM-G998"


    const val S22 = "SM-S901"
    const val S22_JP = "SCG13"
    const val S22_D = "SC-51C"
    const val S22P = "SM-S906"
    const val S22U = "SM-S908"
    const val S22U_JP = "SCG14"
    const val S22U_D = "SC-52C"

    const val ZF2 = "SM-F916"
    const val ZF2_JP = "SCG05"
    const val ZF2_TW = "SM-W202"

    const val TS7W = "SM-T870"
    const val TS7L = "SM-T875"
    const val TS7LW = "SM-T876"
    const val TS75G = "SM-T878"

    const val TS7PL = "SM-T975"
    const val TS7P = "SM-T976"
    const val TS7PW = "SM-T970"
    const val TS7PL_TM = "SM-T978"

    const val ZF3 = "SM-F926"
    const val ZF3_JP = "SCG11"

    const val ZFp3 = "SM-F711"
    const val ZFp3_JP = "SCG12"

    const val TS8 = "SM-X706"
    const val TS8P = "SM-X800"
    const val TS8U = "SM-X900"

    val fordableWithHrrExternal = listOf(ZF3,ZF3_JP)

    val adaptiveModelsLocal: List<String> = listOf(
        N20U,
        N20U5G,
        N20U_JP,
        N20U5G_D,
        ZF2,
        ZF2_JP,
        ZF2_TW,
        S21,
        S21_JP,
        S21_O,
        S21_P,
        S21_P_JP,
        S21_U,
        ZF3,
        ZF3_JP,
        TS7L,
        TS7LW,
        TS75G,
        ZFp3,
        ZFp3_JP,
        S22,
        S22P,
        S22_JP,
        S22_D,
        S22U,
        S22U_JP,
        S22U_D,
        TS8
    )
}
