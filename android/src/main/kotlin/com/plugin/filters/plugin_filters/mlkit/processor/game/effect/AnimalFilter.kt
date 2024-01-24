package com.plugin.filters.plugin_filters.mlkit.processor.game.effect

import android.content.Context
import jp.co.cyberagent.android.gpuimage.ml.FaceSwapFilter
import jp.co.cyberagent.android.gpuimage.ml.FaceUtils
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import java.nio.FloatBuffer

class AnimalFilter(context: Context) :
    FaceSwapFilter(context, MaskMode.TATTOO_CANON) {
    override fun getVtxAlpha(): FloatBuffer {
//        "run to get animal alppha   ${maskMode.alphaMode} ".logd()
        return FaceUtils.animalVtxAlpha(meshSize)
    }
}