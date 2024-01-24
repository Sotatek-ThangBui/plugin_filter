package com.plugin.filters.plugin_filters.enums

import com.google.gson.annotations.SerializedName

enum class FilterType(val previewFileName: String) {
    @SerializedName("DOCTOR_STRANGE")
    DOCTOR_STRANGE("doctor_strange.png"),
    @SerializedName("KOKUSHIBO")
    KOKUSHIBO("kokushibo.png"),
    @SerializedName("ONE_EYE")
    ONE_EYE("duong_tien.png"),
    @SerializedName("REVERT")
    REVERT("revert_face.png"),
    @SerializedName("ANIMAL")
    ANIMAL(""),
    @SerializedName("DOUBLE_FILTER")
    DOUBLE_FILTER("mirror.png"),
    @SerializedName("MIRROR")
    MIRROR("mirror.png"),
    @SerializedName("MASK")
    MASK(""),
    @SerializedName("TATTOO")
    TATTOO(""),
    @SerializedName("EYE_MANGA")
    EYE_MANGA(""),
    @SerializedName("CELEBRITY")
    CELEBRITY("CELEBRITY"),
    @SerializedName("EXPLORE")
    EXPLORE(""),
    @SerializedName("BEARD")
    BEARD("");

    fun isHumanFilter() = this == MASK

    val isCustomFilter get() = (this == DOCTOR_STRANGE || this == KOKUSHIBO || this == ONE_EYE || this == REVERT || this == DOUBLE_FILTER)
}