package com.skymeta.arface.mlkit.processor.game.face.facedance

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.util.Log
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameGroup
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameObject
import com.plugin.filters.plugin_filters.mlkit.processor.game.SizeType
import jp.co.cyberagent.android.gpuimage.ml.FaceSwapFilter
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import jp.co.cyberagent.android.gpuimage.ml.ScaleType
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils
import java.nio.FloatBuffer
import java.util.HashMap
import kotlin.math.roundToInt

class FaceGameFilter(context: Context, mode: MaskMode):
    FaceSwapFilter(context, mode) {
    private var assetTexture: java.util.HashMap<String, GameObject?>? = null


    private var locTextRatioW: Int = 0
    private var locTextRatioH: Int = 0
    private var locTextScale: Int = 0
    private var textRatioH: Float = 1.0f
    private var textRatioW: Float = 1.0f
    private var textScale: ScaleType = ScaleType.CENTER_INSIDE

    private var faceTiles = arrayOf(
        "face_closelefteye.png",
        "face_closerighteye.png",
        "face_down.png",
        "face_left.png",
        "face_mouthtoleft.png",
        "face_mouthtoleft1.png",
        "face_mouthtoright.png",
        "face_mouthtoright1.png",
        "face_openmouth.png",
        "face_right.png",
        "face_sad.png",
        "face_smile1.png",
        "face_smile2.png",
        "face_up.png",
    )

    // 1. Game timer
    private var currentTick: Int = 0
    private var currentTime: Long = 0
    private var timeDelta: Float = 0f
    // 2. Game phase

    // 3. Game object pool
    private var faceTileObjects = java.util.ArrayList<GameObject>()

    private var currentFaceTile: GameObject? = null
    private var isShowFaceTile = false
    private var isPlayPhase = false


    override fun onInit() {
        super.onInit()

        locTextRatioW = GLES20.glGetUniformLocation(program, "u_textRatioW")
        locTextRatioH = GLES20.glGetUniformLocation(program, "u_textRatioH")
        locTextScale = GLES20.glGetUniformLocation(program, "u_textScale")

//        setScaleType(ScaleType.NO_SCALE)

        initGameObjectPool()

    }

    private fun initGameObjectPool() {
        for(i in faceTiles.indices) {
            var startX = FloatArray(8) {(it+1)*0.1f}.random()
            var width = 0.1f
            var startY = -1.0f
            var height = 0.1f

            val textId = faceTiles[i]
            val faceObj = assetTexture!![textId]!!
            val cloneObj = GameObject(
                faceObj
            )
            cloneObj.textScale = ScaleType.CENTER_INSIDE
            cloneObj.x = startX
            cloneObj.y = startY
            cloneObj.w = width
            cloneObj.h = height
            faceTileObjects.add(cloneObj)
        }


    }


    fun uniformTextScale() {
        GLES20.glUniform1f(locTextScale, textScale.value)
        GLES20.glUniform1f(locTextRatioW, textRatioW)
        GLES20.glUniform1f(locTextRatioH, textRatioH)
    }

    fun setTextScale(scaleType: ScaleType,
                     imageWidth: Float, imageHeight: Float,
                     outputWidth: Float, outputHeight: Float,
    ) {
        this.textScale = scaleType
        val ratio1: Float = outputWidth / imageWidth
        val ratio2: Float = outputHeight / imageHeight
        val ratioMax = Math.max(ratio1, ratio2)
        val imageWidthNew = (imageWidth * ratioMax).roundToInt()
        val imageHeightNew = (imageHeight * ratioMax).roundToInt()

        textRatioW = imageWidthNew / outputWidth
        textRatioH = imageHeightNew / outputHeight

        Log.e("FaceDance", "Text-Scale:: $textScale $textRatioW $textRatioH" +
                "\n" +
                "Origin-Image: $imageWidth $imageHeight" +
                "\n" +
                "Origin-Box: $outputWidth $outputHeight")

    }


    fun initAssets(assets: java.util.HashMap<String, GameObject?>) {
        assetTexture = HashMap()
        for (entry in assets.entries.iterator()) {
            if(entry.value != null) {
                Log.e("FaceDance", "init game-object ${entry.key} ${entry.value}")
                setBitmap(entry.key, entry.value!!.image)
                assetTexture!![entry.key] = entry.value
            }

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
//            Log.e("FaceDance", "init asset condition:: " +
//                    "${!assetTexture!!.containsKey(key)} " +
//                    "${assetTexture!![key]!!.textureId == OpenGlUtils.NO_TEXTURE}")
            if ( !assetTexture!!.containsKey(key) ||
                (assetTexture!![key]!!.textureId == OpenGlUtils.NO_TEXTURE)
            ) {
                if (bitmap == null || bitmap.isRecycled) {
                    return@Runnable
                }
                GLES20.glActiveTexture(GLES20.GL_TEXTURE3)
                assetTexture!![key]!!.textureId =
                    OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, false)

                Log.e("FaceDance", "init asset ${key} ${assetTexture!![key]}")
            } else {

            }
        })
    }

    override fun onDraw(
        textureId: Int, cubeBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ) {
        currentTick += 1
        val lastTime = currentTime
        currentTime = System.currentTimeMillis()
        if(lastTime > 0) {
            timeDelta = (currentTime - lastTime).toFloat()
        } else {
            timeDelta = 50f
        }

        Log.e("FacePlay", "Time:: " +
                " tick:$currentTick" +
                " lastTime:$lastTime" +
                " currentTime:$currentTime" +
                " timeDelta:$timeDelta")

        super.onDraw(textureId, cubeBuffer, textureBuffer)

        // Draw game logic

        // 1. prepare phase
        drawPrepare()
//        drawIntroGroup()

        // 2. play phase
        if(currentTick > 10) {
            drawPlay()
        }

        // 3. finish phase
        // Finish and signal camera-activity to done-activity
        // Done-activity:
        // - Auto replay
        // - Share + Tag to challenge friends
        // - Play again
//        drawFinish(textureId, cubeBuffer, textureBuffer)
    }

    private fun drawIntroGroup() {
        val introPos = GameObject.relativeWBottomCenter(0.8f, 0.9f, 0.8f, 0.1f)
        var introGroup = GameGroup(
            sizeType = SizeType.WIDTH,
            gObj = GameObject(id = "",
                assetPath = "",
                textureId = OpenGlUtils.NO_TEXTURE,
                x = introPos[0],
                y = introPos[1],
                w = introPos[2],
                h = introPos[3],
            ),
            children = null
        )

        var children = java.util.ArrayList<GameGroup>()

        val oBeginIcon = assetTexture!!["ic_beginboard.png"]!!
        Log.e("FaceGame", "ic_beginboard origin:: ${oBeginIcon.width} ${oBeginIcon.height}")
        val beginIconPos = GameObject.relativeHMiddleY(
            1f, 0f,
            oBeginIcon.width.toFloat(), oBeginIcon.height.toFloat()
        )
        var beginIcObj = GameGroup(
            sizeType = SizeType.HEIGHT,
            GameObject(id = oBeginIcon.id,
                assetPath = oBeginIcon.assetPath,
                textureId = oBeginIcon.textureId,
                width = oBeginIcon.width,
                height = oBeginIcon.height,
                image = oBeginIcon.image,
                x = beginIconPos[0],
                y = beginIconPos[1],
                w = beginIconPos[2],
                h = beginIconPos[3],
                sizeType = SizeType.HEIGHT
            )
        )

        val oBeginBrd = assetTexture!!["begin_board.png"]!!
        Log.e("FaceGame", "begin_board origin:: ${oBeginBrd.width} ${oBeginBrd.height}")
        val beginBrdPos = GameObject.relativeWMiddleY(
            1f - beginIcObj.gObj!!.w, beginIcObj.gObj!!.x + beginIcObj.gObj!!.w,
            oBeginBrd.width.toFloat(), oBeginBrd.height.toFloat()
        )
        var beginBrdObj = GameGroup(
            sizeType = SizeType.HEIGHT,
            GameObject(id = oBeginBrd.id,
                assetPath = oBeginBrd.assetPath,
                textureId = oBeginBrd.textureId,
                width = oBeginBrd.width,
                height = oBeginBrd.height,
                image = oBeginBrd.image,
                x = beginBrdPos[0],
                y = beginBrdPos[1],
                w = beginBrdPos[2],
                h = beginBrdPos[3],
                sizeType = SizeType.WIDTH,
            )
        )

        children.add(beginIcObj)
        children.add(beginBrdObj)



        introGroup.children = children
        drawGameGroup(0f, 0f, 1f, 1f, introGroup)

    }

    private fun drawGameGroup(x:Float, y:Float, w:Float, h:Float, entity: GameGroup) {

        if(entity.gObj != null) {
            // Draw from parent size
            drawGameObject(x, y, w, h, entity.gObj!!)
            if(entity.children != null) {
                for(gameGroup in entity.children!!) {
                    // Calculate absolute size for children
                    var oX = x + entity.gObj!!.x
                    var oY = y + entity.gObj!!.y
                    var oW =
                        if(entity.sizeType == SizeType.HEIGHT)
                            h * entity.gObj!!.w
                        else
                            w * entity.gObj!!.w
                    var oH =
                        if(entity.sizeType == SizeType.WIDTH)
                            w * entity.gObj!!.h
                        else
                            h * entity.gObj!!.h
                    Log.e("FaceGame", "Draw -|${entity.gObj!!.id}|- $oX $oY $oW $oH" +
                            "\n" +
                            "Origin ${entity.gObj!!.x} ${entity.gObj!!.y} ${entity.gObj!!.w} ${entity.gObj!!.h}")
                    drawGameGroup(oX, oY, oW, oH, gameGroup)
                }
            }
        } else {
            Log.e("FaceGame", "No Draw ")
        }




    }

    private fun drawGameObject(x:Float, y:Float, w:Float, h:Float, entity: GameObject) {
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex

        // 1. Calculate absolute size from parent
        var oX = x + entity.x
        var oY = y + entity.y
        var oW =
            if(entity.sizeType == SizeType.HEIGHT)
                h * entity.w
            else
                w * entity.w
        var oH =
            if(entity.sizeType == SizeType.WIDTH)
                w * entity.h
            else
                h * entity.h

        val positionVertex = floatArrayOf(
            oX, oY,
            oX, oY + oH,
            oX + oW, oY,
            oX + oW, oY + oH,
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

        setDrawMode(MaskMode.OVERLAY_TEXT.drawMode)

        uniformScale()

        // 3. Activate image-texture
        val textureId = entity.textureId
        if(textureId == null || textureId == OpenGlUtils.NO_TEXTURE) {
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

    private fun drawPrepare() {
        Log.e("FaceDance", "Draw Prepare")
        drawTextInside(
            "begin_board.png",
            0.3f,
            0.8f,
            1f,
            0.9f,
            ScaleType.CENTER_CROP
        )
        drawTextInside(
            "ic_beginboard.png",
            0.0f,
            0.8f,
            0.3f,
            0.9f,
            ScaleType.CENTER_CROP
        )
    }

    private fun drawGameObjectInside(gObj: GameObject) {
        drawTextInside(
            gObj,
            gObj.x, gObj.y,
            gObj.x+gObj.w, gObj.y+gObj.h,
            gObj.textScale
        )
    }

    private fun drawTextInside(
        textName: String,
        startX:Float = 0.0f,
        startY:Float = 0.8f,
        endX:Float = 1f,
        endY:Float = 0.9f,
        scaleType: ScaleType = ScaleType.CENTER_INSIDE
    ) {
        // 0. Game object
        val textureObj = assetTexture!![textName]!!
        drawTextInside(textureObj, startX, startY, endX, endY, scaleType)
    }


    private fun drawTextInside(
        textureObj: GameObject,
        startX:Float = 0.0f,
        startY:Float = 0.8f,
        endX:Float = 1f,
        endY:Float = 0.9f,
        scaleType: ScaleType = ScaleType.CENTER_INSIDE
    ) {

        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        // 1. Draw relative size
        val w = textureObj.width.toFloat()
        val h = textureObj.height.toFloat()

        // Consider width as standard - scale by outputWidth
        //val pos = GameObject.relativeWBottomCenter(0.8f, 0.9f, w, h)



        setTextScale(scaleType, w, h,
            (endX - startX) * outputWidth, // absolute width
            (endY - startY) * outputHeight // absolute height
        )

        val newWidth =
            if(scaleType==ScaleType.CENTER_CROP) (endX - startX) * textRatioW
            else if(scaleType==ScaleType.CENTER_INSIDE) (endX - startX) / textRatioH
            else endX - startX

        val newHeight =
            if(scaleType==ScaleType.CENTER_CROP) (endY - startY) * textRatioH
            else if(scaleType==ScaleType.CENTER_INSIDE) (endY - startY) / textRatioW
            else endY - startY

        val centerX = startX + (endX - startX) / 2f
        val centerY = startY + (endY - startY) / 2f

        val positionVertex = floatArrayOf(
            centerX - newWidth/2, centerY - newHeight/2,
            centerX - newWidth/2, centerY + newHeight/2,
            centerX + newWidth/2, centerY - newHeight/2,
            centerX + newWidth/2, centerY + newHeight/2
        )

        setBaseVertex(positionVertex, 2)

//        uniformTextScale()

        // 2.2. Mask-vertex as texture-vertex

        val frameVertex = floatArrayOf(
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
        )
        setOverlayVertex(frameVertex, 2)

        setDrawMode(MaskMode.OVERLAY_TEXT.drawMode)


        // 3. Activate image-texture
        val textureId = textureObj.textureId
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
    ) {
        if(currentFaceTile == null) {
            currentFaceTile = faceTileObjects.random()
            var startX = FloatArray(8) {(it+1)*0.1f}.random()
            var width = 0.2f
            var startY = 0.0f
            var height = 0.2f

            currentFaceTile!!.textScale = ScaleType.CENTER_CROP
            currentFaceTile!!.x = startX
            currentFaceTile!!.y = startY
            currentFaceTile!!.w = width
            currentFaceTile!!.h = height
        } else {
            if(currentFaceTile!!.y < 1f) {
                Log.e("FacePlay", "before:: ${currentFaceTile!!.y}")
                // Move faceTile down
                var width = 0.2f
                // Move 20% of screen-height every second
                val showTime = 4f // 4 seconds alive on screen
                val speed = (timeDelta / 1000f) / 4f
                var startY = currentFaceTile!!.y + speed
                var height = 0.2f

                currentFaceTile!!.textScale = ScaleType.CENTER_INSIDE
                currentFaceTile!!.y = startY

                Log.e("FacePlay", "current:: ${currentFaceTile!!.y}")

                drawGameObjectInside(currentFaceTile!!)
            } else {
                currentFaceTile = null
            }
        }

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

        setDrawMode(MaskMode.OVERLAY_TEXT.drawMode)

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