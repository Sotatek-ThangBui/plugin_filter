package com.plugin.filters.plugin_filters.mlkit.processor.game.effect

import android.content.Context
import jp.co.cyberagent.android.gpuimage.Constants
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class BeardMangaFilter(context: Context) : TattooFilter(context) {
    override fun getVtxAlpha(): FloatBuffer {
        val data = FloatArray(meshSize) { skinPercent }
        val result = ByteBuffer
            .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        result.put(data)
        return result
    }
}