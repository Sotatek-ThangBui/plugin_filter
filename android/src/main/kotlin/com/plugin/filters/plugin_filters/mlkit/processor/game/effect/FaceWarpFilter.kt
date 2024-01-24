package com.plugin.filters.plugin_filters.mlkit.processor.game.effect

import android.content.Context
import android.graphics.PointF
import android.opengl.GLES20
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameUtils
import jp.co.cyberagent.android.gpuimage.ml.FaceShader
import jp.co.cyberagent.android.gpuimage.ml.FaceSwapFilter
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import java.nio.FloatBuffer

class FaceWarpFilter: FaceSwapFilter {

    companion object {
        private const val TAG = "FacePartFilter"


    }

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

        if (this.detectVertex != null) {
            drawTexture(textureId)
            for (faceIndex in 0 until numFace) {
                // basicGridDraw(textureId, faceIndex,50)
                rightEyeCircleDraw(textureId, faceIndex)
                leftEyeCircleDraw(textureId, faceIndex)
            }

        } else {
            drawTexture(textureId)
        }
    }


    private fun foreheadCircle(textureId:Int, faceIndex:Int) {
        val center1 = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 10)
        val center2 = GameUtils.facePointF(faceIndex * meshSize * 3,
            detectVertex!!, 151)

        val center = PointF((center1.x+center2.x)/2, (center1.y+center2.y)/2)

        val target = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 6)
        val radius = GameUtils.distance(center, target)
        basicCircleDraw(textureId, center1, 1f*radius, 4, 40)
    }

    private fun noseCircle(textureId:Int, faceIndex:Int) {
        val center1 = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 374)
        val center2 = GameUtils.facePointF(faceIndex * meshSize * 3,
            detectVertex!!, 386)

        val center = PointF((center1.x+center2.x)/2, (center1.y+center2.y)/2)

        val target = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 282)
        val radius = GameUtils.distance(center, target)
        basicCircleDraw(textureId, center, 0.5f*radius, 4, 40)
    }


    private fun mouthCircle(textureId:Int, faceIndex:Int) {
        val center1 = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 374)
        val center2 = GameUtils.facePointF(faceIndex * meshSize * 3,
            detectVertex!!, 386)

        val center = PointF((center1.x+center2.x)/2, (center1.y+center2.y)/2)

        val target = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 282)
        val radius = GameUtils.distance(center, target)
        basicCircleDraw(textureId, center, 0.5f*radius, 4, 40)
    }

    private fun cheekCircle(textureId:Int, faceIndex:Int) {
        val center1 = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 374)
        val center2 = GameUtils.facePointF(faceIndex * meshSize * 3,
            detectVertex!!, 386)

        val center = PointF((center1.x+center2.x)/2, (center1.y+center2.y)/2)

        val target = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 282)
        val radius = GameUtils.distance(center, target)
        basicCircleDraw(textureId, center, 0.5f*radius, 4, 40)
    }


    private fun rightEyeCircleDraw(textureId:Int, faceIndex:Int) {
        val center1 = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 374)
        val center2 = GameUtils.facePointF(faceIndex * meshSize * 3,
            detectVertex!!, 386)

        val center = PointF((center1.x+center2.x)/2, (center1.y+center2.y)/2)

        val target = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 282)
        val radius = GameUtils.distance(center, target)
        basicCircleDraw(textureId, center, 0.5f*radius, 4, 40)
    }

    private fun leftEyeCircleDraw(textureId:Int, faceIndex:Int) {
        val center1 = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 159)
        val center2 = GameUtils.facePointF(faceIndex * meshSize * 3,
            detectVertex!!, 145)

        val center = PointF((center1.x+center2.x)/2, (center1.y+center2.y)/2)

        val target = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 52)
        val radius = GameUtils.distance(center, target)
        basicCircleDraw(textureId, center, 0.5f*radius, 4, 20)
    }

    /**
     * Grid draw not work for circle warp
     * Need to build triangle by circle points
     *
     */

    private fun basicCircleDraw(textureId:Int, center: PointF, radius:Float, numCircle:Int, numPoint:Int) {
        val step = radius/numCircle
        val points = EffectUtils.generateCirclePoints(numCircle, numPoint, center, radius)
        val triangles = EffectUtils.generateCircleTriangles(numCircle, numPoint)
        val warpPoints =
            EffectUtils.warpCirclePoints(numCircle, numPoint, points, center, radius, step)
        drawBaseTexture(textureId, warpPoints, points, triangles)
    }

    /**
     * Divide unit rectangle 0,0,1,1 into grid of nxn cells
     * build 2nxn triangles from these grid points
     * draw the texture by these triangles
     *
     * warp the coordinates of the grid points to get warping effect
     *
     */

    private fun basicGridDraw(textureId:Int, faceIndex:Int, n:Int) {
        val step = 1.0f/n
        val points = EffectUtils.generateGridPoints(n)
        val triangles = EffectUtils.generateGridTriangles(n)
        val center = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 10)
        val target = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 197)
        val radius = GameUtils.distance(center, target)
        val warpPoints = EffectUtils.warpGridPoints(points, center, radius, step)
        drawBaseTexture(textureId, warpPoints, points, triangles)
    }



}