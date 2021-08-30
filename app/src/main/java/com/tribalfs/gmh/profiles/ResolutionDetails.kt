package com.tribalfs.gmh.profiles

data class ResolutionDetails(
    val resHeight: Int,
    val resWidth: Int,
    val resStrLxw: String,
    val resName: String,
    val refreshRates: List<Float>,
    val lowestHz: Float,
    val highestHz: Float
)

data class ResolutionBasic(
    val resHeight: Int,
    val resWidth: Int
)
