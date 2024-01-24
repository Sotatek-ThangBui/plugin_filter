package com.plugin.filters.plugin_filters.mlkit.processor.game.effect

import android.content.Context
import jp.co.cyberagent.android.gpuimage.ml.MaskMode

class DoctorStrangeFilter(context: Context) : FacePartFilter(context, MaskMode.MASK) {
    override fun eyeDraw(textureId: Int, faceIndex: Int) {
        drawLeftEyeForehead(textureId, faceIndex)
    }
}