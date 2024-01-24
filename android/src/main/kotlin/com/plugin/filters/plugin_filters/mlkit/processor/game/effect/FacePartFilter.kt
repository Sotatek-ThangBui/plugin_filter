package com.plugin.filters.plugin_filters.mlkit.processor.game.effect

import android.content.Context
import android.graphics.PointF
import android.opengl.GLES20
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameUtils
import jp.co.cyberagent.android.gpuimage.Constants
import jp.co.cyberagent.android.gpuimage.ml.FaceShader
import jp.co.cyberagent.android.gpuimage.ml.FaceSwapFilter
import jp.co.cyberagent.android.gpuimage.ml.FaceUtils
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

open class FacePartFilter//FaceShader.VERTEX_SHADER
    (context: Context, mode: MaskMode) : FaceSwapFilter(context, mode) {
    companion object {
        private const val TAG = "FacePartFilter"
    }

    var doubleType = 3

    init {
        this.vertexShader =
            FaceShader.getShaderFromAssets(context, "shader/vertex_mask.glsl")
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

        drawTexture(textureId)

        if (this.detectVertex != null) {
            facePartEffect(textureId)
            // faceSwapEffect(textureId)
        }
    }

    /**
     * =============================================================================================
     * I. FACE SWAP EFFECT
     * =============================================================================================
     */
    /**
     * Write face swap effect for case there are 2 face
     */
    private fun faceSwapEffect(textureId: Int) {
        if (numFace < 2) {
            return
        } else {
            drawFaceMask(textureId, 0, 1);
            drawFaceMask(textureId, 1, 0)
        }
    }

    private fun getFaceVertex(faceIndex: Int): Pair<FloatArray, FloatArray> {
        val faceVertex =
            detectVertex!!.copyOfRange(faceIndex * meshSize * 3, (faceIndex + 1) * meshSize * 3)
        val maskVertex = FloatArray(meshSize * 2)
        for (i in 0 until meshSize) {
            maskVertex[i * 2] = faceVertex[i * 3]
            maskVertex[i * 2 + 1] = faceVertex[i * 3 + 1]
        }

        return Pair(faceVertex, maskVertex)
    }

    open fun drawFaceMask(
        textureId: Int, faceIndex: Int, maskIndex: Int
    ) {
        // 1. Get vertex
        val vertexFace = getFaceVertex(faceIndex)
        val vertexMask = getFaceVertex(maskIndex)
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        setBaseVertex(vertexFace.first, 3)
        // 2.2. Mask-vertex as texture-vertex
        setOverlayVertex(vertexMask.second, 2)
        val alphaVertexBuffer = FaceUtils.borderVtxAlpha(
            meshSize,
            1f, 0.6f,
            1f, 0.8f
        )

        bindBufferLocation(alphaVertexBuffer, locVtxAlpha, 1)

        setDrawMode(5f)
        uniformScale()
        // 3. Activate image-texture and mask-texture
        // 3.1. Bind image-texture
        bindBaseTexture(textureId)
        bindOverlayTexture(textureId)
        // 4. Enable depth for rendering 3d content
        draw3DTriangleByIndex(Constants.face_eye_mouth_tris)

        clearGLES()
    }


    /**
     * =============================================================================================
     * II. FACE-PART EFFECT
     * =============================================================================================
     */

    private fun facePartEffect(textureId: Int) {
        for (i in 0 until numFace) {
            // drawOneEye(textureId, i)
            eyeDraw(textureId, i)
        }
    }

    /**
     * =============================================================================================
     * 2. FACE REVERT EFFECT
     * =============================================================================================
     */
    /**
     * Write function to draw revert face region
     */
    protected fun drawRevertFace(textureId: Int, faceIndex: Int = 0) {
        // 1. Get vertex
        val vertexFace = getFaceVertex(faceIndex)
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        var revertVertex = reflectFaceVertex(vertexFace.first)
        revertVertex = EffectUtils.scaleVertex(revertVertex, 1.1f)
        val o151 = GameUtils.facePointF(vertexFace.first, 151)
        val o175 = GameUtils.facePointF(vertexFace.first, 200)
        val oCenter = PointF((o151.x + o175.x) / 2, (o151.y + o175.y) / 2)
        val r151 = GameUtils.facePointF(revertVertex, 151)
        val r175 = GameUtils.facePointF(revertVertex, 175)
        val rCenter = PointF((r151.x + r175.x) / 2, (r151.y + r175.y) / 2)

        revertVertex = EffectUtils.translateVertex(revertVertex, rCenter, oCenter)
        setBaseVertex(revertVertex, 3)
        // 2.2. Mask-vertex as texture-vertex
        setOverlayVertex(vertexFace.second, 2)
        val triangleIndices = Constants.s_face_tris + Constants.in_mouth_tris
        val alphaVertexBuffer = FaceUtils.borderVtxAlpha(triangleIndices.size)

        bindBufferLocation(alphaVertexBuffer, locVtxAlpha, 1)

        setDrawMode(5f)
        uniformScale()
        // 3. Activate image-texture and mask-texture
        // 3.1. Bind image-texture
        bindBaseTexture(textureId)
        bindOverlayTexture(textureId)
        // 4. Enable depth for rendering 3d content
        draw3DTriangleByIndex(triangleIndices)

        clearGLES()
    }

    private fun revertVertex(vertex: FloatArray): FloatArray {
        val revertVertex = FloatArray(vertex.size)
        for (i in 0 until meshSize) {
            revertVertex[i * 3] = vertex[3 * i]
            revertVertex[i * 3 + 1] = 1f - vertex[3 * i + 1]
            revertVertex[i * 3 + 2] = vertex[3 * i + 2]
        }
        return revertVertex
    }

    private fun reflectVertex(vertex: FloatArray, a: PointF, b: PointF): FloatArray {
        val reflectVertex = FloatArray(vertex.size)
        for (i in 0 until meshSize) {
            val p = PointF(vertex[3 * i], vertex[3 * i + 1])
            val symetricPoint = EffectUtils.reflectPoint(p, a, b)
            reflectVertex[i * 3] = symetricPoint.x
            reflectVertex[i * 3 + 1] = symetricPoint.y
            reflectVertex[i * 3 + 2] = vertex[3 * i + 2]
        }
        return reflectVertex
    }

    private fun reflectFaceVertex(vertex: FloatArray): FloatArray {
        val a = GameUtils.facePointF(vertex, 50)
        val b = GameUtils.facePointF(vertex, 280)
        return reflectVertex(vertex, a, b)
    }



    /**
     * =============================================================================================
     * 3. FACE SKIN-COVER-EYES EFFECT
     * =============================================================================================
     */
    protected fun drawOneEye(textureId: Int, faceIndex: Int = 0) {
        // Texture - vertex
        val center1 = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 151
        )
        val target1 = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 108
        )
        val radius1 = GameUtils.distance(center1, target1)
        // Position - vertex
        val center2 = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 159
        )
        val target2 = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 133
        )
        val radius2 = GameUtils.distance(center2, target2)
        val center3 = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 386
        )
        val target3 = GameUtils.facePointF(
            faceIndex * meshSize * 3,
            detectVertex!!, 362
        )
        val radius3 = GameUtils.distance(center2, target2)

        basicCircleDraw(
            textureId,
            center1, radius1,
            center2, radius2 * 5f / 3f,
            3, 20
        )

        basicCircleDraw(
            textureId,
            center1, radius1,
            center3, radius3 * 5f / 3f,
            3, 8
        )


        drawRightEyeByTargetScaleRotate(
            textureId, faceIndex, 168, 2f, 0f
        )
    }

    private fun basicCircleDraw(
        textureId: Int,
        center1: PointF, radius1: Float,
        center2: PointF, radius2: Float,
        numCircle: Int, numPoint: Int
    ) {
        val points = EffectUtils.generateCirclePoints(numCircle, numPoint, center1, radius1)
        val triangles = EffectUtils.generateCircleTriangles(numCircle, numPoint)
        val warpPoints = EffectUtils.generateCirclePoints(numCircle, numPoint, center2, radius2)
        val alpha = circleVtxAlpha(numCircle, numPoint)
        drawAlphaBaseTexture(textureId, warpPoints, points, alpha, triangles)
    }

    private fun circleVtxAlpha(
        numCircle: Int, numPoint: Int
    ): FloatBuffer {
        val points = FloatArray((numCircle * numPoint + 1))
        var index = 0
        points[index++] = 1f
        // Only warp the points of the inner circles - exclude the biggest circle
        for (i in 0 until numCircle) {
            for (j in 0 until numPoint) {
                points[index++] = if (i < numCircle - 1) {
                    1f
                } else {
                    0f
                }
            }
        }
        val result = ByteBuffer
            .allocateDirect(points.size * Constants.BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        result.put(points)
        return result
    }

    private fun drawAlphaBaseTexture(
        textureId: Int,
        positionVertex: FloatArray,
        textureVertex: FloatArray,
        alphaVertexBuffer: FloatBuffer,
        drawIndex: ShortArray
    ) {
        // 2.1. position vertex
        setBaseVertex(positionVertex, 2)
        // 2.2. texture vertex
        setOverlayVertex(textureVertex, 2)
        // 2.3. Set alpha of vertex
        bindBufferLocation(alphaVertexBuffer, locVtxAlpha, 1)

        setDrawMode(5f)

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


    /**
     * =============================================================================================
     * 4. FACE MULTIPLE-EYES EFFECT
     * =============================================================================================
     */

    open fun eyeDraw(textureId: Int, faceIndex: Int) {
        if(maskVertex != null && getMaskBitmap() != null) {
            drawMaskKokushiboEye(textureId, faceIndex)
        } else {
            drawKokushiboEye(textureId, faceIndex)
        }

    }

    private fun drawCheekEye(textureId: Int, faceIndex: Int = 0) {
        drawRightEyeCheek(textureId, faceIndex)
        drawLeftEyeCheek(textureId, faceIndex)
    }

    private fun drawVerticalEye(textureId: Int, faceIndex: Int = 0) {
        drawRightEyeByTargetScaleRotate(
            textureId, faceIndex, 151, 1.2f, Math.PI.toFloat() / 2f
        )
    }

    private fun drawKokushiboEye(
        textureId: Int, faceIndex: Int = 0
    ) {
        // Right Eye
        drawRightEyeByTargetScaleRotate(
            textureId, faceIndex, 348, 0.9f, Math.PI.toFloat() / 8f
        )

        drawRightEyeByTargetScaleRotate(
            textureId, faceIndex, 282, 1.1f, -Math.PI.toFloat() / 10f
        )
        // Left Eye
        drawLeftEyeByTargetScaleRotate(
            textureId, faceIndex, 119, 0.9f, -Math.PI.toFloat() / 8f
        )

        drawLeftEyeByTargetScaleRotate(
            textureId, faceIndex, 52, 1.1f, Math.PI.toFloat() / 10f
        )
    }

    private fun drawLeftEyeCheek(
        textureId: Int, faceIndex: Int = 0
    ) {
        // Angle in degree
        val angle = GameUtils.getAngleByIndex(
            faceIndex * meshSize * 3,
            detectVertex!!, 229, 111, 226
        )
        drawLeftEyeByTargetScaleRotate(
            textureId, faceIndex, 229, 0.75f, Math.PI.toFloat() / 6f
        )
    }

    protected fun drawLeftEyeForehead(
        textureId: Int, faceIndex: Int = 0
    ) {
        drawLeftEyeByTargetIndex(textureId, faceIndex, 151)
    }

    private fun drawLeftEyeByTargetIndex(
        textureId: Int, faceIndex: Int = 0, targetIndex: Int = 151
    ) {
        // 1. Initialize vertex and texture-coordinates
        val eyeIndices = EffectUtils.LEFT_EYE_INDICES
        // 2. Set triangle-index
        val triangleIndices = EffectUtils.LEFT_EYE_TRIANGLE
        val faceVertex =
            detectVertex!!.copyOfRange(faceIndex * meshSize * 3, (faceIndex + 1) * meshSize * 3)
        val target = PointF(faceVertex[targetIndex * 3], faceVertex[targetIndex * 3 + 1])
        // 2.1. Face-vertex as vertex
        var eyeVertex = EffectUtils.getSubVertices(faceVertex, eyeIndices)
        // translate eye to forehead
        eyeVertex = EffectUtils.translateVertex(eyeVertex, target)
        // 2.2. Mask-vertex as texture-vertex
        val eyeMaskVertex = EffectUtils.getSubMaskVertices(faceVertex, eyeIndices)
        // 2.3. Set alpha-vertex
        // Set alpha for outer eye
        val alphaVertexBuffer = EffectUtils.eyeVtxAlpha(triangleIndices)

        drawMultiEye(
            textureId,
            eyeVertex, eyeMaskVertex, alphaVertexBuffer, triangleIndices
        )
    }

    private fun drawLeftEyeByTargetScale(
        textureId: Int, faceIndex: Int = 0, targetIndex: Int = 151, scale: Float = 0.5f
    ) {
        // 1. Initialize vertex and texture-coordinates
        val eyeIndices = EffectUtils.LEFT_EYE_INDICES
        // 2. Set triangle-index
        val triangleIndices = EffectUtils.LEFT_EYE_TRIANGLE
        val faceVertex =
            detectVertex!!.copyOfRange(faceIndex * meshSize * 3, (faceIndex + 1) * meshSize * 3)
        val target = PointF(faceVertex[targetIndex * 3], faceVertex[targetIndex * 3 + 1])
        // 2.1. Face-vertex as vertex
        var eyeVertex = EffectUtils.getSubVertices(faceVertex, eyeIndices)
        // translate eye to forehead
        eyeVertex = EffectUtils.translateVertex(eyeVertex, target)
        eyeVertex = EffectUtils.scaleVertex(eyeVertex, scale)
        // 2.2. Mask-vertex as texture-vertex
        val eyeMaskVertex = EffectUtils.getSubMaskVertices(faceVertex, eyeIndices)
        // 2.3. Set alpha-vertex
        // Set alpha for outer eye
        val alphaVertexBuffer = EffectUtils.eyeVtxAlpha(triangleIndices)

        drawMultiEye(
            textureId,
            eyeVertex, eyeMaskVertex, alphaVertexBuffer, triangleIndices
        )
    }

    protected fun drawLeftEyeByTargetScaleRotate(
        textureId: Int,
        faceIndex: Int = 0,
        targetIndex: Int = 151,
        scale: Float = 0.5f,
        angle: Float
    ) {
        // 1. Initialize vertex and texture-coordinates
        val eyeIndices = EffectUtils.LEFT_EYE_INDICES
        // 2. Set triangle-index
        val triangleIndices = EffectUtils.LEFT_EYE_TRIANGLE
        val faceVertex =
            detectVertex!!.copyOfRange(faceIndex * meshSize * 3, (faceIndex + 1) * meshSize * 3)
        val target = PointF(faceVertex[targetIndex * 3], faceVertex[targetIndex * 3 + 1])
        // 2.1. Face-vertex as vertex
        var eyeVertex = EffectUtils.getSubVertices(faceVertex, eyeIndices)
        // translate eye to forehead
        eyeVertex = EffectUtils.translateVertex(eyeVertex, target)
        eyeVertex = EffectUtils.scaleVertex(eyeVertex, scale)
        eyeVertex = EffectUtils.rotateVertex(eyeVertex, angle)
        // 2.2. Mask-vertex as texture-vertex
        val eyeMaskVertex = EffectUtils.getSubMaskVertices(faceVertex, eyeIndices)
        // 2.3. Set alpha-vertex
        // Set alpha for outer eye
        val alphaVertexBuffer = EffectUtils.eyeVtxAlpha(triangleIndices)

        drawMultiEye(
            textureId,
            eyeVertex, eyeMaskVertex, alphaVertexBuffer, triangleIndices
        )
    }

    private fun drawRightEyeForehead(
        textureId: Int, faceIndex: Int = 0
    ) {
        drawRightEyeByTargetIndex(
            textureId, faceIndex, 151
        )
    }

    private fun drawRightEyeCheek(
        textureId: Int, faceIndex: Int = 0
    ) {
        val angle = GameUtils.getAngleByIndex(
            faceIndex * meshSize * 3,
            detectVertex!!, 449, 340, 446
        )
        drawRightEyeByTargetScaleRotate(
            textureId, faceIndex, 449, 0.75f, -Math.PI.toFloat() / 6f
        )
    }

    private fun drawRightEyeByTargetIndex(
        textureId: Int, faceIndex: Int = 0, targetIndex: Int = 151
    ) {
        // 1. Initialize vertex and texture-coordinates
        val eyeIndices = EffectUtils.RIGHT_EYE_INDICES
        // 2. Set triangle-index
        val triangleIndices = EffectUtils.RIGHT_EYE_TRIANGLE
        val faceVertex =
            detectVertex!!.copyOfRange(faceIndex * meshSize * 3, (faceIndex + 1) * meshSize * 3)
        val target = PointF(faceVertex[targetIndex * 3], faceVertex[targetIndex * 3 + 1])
        // 2.1. Face-vertex as vertex
        var eyeVertex = EffectUtils.getSubVertices(faceVertex, eyeIndices)
        // translate eye to forehead
        eyeVertex = EffectUtils.translateVertex(eyeVertex, target)
        // 2.2. Mask-vertex as texture-vertex
        val eyeMaskVertex = EffectUtils.getSubMaskVertices(faceVertex, eyeIndices)
        // 2.3. Set alpha-vertex
        // Set alpha for outer eye
        val alphaVertexBuffer = EffectUtils.eyeVtxAlpha(triangleIndices)

        drawMultiEye(
            textureId,
            eyeVertex, eyeMaskVertex, alphaVertexBuffer, triangleIndices
        )
    }

    private fun drawRightEyeByTargetScale(
        textureId: Int, faceIndex: Int = 0, targetIndex: Int = 151, scale: Float = 0.5f
    ) {
        // 1. Initialize vertex and texture-coordinates
        val eyeIndices = EffectUtils.RIGHT_EYE_INDICES
        // 2. Set triangle-index
        val triangleIndices = EffectUtils.RIGHT_EYE_TRIANGLE
        val faceVertex =
            detectVertex!!.copyOfRange(faceIndex * meshSize * 3, (faceIndex + 1) * meshSize * 3)
        val target = PointF(faceVertex[targetIndex * 3], faceVertex[targetIndex * 3 + 1])
        // 2.1. Face-vertex as vertex
        var eyeVertex = EffectUtils.getSubVertices(faceVertex, eyeIndices)
        // translate eye to forehead
        eyeVertex = EffectUtils.translateVertex(eyeVertex, target)
        eyeVertex = EffectUtils.scaleVertex(eyeVertex, scale)
        // 2.2. Mask-vertex as texture-vertex
        val eyeMaskVertex = EffectUtils.getSubMaskVertices(faceVertex, eyeIndices)
        // 2.3. Set alpha-vertex
        // Set alpha for outer eye
        val alphaVertexBuffer = EffectUtils.eyeVtxAlpha(triangleIndices)

        drawMultiEye(
            textureId,
            eyeVertex, eyeMaskVertex, alphaVertexBuffer, triangleIndices
        )
    }

    private fun drawRightEyeByTargetScaleRotate(
        textureId: Int,
        faceIndex: Int = 0,
        targetIndex: Int = 151,
        scale: Float = 0.5f,
        angle: Float
    ) {
        // 1. Initialize vertex and texture-coordinates
        val eyeIndices = EffectUtils.RIGHT_EYE_INDICES
        // 2. Set triangle-index
        val triangleIndices = EffectUtils.RIGHT_EYE_TRIANGLE
        val faceVertex =
            detectVertex!!.copyOfRange(faceIndex * meshSize * 3, (faceIndex + 1) * meshSize * 3)
        val target = PointF(faceVertex[targetIndex * 3], faceVertex[targetIndex * 3 + 1])
        // 2.1. Face-vertex as vertex
        var eyeVertex = EffectUtils.getSubVertices(faceVertex, eyeIndices)
        // translate eye to forehead
        eyeVertex = EffectUtils.translateVertex(eyeVertex, target)
        eyeVertex = EffectUtils.scaleVertex(eyeVertex, scale)
        eyeVertex = EffectUtils.rotateVertex(eyeVertex, angle)
        // 2.2. Mask-vertex as texture-vertex
        val eyeMaskVertex = EffectUtils.getSubMaskVertices(faceVertex, eyeIndices)
        // 2.3. Set alpha-vertex
        // Set alpha for outer eye
        val alphaVertexBuffer = EffectUtils.eyeVtxAlpha(triangleIndices)

        drawMultiEye(
            textureId,
            eyeVertex, eyeMaskVertex, alphaVertexBuffer, triangleIndices
        )
    }

    private fun drawMultiEye(
        textureId: Int, eyeVertex: FloatArray, eyeMaskVertex: FloatArray,
        alphaVertexBuffer: FloatBuffer, triangleIndices: ShortArray
    ) {
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        setBaseVertex(eyeVertex, 3)
        // 2.2. Mask-vertex as texture-vertex
        setOverlayVertex(eyeMaskVertex, 2)
        // 2.3. Set alpha of vertex
        bindBufferLocation(alphaVertexBuffer, locVtxAlpha, 1)
        // 3. Activate image-texture and mask-texture
        // 3.0. Uniform variables
        setDrawMode(5f)
        uniformScale()
        // 3.1. Bind image-texture
        bindBaseTexture(textureId)
        bindOverlayTexture(textureId)
        // 4. Enable depth for rendering 3d content with triangle indices
        draw3DTriangleByIndex(triangleIndices)

        clearGLES()
    }


    /**
     * =============================================================================================
     * 5. FACE MASK EFFECT
     * =============================================================================================
     */
    open fun eyeMaskDraw(textureId: Int, faceIndex: Int) {
        drawMaskKokushiboEye(textureId, faceIndex)
    }

    private fun drawMaskCheekEye(textureId: Int, faceIndex: Int = 0) {
        drawMaskEyeByTargetScaleRotate(
            EffectUtils.RIGHT_EYE_INDICES, EffectUtils.RIGHT_EYE_TRIANGLE,
            textureId, faceIndex, 449, 0.75f, -Math.PI.toFloat() / 6f
        )
        drawMaskEyeByTargetScaleRotate(
            EffectUtils.LEFT_EYE_INDICES, EffectUtils.LEFT_EYE_TRIANGLE,
            textureId, faceIndex, 229, 0.75f, Math.PI.toFloat() / 6f
        )
    }

    private fun drawMaskVerticalEye(textureId: Int, faceIndex: Int = 0) {
        drawMaskEyeByTargetScaleRotate(
            EffectUtils.RIGHT_EYE_INDICES, EffectUtils.RIGHT_EYE_TRIANGLE,
            textureId, faceIndex, 151, 1.2f, Math.PI.toFloat() / 2f
        )
    }

    private fun drawMaskHorizontalEye(textureId: Int, faceIndex: Int = 0) {
        drawMaskEyeByTargetScaleRotate(
            EffectUtils.RIGHT_EYE_INDICES, EffectUtils.RIGHT_EYE_TRIANGLE,
            textureId, faceIndex, 151, 1.2f, 0f
        )
    }

    private fun drawMaskKokushiboEye(
        textureId: Int, faceIndex: Int = 0
    ) {
        // Right Eye
        drawMaskEyeByTargetScaleRotate(
            EffectUtils.RIGHT_EYE_INDICES, EffectUtils.RIGHT_EYE_TRIANGLE,
            textureId, faceIndex, 348, 0.9f, Math.PI.toFloat() / 8f
        )

        drawMaskEyeByTargetScaleRotate(
            EffectUtils.RIGHT_EYE_INDICES, EffectUtils.RIGHT_EYE_TRIANGLE,
            textureId, faceIndex, 282, 1.1f, -Math.PI.toFloat() / 10f
        )
        // Left Eye
        drawMaskEyeByTargetScaleRotate(
            EffectUtils.LEFT_EYE_INDICES, EffectUtils.LEFT_EYE_TRIANGLE,
            textureId, faceIndex, 119, 0.9f, -Math.PI.toFloat() / 8f
        )

        drawMaskEyeByTargetScaleRotate(
            EffectUtils.LEFT_EYE_INDICES, EffectUtils.LEFT_EYE_TRIANGLE,
            textureId, faceIndex, 52, 1.1f, Math.PI.toFloat() / 10f
        )
    }

    protected fun drawMaskEyeByTargetScaleRotate(
        eyeIndices: ShortArray, triangleIndices: ShortArray,
        textureId: Int,
        faceIndex: Int = 0,
        targetIndex: Int = 151,
        scale: Float = 0.5f,
        angle: Float
    ) {
        // 1. Initialize vertex and texture-coordinates
        val faceVertex =
            detectVertex!!.copyOfRange(faceIndex * meshSize * 3, (faceIndex + 1) * meshSize * 3)
        val target = PointF(faceVertex[targetIndex * 3], faceVertex[targetIndex * 3 + 1])
        // 2.1. Face-vertex as vertex
        var eyeVertex = EffectUtils.getSubVertices(faceVertex, eyeIndices)
        // translate eye to forehead
        eyeVertex = EffectUtils.translateVertex(eyeVertex, target)
        eyeVertex = EffectUtils.scaleVertex(eyeVertex, scale)
        eyeVertex = EffectUtils.rotateVertex(eyeVertex, angle)


        // 2.2. Mask-vertex as texture-vertex
        val eyeMaskVertex = EffectUtils.getSubMaskUVVertices(maskVertex!!, eyeIndices)
        // 2.3. Set alpha-vertex
        // Set alpha for outer eye
        val alphaVertexBuffer = EffectUtils.eyeVtxAlpha(triangleIndices)

        drawMaskMultiEye(
            textureId,
            eyeVertex, eyeMaskVertex, alphaVertexBuffer, triangleIndices
        )
    }

    private fun drawMaskMultiEye(
        textureId: Int,
        eyeVertex: FloatArray, eyeMaskVertex: FloatArray,
        alphaVertexBuffer: FloatBuffer, triangleIndices: ShortArray
    ) {
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        setBaseVertex(eyeVertex, 3)
        // 2.2. Mask-vertex as texture-vertex
        setOverlayVertex(eyeMaskVertex, 2)
        // 2.3. Set alpha of vertex
        bindBufferLocation(alphaVertexBuffer, locVtxAlpha, 1)
        // 3. Activate image-texture and mask-texture
        // 3.0. Uniform variables
        setDrawMode(5f)
        uniformScale()
        // 3.1. Bind image-texture
        bindBaseTexture(textureId)
        bindOverlayTexture(maskTexture)
        // 4. Enable depth for rendering 3d content with triangle indices
        draw3DTriangleByIndex(triangleIndices)

        clearGLES()
    }

}