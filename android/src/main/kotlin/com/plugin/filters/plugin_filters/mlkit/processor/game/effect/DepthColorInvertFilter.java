package com.plugin.filters.plugin_filters.mlkit.processor.game.effect;

import android.opengl.GLES20;

import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorInvertFilter;

public class DepthColorInvertFilter extends GPUImageColorInvertFilter {

    public DepthColorInvertFilter() {
        super();
    }

    @Override
    public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        // @Todo: force to enable depth to draw 3d-mask
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glDepthMask(true);

        super.onDraw(textureId, cubeBuffer, textureBuffer);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_NEVER);
        GLES20.glDepthMask(false);
    }
}
