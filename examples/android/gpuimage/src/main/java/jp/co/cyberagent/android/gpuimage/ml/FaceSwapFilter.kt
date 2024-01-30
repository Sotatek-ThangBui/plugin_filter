package jp.co.cyberagent.android.gpuimage.ml

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.util.Log
import jp.co.cyberagent.android.gpuimage.Constants
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.roundToInt

// value is draw-mode
/**
 * Options for pipeline steps
 * 1. Which triangles list to draw
0 -> DEFAULT -> Constants.s_face_tris
1 -> MaskMode.SWAP, MaskMode.SWAP_FRAME -> Constants.face_eye_mouth_tris

 * 2. Draw base-texture or not
True -> maskMode == MaskMode.SWAP_FRAME
|| maskMode == MaskMode.TATTOO
|| maskMode == MaskMode.TATTOO_MASK_CAM

 * 3. Which alpha list to draw
0 -> MaskMode.SWAP, MaskMode.SWAP_FRAME -> FaceUtils.borderVtxAlpha
1 -> MaskMode.DECORATE -> FaceUtils.decorateVtxAlpha
2 -> MaskMode.TATTOO_MASK, MaskMode.TATTOO_MASK_CAM -> FaceUtils.noForeheadVtxAlpha
3 -> default -> FaceUtils.tattooVtxAlpha()

 * 4. Which draw-mode to use
 */
enum class MaskMode(
    val triangleMode: Float,
    val baseTexture: Boolean,
    val alphaMode: Float,
    val drawMode: Float,
) {
    OVERLAY_TEXT(0f, false, 0f, -1f),
    BASE_TEXT(0f, false, 0f, 0f),
    DECORATE(0f, false, 1f, 2f),
    TATTOO(0f, false, 3f, 2.0f),
    TATTOO_CANON(0f, true, 3f, 1f),
    SWAP(1f, false, 0f, 2.0f),
    SWAP_FRAME(1f, true, 0f, 2f),
    MASK(0f, true, 2f, 5f),
    MASK_HSV(0f, true, 2f, 4f),
    MASK_CAM(0f, true, 2f, 2f),
    BEAUTY(0f, false, 4f, 2f),
    SWAP_ONLY_MOUTH_EYE(1f, false, 5f, 2f),
    DECORATE_MOUTH_EYE(0f, false, 6f, 2f),
}

enum class ScaleType(val value: Float) {
    CENTER_FIT(0f),
    CENTER_CROP(1f),
    CENTER_INSIDE(2f),
    NO_SCALE(100f)
}

open class FaceSwapFilter() : GPUImageFilter() {
    protected var skinPercent: Float = 0.6f
    protected var eyePercent: Float = 0.1f
    protected var mouthPercent: Float = 0.1f
    protected var nosePercent: Float = 0.1f
    protected var maskTexture = OpenGlUtils.NO_TEXTURE
    private var maskBitmap: Bitmap? = null

    //    protected var originTexture = OpenGlUtils.NO_TEXTURE
//    private var originBitmap: Bitmap? = null
    protected var locAVertex: Int = 0
    protected var locATexCoord: Int = 0
    protected var locSample: Int = 0
    protected var locSample2: Int = 0
    protected var locVtxAlpha: Int = 0
    private var locTexelWidth: Int = 0
    protected var locTexelHeight: Int = 0
    protected var locDrawMode: Int = 0
    private var drawMode: Float = 0f
    protected var locSkinColor: Int = 0
    var skinColor: FloatArray? = floatArrayOf(0f, 0f, 0f) //
    private var locIsFlip: Int = 0
    var isFlip: Boolean = false
    private var flip: Float = 1f
    private var locIsFlipY: Int = 0
    var flipY: Float = 1f
    private var locRatioWidth: Int = 0
    private var locRatioHeight: Int = 0
    private var locScaleType: Int = 0
    private var ratioHeight: Float = 1.0f
    private var ratioWidth: Float = 1.0f
    private var scaleType: ScaleType = ScaleType.CENTER_CROP
    protected var maskMode: MaskMode = MaskMode.DECORATE
    protected var faceTriangIndex: ShortArray = Constants.s_face_tris
    private var debugFlip = false
    var numFace: Int = 1
    var meshSize: Int = 0
    var faceTrack: HashMap<Int, Int> = HashMap()
    var trackFace: HashMap<Int, Int> = HashMap()
    var detectVertex: FloatArray? = null // Mask-face vertex
    var maskVertex: FloatArray? = null // Mask-face uniform value

