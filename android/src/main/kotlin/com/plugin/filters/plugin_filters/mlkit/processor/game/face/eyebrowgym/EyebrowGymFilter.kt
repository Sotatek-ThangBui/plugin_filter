package com.plugin.filters.plugin_filters.mlkit.processor.game.face.eyebrowgym

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.Log
import com.plugin.filters.plugin_filters.mlkit.processor.MusicCaller
import com.plugin.filters.plugin_filters.mlkit.processor.game.FACE_GAME_STATE
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameObject
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameUtils
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import jp.co.cyberagent.android.gpuimage.ml.ScaleType
import java.nio.FloatBuffer
import java.text.DecimalFormat
import java.util.HashMap
import kotlin.math.abs


/**
 * First experiment logic with single slow face-tile dropping
 */
class EyebrowGymFilter(context: Context, mode: MaskMode):
    GameFilter(context, mode),
    MusicCaller
{

    private var endGameVFX: GameObject? = null
    private var isGameInit = false
    // 4. Music


    override fun musicDonePrepare() {
    }
    override fun musicDonePlay() {
    }
    override fun musicErrorPrepare(s: String) {
    }

    override fun onInit() {
        super.onInit()
        // @Todo: Initialize is call every re-add filter to renderer
        if(!isGameInit) {
            isGameInit = true
        }
    }

    private fun initVFX() {
        // Must init on play game - to wait texture finish init
        if(!timeline.containsKey("init_vfx")) {
            initGameObjectPool()
            timeline["init_vfx"] = 1f
        }
    }

    private fun initGameObjectPool() {

        // 1. Init end-game VFX
        val path = "game/vfx/pink_star/00.png"
        val star0 = assetTexture!![path]!!
        endGameVFX = GameObject(
            star0
        )

        for(i in 0..36) {
            val s = String.format("%02d", i)
            val text = assetTexture!!["game/vfx/pink_star/$s.png"]!!
            endGameVFX!!.animTexts.add(text.textureId)
        }

        endGameVFX!!.textureId = endGameVFX!!.animTexts[0]
        endGameVFX!!.animRate = 100f
        endGameVFX!!.animTimer = 0f

        endGameVFX!!.textScale = ScaleType.CENTER_CROP
        endGameVFX!!.x = 0f
        endGameVFX!!.y = 0f
        endGameVFX!!.w = 1f
        endGameVFX!!.h = 1f

    }

    private fun detectVertex(index: Int): DoubleArray {
        return doubleArrayOf(
            detectVertex!![index*3+0].toDouble(),
            detectVertex!![index*3+1].toDouble(),
            detectVertex!![index*3+2].toDouble(),
        )
    }

    private fun drawSticker(baseTextId: Int) {
        drawTCPSticker()
        drawIrisStickerV3(baseTextId)
    }

    private fun drawIrisSticker(faceIndex: Int = 0) {
        // Ver 1 - simple draw - not care about eye - simple draw iris at right position first
        // Need to convert mlkit detector to mediapipe detector
        val text = "sticker/AttackOnTitan-ReinerBraun-iris-001.png"
        if(detectVertex != null) {

            var leftIris = intArrayOf(
                469, 470, 471, 472
            )
            leftIris.forEachIndexed { index, value ->
                leftIris[index] = value + faceIndex * meshSize
            }

            var rightIris = intArrayOf(
                474, 475, 476, 477
            )
            rightIris.forEachIndexed { index, value ->
                rightIris[index] = value + faceIndex * meshSize
            }

            var textVert = floatArrayOf(
                0f, 1f,
                0f, 0f,
                1f, 0f,
                1f, 1f
            )

            var drawIndex: ShortArray = shortArrayOf(
                0, 1, 2,
                2, 3, 0
            )

            if(isFlip) {
                val tmp = leftIris
                leftIris = rightIris
                rightIris = tmp
            }

            val irises = arrayOf(leftIris, rightIris)

            for(iris in irises) {
                val posVertex = FloatArray(iris.size*3)
                for (i in iris.indices) {
                    val index = iris[i]
                    posVertex[i * 3] =
                        if(isFlip)
                            1 - detectVertex!![index * 3]
                        else
                            detectVertex!![index * 3]
                    posVertex[i * 3 + 1] = detectVertex!![index * 3 + 1]
                    posVertex[i * 3 + 2] = detectVertex!![index * 3 + 2]

//                    Log.e("Iris", "Iris $i - index $index\n" +
//                            "${posVertex[i * 3]} ${posVertex[i * 3+1]} ${posVertex[i * 3+2]}")
                }

                drawTextInside(text, posVertex, 3, textVert, drawIndex)
            }
        }
    }

    private fun drawIrisStickerV3(baseTextId: Int) {
        // Ver 3 - Handle eye-region - no draw iris out of eye-region
        // Simply - draw extended-eye-region - exclude eye-region
        // Handle both case of mask + no-mask
        // Fix issue by fix shader + combining mask+base with no-alpha for covering out-eye-region


        // @todo: noted - baseTextId is whole texture for contain current drawing
        // BaseTextId is not the origin camera texture

        // Ver4 - Handle multiple face drawing
        // Input - faceIndex -->> update detectVertex by faceIndex


        val text = "sticker/AttackOnTitan-ReinerBraun-iris-001.png"
        if(detectVertex != null) {

            for(i in 0 until numFace) {
                // 1. Draw iris
                //drawIrisSticker(i)

                // 2. Draw outer eye region


                if (maskVertex != null && getMaskBitmap() != null) {
                    // 2.2. If mask -->> Draw outer eye region of mask
                    drawOutEyeMask(baseTextId, i)
                } else if (maskVertex == null && getMaskBitmap() == null) {
                    // 2.1. If no mask -->> Draw outer eye region of base
                    drawOutEyeFace(baseTextId, i)
                }
            }
        }
    }

    private fun drawOutEyeMask(textureId: Int, faceIndex: Int = 0) {
        val out_eye_tri = shortArrayOf(
            160, 29, 30, 56, 157, 173, 144, 24, 23, 159, 158, 28, 247, 246, 161, 155, 154, 26, 226, 130, 247, 154, 153, 22, 28, 158, 157, 160, 159, 27, 145, 23, 22, 144, 163, 110, 25, 7, 33, 247, 130, 33, 133, 112, 243, 190, 173, 133, 24, 110, 228, 25, 130, 226, 23, 24, 229, 22, 23, 230, 26, 22, 231, 112, 26, 232, 189, 190, 243, 221, 56, 190, 28, 56, 221, 27, 28, 222, 29, 27, 223, 30, 29, 224, 247, 30, 225, 112, 233, 244, 110, 163, 7, 228, 110, 25, 387, 388, 260, 286, 414, 398, 373, 374, 253, 386, 257, 258, 466, 260, 388, 382, 341, 256, 446, 342, 467, 381, 256, 252, 258, 286, 384, 387, 259, 257, 374, 380, 252, 373, 254, 339, 263, 255, 359, 263, 467, 466, 341, 382, 362, 414, 463, 362, 254, 449, 448, 255, 261, 446, 253, 450, 449, 252, 451, 450, 256, 452, 451, 341, 453, 452, 413, 464, 463, 441, 413, 414, 258, 442, 441, 257, 443, 442, 259, 444, 443, 260, 445, 444, 467, 342, 445, 341, 463, 464, 339, 255, 249, 448, 261, 255, 133, 243, 190, 133, 155, 112, 33, 246, 247, 33, 130, 25, 398, 384, 286, 362, 398, 414, 362, 463, 341, 263, 359, 467, 263, 249, 255, 466, 467, 260, 161, 160, 30, 190, 56, 173, 145, 144, 23, 27, 159, 28, 30, 247, 161, 112, 155, 26, 113, 226, 247, 26, 154, 22, 56, 28, 157, 29, 160, 27, 153, 145, 22, 24, 144, 110, 229, 24, 228, 31, 25, 226, 230, 23, 229, 231, 22, 230, 232, 26, 231, 233, 112, 232, 244, 189, 243, 189, 221, 190, 222, 28, 221, 223, 27, 222, 224, 29, 223, 225, 30, 224, 113, 247, 225, 243, 112, 244, 25, 110, 7, 31, 228, 25, 259, 387, 260, 254, 373, 253, 385, 386, 258, 381, 382, 256, 359, 446, 467, 380, 381, 252, 385, 258, 384, 386, 387, 257, 253, 374, 252, 390, 373, 339, 339, 254, 448, 359, 255, 446, 254, 253, 449, 253, 252, 450, 252, 256, 451, 256, 341, 452, 414, 413, 463, 286, 441, 414, 286, 258, 441, 258, 257, 442, 257, 259, 443, 259, 260, 444, 260, 467, 445, 453, 341, 464, 390, 339, 249, 339, 448, 255
        )

        drawMaskByTriangle( textureId, out_eye_tri, faceIndex )
    }

    private fun drawOutEyeFace(textureId: Int, faceIndex: Int = 0) {
        val out_eye_tri = shortArrayOf(
            160, 29, 30, 56, 157, 173, 144, 24, 23, 159, 158, 28, 247, 246, 161, 155, 154, 26, 226, 130, 247, 154, 153, 22, 28, 158, 157, 160, 159, 27, 145, 23, 22, 144, 163, 110, 25, 7, 33, 247, 130, 33, 133, 112, 243, 190, 173, 133, 24, 110, 228, 25, 130, 226, 23, 24, 229, 22, 23, 230, 26, 22, 231, 112, 26, 232, 189, 190, 243, 221, 56, 190, 28, 56, 221, 27, 28, 222, 29, 27, 223, 30, 29, 224, 247, 30, 225, 112, 233, 244, 110, 163, 7, 228, 110, 25, 387, 388, 260, 286, 414, 398, 373, 374, 253, 386, 257, 258, 466, 260, 388, 382, 341, 256, 446, 342, 467, 381, 256, 252, 258, 286, 384, 387, 259, 257, 374, 380, 252, 373, 254, 339, 263, 255, 359, 263, 467, 466, 341, 382, 362, 414, 463, 362, 254, 449, 448, 255, 261, 446, 253, 450, 449, 252, 451, 450, 256, 452, 451, 341, 453, 452, 413, 464, 463, 441, 413, 414, 258, 442, 441, 257, 443, 442, 259, 444, 443, 260, 445, 444, 467, 342, 445, 341, 463, 464, 339, 255, 249, 448, 261, 255, 133, 243, 190, 133, 155, 112, 33, 246, 247, 33, 130, 25, 398, 384, 286, 362, 398, 414, 362, 463, 341, 263, 359, 467, 263, 249, 255, 466, 467, 260, 161, 160, 30, 190, 56, 173, 145, 144, 23, 27, 159, 28, 30, 247, 161, 112, 155, 26, 113, 226, 247, 26, 154, 22, 56, 28, 157, 29, 160, 27, 153, 145, 22, 24, 144, 110, 229, 24, 228, 31, 25, 226, 230, 23, 229, 231, 22, 230, 232, 26, 231, 233, 112, 232, 244, 189, 243, 189, 221, 190, 222, 28, 221, 223, 27, 222, 224, 29, 223, 225, 30, 224, 113, 247, 225, 243, 112, 244, 25, 110, 7, 31, 228, 25, 259, 387, 260, 254, 373, 253, 385, 386, 258, 381, 382, 256, 359, 446, 467, 380, 381, 252, 385, 258, 384, 386, 387, 257, 253, 374, 252, 390, 373, 339, 339, 254, 448, 359, 255, 446, 254, 253, 449, 253, 252, 450, 252, 256, 451, 256, 341, 452, 414, 413, 463, 286, 441, 414, 286, 258, 441, 258, 257, 442, 257, 259, 443, 259, 260, 444, 260, 467, 445, 453, 341, 464, 390, 339, 249, 339, 448, 255
        )

        drawBaseByTriangle( textureId, out_eye_tri, faceIndex )
    }


    private fun drawTCPSticker() {
        val text = "sticker/cat_sticker_small.png"
        if(detectVertex != null) {
            val overlayPoints = listOf<PointF>(
                // 1. Template 1
//                PointF(417f/1024f, 425f/1024f),
//                PointF(606/1024f, 425f/1024f),
//                PointF(512f/1024f, 572f/1024f),

                // 2. Template 2
                PointF(193f/512f, 197f/512f),
                PointF(319f/512f, 197f/512f),
                PointF(256f/512f, 296f/512f),
            )

            Log.e("2dSticker", "overlay:: ${overlayPoints.joinToString(" ")}")

            val bottomPoints = listOf<PointF>(
                GameUtils.getFlipPoint(if(isFlip) 386 else 159, detectVertex!!, isFlip),
                GameUtils.getFlipPoint(if(isFlip) 159 else 386, detectVertex!!, isFlip),
                GameUtils.getFlipPoint(if(isFlip) 2 else 2, detectVertex!!, isFlip)
            )

            Log.e("2dSticker", "bottom:: ${bottomPoints.joinToString(" ")}")

            val p = transformOverlay2(overlayPoints, bottomPoints)
            val posVertex = FloatArray(8)
            for(i in 0 until 4) {
                posVertex[i*2] = p[i].x
                posVertex[i*2+1] = p[i].y
            }

            drawTextInside(text, posVertex, 2)
        }
    }


    /**
     * Standard sticker format
     * - 1024x1024
     * - Canonical-face at center - size - 600x600
     * index 159 - PointF(417f/1024f, 425f/1024f),
     * index 386 - PointF(606/1024f, 425f/1024f),
     */

    private fun transformOverlay2(overlayPoints: List<PointF>, bottomPoints: List<PointF>) : List<PointF> {
        val src = FloatArray(overlayPoints.size * 2)
        val dst = FloatArray(overlayPoints.size * 2)
        for (i in overlayPoints.indices) {
            src[i * 2] = overlayPoints[i].x
            src[i * 2 + 1] = overlayPoints[i].y
            dst[i * 2] = bottomPoints[i].x
            dst[i * 2 + 1] = bottomPoints[i].y
        }
        val matrix = Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, overlayPoints.size)


        val after = listOf(
            GameUtils.transform(overlayPoints[0], matrix),
            GameUtils.transform(overlayPoints[1], matrix),
        )

        Log.e("2dSticker", "after:: ${after.joinToString(" ")}")


        val result =  listOf(
            GameUtils.transform(PointF(0f,0f), matrix),
            GameUtils.transform(PointF(0f,1f), matrix),
            GameUtils.transform(PointF(1f,0f), matrix),
            GameUtils.transform(PointF(1f,1f), matrix),

            )

        Log.e("2dSticker", "corners:: ${result.joinToString(" ")}")

        return result
    }

    override fun onDraw(
        textureId: Int, cubeBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ) {
        // 0. Update timer
        updateTime()

        // 1. Update AI-decorating
        super.onDraw(textureId, cubeBuffer, textureBuffer)

        // 2. Draw game logic

        // 0. Draw sticker
        drawSticker(textureId)

        // 1. prepare phase
        when (gameState) {
            FACE_GAME_STATE.INTRO -> drawIntro()
            FACE_GAME_STATE.CONFIRM -> drawConfirm()
            FACE_GAME_STATE.READY -> drawReady()
            FACE_GAME_STATE.PLAY_NORMAL -> drawPlay()
            FACE_GAME_STATE.PLAY_PEEK -> drawPlay()
            FACE_GAME_STATE.FINISH -> drawFinish()
        }

        // 3. finish phase
        // Finish and signal camera-activity to done-activity
        // Done-activity:
        // - Auto replay
        // - Share + Tag to challenge friends
        // - Play again
//        drawFinish(textureId, cubeBuffer, textureBuffer)
    }

    private fun drawIntro() {
        if(gameState == FACE_GAME_STATE.INTRO) {
            // Draw game-icon
            if(gameStep == 0) {
                if(!timeline.containsKey("intro_step_1")) {
                    timeline["intro_step_1"] = 0f
                }

                if(timeline["intro_step_1"]!! < 1500f) {

                    drawTextInside(
                        "eyebrowgym/ic_eyebrowgym.png",
                        0.4f,
                        0.1f,
                        0.6f,
                        0.3f,
                        ScaleType.CENTER_CROP
                    )

                    timeline["intro_step_1"] = timeline["intro_step_1"]!! + timeDelta
                } else {
                    timeline.remove("intro_step_1")
                    gameStep = 1
                }
            } else if(gameStep == 1) {
                if(!timeline.containsKey("intro_step_2")) {
                    timeline["intro_step_2"] = 0f
                }

                if(timeline["intro_step_2"]!! < 3000f) {

                    drawTextInside(
                        "eyebrowgym/eyebrowgym_text.png",
                        0.45f,
                        0.4f,
                        0.55f,
                        0.5f,
                        ScaleType.CENTER_CROP
                    )

                    if(timeline["intro_step_2"]!! < 1000) {
                        drawTextInside(
                            "game/intro/count_1.png",
                            0.3f,
                            0.5f,
                            0.7f,
                            0.8f,
                            ScaleType.CENTER_INSIDE
                        )
                    } else if(timeline["intro_step_2"]!! < 2000) {
                        drawTextInside(
                            "game/intro/count_2.png",
                            0.3f,
                            0.5f,
                            0.7f,
                            0.8f,
                            ScaleType.CENTER_INSIDE
                        )
                    } else {
                        drawTextInside(
                            "game/intro/count_3.png",
                            0.3f,
                            0.5f,
                            0.7f,
                            0.8f,
                            ScaleType.CENTER_INSIDE
                        )
                    }

                    timeline["intro_step_2"] = timeline["intro_step_2"]!! + timeDelta
                } else {
                    timeline.remove("intro_step_2")
                    gameState = FACE_GAME_STATE.CONFIRM
                    gameStep = 0
                }
            }

        }

    }


    private fun initStateKey(key: String) {
        if (!logicState.containsKey(key)) {
            logicState[key] = HashMap()
        }
    }

    private fun initStateKey(key: String, subKey:String, value:Float) {
        if (logicState.containsKey(key)) {
            if(!logicState[key]!!.containsKey(subKey)) {
                logicState[key]!![subKey] = value
            }
        }
    }


    private fun drawConfirm() {
        debugLogic()

        if(gameState == FACE_GAME_STATE.CONFIRM) {
            // Draw bg-face-detect
            initKey("confirm_state", 0f)
            initKey("confirm_face_time", 0f)

            initKey("left_eyebrow_total", 0f)
            initKey("left_eyebrow_count", 0f)
            initKey("left_eyebrow", 0f)
            initKey("right_eyebrow_total", 0f)
            initKey("right_eyebrow_count", 0f)
            initKey("right_eyebrow", 0f)


            // Check if face exist
            if(GameUtils.confirmFace(detectVertex, isFlip)) {
                timeline["confirm_state"] = 1f
            } else {
                timeline["confirm_state"] = 0f
                timeline["confirm_face_time"] = 0f
            }

            if (timeline["confirm_state"]!! == 0f) {
                drawTextInside(
                    "game/confirm/bg_detect_face.png",
                    0.1f,
                    0.1f,
                    0.9f,
                    0.9f,
                    ScaleType.CENTER_INSIDE
                )

                if(detectVertex == null) {
                    drawTextInside(
                        "game/confirm/text_no_face.png",
                        0.0f,
                        0.05f,
                        1f,
                        0.2f,
                        ScaleType.CENTER_INSIDE
                    )
                } else {
                    drawTextInside(
                        "game/confirm/text_confirm.png",
                        0.0f,
                        0.05f,
                        1f,
                        0.2f,
                        ScaleType.CENTER_INSIDE
                    )
                }

            } else {

                // Update eyebrow - normal angle
                val faceAngle = GameUtils.getEyebrowAngle(detectVertex!!, isFlip)
                val leftEyebrow = faceAngle[15]
                val rightEyebrow = faceAngle[16]

                timeline["left_eyebrow_total"] = timeline["left_eyebrow_total"]!! + leftEyebrow
                timeline["left_eyebrow_count"] = timeline["left_eyebrow_count"]!! + 1
                timeline["left_eyebrow"] = timeline["left_eyebrow_total"]!! / timeline["left_eyebrow_count"]!!

                timeline["right_eyebrow_total"] = timeline["right_eyebrow_total"]!! + rightEyebrow
                timeline["right_eyebrow_count"] = timeline["right_eyebrow_count"]!! + 1
                timeline["right_eyebrow"] = timeline["right_eyebrow_total"]!! / timeline["right_eyebrow_count"]!!


                // Draw confirm logic

                drawTextInside(
                    "game/confirm/bg_detect_face.png",
                    0.1f,
                    0.1f,
                    0.9f,
                    0.9f,
                    ScaleType.CENTER_INSIDE
                )

                drawTextInside(
                    "game/confirm/text_face_detect.png",
                    0.0f,
                    0.05f,
                    1f,
                    0.2f,
                    ScaleType.CENTER_INSIDE
                )


                if(timeline["confirm_face_time"]!! < 3000f) {

                    if(timeline["confirm_face_time"]!! < 1000) {
                        drawTextInside(
                            "game/intro/count_1.png",
                            0.3f,
                            0.5f,
                            0.7f,
                            0.8f,
                            ScaleType.CENTER_INSIDE
                        )
                    } else if(timeline["confirm_face_time"]!! < 2000) {
                        drawTextInside(
                            "game/intro/count_2.png",
                            0.3f,
                            0.5f,
                            0.7f,
                            0.8f,
                            ScaleType.CENTER_INSIDE
                        )
                    } else {
                        drawTextInside(
                            "game/intro/count_3.png",
                            0.3f,
                            0.5f,
                            0.7f,
                            0.8f,
                            ScaleType.CENTER_INSIDE
                        )
                    }

                    timeline["confirm_face_time"] = timeline["confirm_face_time"]!! + timeDelta
                } else {
                    gameState = FACE_GAME_STATE.READY
                    timeline.remove("confirm_state")
                    timeline.remove("confirm_face_time")
                    gameStep = 0
                }

            }
        }


    }




    private fun drawReady() {
        // Done music prepare
        gameState = FACE_GAME_STATE.PLAY_NORMAL
        // Start time counter to generate onset exact with time
        timeline["play_time"] = 0f
    }

    private fun drawPlay(
    ) {
        debugLogic()

        initVFX()
        // Main game logic
        if(gameState == FACE_GAME_STATE.PLAY_NORMAL
            || gameState == FACE_GAME_STATE.PLAY_PEEK
        ) {
            // 1. Draw time-counter bar
            timeline["play_time"] = timeline["play_time"]!! + timeDelta

            // 5 elements each 1 second
            val elemPerSecond = 5
            val progress = elemPerSecond * (timeline["play_time"]!!.toInt() / 1000)
            //
            val totalTime = 10
            val totalElem = totalTime * elemPerSecond

            if(progress <= totalElem) {
                drawProgress(
                    progress, totalElem, "game/progress/linear_element.png",
                    ScaleType.CENTER_FIT,
                    0.25f, 0.87f, 0.95f, 0.89f
                )

                // Draw clock
                drawTextInside(
                    "game/progress/clock2.png",
                    0.01f, 0.86f, 0.2f, 0.9f,
                    ScaleType.CENTER_CROP
                )

                // Draw timer - need to draw string
                val timer = DecimalFormat("#.").format(
                    timeline["play_time"]!! / 1000.0
                )

                drawString(
                    "$timer seconds",
                    ScaleType.CENTER_INSIDE,
                    0.50f, 0.90f, 0.70f, 0.95f

                )

                // 2. Check eyebrow lifting and count score
                if (!timeline.containsKey("play_score")) {
                    timeline["play_score"] = 0f
                }

                playEyebrowGym()


            } else {
                drawString(
                    "Timeup! ~END GAME~",
                    ScaleType.CENTER_INSIDE,
                    0.0f, 0.50f, 1.0f, 0.60f
                )

                // Draw end game celebrating

                drawEndVFX()
            }

            // Draw score By draw-text
            drawNumber(timeline["play_score"]!!.toInt(), ScaleType.CENTER_INSIDE,
                0f, 0.1f, 1f, 0.2f
            )

        }

    }

    private fun drawEndVFX() {
        endGameVFX!!.updateTimer(timeDelta)
        drawGameObjectInside(endGameVFX!!)
        Log.e("vfx", "frame-size:: ${endGameVFX!!.animTexts.size}\n" +
                "current-index:: ${endGameVFX!!.animTextIndex}")
    }

    private fun diffPercent(current:Float, normal:Float): Float {
        return (current - normal) / normal
    }

    private fun playEyebrowGym() {
        if(detectVertex == null) {
            return
        }
        // 0f - normal
        // 1f - down
        // 2f - up

        initKey("left_eyebrow_state", 0f)
        initKey("right_eyebrow_state", 0f)

        initKey("eyebrow_state", 0f)

        initStateKey("left_eyebrow")
        initStateKey("left_eyebrow", "normal_time", -1f)
        initStateKey("left_eyebrow", "down_time", -1f)
        initStateKey("left_eyebrow", "up_time", -1f)


        initStateKey("right_eyebrow")
        initStateKey("right_eyebrow", "normal_time", -1f)
        initStateKey("right_eyebrow", "down_time", -1f)
        initStateKey("right_eyebrow", "up_time", -1f)


        val faceAngle = GameUtils.getEyebrowAngle(detectVertex!!, isFlip)
        val cLeftEye = faceAngle[15]
        val cRightEye = faceAngle[16]
        val nLeftEye = timeline["left_eyebrow"]!!
        val nRightEye = timeline["right_eyebrow"]!!

        val leftDiff = diffPercent(cLeftEye, nLeftEye)
        val rightDiff = diffPercent(cRightEye, nRightEye)

        if(leftDiff > 0.1) {
            timeline["left_eyebrow_state"] = 2.0f
            logicState["left_eyebrow"]!!["up_time"] = timeline["play_time"]!!
        } else if(leftDiff < -0.1) {
            timeline["left_eyebrow_state"] = 1.0f
            logicState["left_eyebrow"]!!["down_time"] = timeline["play_time"]!!
        } else {
            timeline["left_eyebrow_state"] = 0.0f
            logicState["left_eyebrow"]!!["normal_time"] = timeline["play_time"]!!
        }

        if(rightDiff > 0.1) {
            timeline["right_eyebrow_state"] = 2.0f
            logicState["right_eyebrow"]!!["up_time"] = timeline["play_time"]!!
        } else if(rightDiff < -0.1) {
            timeline["right_eyebrow_state"] = 1.0f
            logicState["right_eyebrow"]!!["down_time"] = timeline["play_time"]!!
        } else {
            timeline["right_eyebrow_state"] = 0.0f
            logicState["right_eyebrow"]!!["normal_time"] = timeline["play_time"]!!
        }


        val diff = 0.15f
        if(rightDiff > diff && leftDiff > diff) {
            // draw up gym
            drawSimpleGym("eyebrowgym/gym/3.png")

            // count score
            if(timeline["eyebrow_state"] == 1.0f) {
                timeline["play_score"] = timeline["play_score"]!! + 1f

                // Draw celebrating effect

            }
            timeline["eyebrow_state"] = 0f

        } else if(rightDiff < -diff && leftDiff < -diff) {
            // draw down gym
            drawSimpleGym("eyebrowgym/gym/1.png")

            // check score
            timeline["eyebrow_state"] = 1.0f
        } else if(abs(rightDiff) < diff && abs(leftDiff) < diff) {
            // draw normal gym
            drawSimpleGym("eyebrowgym/gym/2.png")
        } else {
            // draw normal gym
            drawSimpleGym("eyebrowgym/gym/2.png")
        }

        // check count eyebrow gym lift
        if(logicState["right_eyebrow"]!!["up_time"]!! != -1f
            && logicState["right_eyebrow"]!!["down_time"]!! != -1f
            && logicState["right_eyebrow"]!!["normal_time"]!! != -1f

            && logicState["left_eyebrow"]!!["up_time"]!! != -1f
            && logicState["left_eyebrow"]!!["down_time"]!! != -1f
            && logicState["left_eyebrow"]!!["normal_time"]!! != -1f


        ) {

        }


    }

    private fun drawGym() {
        // Draw at face position
        // Might update with rotate as face angle
    }

    private fun drawSimpleGym(text: String) {
        if(detectVertex != null) {
            var index = 151
            var cX = detectVertex!![index*3+0]
            var cY = detectVertex!![index*3+1]

            if(isFlip) {
                cX = 1.0f - cX
            }


            val dist = GameUtils.distByIndex(detectVertex!!, 168, 18)


            drawTextInside(
                text,
                cX-dist, cY-dist, cX+dist, cY+dist,
                ScaleType.CENTER_INSIDE
            )
        }

    }

    private fun drawFaceGym() {

    }


    override fun debugLogic() {
        if(debug != null) {

            if (detectVertex == null) {
                debug!!.updateDebug("No face detected")
            } else {
                val faceAngle = GameUtils.getEyebrowAngle(detectVertex!!, isFlip)

                val index = 1
                val x = detectVertex!![3 * index]
                val y = detectVertex!![3 * index + 1]
                val leftEye = timeline["left_eyebrow"]
                val rightEye = timeline["right_eyebrow"]
                debug!!.updateDebug(
                    "" +
                            "eyebrow ${faceAngle[15]} || ${faceAngle[16]} \n" +
                            "average ${leftEye} || ${rightEye}" +
                            ""
                )
            }
        }
    }


    private fun drawFinish() {
    }


}