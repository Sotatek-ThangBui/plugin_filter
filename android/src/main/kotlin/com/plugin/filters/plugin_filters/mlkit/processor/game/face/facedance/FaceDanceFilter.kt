package com.skymeta.arface.mlkit.processor.game.face.facedance

import android.content.Context
import android.util.Log
import com.plugin.filters.plugin_filters.mlkit.processor.MusicCaller
import com.plugin.filters.plugin_filters.mlkit.processor.game.FACE_GAME_STATE
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameObject
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameUtils
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import jp.co.cyberagent.android.gpuimage.ml.ScaleType
import org.json.JSONObject
import java.nio.FloatBuffer


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

 */
class FaceDanceFilter(context: Context, mode: MaskMode):
    GameFilter(context, mode),
    MusicCaller
{

    // 3. Game object pool
    private var faceTiles = FaceDanceConstants.FACE_TILES
    // 3.1. Face-tiles
    private var faceTileObjects = java.util.ArrayList<GameObject>()
    private var currentTiles: java.util.ArrayList<DanceGO> = ArrayList()

    // 3.2. Hit-bar
    private var hitBar: GameObject? = null
    private var isGameInit = false




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
            // Position and setting update on the first time only
            initGameObjectPool()
            initMusicPlayer()
            initMusicOnset()
            isGameInit = true
        }

        // @Todo: some texture is not initialized so need to re-update
        updateGameTexture()

    }

    private fun initMusicPlayer() {
        donePrepare = 0
        donePlay = false
        val url = "https://www.chosic.com/wp-content/uploads/2020/05/Scott_Joplin_-_04_-_The_Entertainer_1902_piano_roll.mp3"
        musicControl?.musicPrepare(url, this)
    }

    private fun initMusicOnset() {
        val url = "https://mangaverse.skymeta.pro/meme/song_meme/song-onset?id=24161"
        GameUtils.fetchData(url) { data ->
            if (data != null) {
                // Do something with the data
                try {
                    val jsonObject = JSONObject(data)
                    val jsonOnset = jsonObject.getJSONArray("data")
                    onset = FloatArray(jsonOnset.length())
                    onsetAgg = FloatArray(onset!!.size)
                    var j = 0
                    // reduce onset size
                    var list = ArrayList<Float>()
                    for (i in 0 until jsonOnset.length() step 7) {
                        var e = jsonOnset.getDouble(i).toFloat() * 1000 // convert to millisecond
                        list.add(e)
                    }
                    onset = list.toFloatArray()
                    onsetAgg = list.toFloatArray()
                    doneOnset = true
                } catch (e: Exception) {
                    Log.e("extractArrayFromJson", "Error parsing JSON or extracting array: $e")
                    doneOnset = false
                    onset = null
                }
            } else {
                // Handle error
                doneOnset = false
                onset = null
            }
        }
    }


    private fun initGameObjectPool() {
        // 1. Init face-tiles
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

        // 2. Init hit-bar
        val path = "facedance/hit_bar/0.png"
        val hitBar0 = assetTexture!![path]!!
        hitBar = GameObject(
            hitBar0
        )

        for(i in 0..5) {
            val text = assetTexture!!["facedance/hit_bar/$i.png"]!!
            hitBar!!.animTexts.add(text.textureId)
        }

        hitBar!!.textureId = hitBar!!.animTexts[0]
        hitBar!!.animRate = 100f
        hitBar!!.animTimer = 0f

//        Log.e("FaceAnim", "\n\n------------Hit-bar INITIALIZE-------------\n\n")

        hitBar!!.textScale = ScaleType.CENTER_FIT
        hitBar!!.x = 0f
        hitBar!!.y = 0.57f
        hitBar!!.w = 1f
        hitBar!!.h = 0.06f


    }


    private fun updateGameTexture() {

        // 1. Init face-tiles
        for(i in faceTileObjects.indices) {
            val textId = faceTiles[i]
            val faceObj = assetTexture!![textId]!!

            faceTileObjects[i].textureId = faceObj.textureId
        }

        // 2. Init hit-bar
        hitBar!!.animTexts = java.util.ArrayList<Int>()
        for(i in 0..5) {
            val text = assetTexture!!["facedance/hit_bar/$i.png"]!!
            hitBar!!.animTexts.add(text.textureId)
        }
        hitBar!!.textureId = hitBar!!.animTexts[0]

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
                        "facedance/ic_beginboard.png",
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
                        "facedance/ic_facedance.png",
                        0.3f,
                        0.4f,
                        0.7f,
                        0.5f,
                        ScaleType.CENTER_INSIDE
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


    private fun drawConfirm() {
        debugLogic()

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
        if(donePrepare==1) {
            if(doneOnset) {
                // Done music prepare
                gameState = FACE_GAME_STATE.PLAY_NORMAL
                // Start music play
                musicControl?.musicPlay(this)

                // Start time counter to generate onset exact with time
                timeline["play_time"] = 0f

            } else {
                drawTextInside(
                    "game/confirm/prepare_song.png",
                    0.3f,0.5f,
                    0.7f,0.8f,
                    ScaleType.CENTER_INSIDE
                )
            }
        } else if(donePrepare == 0){
            drawTextInside(
                "game/confirm/prepare_song.png",
                0.3f,0.5f,
                0.7f,0.8f,
                ScaleType.CENTER_INSIDE
            )
        } else {
            drawTextInside(
                "game/confirm/error_music.png",
                0.3f,0.5f,
                0.7f,0.8f,
                ScaleType.CENTER_INSIDE
            )
        }
    }

    private fun drawPlay(
    ) {
        debugLogic()
        // Main game logic
        if(gameState == FACE_GAME_STATE.PLAY_NORMAL
            || gameState == FACE_GAME_STATE.PLAY_PEEK
        ) {
            moveTileList()
            if (!timeline.containsKey("play_score")) {
                timeline["play_score"] = 0f
            }
            playMatchFaceTiles()
            // Draw score By draw-text
            drawNumber(timeline["play_score"]!!.toInt(), ScaleType.CENTER_INSIDE,
                0f, 0.1f, 1f, 0.2f
            )

            // Update play-time counter
            timeline["play_time"] = timeline["play_time"]!! + timeDelta
        }
    }

    private fun generateNewTile(): DanceGO {
        // Generate new tile
        var tile = faceTileObjects.random()
        var startX = FloatArray(8) { it * 0.1f }.random()
        var width = 0.2f
        var height = 0.2f
        var startY = 0.0f

        tile.textScale = ScaleType.CENTER_CROP
        tile.x = startX
        tile.y = startY
        tile.w = width
        tile.h = height

        return DanceGO(tile)
    }

    fun moveTileList() {
        /**
         * Drop face-tile down from top to bottom
         * Check if user show fit face-expression with face-tile
         * Show result and score for user-input-action
         */

        // 0. Prepare variables
        // Keep track of onset_index
        if (!timeline.containsKey("current_onset")) {
            timeline["current_onset"] = -1f
        }

        // Start onset should be calculate to match the first onset on time
        // that music hit from distance moving to hit-bar
        val startOnset = 0f

        // To control waiting or foward time to let onset move from start to end
        val showTime = 4000f

        // 1. Generate tiles - Generate by onset time - match currentTime
        // Find closest onset which smaller than currentTime
        // Initialize step - init the first onset-index - closest to showTime
        // The first tile is fit with the first onset-index
        // The first tile need showTime to move from start to hitbar
        if(timeline["current_onset"] == -1f) {
            Log.e("InitOnset", "Onset data:: ${onset!!.joinToString(", ")}")
            var matchTime = showTime * 0.6f // time to hit-bar at y = 0.6f

            if(matchTime < onsetAgg!![0]) {
                // Time has not come to generate any onset
            } else {
                // Time has come
                // find closest time
                var index = 0
                while(index < onsetAgg!!.size && onsetAgg!![index] < matchTime) {
                    index += 1
                }
                timeline["current_onset"] = index.toFloat()


                Log.e("InitOnset", "Onset data:: " +
                        "init-index $index || " +
                        "onset-init ${onsetAgg!![index]} || " +
                        "show-time ${showTime}")

                if(index > onsetAgg!!.size) {
                    // end of onset - end game here
                }
            }


        } else if(timeline["current_onset"]!! < onsetAgg!!.size){
            // Continue generate new onset - tile need showTime to move from start to hitBar
            var matchTime = timeline["play_time"]!! + showTime * 0.6f
            var index = timeline["current_onset"]!!.toInt()
            Log.e("InitOnset", "Checking gen tile:: " +
                    "index $index || " +
                    "onset ${onsetAgg!![index]} || " +
                    "match-time ${matchTime} || play-time ${timeline["play_time"]}")
            if(matchTime < onsetAgg!![index]) {
                // Time has not come to generate new onset
            } else {
                // Time has come - generate new onset
                val newTileOnset = generateNewTile()
                currentTiles.add(newTileOnset)

                // find closest time - to update index
                while(index < onsetAgg!!.size && onsetAgg!![index] < matchTime) {
                    index += 1
                }
                timeline["current_onset"] = index.toFloat()

                if(index > onsetAgg!!.size) {
                    // end of onset - end game here
                }
            }
        } else {
            // end game
            drawTextInside(
                "game/confirm/end_game.png",
                0.0f,
                0.5f,
                1f,
                0.6f,
                ScaleType.CENTER_INSIDE
            )
        }

        // 2. Move tiles
        if( currentTiles != null && currentTiles.size > 0){

            val updatedTiles = ArrayList<DanceGO>()
            for( i in 0 until currentTiles.size) {
                var tile = currentTiles[i]
                // Move tile
                if(tile.y < 1f) {
                    Log.e("FacePlay", "before:: ${tile.y}")
                    // Move faceTile down
                    var width = 0.2f
                    var height = 0.2f
                    // Move 20% of screen-height every second
                    val speed = (timeDelta / showTime)
                    var startY = tile!!.y + speed


                    tile!!.textScale = ScaleType.CENTER_INSIDE
                    tile!!.y = startY

                    Log.e("FacePlay", "currentTiles :: ${i} :: ${tile!!.y}")

                    drawGameObjectInside(tile)
                    updatedTiles.add(tile)
                }
            }

            currentTiles = updatedTiles
        }
    }

    private fun playMatchTileGO(tile: DanceGO) {
        // Check if face exist
        // @Todo: update game-rule:: the exact face-match + exact timing tile position - The more score you get
        if( (tile!!.y > 0.5 && tile!!.y < 0.7) &&
            GameUtils.playMatchFace(detectVertex, tile, isFlip)
        ) {
            tile.matchState = 1f

            val center = tile.y + tile.height / 2f

            if(center < 0.52 || center > 0.68) {
                drawTextInside(
                    "game/result/ok.png",
                    0.3f,
                    0.5f,
                    0.7f,
                    0.8f,
                    ScaleType.CENTER_INSIDE
                )
                if(tile.matchScore == 0f) {
                    timeline["play_score"] = timeline["play_score"]!! + 10f
                }
                tile.matchScore = 1f
            } else if(center < 0.54 || center > 0.66) {
                drawTextInside(
                    "game/result/cool.png",
                    0.3f,
                    0.5f,
                    0.7f,
                    0.8f,
                    ScaleType.CENTER_INSIDE
                )
                if(tile.matchScore == 1f) {
                    timeline["play_score"] = timeline["play_score"]!! + 20f
                }
                tile.matchScore = 2f
            } else if(center < 0.56 || center > 0.64) {
                drawTextInside(
                    "game/result/excellence.png",
                    0.3f,
                    0.5f,
                    0.7f,
                    0.8f,
                    ScaleType.CENTER_INSIDE
                )
                if(tile.matchScore == 2f) {
                    timeline["play_score"] = timeline["play_score"]!! + 30f
                }
                tile.matchScore = 3f
            } else if(center < 0.58 || center > 0.62) {
                drawTextInside(
                    "game/result/great.png",
                    0.3f,
                    0.5f,
                    0.7f,
                    0.8f,
                    ScaleType.CENTER_INSIDE
                )
                if(tile.matchScore == 3f) {
                    timeline["play_score"] = timeline["play_score"]!! + 40f
                }
                tile.matchScore = 4f
            } else if(center < 0.59 || center > 0.61) {
                drawTextInside(
                    "game/result/perfect.png",
                    0.3f,
                    0.5f,
                    0.7f,
                    0.8f,
                    ScaleType.CENTER_INSIDE
                )
                if(tile.matchScore == 4f) {
                    timeline["play_score"] = timeline["play_score"]!! + 50f
                }
                tile.matchScore = 5f
            }

        } else {
            tile.matchState = 0f
        }


    }

    private fun playMatchFaceTiles() {
        /**
        Check score hit
        Scoring rule
        Keep face-expression same as current-face-tile for a period
        Show matching signal near current-face-tile
        Getting score - correspondent to scoring-region
         */

        // Iterate all face tile
        if(detectVertex == null) {
            drawTextInside(
                "game/confirm/text_no_face.png",
                0.0f,
                0.85f,
                1f,
                0.95f,
                ScaleType.CENTER_INSIDE
            )
        } else {
            // 1. Draw hit-bar
            drawTextInside(
                "facedance/hit_region_1.png",
                0.0f,
                0.5f,
                1.0f,
                0.7f,
                ScaleType.CENTER_FIT
            )

            drawTextInside(
                "facedance/hit_region_2.png",
                0.0f,
                0.5f,
                1.0f,
                0.7f,
                ScaleType.CENTER_FIT
            )

            // 2. Draw game hit-bar
            hitBar!!.updateTimer(timeDelta)
            drawGameObjectInside(hitBar!!)

            // 2. check tile matching
            for (tile in currentTiles) {
                playMatchTileGO(tile)
            }
        }

    }

    private fun drawFinish() {

    }


}