    //    constructor(mode: MaskMode) : this() {
//        this.vertexShader =
//            FaceShader.VERTEX_SHADER
//        this.fragmentShader =
//            FaceShader.FRAGMENT_SHADER
//        setMode(mode)
//    }
    constructor(context: Context, mode: MaskMode) : this() {
        this.vertexShader =
            FaceShader.getShaderFromAssets(context, "shader/vertex_mask.glsl")
        //FaceShader.VERTEX_SHADER
        this.fragmentShader =
            FaceShader.getShaderFromAssets(context, "shader/fragment_mask.glsl")
        //FaceShader.FRAGMENT_SHADER
        setMode(mode)
    }

    open fun setMode(mode: MaskMode) {
        this.maskMode = mode
        Log.d(
            "duongnv",
            "drawMultiMask: draw  : ${maskMode.name} ${maskMode.drawMode}    ${mode.drawMode}"
        )
        faceTriangIndex = when (maskMode.triangleMode) {
            0f -> Constants.s_face_tris
            1f -> Constants.face_eye_mouth_tris
            else -> Constants.s_face_tris
        }
    }

    override fun onInit() {
        super.onInit()
        getLocatedVariable()
    }

    private fun getLocatedVariable() {
        locAVertex = GLES20.glGetAttribLocation(program, "a_Vertex")
        locATexCoord = GLES20.glGetAttribLocation(program, "a_TexCoord")
        locSample = GLES20.glGetUniformLocation(program, "u_sampler")
        locSample2 = GLES20.glGetUniformLocation(program, "u_sampler2")
        locVtxAlpha = GLES20.glGetAttribLocation(program, "a_vtxalpha")
        locDrawMode = GLES20.glGetUniformLocation(program, "u_drawMode")
        locSkinColor = GLES20.glGetUniformLocation(program, "u_skinColor")
        locIsFlip = GLES20.glGetUniformLocation(program, "u_flip")
        locIsFlipY = GLES20.glGetUniformLocation(program, "u_flipY")
        locRatioWidth = GLES20.glGetUniformLocation(program, "u_ratioWidth")
        locRatioHeight = GLES20.glGetUniformLocation(program, "u_ratioHeight")
        locScaleType = GLES20.glGetUniformLocation(program, "u_scaleType")
        locTexelWidth = GLES20.glGetUniformLocation(program, "u_texelWidth")
        locTexelHeight = GLES20.glGetUniformLocation(program, "u_texelHeight")
    }

    override fun onInitialized() {
        super.onInitialized()
        if (maskBitmap != null && !maskBitmap!!.isRecycled) {
            setMaskBitmap(maskBitmap)
        }
    }

    fun changeAlpha(percent: Float) {
        skinPercent = percent
    }

    fun setDebugFlip() {
        runOnDraw {
            debugFlip = true
        }
    }

    fun setData(
        isFlip: Boolean,
        meshSize: Int,
        numFace: Int,
        vtx: FloatArray?, // Origin-face vertex
        uv: FloatArray?, // Mask-face uniform value
    ) {
        // Force to finish setting vertex before drawing
        runOnDraw {
            this.meshSize = meshSize
            this.isFlip = isFlip
            this.numFace = numFace
            this.detectVertex = vtx
            this.maskVertex = uv
        }
    }

    fun setData(
        vtx: FloatArray?, // Origin-face vertex
        uv: FloatArray?, // Mask-face uniform value
    ) {
        // Force to finish setting vertex before drawing
        runOnDraw {
            this.detectVertex = vtx
            this.maskVertex = uv
        }
    }

    fun setPercent(
        skinPercent: Float, eyePercent: Float, nosePercent: Float, mouthPercent: Float
    ) {
        runOnDraw {
            this.skinPercent = skinPercent
            this.eyePercent = eyePercent
            this.nosePercent = nosePercent
            this.mouthPercent = mouthPercent
        }
    }

    fun setMaskBitmap(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) {
            this.maskBitmap = bitmap
            return
        }
        this.maskBitmap = bitmap

        runOnDraw(Runnable {
            // 1 time initialized texture - not need to run every drawing
            if (maskTexture == OpenGlUtils.NO_TEXTURE) {
                if (bitmap == null || bitmap.isRecycled) {
                    return@Runnable
                }
                maskTexture =
                    OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, false)
            }
        })
    }

    fun getMaskBitmap(): Bitmap? {
        return maskBitmap
    }

    //    fun setOriginBitmap(bitmap: Bitmap?) {
