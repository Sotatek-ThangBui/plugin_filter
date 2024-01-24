/*
 * Copyright 2022 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.plugin.filters.plugin_filters.mlkit.processor

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.plugin.filters.plugin_filters.mlkit.BitmapUtils
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameBase
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameObject
import com.plugin.filters.plugin_filters.mlkit.processor.game.SizeType
import com.plugin.filters.plugin_filters.mlkit.processor.game.face.eyebrowgym.EyebrowGymGame
import com.plugin.filters.plugin_filters.mlkit.processor.game.sticker.BlendConfig
import com.plugin.filters.plugin_filters.mlkit.processor.game.sticker.StickerObject
import com.plugin.filters.plugin_filters.enums.FilterType
import com.plugin.filters.plugin_filters.mlkit.PreferenceUtils
import com.plugin.filters.plugin_filters.mlkit.graphic.CameraImageGraphic
import com.plugin.filters.plugin_filters.mlkit.graphic.GraphicOverlay
import com.plugin.filters.plugin_filters.util.VConstants
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import jp.co.cyberagent.android.gpuimage.ml.ScaleType
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils
import kotlin.math.abs

interface DebugView {
    fun updateDebug(text: String)
}

interface MusicControl {
    fun musicPrepare(url: String, caller: MusicCaller?)
    fun musicPlay(caller: MusicCaller?)
    fun musicPause()
    fun musicStop()
}

interface MusicCaller {
    fun musicDonePrepare()
    fun musicErrorPrepare(s: String)
    fun musicDonePlay()
}

/** Face Mesh Detector Demo. */
class FaceMeshDetectorProcessor(var context: Context) :
    VisionProcessorBase<List<FaceMesh>>(context) {

    private val detector: FaceMeshDetector


    private val maskDetector: FaceMeshDetector
    private var maskImage: Bitmap? = null

    private var faceSticker: StickerObject? = null


    private var filter: GameFilter? = null
    private var filterGroup: GPUImageFilterGroup
    private var faceDanceGame: GameBase
    private var faceUv: FloatArray? = null


    private var isFlip = true
    private var scaleType: ScaleType = ScaleType.CENTER_CROP

    private var processTime: Long = 0

    init {
        val optionsBuilder = FaceMeshDetectorOptions.Builder()
        if (PreferenceUtils.getFaceMeshUseCase(context) == FaceMeshDetectorOptions.BOUNDING_BOX_ONLY) {
            optionsBuilder.setUseCase(FaceMeshDetectorOptions.BOUNDING_BOX_ONLY)
        }

        detector = FaceMeshDetection.getClient(optionsBuilder.build())

        maskDetector = FaceMeshDetection.getClient(optionsBuilder.build())
        initSticker()


        faceDanceGame = EyebrowGymGame(context)
        filterGroup = GPUImageFilterGroup()
        initFaceDance()

        faceDanceGame.gameFilter.initSticker(faceSticker!!)
    }

    private fun initFaceDance() {
        faceDanceGame.init()
        filter = faceDanceGame.gameFilter
    }

    private fun initSticker() {
        val path = "sticker/AttackOnTitan-Jean-hair-001.png"
        val img = BitmapUtils.getBitmapFromAsset(path, context.assets)
        val gameObject = GameObject(
            id = path,
            assetPath = path,
            textureId = OpenGlUtils.NO_TEXTURE, // unloaded texture - init to be loaded
            width = img?.width ?: 0,
            height = img?.height ?: 0,
            image = img,

            x = 0f, y = 0f, w = 0f, h = 0f,
            sizeType = SizeType.HEIGHT,
        )

        faceSticker = StickerObject(gameObject)
        faceSticker?.blendConfig = BlendConfig(
            0.0, 0.0, -0.6, -0.6, 4.1
        )
    }

    override fun setDebugView(debug: DebugView) {
        faceDanceGame.gameFilter.debug = debug
    }

    override fun setMusicControl(musicControl: MusicControl) {
        faceDanceGame.gameFilter.musicControl = musicControl
    }

    override fun setLensFacing(facing: Int) {
        isFlip = (facing == CameraSelector.LENS_FACING_FRONT)
    }

    override fun setMaskImage(image: Bitmap?, filterType: FilterType?) {
        TODO("Not yet implemented")
    }

    override fun clearMask() {

    }

    override fun setMaskImage(image: Bitmap, isHumanFace: Boolean) {
        maskImage = image
        if (!isHumanFace) {
            prepareStickerData()
        } else {
            prepareMaskData()
        }
    }

    override fun resume() {
    }

    override fun changeFilter(filterType: FilterType) {
        TODO("Not yet implemented")
    }

    private fun prepareMaskData() {
        val inputImage = InputImage.fromBitmap(maskImage!!, 0)
        maskDetector.process(inputImage).addOnSuccessListener {
            if (it.isNotEmpty()) {
                val maskLandmark = it[0].allPoints
                faceUv = FloatArray(maskLandmark!!.size * 2)
                for (i in maskLandmark.indices) {
                    val q = maskLandmark[i]
                    faceUv!![2 * i + 0] = q.position.x / maskImage!!.width.toFloat()
                    faceUv!![2 * i + 1] = q.position.y / maskImage!!.height.toFloat()
                }
            }
        }


    }

    private fun prepareStickerData() {
        val data = VConstants.s_face_uv
        faceUv = FloatArray(data.size)
        for (i in data.indices step 2) {
            faceUv!![i + 0] = data[i].toFloat()
            faceUv!![i + 1] = data[i + 1].toFloat()
        }

        this.filter!!.setMode(MaskMode.TATTOO_CANON)
    }

    override fun stop() {
        super.stop()
        detector.close()
    }

    override fun detectInImage(image: InputImage, cameraImage: Bitmap?): Task<List<FaceMesh>> {
        processTime = System.currentTimeMillis()
        return detector.process(image)
    }

    override fun onSuccess(faces: List<FaceMesh>, graphicOverlay: GraphicOverlay) {
        var cameraImage: Bitmap? = null
        for (graphic in graphicOverlay.graphics)
            if (graphic is CameraImageGraphic) {
                cameraImage = graphic.bitmap
                break
            }

        if (cameraImage == null || cameraImage.isRecycled) return


        if (faces.isEmpty()) {
            filterNullDetectVertex()
            //recordableSurfaceUtils!!.glSurfaceView.setImageBitmap(cameraImage)
            filter!!.setScaleRatio(
                scaleType,
                cameraImage.width.toFloat(), cameraImage.height.toFloat()
            )

            // recordableSurfaceUtils!!.glSurfaceView!!.setFilter(filter)
        }

        for (face in faces) {
            filterDetectVertex(face, cameraImage)
            //recordableSurfaceUtils!!.glSurfaceView.setImageBitmap(cameraImage)
            filter!!.setScaleRatio(
                scaleType,
                cameraImage.width.toFloat(), cameraImage.height.toFloat()
            )

            // recordableSurfaceUtils!!.glSurfaceView!!.setFilter(filter)
        }

        processTime = System.currentTimeMillis() - processTime
        Log.e("processTime", "$processTime")
    }

    override fun onFailure(e: Exception) {
        e.printStackTrace()
    }

    private fun filterDetectVertex(
        faceMesh: FaceMesh,
        cameraImage: Bitmap
    ) {
        val keyPoints = faceMesh.allPoints
        val faceVtx = FloatArray(keyPoints.size * 3)
        val width = cameraImage.width
        val height = cameraImage.height
        var zMin = Float.MAX_VALUE
        var zMax = Float.MIN_VALUE
        for (point in keyPoints) {
            zMin = zMin.coerceAtMost(point.position.z)
            zMax = zMax.coerceAtLeast(point.position.z)
        }

        for (i in 0 until keyPoints.size) {
            val p = keyPoints[i]
            faceVtx[3 * i + 0] = p.position.x / width.toFloat()
            faceVtx[3 * i + 1] = p.position.y / height.toFloat()
            if (p.position.z < 0) {
                faceVtx[3 * i + 2] = p.position.z / abs(zMin)
            } else {
                faceVtx[3 * i + 2] = p.position.z / zMax
            }
        }

        this.filter!!.isFlip = isFlip
        filter!!.setData(faceVtx, faceUv)
        filter!!.setMaskBitmap(maskImage)
    }

    private fun filterNullDetectVertex() {
        filter!!.isFlip = isFlip
        filter!!.setMaskBitmap(null)
        filter!!.setData(null, null)
    }

    companion object {
        private const val TAG = "FaceMesh"
    }
}
