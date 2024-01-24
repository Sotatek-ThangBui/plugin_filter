package com.skymeta.arface.mlkit.processor.game.face.facedance

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.util.Log
import com.plugin.filters.plugin_filters.mlkit.processor.DebugView
import com.plugin.filters.plugin_filters.mlkit.processor.MusicCaller
import com.plugin.filters.plugin_filters.mlkit.processor.MusicControl
import com.plugin.filters.plugin_filters.mlkit.processor.game.FACE_GAME_STATE
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameObject
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameUtils
import jp.co.cyberagent.android.gpuimage.ml.FaceSwapFilter
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import jp.co.cyberagent.android.gpuimage.ml.ScaleType
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils
import java.nio.FloatBuffer
import java.util.HashMap
import kotlin.math.roundToInt


/**
 * First experiment logic with single slow face-tile dropping
 */
class FaceSlowDanceFilter(context: Context, mode: MaskMode):
    FaceSwapFilter(context, mode),
    MusicCaller
{

    private var assetTexture: java.util.HashMap<String, GameObject?>? = null


    // 1. Game timer
    private var currentTick: Int = 0
    private var currentTime: Long = 0
    private var timeDelta: Float = 0f
    // 2. Game phase
    private var gameState: FACE_GAME_STATE = FACE_GAME_STATE.INTRO
    private var gameStep: Int = 0
    private var timeline: HashMap<String, Float> = HashMap()

    // 3. Game object pool

    // 3.1. Face-tiles
    private var faceTileObjects = java.util.ArrayList<GameObject>()
    private var currentFaceTile: GameObject? = null

    // 3.2. Hit-bar
    private var hitBar: GameObject? = null

    private var isGameInit = false

    var debug: DebugView? = null

    // 4. Music
    var musicControl: MusicControl? = null
    var donePrepare = 0
    var errMsg: String = ""
    var donePlay = false
    var onset:FloatArray? = null
    var doneOnset = false


    override fun musicDonePrepare() {
        donePrepare = 1
    }
    override fun musicDonePlay() {
        donePlay = true
    }

    override fun musicErrorPrepare(s: String) {
        donePrepare = -1 // Error prepare
        errMsg = s
    }


    override fun onInit() {
        super.onInit()

        // @Todo: Initialize is call every re-add filter to renderer
        if(!isGameInit) {
            initMusicPlayer()
            isGameInit = true
        }


    }

    private fun initMusicPlayer() {
        donePrepare = 0
        donePlay = false
        val url = "https://www.chosic.com/wp-content/uploads/2020/05/Scott_Joplin_-_04_-_The_Entertainer_1902_piano_roll.mp3"
        musicControl?.musicPrepare(url, this)
    }


    fun calTextRatio(
                     imageWidth: Float, imageHeight: Float,
                     outputWidth: Float, outputHeight: Float,
    ):FloatArray {
        val ratio1: Float = outputWidth / imageWidth
        val ratio2: Float = outputHeight / imageHeight
        val ratioMax = Math.max(ratio1, ratio2)
        val imageWidthNew = (imageWidth * ratioMax).roundToInt()
        val imageHeightNew = (imageHeight * ratioMax).roundToInt()

        val textRatioW = imageWidthNew / outputWidth
        val textRatioH = imageHeightNew / outputHeight

        return floatArrayOf(textRatioW, textRatioH)
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

    private fun updateTime() {
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

    override fun onDraw(
        textureId: Int, cubeBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ) {
        // 0. Update timer
        updateTime()

        // 1. Update AI-decorating
        super.onDraw(textureId, cubeBuffer, textureBuffer)

        // 2. Draw game logic

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
                        "intro/ic_beginboard.png",
                        0.3f,
                        0.1f,
                        0.7f,
                        0.5f,
                        ScaleType.CENTER_INSIDE
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
                        "intro/ic_facedance.png",
                        0.3f,
                        0.4f,
                        0.7f,
                        0.5f,
                        ScaleType.CENTER_INSIDE
                    )

                    if(timeline["intro_step_2"]!! < 1000) {
                        drawTextInside(
                            "intro/count_1.png",
                            0.3f,
                            0.5f,
                            0.7f,
                            0.8f,
                            ScaleType.CENTER_INSIDE
                        )
                    } else if(timeline["intro_step_2"]!! < 2000) {
                        drawTextInside(
                            "intro/count_2.png",
                            0.3f,
                            0.5f,
                            0.7f,
                            0.8f,
                            ScaleType.CENTER_INSIDE
                        )
                    } else {
                        drawTextInside(
                            "intro/count_3.png",
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


    private fun drawConfirm() {
        debugFace()

        if(gameState == FACE_GAME_STATE.CONFIRM) {
            // Draw bg-face-detect
            if (!timeline.containsKey("confirm_state")) {
                timeline["confirm_state"] = 0f
            }
            if (!timeline.containsKey("confirm_face_time")) {
                timeline["confirm_face_time"] = 0f
            }

            // Check if face exist
            if(GameUtils.confirmFace(detectVertex, isFlip)) {
                timeline["confirm_state"] = 1f
            } else {
                timeline["confirm_state"] = 0f
                timeline["confirm_face_time"] = 0f
            }

            if (timeline["confirm_state"]!! == 0f) {
                drawTextInside(
                    "confirm/bg_detect_face.png",
                    0.1f,
                    0.1f,
                    0.9f,
                    0.9f,
                    ScaleType.CENTER_INSIDE
                )

                if(detectVertex == null) {
                    drawTextInside(
                        "confirm/text_no_face.png",
                        0.0f,
                        0.05f,
                        1f,
                        0.2f,
                        ScaleType.CENTER_INSIDE
                    )
                } else {
                    drawTextInside(
                        "confirm/text_confirm.png",
                        0.0f,
                        0.05f,
                        1f,
                        0.2f,
                        ScaleType.CENTER_INSIDE
                    )
                }

            } else {

                drawTextInside(
                    "confirm/bg_detect_face.png",
                    0.1f,
                    0.1f,
                    0.9f,
                    0.9f,
                    ScaleType.CENTER_INSIDE
                )

                drawTextInside(
                    "confirm/text_face_detect.png",
                    0.0f,
                    0.05f,
                    1f,
                    0.2f,
                    ScaleType.CENTER_INSIDE
                )


                if(timeline["confirm_face_time"]!! < 3000f) {

                    if(timeline["confirm_face_time"]!! < 1000) {
                        drawTextInside(
                            "intro/count_1.png",
                            0.3f,
                            0.5f,
                            0.7f,
                            0.8f,
                            ScaleType.CENTER_INSIDE
                        )
                    } else if(timeline["confirm_face_time"]!! < 2000) {
                        drawTextInside(
                            "intro/count_2.png",
                            0.3f,
                            0.5f,
                            0.7f,
                            0.8f,
                            ScaleType.CENTER_INSIDE
                        )
                    } else {
                        drawTextInside(
                            "intro/count_3.png",
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

    /**
     * Draw by using paint + canvas + initialized every draw is resource exhausting
     * And cause DeadObjectException
     * - Simply continuous update to android text-view
     */
    private fun debugFace() {
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


    private fun drawReady() {
        if(donePrepare==1) {
            if(doneOnset) {
                // Done music prepare
                gameState = FACE_GAME_STATE.PLAY_NORMAL
                // Start music play
                musicControl?.musicPlay(this)
            } else {
                drawTextInside(
                    "confirm/prepare_song.png",
                    0.3f,0.5f,
                    0.7f,0.8f,
                    ScaleType.CENTER_INSIDE
                )
            }
        } else if(donePrepare == 0){
            drawTextInside(
                "confirm/prepare_song.png",
                0.3f,0.5f,
                0.7f,0.8f,
                ScaleType.CENTER_INSIDE
            )
        } else {
            drawTextInside(
                "confirm/error_music.png",
                0.3f,0.5f,
                0.7f,0.8f,
                ScaleType.CENTER_INSIDE
            )
        }
    }

    private fun drawPlay(
    ) {
        debugFace()
        // Main game logic
        if(gameState == FACE_GAME_STATE.PLAY_NORMAL
            || gameState == FACE_GAME_STATE.PLAY_PEEK
        ) {
            moveCurrentTile()
            if (!timeline.containsKey("play_score")) {
                timeline["play_score"] = 0f
            }
            playMatchFace()
            // Draw score By draw-text
            drawNumber(timeline["play_score"]!!.toInt(), ScaleType.CENTER_INSIDE,
                0f, 0.1f, 1f, 0.2f
            )
        }

    }

    fun moveCurrentTile() {
        /**
         * Drop face-tile down from top to bottom
         * Check if user show fit face-expression with face-tile
         * Show result and score for user-input-action
         */

        /**
         * Update-2: with onset data
         * Generate tile by onset time data
         * Hit tile will update color to green to let user know
         * There can be more than 1 tile on screen
         * Keep track current tile by list of tile
         * Check score for list of tile
         * Scored-tile will not be score again
         * Matching score is evaluate by match-position
         * Draw hit-bar to highlight match-position

         */
        if(currentFaceTile == null) {
            if (!timeline.containsKey("tile_break_time")) {
                timeline["tile_break_time"] = 0f
            } else {
                timeline["tile_break_time"] = timeline["tile_break_time"]!! + timeDelta
            }

            if(timeline["tile_break_time"]!! > 1000) {
                // Generate new tile
                currentFaceTile = faceTileObjects.random()
                var startX = FloatArray(8) { it * 0.1f }.random()
                var width = 0.2f
                var height = 0.2f
                var startY = -0.2f

                currentFaceTile!!.textScale = ScaleType.CENTER_CROP
                currentFaceTile!!.x = startX
                currentFaceTile!!.y = startY
                currentFaceTile!!.w = width
                currentFaceTile!!.h = height

                timeline.remove("tile_break_time")
            }
        } else {

            // Move tile
            if(currentFaceTile!!.y < 1f) {
                Log.e("FacePlay", "before:: ${currentFaceTile!!.y}")
                // Move faceTile down
                var width = 0.2f
                var height = 0.2f
                // Move 20% of screen-height every second
                val showTime = 4f // 4 seconds alive on screen
                val scoreRange = 0.8f
                val speed = ((timeDelta / 1000f) / showTime) / scoreRange
                var startY = currentFaceTile!!.y + speed


                currentFaceTile!!.textScale = ScaleType.CENTER_INSIDE
                currentFaceTile!!.y = startY

                Log.e("FacePlay", "current:: ${currentFaceTile!!.y}")

                drawGameObjectInside(currentFaceTile!!)
            } else {
                currentFaceTile = null
            }
        }
    }

    private fun tileInScoreRange():Boolean {
        if(currentFaceTile!= null) {
            return (currentFaceTile!!.y > 0.05 && currentFaceTile!!.y < 0.95)
        }
        return false
    }

    private fun playMatchFace() {
        /**
            Check score hit
            Scoring rule
            Keep face-expression same as current-face-tile for a period
            Show matching signal near current-face-tile
            Getting score - correspondent to scoring-region
         */
        if (!timeline.containsKey("match_state")) {
            timeline["match_state"] = 0f
        }
        if (!timeline.containsKey("match_score_add")) {
            timeline["match_score_add"] = 0f
        }
        if (!timeline.containsKey("match_face_time")) {
            timeline["match_face_time"] = 0f
        }

        // Check if face exist
        // @Todo: update game-rule:: the exact face-match + exact timing tile position - The more score you get
        if( tileInScoreRange() &&
            GameUtils.playMatchFace(detectVertex, currentFaceTile, isFlip)
        ) {
            timeline["match_state"] = 1f

        } else {
            timeline["match_state"] = 0f
            timeline["match_face_time"] = 0f
        }

        if (timeline["match_state"]!! == 0f) {
            if(detectVertex == null) {
                drawTextInside(
                    "confirm/text_no_face.png",
                    0.0f,
                    0.85f,
                    1f,
                    0.95f,
                    ScaleType.CENTER_INSIDE
                )
            }

        } else {

            if(timeline["match_face_time"]!! < 3000f) {

                val scoreTime = 800

                // @Todo: Current game-rule:: The more time match-face - the more score

                if(timeline["match_face_time"]!! < 1*scoreTime) {
                    drawTextInside(
                        "result/ok.png",
                        0.3f,
                        0.5f,
                        0.7f,
                        0.8f,
                        ScaleType.CENTER_INSIDE
                    )
                    if(timeline["match_score_add"] == 0f) {
                        timeline["play_score"] = timeline["play_score"]!! + 10f
                    }
                    timeline["match_score_add"] = 1f
                } else if(timeline["match_face_time"]!! < 2*scoreTime) {
                    drawTextInside(
                        "result/cool.png",
                        0.3f,
                        0.5f,
                        0.7f,
                        0.8f,
                        ScaleType.CENTER_INSIDE
                    )
                    if(timeline["match_score_add"] == 1f) {
                        timeline["play_score"] = timeline["play_score"]!! + 20f
                    }
                    timeline["match_score_add"] = 2f
                } else if(timeline["match_face_time"]!! < 3*scoreTime) {
                    drawTextInside(
                        "result/excellence.png",
                        0.3f,
                        0.5f,
                        0.7f,
                        0.8f,
                        ScaleType.CENTER_INSIDE
                    )
                    if(timeline["match_score_add"] == 2f) {
                        timeline["play_score"] = timeline["play_score"]!! + 30f
                    }
                    timeline["match_score_add"] = 3f
                } else if(timeline["match_face_time"]!! < 4*scoreTime) {
                    drawTextInside(
                        "result/great.png",
                        0.3f,
                        0.5f,
                        0.7f,
                        0.8f,
                        ScaleType.CENTER_INSIDE
                    )
                    if(timeline["match_score_add"] == 3f) {
                        timeline["play_score"] = timeline["play_score"]!! + 40f
                    }
                    timeline["match_score_add"] = 4f
                } else if(timeline["match_face_time"]!! < 5*scoreTime) {
                    drawTextInside(
                        "result/perfect.png",
                        0.3f,
                        0.5f,
                        0.7f,
                        0.8f,
                        ScaleType.CENTER_INSIDE
                    )
                    if(timeline["match_score_add"] == 4f) {
                        timeline["play_score"] = timeline["play_score"]!! + 50f
                    }
                    timeline["match_score_add"] = 5f
                }

                timeline["match_face_time"] = timeline["match_face_time"]!! + timeDelta
            }

        }

        if(currentFaceTile == null) {
            timeline.remove("match_state")
            timeline.remove("match_face_time")
            timeline.remove("match_score_add")
        }
    }

    private fun drawNumber(value: Int, scaleType:ScaleType,
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
        val textureObj = assetTexture!!["number/0.png"]
        val w = textureObj!!.width.toFloat() * total
        val h = textureObj!!.height.toFloat()

        // Consider width as standard - scale by outputWidth
        //val pos = GameObject.relativeWBottomCenter(0.8f, 0.9f, w, h)


        val textRatio = calTextRatio(w, h,
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
                "number/${digits[i]}.png",
                nStartX + i * dW, nStartY, nStartX + (i + 1) * dW, nStartY + nH,
                ScaleType.CENTER_FIT
            )
        }
    }


    private fun drawFinish() {

    }


    private fun drawPrepare() {
        Log.e("FaceDance", "Draw Prepare")

        // 1. Draw game logo
        drawTextInside(
            "begin_board.png",
            0.3f,
            0.9f,
            1f,
            1f,
            ScaleType.CENTER_CROP
        )
        drawTextInside(
            "ic_beginboard.png",
            0.0f,
            0.9f,
            0.3f,
            1f,
            ScaleType.CENTER_CROP
        )

        // 2. Draw game hit-bar
        if(hitBar != null && hitBar!!.animTexts.size > 0) {
            // Update timer
            hitBar!!.animTimer += timeDelta
            if (hitBar!!.animTimer >= hitBar!!.animRate) {
                hitBar!!.animTimer = 0f
                hitBar!!.animTextIndex = (hitBar!!.animTextIndex + 1) % (hitBar!!.animTexts.size)
                hitBar!!.textureId = hitBar!!.animTexts[hitBar!!.animTextIndex]
            }
        }
        drawGameObjectInside(hitBar!!)

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
        val textRatio = calTextRatio(w, h,
            (endX - startX) * outputWidth, // absolute width
            (endY - startY) * outputHeight // absolute height
        )
        val textRatioW = textRatio[0]
        val textRatioH = textRatio[1]


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

        var pos = ""
        positionVertex.forEach { pos += " $it" }

        Log.e("FaceScale", "drawTextInside::" +
                "\n textureObj:: ${textureObj}" +
                "\n scaleType:: ${scaleType}" +
                "\n position:: (${pos})")

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

//        Log.e("FaceAnim", "drawTextInside 3" +
//                "\nTime: ${hitBar!!.animTimer} || ${hitBar!!.animRate} || ${timeDelta}" +
//                "\nCond: ${hitBar!!.animTimer >= hitBar!!.animRate}")
        // 3. Activate image-texture
        val textureId = textureObj.textureId
        if(textureId == null) {
            clearGLES()
            return
        }
        bindOverlayTexture(textureId)

//        Log.e("FaceAnim", "drawTextInside 4" +
//                "\nTime: ${hitBar!!.animTimer} || ${hitBar!!.animRate} || ${timeDelta}" +
//                "\nCond: ${hitBar!!.animTimer >= hitBar!!.animRate}")

        // 5. Draw face-triangle mesh
        // 5.1. Init triangle-indices
        val drawIndex = shortArrayOf(
            0, 1, 2,
            1, 2, 3
        )
        drawTriangleByIndex(drawIndex)

        // 6. Clear GLES
        clearGLES()

//        Log.e("FaceAnim", "drawTextInside 5" +
//                "\nTime: ${hitBar!!.animTimer} || ${hitBar!!.animRate} || ${timeDelta}" +
//                "\nCond: ${hitBar!!.animTimer >= hitBar!!.animRate}")
    }





}