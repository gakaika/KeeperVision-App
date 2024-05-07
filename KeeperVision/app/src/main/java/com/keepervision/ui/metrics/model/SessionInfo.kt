package com.keepervision.ui.metrics.model

import com.google.gson.annotations.SerializedName

data class SessionInfo(
    @SerializedName("id") val id: Int,
    @SerializedName("session_start") val startDateTimestamp: String,
    @SerializedName("session_end") val dateTimestamp: String,
    @SerializedName("initial_image") val startPicture: String,
    @SerializedName("final_image") val endPicture: String,
    @SerializedName("f") val frontCount: Int,
    @SerializedName("b") val backCount: Int,
    @SerializedName("l") val leftCount: Int,
    @SerializedName("r") val rightCount: Int,
    @SerializedName("fl") val frontLeftCount: Int,
    @SerializedName("fr") val frontRightCount: Int,
    @SerializedName("bl") val backLeftCount: Int,
    @SerializedName("br") val backRightCount: Int,
    @SerializedName("s") val stopCount: Int?) {

    }