//        if (bitmap != null && bitmap.isRecycled) {
//            return
//        }
//        this.originBitmap = bitmap
//        if (this.originBitmap == null) {
//            return
//        }
//
//        runOnDraw(Runnable {
//            // 1 time initialized texture - not need to run every drawing
//            if (originTexture == OpenGlUtils.NO_TEXTURE) {
//                if (bitmap == null || bitmap.isRecycled) {
//                    return@Runnable
//                }
//                originTexture =
//                    OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, false)
//            }
//        })
//    }
//    fun getOriginBitmap(): Bitmap? {
//        return originBitmap
//    }
    fun recycleBitmap() {
        if (maskBitmap != null && !maskBitmap!!.isRecycled) {
            maskBitmap!!.recycle()
            maskBitmap = null
        }
//        if (originBitmap != null && !originBitmap!!.isRecycled) {
//            originBitmap!!.recycle()
//            originBitmap = null
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        GLES20.glDeleteTextures(1, intArrayOf(maskTexture), 0)
        maskTexture = OpenGlUtils.NO_TEXTURE
//        GLES20.glDeleteTextures(1, intArrayOf(originTexture), 0)
//        originTexture = OpenGlUtils.NO_TEXTURE
    }

    private fun defaultVtxAlpha(): FloatBuffer {
        val faceIndex = faceTriangIndex
        val data = FloatArray(faceIndex.size) { 1f }
        val result = ByteBuffer
            .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        result.put(data)
        return result
    }

    //    fun setScaleType(scaleType: ScaleType) {
