package com.plugin.filters.plugin_filters.mlkit.processor.game.effect

import android.content.Context
import android.graphics.PointF
import android.opengl.GLES20
import android.util.Log
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameUtils
import jp.co.cyberagent.android.gpuimage.ml.FaceShader
import jp.co.cyberagent.android.gpuimage.ml.FaceSwapFilter
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import java.nio.FloatBuffer

class DoubleFaceFilter: FaceSwapFilter {
    var doubleType = 3

    constructor(context: Context, mode: MaskMode): super(context, mode) {
        this.vertexShader =
            FaceShader.getShaderFromAssets(context, "shader/vertex_mask.glsl")
        //FaceShader.VERTEX_SHADER
        this.fragmentShader =
            FaceShader.getShaderFromAssets(context, "shader/fragment_mask.glsl")

    }

    override fun onDraw(
        textureId: Int, cubeBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ) {
        GLES20.glUseProgram(this.program)

        allowTransparent()

        runPendingOnDrawTasks()
        if (!isInitialized) {
            return
        }

        drawDoubleStraight(textureId, cubeBuffer, textureBuffer)

    }

    /**
     * Simply draw 2 rectangles - of same side with flip texture
     */
    fun drawDoubleStraight(
        textureId: Int, cubeBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ) {
        // 2. Initialize vertex and texture-coordinates
        if(detectVertex != null) {
            when(doubleType)  {
//                1 -> drawFirstDoubleType(textureId)
//                2 ->
//                    drawTunePointsDoubleType(textureId, 386, 14)
////                    draw2PointsDoubleType(textureId, 386, 14)
//                3 ->
//                    drawTunePointsDoubleType(textureId, 168, 14)
////                    draw2PointsDoubleType(textureId, 168, 14)
//                4 ->
//                    drawTunePointsDoubleType(textureId, 386, 291)
////                    draw2PointsDoubleType(textureId, 386, 291)
//                5 ->
//                    drawTunePointsDoubleType(textureId, 168, 291)
////                    draw2PointsDoubleType(textureId, 168, 291)
//                6 ->
//                    drawTunePointsDoubleType(textureId, 159, 14)
////                    draw2PointsDoubleType(textureId, 159, 14)

//                1 -> drawFirstDoubleType(textureId)
//                2 ->
////                    drawTunePointsDoubleType(textureId, 386, 14)
//                    draw2PointsDoubleType(textureId, 386, 14)
//                3 ->
////                    drawTunePointsDoubleType(textureId, 168, 14)
//                    draw2PointsDoubleType(textureId, 168, 14)
//                4 ->
////                    drawTunePointsDoubleType(textureId, 386, 291)
//                    draw2PointsDoubleType(textureId, 386, 291)
//                5 ->
////                    drawTunePointsDoubleType(textureId, 168, 291)
//                    draw2PointsDoubleType(textureId, 168, 291)
//                6 ->
////                    drawTunePointsDoubleType(textureId, 159, 14)
//                    draw2PointsDoubleType(textureId, 159, 14)

                1 -> drawFirstDoubleType(textureId)
                2 ->
//                    drawTunePointsDoubleType(textureId, 386, 14)
                    draw4PointsDoubleType(textureId, 386, 14)
                3 ->
//                    drawTunePointsDoubleType(textureId, 168, 14)
                    draw4PointsDoubleType(textureId, 168, 14)
                4 ->
//                    drawTunePointsDoubleType(textureId, 386, 291)
                    draw4PointsDoubleType(textureId, 386, 291)
                5 ->
//                    drawTunePointsDoubleType(textureId, 168, 291)
                    draw4PointsDoubleType(textureId, 168, 291)
                6 ->
//                    drawTunePointsDoubleType(textureId, 159, 14)
                    draw4PointsDoubleType(textureId, 159, 14)

            }
        } else {
            drawTexture(textureId)
        }

    }


    fun drawFirstDoubleType(
        textureId: Int
    ) {
        // 2. Initialize vertex and texture-coordinates
        if(detectVertex != null) {
            val index = 386
            val x = detectVertex!![index*3]

            // 2.1. position vertex
            val positionVertex = floatArrayOf(
                0f, 0f,
                0f, 1f,
                0.5f, 0f,
                0.5f, 1f,
                1f, 0f,
                1f, 1f,
                )
            // 2.2. texture vertex
            val textureVertex = floatArrayOf(
                0f, 0f,
                0f, 1f,
                x, 0f,
                x, 1f,
                0f, 0f,
                0f, 1f,
            )
            val drawIndex = shortArrayOf(
                0, 1, 2,
                1, 2, 3,
                2, 3, 5,
                2, 4, 5,
            )

            drawDoubleType(
                textureId,
                positionVertex,
                textureVertex,
                drawIndex
            )
        }
    }


