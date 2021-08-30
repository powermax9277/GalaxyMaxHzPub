package com.tribalfs.gmh.helpers

import kotlin.math.abs

object UtilsCommon {
    internal fun List<Int>.closestValue(value: Int) = minByOrNull { abs(value - it) }
    /*internal fun String.capitalizeWords(): String =  split(" ").asSequence().map {map -> map.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(
            Locale.ROOT
        ) else it.toString()
    } }.joinToString(" ")*/
}