//        this.scaleType = scaleType
//    }
    fun setScaleRatio(scaleType: ScaleType, imageWidth: Float, imageHeight: Float) {
        runOnDraw {
            this.scaleType = scaleType
            val outputWidth = outputWidth.toFloat()
            val outputHeight = outputHeight.toFloat()
            val ratio1: Float = outputWidth / imageWidth
            val ratio2: Float = outputHeight / imageHeight
            val ratioMax = ratio1.coerceAtLeast(ratio2)
            val imageWidthNew = (imageWidth * ratioMax).roundToInt()
            val imageHeightNew = (imageHeight * ratioMax).roundToInt()



            ratioWidth = imageWidthNew / outputWidth
            ratioHeight = imageHeightNew / outputHeight
//            Log.e("DebugFlip", "cameraImageSize:: " +
//                    "${imageWidth} ${imageHeight} || " +
//                    "$outputWidth $outputHeight ||" +
//                    "$ratioWidth $ratioHeight")
        }
    }

    protected fun allowTransparent() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, .0f) //
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
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

        if (maskMode.baseTexture) {
            drawTexture(textureId)
        }
        if (this.detectVertex != null && this.maskVertex != null && this.maskBitmap != null) {
            for (i in 0 until numFace) {
                drawMultiMask(textureId, cubeBuffer, textureBuffer, i)
            }
        }
    }

    fun setDrawMode(drawMode: Float) {
        this.drawMode = drawMode
        GLES20.glUniform1f(locDrawMode, drawMode)
    }

    protected fun drawTexture(
        textureId: Int
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

        setDrawMode(MaskMode.BASE_TEXT.drawMode)
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

    private fun drawBaseTexture(
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

        setDrawMode(MaskMode.BASE_TEXT.drawMode)
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

    protected fun drawBaseTexture(
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

    fun setBaseVertex(baseVertex: FloatArray, vertexSize: Int) {
        val vertexBuffer = arrayToBuffer(baseVertex)
        bindBufferLocation(vertexBuffer, locAVertex, vertexSize)
    }

    fun scaleBaseVertex(baseVertex: FloatArray, vertexSize: Int): FloatArray {
        val result = FloatArray(baseVertex.size) { 0f }
        if (drawMode == -1.0f) {
            for (i in baseVertex.indices step vertexSize) {
                result[i] = (2.0 * (baseVertex[i]) - 1.0).toFloat()
                result[i + 1] = (1.0 - 2.0 * (baseVertex[i + 1])).toFloat()
                if (vertexSize > 2)
                    result[i + 2] = baseVertex[i + 2]
            }
        } else {
            when (scaleType) {
                ScaleType.CENTER_FIT -> {
                    for (i in baseVertex.indices step vertexSize) {
                        result[i] = flip * (2.0 * (baseVertex[i]) - 1.0).toFloat()
                        result[i + 1] = (1.0 - 2.0 * (baseVertex[i + 1])).toFloat()
                        if (vertexSize > 2)
                            result[i + 2] = baseVertex[i + 2]
                    }
                }
                ScaleType.CENTER_CROP -> {
                    for (i in baseVertex.indices step vertexSize) {
                        result[i] = ratioWidth * flip * (2.0 * (baseVertex[i]) - 1.0).toFloat()
                        result[i + 1] = ratioHeight * (1.0 - 2.0 * (baseVertex[i + 1])).toFloat()
                        if (vertexSize > 2)
                            result[i + 2] = baseVertex[i + 2]
                    }
                }
                ScaleType.CENTER_INSIDE -> {
                    for (i in baseVertex.indices step vertexSize) {
                        result[i] =
                            1f / ratioHeight * flip * (2.0 * (baseVertex[i]) - 1.0).toFloat()
                        result[i + 1] =
                            1f / ratioWidth * (1.0 - 2.0 * (baseVertex[i + 1])).toFloat()
                        if (vertexSize > 2)
                            result[i + 2] = baseVertex[i + 2]
                    }
                }
                else -> {
                    for (i in baseVertex.indices step vertexSize) {
                        result[i] = (2.0 * (baseVertex[i]) - 1.0).toFloat()
                        result[i + 1] = (1.0 - 2.0 * (baseVertex[i + 1])).toFloat()
                        if (vertexSize > 2)
                            result[i + 2] = baseVertex[i + 2]
                    }
                }
            }
        }

        return result
    }

    fun setOverlayVertex(overlayVertex: FloatArray, vertexSize: Int) {
        val vertexBuffer = arrayToBuffer(overlayVertex)
        bindBufferLocation(vertexBuffer, locATexCoord, vertexSize)
    }

    fun bindBaseTexture(textureId: Int) {
        // 3.1. Bind image-texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(this.locSample, 0)
    }

    fun bindOverlayTexture(textureId: Int) {
        // 3.2. Bind mask-texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(this.locSample2, 3)
    }

    open fun drawMask(
        textureId: Int, cubeBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ) {
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        setBaseVertex(detectVertex!!, 3)
        // 2.2. Mask-vertex as texture-vertex
        maskVertex?.let { setOverlayVertex(it, 2) }
        val alphaVertexBuffer = getVtxAlpha()

        bindBufferLocation(alphaVertexBuffer, locVtxAlpha, 1)

        if (maskMode == MaskMode.SWAP_FRAME) {
            GLES20.glUniform3fv(locSkinColor, 1, FloatBuffer.wrap(skinColor!!))
        }

        setDrawMode(maskMode.drawMode)
        uniformScale()
        // 3. Activate image-texture and mask-texture
        // 3.1. Bind image-texture
        bindBaseTexture(textureId)
        bindOverlayTexture(maskTexture)
        // 4. Enable depth for rendering 3d content
        draw3DTriangleByIndex(faceTriangIndex)

        clearGLES()
    }

    open fun getVtxAlpha(): FloatBuffer {
        // return FaceUtils.noVtxAlpha(meshSize)
        return FaceUtils.getVtxAlpha(
            maskMode.alphaMode,
            meshSize,
            skinPercent,
            eyePercent,
            nosePercent,
            mouthPercent
        )
    }

    open fun drawMultiMask(
        textureId: Int, cubeBuffer: FloatBuffer,
        textureBuffer: FloatBuffer, faceIndex: Int = 0
    ) {
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        if (detectVertex == null) return
        val faceVertex =
            detectVertex!!.copyOfRange(faceIndex * meshSize * 3, (faceIndex + 1) * meshSize * 3)
        setBaseVertex(faceVertex, 3)
        // 2.2. Mask-vertex as texture-vertex
        setOverlayVertex(maskVertex!!, 2)
        val alphaVertexBuffer = getVtxAlpha()

        bindBufferLocation(alphaVertexBuffer, locVtxAlpha, 1)

        if (maskMode == MaskMode.SWAP_FRAME) {
            GLES20.glUniform3fv(locSkinColor, 1, FloatBuffer.wrap(skinColor!!))
        }
        setDrawMode(maskMode.drawMode)
        uniformScale()
        // 3. Activate image-texture and mask-texture
        // 3.1. Bind image-texture
        bindBaseTexture(textureId)
        bindOverlayTexture(maskTexture)
        // 4. Enable depth for rendering 3d content
        draw3DTriangleByIndex(faceTriangIndex)

        clearGLES()
    }

    open fun drawMaskByTriangle(
        textureId: Int, triangIndex: ShortArray, faceIndex: Int = 0
    ) {
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        val faceVertex =
            detectVertex!!.copyOfRange(faceIndex * meshSize * 3, (faceIndex + 1) * meshSize * 3)
        setBaseVertex(faceVertex, 3)
        // 2.2. Mask-vertex as texture-vertex
        setOverlayVertex(maskVertex!!, 2)
        //val alphaVertexBuffer = FaceUtils.noVtxAlpha(triangIndex)
        val alphaVertexBuffer = FaceUtils.getVtxAlpha(
            maskMode.alphaMode,
            meshSize,
            skinPercent,
            eyePercent,
            nosePercent,
            mouthPercent
        )
        bindBufferLocation(alphaVertexBuffer, locVtxAlpha, 1)

        if (maskMode == MaskMode.SWAP_FRAME) {
            GLES20.glUniform3fv(locSkinColor, 1, FloatBuffer.wrap(skinColor!!))
        }

        setDrawMode(2f)// maskMode.drawMode)
        uniformScale()
        // 3. Activate image-texture and mask-texture
        // 3.1. Bind image-texture
        bindBaseTexture(textureId)
        bindOverlayTexture(maskTexture)
        // 4. Enable depth for rendering 3d content
        draw3DTriangleByIndex(triangIndex)

        clearGLES()
    }

    open fun drawBaseByTriangle(
        textureId: Int, triangIndex: ShortArray, faceIndex: Int = 0
    ) {
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        val faceVertex =
            detectVertex!!.copyOfRange(faceIndex * meshSize * 3, (faceIndex + 1) * meshSize * 3)
        setBaseVertex(faceVertex, 3)

        setDrawMode(MaskMode.BASE_TEXT.drawMode)
        uniformScale()
        // 3. Activate image-texture and mask-texture
        // 3.1. Bind image-texture
        bindBaseTexture(textureId)
        // 4. Enable depth for rendering 3d content
        draw3DTriangleByIndex(triangIndex)

        clearGLES()
    }

    protected fun draw3DTriangleByIndex(drawIndex: ShortArray) {
        // @Todo: force to enable depth to draw 3d-mask
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDepthMask(true)
        // 5. Draw face-triangle mesh
        // 5.1. Init triangle-indices
        drawTriangleByIndex(drawIndex)
        // 6. Clear GLES
        //@Todo: Force to disable depth drawing mask - to overlay on previous drawing
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
    }

    fun drawTriangleByIndex(drawIndex: ShortArray) {
        val triangleIndices = initShortBuffer(drawIndex)
        triangleIndices.position(0)
        // 5.2. Draw triangles
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            drawIndex.size,
            GLES20.GL_UNSIGNED_SHORT, triangleIndices
        )
    }

    fun clearGLES() {
        GLES20.glDisableVertexAttribArray(locAVertex)
        GLES20.glDisableVertexAttribArray(locATexCoord)
        GLES20.glDisableVertexAttribArray(locVtxAlpha)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    fun uniformScale() {
        GLES20.glUniform1f(locScaleType, scaleType.value)

        GLES20.glUniform1f(locTexelWidth, 1f / outputWidth)
        GLES20.glUniform1f(locTexelWidth, 1f / outputHeight)

        GLES20.glUniform1f(locRatioWidth, ratioWidth)
        GLES20.glUniform1f(locRatioHeight, ratioHeight)
//        Log.e("GPURender", "Ratio || w:: $ratioWidth  || h:: $ratioHeight")
        if (isFlip) {
            flip = -1f
            GLES20.glUniform1f(locIsFlip, -1.0f)
        } else {
            flip = 1f
            GLES20.glUniform1f(locIsFlip, 1.0f)
        }

        GLES20.glUniform1f(locIsFlipY, flipY)
    }

    private fun initShortBuffer(data: ShortArray): ShortBuffer {
        val result = ByteBuffer
            .allocateDirect(data.size * Constants.BYTES_PER_SHORT)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()

        result.put(data)
        return result
    }

    private fun arrayToBuffer(data: FloatArray): FloatBuffer {
        val result = ByteBuffer
            .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        result.put(data)
        return result
    }

    protected fun bindBufferLocation(data: FloatBuffer, location: Int, size: Int) {
        data.position(0) // data start from 0
        GLES20.glVertexAttribPointer(
            location, size, GLES20.GL_FLOAT, false, 0,
            data
        )
        GLES20.glEnableVertexAttribArray(location)
    }
}