    fun draw2PointsDoubleType(
        textureId: Int, index1: Int, index2: Int
    ) {
        // 2. Initialize vertex and texture-coordinates
        if(detectVertex != null) {
            val x1 = detectVertex!![index1*3]
            val y1 = detectVertex!![index1*3+1]

            val x2 = detectVertex!![index2*3]
            val y2 = detectVertex!![index2*3+1]

            val intersects = GameUtils.lineUnitRec(x1, y1, x2, y2)

            val p1 = intersects[0]
            val p2 = intersects[1]

            if(p1 != null && p2 != null ) {
                // 2.1. position vertex
                val positionVertex = floatArrayOf(
                    0f, 0f,
                    0f, 1f,
                    0.5f, 0f,
                    0.5f, 1f,
                    1f, 0f,
                    1f, 1f,
                )
                // 2.2. texture vertex
                val textureVertex = floatArrayOf(
                    0f, 0f,
                    0f, 1f,
                    p1.x, p1.y, //x, 0f,
                    p2.x, p2.y,//x, 1f,
                    0f, 0f,
                    0f, 1f,
                )
                val drawIndex = shortArrayOf(
                    0,1,3, 0,2,3,
                    2,3,4, 3,4,5
                )

                drawDoubleType(
                    textureId,
                    positionVertex,
                    textureVertex,
                    drawIndex
                )
            }
        }
    }


    fun draw4PointsDoubleType(
        textureId: Int, index1: Int, index2: Int
    ) {
        // 2. Initialize vertex and texture-coordinates
        if(detectVertex != null) {
            val x1 = detectVertex!![index1*3]
            val y1 = detectVertex!![index1*3+1]

            val x2 = detectVertex!![index2*3]
            val y2 = detectVertex!![index2*3+1]

            val intersects = GameUtils.lineUnitRec(x1, y1, x2, y2)

            val p1 = intersects[0]
            val p2 = intersects[1]

            if(p1 != null && p2 != null
                && (p1.x < 0.7 && p1.x > 0.3) && (p2.x < 0.7 && p2.x > 0.3)
            ) {

                // 2.1. position vertex
                val dist1 = GameUtils.distance(p1, PointF(0f, 0.5f))
                val dist2 = GameUtils.distance(p2, PointF(0f, 0.5f))

                val xx = dist1 / (dist1 + dist2)


                val positionVertex = floatArrayOf(
                    0f, 0f,
                    0f, 1f,
                    0.5f, 0f,
                    0.5f, 1f,
                    1f, 0f,
                    1f, 1f,
                    0f, xx,
                    1f, xx

                )
                // 2.2. texture vertex
                val textureVertex = floatArrayOf(
                    0f, 0f,
                    0f, 1f,
                    p1.x, p1.y, //x, 0f,
                    p2.x, p2.y,//x, 1f,
                    0f, 0f,
                    0f, 1f,
                    0f, 0.5f,
                    0f, 0.5f,
                )
                val drawIndex = shortArrayOf(
                    0,2,6, 2,6,3, 6,3,1,
                    2,3,7, 2,4,7, 3,5,7
                )

                drawDoubleType(
                    textureId,
                    positionVertex,
                    textureVertex,
                    drawIndex
                )
            } else {
                drawTexture(textureId)
            }
        }
    }

    fun drawTunePointsDoubleType(
        textureId: Int, index1: Int, index2: Int
    ) {
        // 2. Initialize vertex and texture-coordinates
        if(detectVertex != null) {
            val x1 = detectVertex!![index1*3]
            val y1 = detectVertex!![index1*3+1]

            val x2 = detectVertex!![index2*3]
            val y2 = detectVertex!![index2*3+1]

            val intersects = GameUtils.lineUnitRec(x1, y1, x2, y2)

            val p1 = intersects[0]
            val p2 = intersects[1]



            if(p1 != null && p2 != null ) {
                val rect = GameUtils.findSuitableRect(p1, p2) ?: return

                // join rect element into string
                val rectStr = rect.joinToString { "$it | " }

                Log.e("ReflectFace", "rect: $rectStr")

                // 2.1. position vertex
                val positionVertex = floatArrayOf(
                    0f, 0f,
                    0f, 1f,
                    0.5f, 0f,
                    0.5f, 1f,
                    1f, 0f,
                    1f, 1f,
                )
                // 2.2. texture vertex
                val textureVertex = floatArrayOf(
                    rect[0].x, rect[0].y, // 0f, 0f,
                    rect[1].x, rect[1].y, //0f, 1f,
                    rect[2].x, rect[2].y, // 0f, 0f,
                    rect[3].x, rect[3].y, //0f, 1f,
                    rect[0].x, rect[0].y, // 0f, 0f,
                    rect[1].x, rect[1].y, //0f, 1f,
                )
                val drawIndex = shortArrayOf(
                    0,1,3, 0,2,3,
                    2,3,4, 3,4,5
                )

                drawDoubleType(
                    textureId,
                    positionVertex,
                    textureVertex,
                    drawIndex
                )
            }
        }
    }


    private fun drawDoubleType(
        textureId: Int,
        positionVertex: FloatArray,
        textureVertex: FloatArray,
        drawIndex: ShortArray
    ) {
        // 2.1. position vertex
        setBaseVertex(positionVertex, 2)
        // 2.2. texture vertex
        setOverlayVertex(textureVertex, 2)

        setDrawMode(-1f)
        uniformScale()
        // 3. Activate image-texture
        bindBaseTexture(textureId)
        bindOverlayTexture(textureId)
        // 5. Draw face-triangle mesh
        // 5.1. Init triangle-indices
        drawTriangleByIndex(drawIndex)
        // 6. Clear GLES
        clearGLES()
    }


}