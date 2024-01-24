package com.plugin.filters.plugin_filters.mlkit.processor.game

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.util.Log
import com.plugin.filters.plugin_filters.mlkit.processor.DebugView
import com.plugin.filters.plugin_filters.mlkit.processor.MusicControl
import com.plugin.filters.plugin_filters.mlkit.processor.game.sticker.StickerObject
import jp.co.cyberagent.android.gpuimage.ml.FaceSwapFilter
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import jp.co.cyberagent.android.gpuimage.ml.ScaleType
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils
import java.util.HashMap


/**
 * Update-2: with onset data

 * 1. Keep track current tile by list of tile
 * There can be more than 1 tile on screen
 *
 * 2. Check score for list of tile
 * Scored-tile will not be score again
 * Hit tile will update color to green to let user know
 *
 * 3. Matching score is evaluate by match-position
 * Draw hit-bar to highlight match-position
 *
 * 4. Generate tile by onset time data
 *
 *
 * 5. Some GAME-FILTER with left-right sensitive
 * -->> Focus on solving with only front or back camera
 * -->> Working with both camera require to handle
 * the different between left-right of front people and back people

 */
open class GameFilter(context: Context, mode: MaskMode):
    FaceSwapFilter(context, mode)
{

    var stickerTexture: HashMap<String, StickerObject?> = HashMap()

    var assetTexture: HashMap<String, GameObject?> = HashMap()


    var debug: DebugView? = null

    // 1. Game timer
    var currentTick: Int = 0
    var currentTime: Long = 0
    var timeDelta: Float = 0f
    // 2. Game phase
    var gameState: FACE_GAME_STATE = FACE_GAME_STATE.INTRO
    var gameStep: Int = 0
    var timeline: HashMap<String, Float> = HashMap()
    var logicState: HashMap<String, HashMap<String, Float>> = HashMap()


    // 4. Music
    var musicControl: MusicControl? = null
    var donePrepare = 0
    var errMsg: String = ""
    var donePlay = false
    var onset:FloatArray? = null
    var onsetAgg:FloatArray? = null
    var doneOnset = false


    // @Todo: 1.initialize game-object texture from asset images - in future might use bitmap from service

    fun initAssets(assets: HashMap<String, GameObject?>) {
//        assetTexture = HashMap()
        for (entry in assets.entries.iterator()) {
            if(entry.value != null) {
                Log.e("FaceDance", "init game-object ${entry.key} ${entry.value}")
                setBitmap(entry.key, entry.value!!.image)
                assetTexture!![entry.key] = entry.value
            }
        }
    }

    fun initKey(key: String, value: Float) {
        if (!timeline.containsKey(key)) {
            timeline[key] = value
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




    // @Todo: 2.initialize sticker-object texture from outside bitmap - in future might use bitmap from service
    fun initSticker(sticker: StickerObject) {
        assetTexture!![sticker.id] = sticker
        setBitmap(sticker.id, sticker.image)
    }






    fun updateTime() {
        currentTick += 1
        val lastTime = currentTime
        currentTime = System.currentTimeMillis()
        timeDelta = if(lastTime > 0) {
            (currentTime - lastTime).toFloat()
        } else {
            50f
        }

        Log.e("FaceAnim", "Time:: " +
                " tick:$currentTick" +
                " lastTime:$lastTime" +
                " currentTime:$currentTime" +
                " timeDelta:$timeDelta")
    }


    open fun showDebug(message:String) {
        debug!!.updateDebug(message)
    }


    /**
     * Draw by using paint + canvas + initialized every draw is resource exhausting
     * And cause DeadObjectException
     * - Simply continuous update to android text-view
     */
    open fun debugLogic() {
        if(debug != null) {

            if (detectVertex == null) {
                debug!!.updateDebug("No face detected")
            } else {
                val faceAngle = GameUtils.getFaceAngle(detectVertex!!, isFlip)

                val index = 1
                val x = detectVertex!![3 * index]
                val y = detectVertex!![3 * index + 1]
                debug!!.updateDebug(
                    "" +
                            "Eye-xy ${faceAngle[0]} | " +
                            "Eye-xz ${faceAngle[1]} \n" +
                            "Nose-yz ${faceAngle[2]} | " +
                            "Nose-ratio ${faceAngle[3]} | " +
                            "Center $x $y\n" +
                            "Eye:: Left ${faceAngle[4]} \n" +
                            "Right ${faceAngle[5]} | " +
                            "Mouth ${faceAngle[6]} \n" +
                            "Lip:: inLow ${faceAngle[7]} || ${faceAngle[8]} \n" +
                            "inUp ${faceAngle[9]} || ${faceAngle[10]} \n" +
                            "Lip:: outLow ${faceAngle[11]} || ${faceAngle[12]} \n" +
                            "outUp ${faceAngle[13]} || ${faceAngle[14]} \n" +
                            ""
                )
            }
        }
    }


    fun drawProgress(value: Int, max: Int, textName: String, scaleType:ScaleType,
                   startX:Float, startY:Float,
                   endX:Float, endY:Float
    ) {

        // 2. Calculate position for each element
        val total = max


        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        // 1. Draw relative size
        val textureObj = assetTexture!![textName]
        val w = textureObj!!.width.toFloat() * total
        val h = textureObj!!.height.toFloat()

        // Consider width as standard - scale by outputWidth
        //val pos = GameObject.relativeWBottomCenter(0.8f, 0.9f, w, h)


        val textRatio = GameUtils.calTextRatio(
            w, h,
            (endX - startX) * outputWidth, // absolute width
            (endY - startY) * outputHeight // absolute height
        )
        val textRatioW = textRatio[0]
        val textRatioH = textRatio[1]

        val nW =
            if(scaleType==ScaleType.CENTER_CROP) (endX - startX) * textRatioW
            else if(scaleType==ScaleType.CENTER_INSIDE) (endX - startX) / textRatioH
            else endX - startX

        val nH =
            if(scaleType==ScaleType.CENTER_CROP) (endY - startY) * textRatioH
            else if(scaleType==ScaleType.CENTER_INSIDE) (endY - startY) / textRatioW
            else endY - startY

        val centerX = startX + (endX - startX) / 2f
        val centerY = startY + (endY - startY) / 2f



        val nStartX = centerX - nW/2
        val nStartY = centerY - nH/2

        val dW = nW / total

        val num = Math.min(max, value)
        for(i in 0 until num) {
            drawTextInside(
                textName,
                nStartX + i * dW, nStartY, nStartX + (i + 1) * dW, nStartY + nH,
                ScaleType.CENTER_FIT
            )
        }
    }


    fun drawString(value: String, scaleType:ScaleType,
                   startX:Float, startY:Float,
                   endX:Float, endY:Float
    ) {
        // 1. Get char list
        var chars = java.util.ArrayList<String>()
        for( s in value) {
            chars.add(s.toString())
        }

        // 2. Calculate position for each char
        val total = chars.size


        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        // 1. Draw relative size
        val w:Float = (GameUtils.CHAR_W * total).toFloat()
        val h:Float = GameUtils.CHAR_H.toFloat()


        val textRatio = GameUtils.calTextRatio(
            w, h,
            (endX - startX) * outputWidth, // absolute width
            (endY - startY) * outputHeight // absolute height
        )
        val textRatioW = textRatio[0]
        val textRatioH = textRatio[1]

        val nW =
            if(scaleType==ScaleType.CENTER_CROP) (endX - startX) * textRatioW
            else if(scaleType==ScaleType.CENTER_INSIDE) (endX - startX) / textRatioH
            else endX - startX

        val nH =
            if(scaleType==ScaleType.CENTER_CROP) (endY - startY) * textRatioH
            else if(scaleType==ScaleType.CENTER_INSIDE) (endY - startY) / textRatioW
            else endY - startY

        val centerX = startX + (endX - startX) / 2f
        val centerY = startY + (endY - startY) / 2f



        val nStartX = centerX - nW/2
        val nStartY = centerY - nH/2

        val dW = nW / total

        val charText = GameUtils.fontCharTexture()
        for(i in 0 until total) {
            drawTextInside(
                GameUtils.FONT_PATH,
                nStartX + i * dW, nStartY, nStartX + (i + 1) * dW, nStartY + nH,
                ScaleType.CENTER_FIT,
                charText[chars[i]]!!
            )
        }
    }


    fun drawNumber(value: Int, scaleType:ScaleType,
                           startX:Float, startY:Float,
                           endX:Float, endY:Float
    ) {
        // 1. Get digit list
        var digits = java.util.ArrayList<Int>()
        var num = value
        if(num > 0) {
            while (num > 0) {
                val digit = num % 10
                digits.add(0, digit)
                num /= 10;
            }
        } else {
            digits.add(0)
        }

        // 2. Calculate position for each digit
        val total = digits.size


        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        // 1. Draw relative size
        val textureObj = assetTexture!!["game/number/0.png"]
        val w = textureObj!!.width.toFloat() * total
        val h = textureObj!!.height.toFloat()

        // Consider width as standard - scale by outputWidth
        //val pos = GameObject.relativeWBottomCenter(0.8f, 0.9f, w, h)


        val textRatio = GameUtils.calTextRatio(
            w, h,
            (endX - startX) * outputWidth, // absolute width
            (endY - startY) * outputHeight // absolute height
        )
        val textRatioW = textRatio[0]
        val textRatioH = textRatio[1]

        val nW =
            if(scaleType==ScaleType.CENTER_CROP) (endX - startX) * textRatioW
            else if(scaleType==ScaleType.CENTER_INSIDE) (endX - startX) / textRatioH
            else endX - startX

        val nH =
            if(scaleType==ScaleType.CENTER_CROP) (endY - startY) * textRatioH
            else if(scaleType==ScaleType.CENTER_INSIDE) (endY - startY) / textRatioW
            else endY - startY

        val centerX = startX + (endX - startX) / 2f
        val centerY = startY + (endY - startY) / 2f



        val nStartX = centerX - nW/2
        val nStartY = centerY - nH/2

        val dW = nW / total
        for(i in 0 until total) {
            drawTextInside(
                "game/number/${digits[i]}.png",
                nStartX + i * dW, nStartY, nStartX + (i + 1) * dW, nStartY + nH,
                ScaleType.CENTER_FIT
            )
        }
    }



    fun drawGameObjectInside(gObj: GameObject) {
        drawTextInside(
            gObj,
            floatArrayOf(
                0f, 0f,
                0f, 1f,
                1f, 0f,
                1f, 1f
            )
        )
    }

    fun drawTextInside(
        textName: String,
        startX:Float = 0.0f,
        startY:Float = 0.8f,
        endX:Float = 1f,
        endY:Float = 0.9f,
        scaleType: ScaleType = ScaleType.CENTER_INSIDE
    ) {
        // 0. Game object
        val textureObj = assetTexture!![textName]!!
        textureObj.textScale = scaleType
        textureObj.x = startX
        textureObj.y = startY
        textureObj.w = endX - startX
        textureObj.h = endY - startY
        drawTextInside(
            textureObj,
            floatArrayOf(
                0f, 0f,
                0f, 1f,
                1f, 0f,
                1f, 1f
            )
        )
    }

    private fun drawTextInside(
        textName: String,
        startX:Float = 0.0f,
        startY:Float = 0.8f,
        endX:Float = 1f,
        endY:Float = 0.9f,
        scaleType: ScaleType = ScaleType.CENTER_INSIDE,
        frameVertex: FloatArray = floatArrayOf(
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
        )
    ) {
        // 0. Game object
        val textureObj = assetTexture!![textName]!!
        textureObj.textScale = scaleType
        textureObj.x = startX
        textureObj.y = startY
        textureObj.w = endX - startX
        textureObj.h = endY - startY
        drawTextInside(textureObj,
            frameVertex
        )
    }


    private fun drawTextInside(
        gObj: GameObject,
        frameVertex: FloatArray = floatArrayOf(
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
        )
    ) {
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex

        gObj.drawGLSize(outputWidth.toFloat(), outputHeight.toFloat())


        val centerX = gObj.x + gObj.w / 2f
        val centerY = gObj.y + gObj.h / 2f

        val positionVertex = floatArrayOf(
            centerX - gObj.drawW/2, centerY - gObj.drawH/2,
            centerX - gObj.drawW/2, centerY + gObj.drawH/2,
            centerX + gObj.drawW/2, centerY - gObj.drawH/2,
            centerX + gObj.drawW/2, centerY + gObj.drawH/2
        )

        setBaseVertex(positionVertex, 2)

        var pos = ""
        positionVertex.forEach { pos += " $it" }

        Log.e("FaceScale", "drawTextInside::" +
                "\n textureObj:: ${gObj}" +
                "\n scaleType:: ${gObj.textScale}" +
                "\n position:: (${pos})")

        // 2.2. Mask-vertex as texture-vertex

        setOverlayVertex(frameVertex, 2)

        setDrawMode(MaskMode.OVERLAY_TEXT.drawMode)

        // 3. Activate image-texture
        val textureId = gObj.textureId
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


    fun drawTextInside(
        textName:String,
        posVertex: Array<DoubleArray>,
        frameVertex: FloatArray = floatArrayOf(
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
        )
    ) {

        val gObj: GameObject = assetTexture!![textName]!!
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex


        val positionVertex = FloatArray(12)
        for(i in 0 until 4) {
            val row = posVertex[i]
            for( j in 0 until 3) {
                positionVertex[i*3 + j] = row[j].toFloat()
            }
        }

        setBaseVertex(positionVertex, 3)

        var pos = ""
        positionVertex.forEachIndexed { index, it ->
            if(index % 3 == 0)
                pos += "\n"
            pos += " $it" //${String.format("%.2f", it)}"

        }

        Log.e("StickerScale", "drawTextInside::" +
                "\n textureObj:: ${gObj}" +
                "\n scaleType:: ${gObj.textScale}" +
                "\n position:: (${pos})")

        // 2.2. Mask-vertex as texture-vertex

        setOverlayVertex(frameVertex, 2)

        setDrawMode(MaskMode.OVERLAY_TEXT.drawMode)

        // 3. Activate image-texture
        val textureId = gObj.textureId
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


    fun drawTextInside(
        textName:String,
        posVertex: FloatArray, posSize: Int,
        frameVertex: FloatArray = floatArrayOf(
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
        ),
        drawIndex: ShortArray = shortArrayOf(
            0, 1, 2,
            1, 2, 3
        )
    ) {

        val gObj: GameObject = assetTexture!![textName]!!
        // 2. Initialize vertex and texture-coordinates
        // 2.1. Face-vertex as vertex
        setBaseVertex(posVertex, posSize)

        var pos = ""
        posVertex.forEachIndexed { index, it ->
            if(index % posSize == 0)
                pos += "\n"
            pos += " $it" //${String.format("%.2f", it)}"

        }

        Log.e("StickerScale", "drawTextInside::" +
                "\n textureObj:: ${gObj}" +
                "\n scaleType:: ${gObj.textScale}" +
                "\n position:: (${pos})")

        // 2.2. Mask-vertex as texture-vertex

        setOverlayVertex(frameVertex, 2)

        setDrawMode(MaskMode.OVERLAY_TEXT.drawMode)

        // 3. Activate image-texture
        val textureId = gObj.textureId
        if(textureId == null) {
            clearGLES()
            return
        }
        bindOverlayTexture(textureId)

        // 5. Draw face-triangle mesh
        // 5.1. Init triangle-indices

        drawTriangleByIndex(drawIndex)

        // 6. Clear GLES
        clearGLES()

    }

}