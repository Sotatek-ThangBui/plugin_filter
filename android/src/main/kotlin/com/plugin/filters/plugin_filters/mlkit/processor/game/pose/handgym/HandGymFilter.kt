package com.plugin.filters.plugin_filters.mlkit.processor.game.pose.handgym

import android.content.Context
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


/**
 * First experiment logic with single slow face-tile dropping
 */
class HandGymFilter(context: Context, mode: MaskMode):
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
            initKey("confirm_detect_time", 0f)



            // Check if face exist
            if(detectVertex != null) {
                timeline["confirm_state"] = 1f
            } else {
                timeline["confirm_state"] = 0f
                timeline["confirm_detect_time"] = 0f
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


                if(timeline["confirm_detect_time"]!! < 3000f) {

                    if(timeline["confirm_detect_time"]!! < 1000) {
                        drawTextInside(
                            "game/intro/count_1.png",
                            0.3f,
                            0.5f,
                            0.7f,
                            0.8f,
                            ScaleType.CENTER_INSIDE
                        )
                    } else if(timeline["confirm_detect_time"]!! < 2000) {
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

                    timeline["confirm_detect_time"] = timeline["confirm_detect_time"]!! + timeDelta
                } else {
                    gameState = FACE_GAME_STATE.READY
                    timeline.remove("confirm_state")
                    timeline.remove("confirm_detect_time")
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
            val progress = elemPerSecond * (timeline["play_time"]!! / 1000).toInt()
            //
            val totalTime = 20
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

                playHandGym()


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

    private fun playHandGym() {
        if(detectVertex == null) {
            return
        }
        // 0f - normal
        // 1f - down
        // 2f - up


        initKey("hand_state", 0f)


        val poseAngle = GameUtils.getPoseAngle(detectVertex!!, isFlip)
        val handAngle = poseAngle[0]

        var index = 0
        val nose = PointF(detectVertex!![3 * index], detectVertex!![3 * index + 1])

        index = 15
        val leftWrist = PointF(detectVertex!![3 * index], detectVertex!![3 * index + 1])

        index = 16
        val rightWrist = PointF(detectVertex!![3 * index], detectVertex!![3 * index + 1])



        if(leftWrist.y < nose.y && rightWrist.y < nose.y && handAngle > 0) {
            // draw up gym
            drawSimpleGym("eyebrowgym/gym/3.png")

            // count score
            if(timeline["hand_state"] == 1.0f) {
                timeline["play_score"] = timeline["play_score"]!! + 1f

                // Draw celebrating effect

            }
            timeline["hand_state"] = 0f

        } else if(leftWrist.y > nose.y && rightWrist.y > nose.y && handAngle < 0) {
            // draw down gym
            drawSimpleGym("eyebrowgym/gym/1.png")

            // check score
            timeline["hand_state"] = 1.0f
        } else {
            // draw normal gym
            drawSimpleGym("eyebrowgym/gym/2.png")
        }




    }

    private fun drawGym() {
        // Draw at face position
        // Might update with rotate as face angle
    }

    private fun drawSimpleGym(text: String) {
        if(detectVertex != null) {
            var index = 0
            var cX = detectVertex!![index*3+0]
            var cY = detectVertex!![index*3+1]

            if(isFlip) {
                cX = 1.0f - cX
            }


            val dist = GameUtils.distByIndex(detectVertex!!, 11, 12)


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

            if (detectVertex == null || detectVertex?.size == 0) {
                debug!!.updateDebug("No pose detected")
            } else {
                val faceAngle = GameUtils.getPoseAngle(detectVertex!!, isFlip)

                var index = 0
                val nose = PointF(detectVertex!![3 * index], detectVertex!![3 * index + 1])

                index = 15
                val leftWrist = PointF(detectVertex!![3 * index], detectVertex!![3 * index + 1])

                index = 16
                val rightWrist = PointF(detectVertex!![3 * index], detectVertex!![3 * index + 1])


                debug!!.updateDebug(
                    "" +
                            "left2Right ${faceAngle[0]} \n" +
                            "nose ${nose} \n" +
                            "leftWrist ${leftWrist}\n" +
                            "rightWrist ${rightWrist}\n" +
                            ""
                )
            }
        }
    }


    private fun drawFinish() {
    }


}