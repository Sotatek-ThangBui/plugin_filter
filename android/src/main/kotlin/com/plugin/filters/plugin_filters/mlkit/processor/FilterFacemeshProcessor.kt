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
import com.plugin.filters.plugin_filters.enums.FilterType
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.plugin.filters.plugin_filters.extension.toArray
import com.plugin.filters.plugin_filters.mlkit.PreferenceUtils
import com.plugin.filters.plugin_filters.mlkit.graphic.CameraImageGraphic
import com.plugin.filters.plugin_filters.mlkit.graphic.GraphicOverlay
import com.plugin.filters.plugin_filters.util.VConstants
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.DoubleFaceFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.ml.FaceSwapFilter
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import jp.co.cyberagent.android.gpuimage.ml.ScaleType

/** Face Mesh Detector Demo. */
class FilterFacemeshProcessor(
    var context: Context
) :
    VisionProcessorBase<List<FaceMesh>>(context) {
    private val detector: FaceMeshDetector
    private val maskDetector: MPFaceMeshDetector
    private var maskImage: Bitmap? = null

    //    private var faceSticker: StickerObject? = null
    private var filter: FaceSwapFilter? = null
    private var filterGroup: GPUImageFilterGroup? = null

    //    private var filterGroup: GPUImageFilterGroup
//    private var faceDanceGame: GameBase
    private var faceUv: FloatArray? = null
    private var isFlip = true
    private var scaleType: ScaleType = ScaleType.CENTER_CROP
    private var processTime: Long = 0


    private var debugFrame = false

    init {
        val optionsBuilder = FaceMeshDetectorOptions.Builder()
        if (PreferenceUtils.getFaceMeshUseCase(context) == FaceMeshDetectorOptions.BOUNDING_BOX_ONLY) {
            optionsBuilder.setUseCase(FaceMeshDetectorOptions.BOUNDING_BOX_ONLY)
        }

        detector = FaceMeshDetection.getClient(optionsBuilder.build())
        maskDetector = MPFaceMeshDetector(context)
//        initSticker()
//        faceDanceGame = EyebrowGymGame(context)
        filterGroup = GPUImageFilterGroup()
//        initFaceDance()
//
//        faceDanceGame.gameFilter.initSticker(faceSticker!!)

//        filterGroup!!.addFilter(
//            //GPUImageSmoothToonFilter()
//            GPUImageColorInvertFilter()
//        )

        filter = DoubleFaceFilter(context, MaskMode.MASK)
        // EffectFilter(context, MaskMode.MASK)


        // @todo: when combining filter-group
        //  1. noise on preview + not right position for mask on recorded
        // -->> Need to handle scaleType for every filter - because fit preview region on screen
//        filter!!.flipY = -1.0f
//        filterGroup!!.addFilter(FaceSwapFilter(context, MaskMode.MASK))
        filterGroup!!.addFilter(filter)
    }


    private fun initFaceDance() {
//        faceDanceGame.init()
//        filter = faceDanceGame.gameFilter
    }

    private fun initSticker() {
//        val path = "sticker/AttackOnTitan-Jean-hair-001.png"
//        val img = BitmapUtils.getBitmapFromAsset(path, context.assets)
//        val gameObject = GameObject(
//            id = path,
//            assetPath = path,
//            textureId = OpenGlUtils.NO_TEXTURE, // unloaded texture - init to be loaded
//            width = img?.width ?: 0,
//            height = img?.height ?: 0,
//            image = img,
//            x = 0f, y = 0f, w = 0f, h = 0f,
//            sizeType = SizeType.HEIGHT,
//        )
//
//        faceSticker = StickerObject(gameObject)
//        faceSticker?.blendConfig = BlendConfig(
//            0.0, 0.0, -0.6, -0.6, 4.1
//        )
    }

    override fun setDebugView(debug: DebugView) {
//        faceDanceGame.gameFilter.debug = debug
    }

    override fun setMusicControl(musicControl: MusicControl) {
//        faceDanceGame.gameFilter.musicControl = musicControl
    }

    override fun setLensFacing(facing: Int) {

        isFlip = (facing == CameraSelector.LENS_FACING_FRONT)

        debugFrame = true
    }

    override fun setMaskImage(image: Bitmap?, filterType: FilterType?) {
        TODO("Not yet implemented")
    }

    override fun clearMask() {
        maskImage = null
        filter?.setMaskBitmap(null)
        filter?.setData(null, null)
    }

    override fun setMaskImage(image: Bitmap, isHumanFace: Boolean) {
        clearMask()

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
//        val inputImage = InputImage.fromBitmap(maskImage!!, 0)
        maskDetector.process(maskImage).addOnSuccessListener {
            if (it.multiFaceLandmarks().isEmpty()) {
                return@addOnSuccessListener
            }
            val maskLandmark = it.multiFaceLandmarks().first().landmarkList
            faceUv = FloatArray(maskLandmark.size * 2)
            for (i in maskLandmark.indices) {
                val q = maskLandmark[i]
                faceUv!![2 * i + 0] = q.x
                faceUv!![2 * i + 1] = q.y
            }
        }
//        maskDetector.process(inputImage).addOnSuccessListener {
//            if (it.isNotEmpty()) {
//                val maskLandmark = it.first().allPoints
//                faceUv = FloatArray(maskLandmark.size * 2)
//                for (i in maskLandmark.indices) {
//                    val q = maskLandmark[i]
//                    faceUv!![2 * i + 0] = q.position.x / maskImage!!.width.toFloat()
//                    faceUv!![2 * i + 1] = q.position.y / maskImage!!.height.toFloat()
//                }
//            }
//        }
    }

    private fun prepareStickerData() {
        val data = VConstants.s_face_uv
        faceUv = FloatArray(data.size)
        for (i in data.indices step 2) {
            faceUv!![i + 0] = data[i].toFloat()
            faceUv!![i + 1] = data[i + 1].toFloat()
        }
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

        if (cameraImage == null || cameraImage.isRecycled) {
            Log.e("DebugFlip", "null camera image!!!")
            return
        }


//        filter!!.setOriginBitmap(cameraImage)
        val numFace = faces.size
        if (numFace == 0) {
            filterNullDetectVertex()
            filter!!.setScaleRatio(
                scaleType,
                cameraImage.width.toFloat(), cameraImage.height.toFloat()
            )

        } else {
            filterDetectVertex(faces, cameraImage)
            filter!!.setScaleRatio(
                scaleType,
                cameraImage.width.toFloat(), cameraImage.height.toFloat()
            )

        }

        processTime = System.currentTimeMillis() - processTime
        Log.e("processTime", "$processTime")
    }

    override fun onFailure(e: Exception) {
        e.printStackTrace()
    }

    private fun filterDetectVertex(
        faces: List<FaceMesh>,
        cameraImage: Bitmap
    ) {
        var faceVtxs = FloatArray(0)
        var meshSize = 0
        for (faceMesh in faces) {
            val keyPoints = faceMesh.allPoints
            val faceVtx = faceMesh.toArray(cameraImage.width, cameraImage.height)
            faceVtxs += faceVtx
            meshSize = keyPoints.size
        }

        filter!!.setData(
            isFlip,
            meshSize,
            faces.size,
            faceVtxs,
            faceUv
        ) // null for testing no mask case
        filter!!.setMaskBitmap(maskImage) //null) // For testing no mask case
    }

    private fun filterNullDetectVertex() {
        filter!!.setMaskBitmap(null)
        filter!!.setData(isFlip, 0, 0, null, null)
    }

    companion object {
        private const val TAG = "SelfieFaceProcessor"
    }
}
