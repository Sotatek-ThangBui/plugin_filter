package com.plugin.filters.plugin_filters.mlkit.processor.game.pose.posedance

import android.content.Context
import android.graphics.PointF
import android.util.Log
import com.plugin.filters.plugin_filters.mlkit.processor.MusicCaller
import com.plugin.filters.plugin_filters.mlkit.processor.game.FACE_GAME_STATE
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameObject
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameUtils
import com.skymeta.arface.mlkit.processor.game.face.facedance.DanceGO
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
class PoseDanceFilter(context: Context, mode: MaskMode):
    GameFilter(context, mode),
    MusicCaller
{

    // 3. Game object pool
    private var tiles = PoseDanceConstants.TILES
    // 3.1. dance-tiles
    private var tileObjects = java.util.ArrayList<GameObject>()
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
        // Can be update for every frame-draw or update on logic function like gym-game logic
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

                    val step = 20 // 10 - for slow dance - test showing effect
                    // reduce onset size
                    var list = ArrayList<Float>()
                    for (i in 0 until jsonOnset.length() step step) {
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
        for(i in tiles.indices) {
            var startX = FloatArray(8) {(it+1)*0.1f}.random()
            var width = 0.1f
            var startY = -1.0f
            var height = 0.1f

            val textId = tiles[i]
            val faceObj = assetTexture!![textId]!!
            val cloneObj = GameObject(
                faceObj
            )
            cloneObj.textScale = ScaleType.CENTER_INSIDE
            cloneObj.x = startX
            cloneObj.y = startY
            cloneObj.w = width
            cloneObj.h = height
            tileObjects.add(cloneObj)
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
        for(i in tileObjects.indices) {
            val textId = tiles[i]
            val faceObj = assetTexture!![textId]!!

            tileObjects[i].textureId = faceObj.textureId
        }

        // 2. Init hit-bar
        hitBar!!.animTexts = java.util.ArrayList<Int>()
        for(i in 0..5) {
            val text = assetTexture!!["facedance/hit_bar/$i.png"]!!
            hitBar!!.animTexts.add(text.textureId)
        }
        hitBar!!.textureId = hitBar!!.animTexts[0]

    }




    private fun drawEffect(textureId: Int) {
        crackEffectV1(textureId)
    }


    private fun crackEffectV1(textureId: Int) {
        //simpleMirror(textureId)

        simpleGlass(textureId)
    }

    private fun simpleMirror(textureId: Int) {
        if(detectVertex != null) {

            val index = 0
            val x = detectVertex!![index*3]
            val y = detectVertex!![index*3+1]
            val z = detectVertex!![index*3+2]

            if( x > 0 && x < 1) {
                var posVertex = floatArrayOf(
                    x, 0f,
                    x, 1f,
                    1f, 0f,
                    1f, 1f
                )

                var frameVertex = floatArrayOf(
                    x, 0f,
                    x, 1f,
                    0f, 0f,
                    0f, 1f,
                )

                mirrorSize(textureId, posVertex, frameVertex)


                posVertex = floatArrayOf(
                    0f, 0f,
                    0f, 1f,
                    x, 0f,
                    x, 1f
                )

                frameVertex = floatArrayOf(
                    1f, 0f,
                    1f, 1f,
                    x, 0f,
                    x, 1f,
                )

                mirrorSize(textureId, posVertex, frameVertex)
            }
        }
    }

    private fun mirrorSize(textureId: Int, posVertex: FloatArray, frameVertex: FloatArray) {
        val posSize = 2
        setBaseVertex(posVertex, posSize)
        // 2.2. Mask-vertex as texture-vertex
        // Flip order of x to get mirror effect
        if(isFlip) {
            for( i in frameVertex.indices step 2) {
                frameVertex[i] = 1f - frameVertex[i]
            }
        }
        setOverlayVertex(frameVertex, 2)
        setDrawMode(MaskMode.OVERLAY_TEXT.drawMode)
        // 3. Activate image-texture
        bindOverlayTexture(textureId)
        // 5. Draw face-triangle mesh
        // 5.1. Init triangle-indices
        val drawIndex: ShortArray = shortArrayOf(
            0, 1, 2,
            1, 2, 3
        )
        drawTriangleByIndex(drawIndex)
        // 6. Clear GLES
        clearGLES()

    }


    private fun simpleGlass(textureId: Int) {
        if(detectVertex != null) {

            val index = 0
            val x = detectVertex!![index*3]
            val y = detectVertex!![index*3+1]
            val z = detectVertex!![index*3+2]

            val rand = java.util.Random()

            if( x > 0 && x < 1) {
                val tris = GameUtils.random4Triangle(PointF(x,y))

                for(tri in tris) {
                    val a = PointF(x,y)
                    val b = tri.first
                    val c = tri.second
                    var posVertex = floatArrayOf(
                        a.x, a.y,
                        b.x, b.y,
                        c.x, c.y,
                    )


                    var frameVertex = floatArrayOf(
                        tweak10(a.x, rand), tweak10(a.y, rand),
                        tweak10(b.x, rand), tweak10(b.y, rand),
                        tweak10(c.x, rand), tweak10(c.y, rand),
                    )

                    triangleGlass(textureId, posVertex, frameVertex)
                }

            }
        }
    }

    private fun tweak10(x:Float, rand: java.util.Random) = GameUtils.tweak(x, rand, 0.1f)

    private fun triangleGlass(textureId: Int, posVertex: FloatArray, frameVertex: FloatArray) {
        val posSize = 2
        setBaseVertex(posVertex, posSize)
        // 2.2. Mask-vertex as texture-vertex
        // Flip order of x to get mirror effect
        if(isFlip) {
            for( i in frameVertex.indices step 2) {
                frameVertex[i] = 1f - frameVertex[i]
            }
        }
        setOverlayVertex(frameVertex, 2)
        setDrawMode(MaskMode.OVERLAY_TEXT.drawMode)
        // 3. Activate image-texture
        bindOverlayTexture(textureId)
        // 5. Draw face-triangle mesh
        // 5.1. Init triangle-indices
        val drawIndex: ShortArray = shortArrayOf(
            0, 1, 2
        )
        drawTriangleByIndex(drawIndex)
        // 6. Clear GLES
        clearGLES()

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

        // 0. Draw effect
        drawEffect(textureId)

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

    private fun drawIntro() {
        if(gameState == FACE_GAME_STATE.INTRO) {
            // Draw game-icon
            if(gameStep == 0) {
                if(!timeline.containsKey("intro_step_1")) {
                    timeline["intro_step_1"] = 0f
                }

                if(timeline["intro_step_1"]!! < 1500f) {

                    drawTextInside(
                        "posedance/ic_beginboard.png",
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
                        "posedance/ic_posedance.png",
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
        // Main game logic
        if(gameState == FACE_GAME_STATE.PLAY_NORMAL
            || gameState == FACE_GAME_STATE.PLAY_PEEK
        ) {
            updateTileList()
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

    private fun genNewTile(startY:Float): DanceGO {
        // Generate new tile
        var tile = tileObjects.random()
        var startX = floatArrayOf(0.0f, 0.2f).random()
        var width = 0.8f
        var height = 0.4f

        tile.textScale = ScaleType.CENTER_CROP
        tile.x = startX
        tile.y = startY
        tile.w = width
        tile.h = height

        return DanceGO(tile)
    }

    private fun genTileByOnset(showTime: Float, hitBar: Float, startY: Float) {
        // 1. Generate tiles - Generate by onset time - match currentTime
        // Find closest onset which smaller than currentTime
        // Initialize step - init the first onset-index - closest to showTime
        // The first tile is fit with the first onset-index
        // The first tile need showTime to move from start to hitbar
        if(timeline["current_onset"] == -1f) {
            var matchTime = showTime * (hitBar-startY) //0.6f // time to hit-bar at y = 0.6f
            if(matchTime >= onsetAgg!![0]) {
                // Time has come find closest time
                var index = 0
                while(index < onsetAgg!!.size && onsetAgg!![index] < matchTime) {
                    index += 1
                }
                timeline["current_onset"] = index.toFloat()
            }

        } else if(timeline["current_onset"]!! < onsetAgg!!.size){
            // Continue generate new onset - tile need showTime to move from start to hitBar
            var matchTime = timeline["play_time"]!! + showTime * (hitBar-startY)
            var index = timeline["current_onset"]!!.toInt()

            if(matchTime >= onsetAgg!![index]) {
                // Time has come - generate new onset
                val newTileOnset = genNewTile(startY)
                currentTiles.add(newTileOnset)

                // find closest time - to update index
                while(index < onsetAgg!!.size && onsetAgg!![index] < matchTime) {
                    index += 1
                }
                timeline["current_onset"] = index.toFloat()
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
    }


    private fun moveTiles(showTime: Float) {
        if( currentTiles != null && currentTiles.size > 0){

            val updatedTiles = ArrayList<DanceGO>()
            for( i in 0 until currentTiles.size) {
                var tile = currentTiles[i]
                // Move tile
                if(tile.y < 1f) {
                    Log.e("FacePlay", "before:: ${tile.y}")
                    // Move faceTile down
                    val speed = (timeDelta / showTime)
                    tile!!.y = tile!!.y + speed

                    Log.e("FacePlay", "currentTiles :: ${i} :: ${tile!!.y}")

                    drawGameObjectInside(tile)
                    updatedTiles.add(tile)
                }
            }

            currentTiles = updatedTiles
        }
    }


    private fun updateTileList() {
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

        val hitBarPos = 0.6f

        val startY = -0.3f

        // 1. Generate new tile
        genTileByOnset(showTime, hitBarPos, startY)

        // 2. Move tiles
        moveTiles(showTime)
    }

    private fun playMatchTileGO(tile: DanceGO) {
        // Check if face exist
        // @Todo: update game-rule:: the exact face-match + exact timing tile position - The more score you get
        val center = tile.y + tile.h / 2f

        val flipVertex = GameUtils.getFlipPoseVertex(detectVertex!!, isFlip)
//        Log.e("poseLogic",
//            "flip $isFlip\n" +
//                    "")
        // Larger scoring range for easier game
        if( (center > 0.3 && center < 0.9)
            && GameUtils.playMatchPose(flipVertex, tile, this)
        ) {
            tile.matchState = 1f

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

                Log.e("PoseScore", "Current center:: $center\n" +
                        "expect (${tile.x}, ${tile.y}, ${tile.w}, ${tile.h})\n" +
                        "draw (${tile.drawW}, ${tile.drawH})")
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
                0.0f,0.85f,
                1f,0.95f,
                ScaleType.CENTER_INSIDE
            )
        } else {
            // 1. Draw hit-bar
            drawTextInside(
                "posedance/hit_region/hit_region_1.png",
                0.0f,0.5f,
                1.0f,0.7f,
                ScaleType.CENTER_FIT
            )

            drawTextInside(
                "posedance/hit_region/hit_region_2.png",
                0.0f,0.5f,
                1.0f,0.7f,
                ScaleType.CENTER_FIT
            )

            // @Todo: draw 4 corners for clear recognize of hit-region

            // 2. Draw game hit-bar
            // @Todo: update hitBar later
//            hitBar!!.updateTimer(timeDelta)
//            drawGameObjectInside(hitBar!!)

            // 2. check tile matching
            for (tile in currentTiles) {
                playMatchTileGO(tile)
            }
        }

    }

    private fun drawFinish() {

    }


}