package com.plugin.filters.plugin_filters.mlkit.processor.game.effect

import android.content.Context
import jp.co.cyberagent.android.gpuimage.Constants
import jp.co.cyberagent.android.gpuimage.ml.FaceSwapFilter
import jp.co.cyberagent.android.gpuimage.ml.MaskMode

class ExploreFilter(context: Context) : FaceSwapFilter(context, MaskMode.TATTOO_CANON) {
    override fun setMode(mode: MaskMode) {
        this.maskMode = mode
        faceTriangIndex = Constants.face_eye_mouth_tris
    }
}