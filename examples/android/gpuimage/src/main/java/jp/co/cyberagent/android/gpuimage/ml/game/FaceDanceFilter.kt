package jp.co.cyberagent.android.gpuimage.ml.game

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.util.Log
import jp.co.cyberagent.android.gpuimage.ml.FaceSwapFilter
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils
import java.nio.FloatBuffer

class FaceDanceFilter(context: Context, mode: MaskMode):
    FaceSwapFilter(context, mode) {
    private var assetTexture: java.util.HashMap<String, Int>? = null



    fun initAssets(assets: java.util.HashMap<String, Bitmap?>) {
        assetTexture = java.util.HashMap<String, Int>()
        for (entry in assets.entries.iterator()) {
            setBitmap(entry.key, entry.value)
        }
    }

    fun setBitmap(key:String, bitmap: Bitmap?) {
        if (bitmap != null && bitmap.isRecycled) {
            return
        }
        if (bitmap == null) {
            return
        }
        runOnDraw(Runnable {
            if ( !assetTexture!!.containsKey(key) ||
                (assetTexture!![key] == OpenGlUtils.NO_TEXTURE)
            ) {
                if (bitmap == null || bitmap.isRecycled) {
                    return@Runnable
                }
                GLES20.glActiveTexture(GLES20.GL_TEXTURE3)
                assetTexture!![key] =
                    OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, false)

                Log.e("FaceDance", "init asset ${key} ${assetTexture!![key]}")
            }
        })
    }

    override fun onDraw(
        textureId: Int, cubeBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ) {
        super.onDraw(textureId, cubeBuffer, textureBuffer)

        // Draw game logic

        // 1. prepare phase
        drawPrepare(textureId, cubeBuffer, textureBuffer)

        // 2. play phase
//        drawPlay(textureId, cubeBuffer, textureBuffer)

        // 3. finish phase
        // Finish and signal camera-activity to done-activity
        // Done-activity:
        // - Auto replay
        // - Share + Tag to challenge friends
        // - Play again
//        drawFinish(textureId, cubeBuffer, textureBuffer)
    }

    private fun drawPrepare(
        textureId: Int, cubeBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ) {
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        val oW = outputWidth.toFloat()
        val oH = outputHeight.toFloat()

        val positionVertex = floatArrayOf(
            0.3f, 0.8f,
            0.3f, 0.9f,
            0.7f, 0.8f,
            0.7f, 0.9f
        )
        setBaseVertex(positionVertex, 2)

        // 2.2. Mask-vertex as texture-vertex

        val frameVertex = floatArrayOf(
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
        )
        setOverlayVertex(frameVertex, 2)

        setDrawMode(-1.0f)

        uniformScale()

        // 3. Activate image-texture
        val textureId = assetTexture!!["begin_board.png"]
        if(textureId == null) {
            clearGLES()
            return
        }
        bindOverlayTexture(textureId)

        // 5. Draw face-triangle mesh
        // 5.1. Init triangle-indices
        val drawIndex = shortArrayOf(
            0, 1, 2,
            1, 2, 3
        )
        drawTriangleByIndex(drawIndex)

        // 6. Clear GLES
        clearGLES()
    }

    private fun drawPlay(
        textureId: Int, cubeBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ) {
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        val vertex = floatArrayOf(
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
        )
        // 2.1. Face-vertex as vertex
        setBaseVertex(vertex, 2)
        // 2.2. Mask-vertex as texture-vertex
        setOverlayVertex(vertex, 2)

        setDrawMode(0.0f)

        uniformScale()

        // 3. Activate image-texture
        bindBaseTexture(textureId)

        // 5. Draw face-triangle mesh
        // 5.1. Init triangle-indices
        val drawIndex = shortArrayOf(
            0, 1, 2,
            1, 2, 3
        )
        drawTriangleByIndex(drawIndex)

        // 6. Clear GLES
        clearGLES()
    }

    private fun drawFinish(
        textureId: Int, cubeBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ) {
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        val vertex = floatArrayOf(
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
        )
        // 2.1. Face-vertex as vertex
        setBaseVertex(vertex, 2)
        // 2.2. Mask-vertex as texture-vertex
        setOverlayVertex(vertex, 2)

        setDrawMode(0.0f)

        uniformScale()

        // 3. Activate image-texture
        bindBaseTexture(textureId)

        // 5. Draw face-triangle mesh
        // 5.1. Init triangle-indices
        val drawIndex = shortArrayOf(
            0, 1, 2,
            1, 2, 3
        )
        drawTriangleByIndex(drawIndex)

        // 6. Clear GLES
        clearGLES()
    }

}