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
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.BeardMangaFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.CelebrityFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.DoctorStrangeFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.DoubleFaceFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.ExploreFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.EyeMangaFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.FacePartFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.OneEyeFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.RevertFaceFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.TattooFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.ml.FaceSwapFilter
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import jp.co.cyberagent.android.gpuimage.ml.ScaleType

open class BaseProcessor(
    protected val context: Context
) : VisionProcessorBase<List<FaceMesh>>(context) {
    interface OnProcessedFrameListener {
        fun onProcessedFrame(cameraImage: Bitmap, filter: GPUImageFilter)
    }

    private lateinit var detector: FaceMeshDetector
    private lateinit var maskDetector: MPFaceMeshDetector
    private var maskImage: Bitmap? = null
    protected var filter: FaceSwapFilter? = null
    private var faceUv: FloatArray? = null
    private var isFlip = true
    private var scaleType: ScaleType = ScaleType.CENTER_CROP
    private var processTime: Long = 0
    private var debugFrame = false
    private var onProcessedFrameListener: OnProcessedFrameListener? = null

    init {
        initDetector()
        initFilter()
    }

    fun setOnProcessedFrameListener(listener: OnProcessedFrameListener) {
        this.onProcessedFrameListener = listener
    }

    private fun initDetector() {
        val optionsBuilder = FaceMeshDetectorOptions.Builder()
        if (PreferenceUtils.getFaceMeshUseCase(context) == FaceMeshDetectorOptions.BOUNDING_BOX_ONLY) {
            optionsBuilder.setUseCase(FaceMeshDetectorOptions.BOUNDING_BOX_ONLY)
        }

        detector = FaceMeshDetection.getClient(optionsBuilder.build())
        maskDetector = MPFaceMeshDetector(context)
    }

    private fun initFilter() {
        filter = FaceSwapFilter(context, MaskMode.MASK)
    }

    fun changeAlpha(percent: Float) {
        filter?.changeAlpha(percent)
    }


    override fun setLensFacing(facing: Int) {
        isFlip = (facing == CameraSelector.LENS_FACING_FRONT)
        debugFrame = true
    }

    override fun clearMask() {
        maskImage = null
        filter?.setMaskBitmap(null)
        filter?.setData(null, null)
    }

    override fun setMaskImage(image: Bitmap, filterType: FilterType) {
        clearMask()
        maskImage = image
        when (filterType) {
            FilterType.TATTOO -> prepareTattooData()
            FilterType.EYE_MANGA -> prepareEyeFilter()
            FilterType.BEARD -> prepareBeardFilter()
            FilterType.ANIMAL -> prepareStickerData()
            FilterType.CELEBRITY -> prepareCelebrityData()
            FilterType.EXPLORE -> {
                if (filter !is ExploreFilter) {
                    filter = ExploreFilter(context)
                }
                prepareNotHumanIndex()
            }

            else -> prepareMaskData()
        }
    }

    private fun prepareEyeFilter() {
        if (filter !is EyeMangaFilter) {
            filter = EyeMangaFilter(context)
        }
        prepareNotHumanIndex()
    }

    private fun prepareBeardFilter() {
        if (filter !is BeardMangaFilter) {
            filter = BeardMangaFilter(context)
        }
        prepareNotHumanIndex()
    }

    private fun prepareTattooData() {
        if (filter !is TattooFilter || filter is EyeMangaFilter) {
            filter = TattooFilter(context)
        }
        prepareNotHumanIndex()
    }

    private fun prepareNotHumanIndex() {
        val data = VConstants.s_face_uv
        faceUv = FloatArray(data.size)
        for (i in data.indices step 2) {
            faceUv!![i + 0] = data[i].toFloat()
            faceUv!![i + 1] = data[i + 1].toFloat()
        }
    }

    override fun setMaskImage(image: Bitmap?, isHumanFace: Boolean) {
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
        filter = when (filterType) {
            FilterType.KOKUSHIBO -> FacePartFilter(context, MaskMode.MASK)
            FilterType.DOUBLE_FILTER -> DoubleFaceFilter(context, MaskMode.MASK)
            FilterType.DOCTOR_STRANGE -> DoctorStrangeFilter(context)
            FilterType.REVERT -> RevertFaceFilter(context)
            FilterType.ONE_EYE -> OneEyeFilter(context)
            else -> FaceSwapFilter(context, MaskMode.MASK)
        }
    }

    open fun prepareMaskData() {
        maskDetector.process(maskImage).addOnSuccessListener {
            if (it.multiFaceLandmarks().isEmpty()) {
                return@addOnSuccessListener
            }

            val maskLandmark = it.multiFaceLandmarks().first().landmarkList
            faceUv = FloatArray(maskLandmark.size * 2)
            for (i in maskLandmark.indices) {
                val q = maskLandmark[i]
                val a: Int = i
                faceUv!![2 * a + 0] = q.x
                faceUv!![2 * a + 1] = q.y
            }
        }
    }

    open fun prepareCelebrityData() {
        if (filter !is CelebrityFilter) {
            filter = CelebrityFilter(context)
        }
        prepareMaskData()
    }

    open fun prepareStickerData() {
        prepareNotHumanIndex()
    }

    override fun stop() {
        super.stop()
        detector.close()
    }

    override fun detectInImage(image: InputImage, cameraImage: Bitmap?): Task<List<FaceMesh>> {
        processTime = System.currentTimeMillis()
        return detector.process(image)
    }

    override fun onFailure(e: Exception) {
        e.printStackTrace()
    }

    override fun setDebugView(debug: DebugView?) {
        TODO("Not yet implemented")
    }

    override fun setMusicControl(musicControl: MusicControl?) {
        TODO("Not yet implemented")
    }

    override fun onSuccess(results: List<FaceMesh>, graphicOverlay: GraphicOverlay) {
        var cameraImage: Bitmap? = null
        for (graphic in graphicOverlay.graphics)
            if (graphic is CameraImageGraphic) {
                cameraImage = graphic.bitmap
                break
            }

        if (cameraImage == null || cameraImage!!.isRecycled) {
            Log.e("DebugFlip", "null camera image!!!")
            return
        }

        if (results.isEmpty()) {
            filterNullDetectVertex()
        } else {
            filterDetectVertex(results, cameraImage.width, cameraImage.height)
        }

        filter!!.setScaleRatio(
            scaleType,
            cameraImage.width.toFloat(), cameraImage.height.toFloat()
        )

        onProcessedFrameListener?.onProcessedFrame(cameraImage!!, filter!!)
        processTime = System.currentTimeMillis() - processTime
        Log.e("processTime", "$processTime")
    }

    private fun filterDetectVertex(
        faces: List<FaceMesh>,
        cameraImageWidth: Int,
        cameraImageHeight: Int
    ) {
        var faceVtxs = FloatArray(0)
        var meshSize = 0
        for (faceMesh in faces) {
            val keyPoints = faceMesh.allPoints
            val faceVtx = faceMesh.toArray(cameraImageWidth, cameraImageHeight)
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
}