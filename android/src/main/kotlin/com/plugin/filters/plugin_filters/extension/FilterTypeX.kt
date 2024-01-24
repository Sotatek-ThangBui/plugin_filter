package com.plugin.filters.plugin_filters.extension

import android.content.Context
import com.plugin.filters.plugin_filters.enums.FilterType
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.AnimalFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.ml.FaceSwapFilter
import jp.co.cyberagent.android.gpuimage.ml.MaskMode

fun FilterType.getFilter(context: Context): GPUImageFilter {
    return when (this) {
        FilterType.ANIMAL -> AnimalFilter(context)
        else -> FaceSwapFilter(context, MaskMode.MASK)
    }
}