package com.plugin.filters.plugin_filters.mlkit.processor.game.effect

import android.content.Context
import jp.co.cyberagent.android.gpuimage.ml.FaceShader
import jp.co.cyberagent.android.gpuimage.ml.FaceSwapFilter
import jp.co.cyberagent.android.gpuimage.ml.FaceUtils
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import java.nio.FloatBuffer

class EffectFilter: FaceSwapFilter {

    constructor(context: Context, mode: MaskMode): super(context, mode) {
        this.vertexShader =
            FaceShader.getShaderFromAssets(context, "shader/vertex_mask.glsl")
        //FaceShader.VERTEX_SHADER
        this.fragmentShader =
            FaceShader.getShaderFromAssets(context, "shader/fragment_mask.glsl")

    }



    override fun getVtxAlpha(): FloatBuffer {
//        "run to get animal alppha   ${maskMode.alphaMode} ".logd()
        return FaceUtils.getVtxAlpha(
            7f,
            meshSize,
            0f,
            0f,
            0f,
            0f
        )
    }
}