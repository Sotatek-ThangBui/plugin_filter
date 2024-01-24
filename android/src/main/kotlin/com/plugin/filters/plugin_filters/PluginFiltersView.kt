package com.plugin.filters.plugin_filters

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.hardware.Camera
import android.media.MediaCodecInfo
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.ViewGroupOverlay
import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.core.app.ActivityCompat
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.plugin.filters.plugin_filters.enums.FilterType
import com.plugin.filters.plugin_filters.enums.RecordState
import com.plugin.filters.plugin_filters.extension.isMask
import com.plugin.filters.plugin_filters.grafika.AudioRecorder
import com.plugin.filters.plugin_filters.grafika.AudioRecorder.AudioRecordCallback
import com.plugin.filters.plugin_filters.grafika.HWEncoder
import com.plugin.filters.plugin_filters.grafika.InternalAudioRecorder
import com.plugin.filters.plugin_filters.grafika.MiscUtils
import com.plugin.filters.plugin_filters.grafika.TextureMovieEncoder2
import com.plugin.filters.plugin_filters.grafika.TextureMovieEncoder2.OnMovieEncodeListener
import com.plugin.filters.plugin_filters.grafika.VideoEncoderCore
import com.plugin.filters.plugin_filters.grafika.gles.EglCore
import com.plugin.filters.plugin_filters.grafika.gles.FullFrameRect
import com.plugin.filters.plugin_filters.grafika.gles.GlUtil
import com.plugin.filters.plugin_filters.grafika.gles.Texture2dProgram
import com.plugin.filters.plugin_filters.grafika.gles.WindowSurface
import com.plugin.filters.plugin_filters.mlkit.CameraSource
import com.plugin.filters.plugin_filters.mlkit.graphic.GraphicOverlay
import com.plugin.filters.plugin_filters.mlkit.processor.BaseProcessor
import com.plugin.filters.plugin_filters.mlkit.processor.MPFaceMeshProcessor
import com.plugin.filters.plugin_filters.model.Pin
import com.plugin.filters.plugin_filters.util.BitmapUtils
import com.plugin.filters.plugin_filters.util.glide.GlideApp
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.platform.PlatformView
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.GPUImageRenderer
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

private const val HD_VIDEO_WIDTH = 720
private const val STORAGE_IMAGE = 0L
private val TEMPLATE_NONE = Pin("", "", -1, "", "", "", "", "", false)
private const val listEffect =
    "[{\"face\":{\"boxH\":1084.0,\"boxW\":1009.0,\"xMin\":80.0,\"yMin\":335.0},\"id\":2885187254514393,\"image\":\"file:///android_asset/mask/2885187254514393.jpeg\",\"tags\":{\"topic\":[{\"name\":\"beauty\",\"value\":[\"beauty\"]}],\"type\":[{\"name\":\"mask\",\"value\":[\"mask\"]}]},\"title\":\"\"},{\"face\":{\"boxH\":606.0,\"boxW\":525.0,\"xMin\":215.0,\"yMin\":415.0},\"id\":111041947053910987,\"image\":\"file:///android_asset/mask/111041947053910987.jpeg\",\"tags\":{\"topic\":[{\"name\":\"fantasy\",\"value\":[\"alien\"]}],\"type\":[{\"name\":\"mask\",\"value\":[\"mask\"]}]},\"title\":\"\"},{\"face\":{\"boxH\":0.949582,\"boxW\":1.3504437,\"xMin\":-0.1816539,\"yMin\":0.05767002},\"id\":637329784788543343,\"image\":\"file:///android_asset/mask/637329784788543343.jpg\",\"tags\":{\"topic\":[{\"name\":\"animal\",\"value\":[\"lion\"]}],\"type\":[{\"name\":\"other\",\"value\":[\"other\"]}]},\"title\":\"\"},{\"face\":{\"boxH\":612.0,\"boxW\":536.0,\"xMin\":230.0,\"yMin\":173.0},\"id\":999869554749436686,\"image\":\"file:///android_asset/mask/999869554749436686.jpeg\",\"tags\":{\"topic\":[{\"name\":\"beauty\",\"value\":[\"beauty\"]}],\"type\":[{\"name\":\"mask\",\"value\":[\"mask\"]}]},\"title\":\"\"}]"

class PluginFiltersView(
    private val context: Context,
    private val viewId: Int?,
    private val args: Any?,
    private val messenger: BinaryMessenger?,
    private val attrs: AttributeSet?
) : PlatformView,
    MethodCallHandler, SurfaceHolder.Callback,
    Choreographer.FrameCallback, OnMovieEncodeListener {

    // See the (lengthy) notes at the top of HardwareScalerActivity for thoughts about
    // Activity / Surface lifecycle management.

    // See the (lengthy) notes at the top of HardwareScalerActivity for thoughts about
    // Activity / Surface lifecycle management.
    private var methodChannel: MethodChannel? = null
    private var parentView: FrameLayout = FrameLayout(context)
    private var graphicOverlay: GraphicOverlay? = null
    private var surfaceView: SurfaceView? = null
    private var mRecordingEnabled = false // controls button state

    private var mBlitFramebufferAllowed = false // requires GLES3

    private var mSelectedRecordMethod = 0 // current radio button
    private var data = arrayListOf<Pin>()
    private var maskSelected = 3

    private var mRenderThread: RenderThread? = null
    private var mRenderer: GPUImageRenderer? = null
    private var mVideoWidth = 720
    private var mVideoHeight = 1280
    private var resolutions = arrayListOf<CameraSource.SizePair>()
    private val quality = arrayListOf("SD", "HD", "FullHD")
    private var currentResolutionIdx = 0
    private var imageProcessor: BaseProcessor? = null
    private var videoWidth = HD_VIDEO_WIDTH
    private var videoHeight = 1280
    private var recordState: RecordState = RecordState.STOP
    private lateinit var pref: SharedPreferences
    private var mediaPlayer: MediaPlayer? = null
    private var cameraSource: CameraSource? = null
    private var isFrontCamera = true

    companion object {
        private val TAG = "thang"
        private val RECMETHOD_DRAW_TWICE = 0
        private val RECMETHOD_FBO = 1
        private val RECMETHOD_BLIT_FRAMEBUFFER = 2
    }

    init {
        methodChannel = MethodChannel(messenger!!, "plugin_filters")
        methodChannel!!.setMethodCallHandler(this)
        val token = object : TypeToken<List<Pin>>() {}.type
        val result = Gson().fromJson<List<Pin>>(listEffect, token)
        data.add(TEMPLATE_NONE)
        data.addAll(result)
        graphicOverlay = GraphicOverlay(context, null)
        setupObserver()
    }

    fun handleShowGlesVersion(version: Int) {
        if (version >= 3) {
            mSelectedRecordMethod = RECMETHOD_BLIT_FRAMEBUFFER
            mBlitFramebufferAllowed = true
        }
    }

    fun handleUpdateFps(tfps: Int, dropped: Int) {
//        String str = getString(R.string.frameRateFormat, tfps / 1000.0f, dropped);
//        TextView tv = findViewById(R.id.frameRateValue_text);
//        if (tv != null)
//            tv.setText(str);
    }

    private fun handleCapturedImage(image: Bitmap?) {}

    private fun getOutputFolder(): File {
        val file = File(
            ContextWrapper(context).filesDir,
            "Movies"
        ) //new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "ArFaces");
        if (!file.exists()) file.mkdirs()
        return file
    }

    private fun isPortraitMode(): Boolean {
        val orientation = context.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false
        }

        return orientation == Configuration.ORIENTATION_PORTRAIT
    }

    private fun removeSurface() {
        surfaceView?.holder?.removeCallback(this)
        surfaceView?.holder?.surface?.release()
        surfaceView = null

        imageProcessor?.stop()
        imageProcessor = null
    }

    private fun fetchResolutions() {
        val resolution = cameraSource?.resolutions
        this.resolutions.clear()
        resolution?.let { resolutions.addAll(it) }
    }

    private fun addSurfaceView(
        parentView: ViewGroup, requiredVideoWidth: Int, requiredVideoHeight: Int
    ) {
        parentView.post {
            if (parentView.width <= 0 || parentView.height <= 0) {
                parentView.postDelayed({
                    addSurfaceView(
                        parentView, requiredVideoWidth, requiredVideoHeight
                    )
                }, 1000)
                return@post
            }

            if (requiredVideoWidth < 1 || requiredVideoHeight < 1) {
                videoWidth = HD_VIDEO_WIDTH
                val height = HD_VIDEO_WIDTH * parentView.height / parentView.width
                videoHeight = height - height % 2
            } else {
                videoWidth = requiredVideoWidth
                videoHeight = requiredVideoHeight
            }
            //Create size of glSurfaceView which is depended on video size
            val parentWidth: Int = parentView.measuredWidth
            val parentHeight: Int = parentView.measuredHeight
            val ratioWidth: Float = videoWidth.toFloat() / parentWidth
            val ratioHeight: Float = videoHeight.toFloat() / parentHeight
            val ratio = ratioWidth.coerceAtLeast(ratioHeight)
            val viewWidth: Int
            val viewHeight: Int
            if (ratio == ratioWidth) {
                viewWidth = parentWidth
                viewHeight = (videoHeight / ratio).toInt()
            } else {
                viewWidth = (videoWidth / ratio).toInt()
                viewHeight = parentHeight
            }
            //Add glSurfaceView to parent view
            Handler(Looper.getMainLooper()).post {
                parentView.removeAllViews()
                surfaceView?.holder?.removeCallback(this)
                surfaceView?.holder?.surface?.release()
                surfaceView = SurfaceView(context)
                val surfaceLayoutParams =
                    FrameLayout.LayoutParams(viewWidth, viewHeight, Gravity.CENTER)
                surfaceView?.setZOrderOnTop(false)
                surfaceView?.holder?.addCallback(this)
                parentView.addView(surfaceView, surfaceLayoutParams)
                setVideoSize(videoWidth, videoHeight)
            }
        }
    }

    private fun changeResolution(position: Int) {
        val currentResolution = cameraSource?.previewSize
        if (position >= resolutions.size) return
        val selectResolution = resolutions[position]
        if (currentResolution == selectResolution.preview) return

        removeSurface()
        cameraSource?.setResolution(selectResolution)
        cameraSource?.stop()
        setupObserver()
    }

    private fun createAndStartCamera() {
        if (cameraSource == null) {
            cameraSource = CameraSource(context, graphicOverlay)
        }

        imageProcessor?.stop()
        imageProcessor = MPFaceMeshProcessor(context)
        val mask = data[maskSelected]
        setMask(mask)
        imageProcessor?.setOnProcessedFrameListener(object :
            BaseProcessor.OnProcessedFrameListener {
            override fun onProcessedFrame(cameraImage: Bitmap, filter: GPUImageFilter) {
                mRenderer?.setImageBitmap(cameraImage)
                mRenderer?.setFilter(filter)
            }
        })

        cameraSource?.setMachineLearningFrameProcessor(imageProcessor)

        if (isFrontCamera) {
            cameraSource?.setFacing(CameraSource.CAMERA_FACING_FRONT)
            imageProcessor?.setLensFacing(CameraSelector.LENS_FACING_FRONT)
        } else {
            cameraSource?.setFacing(CameraSource.CAMERA_FACING_BACK)
            imageProcessor?.setLensFacing(CameraSelector.LENS_FACING_BACK)
        }
        //Start camera
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                cameraSource?.start()
                fetchResolutions()
                //Show image size on debug view
                val size = cameraSource?.previewSize
                size?.let {
                    val min = it.width.coerceAtMost(it.height)
                    val max = it.width.coerceAtLeast(it.height)
                    val isImageFlipped =
                        cameraSource!!.cameraFacing == CameraSource.CAMERA_FACING_FRONT
                    if (isPortraitMode()) {
                        // Swap width and height sizes when in portrait, since it will be rotated by 90 degrees.
                        // The camera preview and the image being processed have the same size.
                        graphicOverlay?.setImageSourceInfo(min, max, isImageFlipped)
                    } else {
                        graphicOverlay?.setImageSourceInfo(max, min, isImageFlipped)
                    }

                    graphicOverlay?.clear()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun setMask(pin: Pin) {
        if (pin.id == -1L) {
            imageProcessor?.clearMask()
            return
        }
        if (pin.blendConfig != null) {
            processSticker(pin)
            return
        }

        if (pin.filter == FilterType.EYE_MANGA || pin.filter == FilterType.BEARD || pin.filter == FilterType.EXPLORE) {
            loadImageFromURL(pin.image) { image ->
                imageProcessor?.setMaskImage(
                    image,
                    pin.filter
                )
            }
            return
        }

        if (pin.filter != null && pin.filter != FilterType.MASK) {
            // @Todo: not remove mask for combining filter + mask
            imageProcessor?.clearMask()
            imageProcessor?.changeFilter(pin.filter)
            return
        }

        if (pin.id == STORAGE_IMAGE) {
            val file = File(pin.image)
            if (!file.exists()) {
                return
            }
            val bitmap = BitmapUtils.getBitmapFromFile(file, 100, 100)

            imageProcessor?.setMaskImage(bitmap, FilterType.MASK)

            return
        }

        loadImageFromURL(pin.image) { resource ->
            if (!pin.tags.isMask()) {
                cropImage(
                    resource,
                    (pin.face!!.xMin * resource.width).toInt(),
                    (pin.face!!.yMin * resource.height).toInt(),
                    (pin.face!!.boxW * resource.width).toInt(),
                    (pin.face!!.boxH * resource.height).toInt()
                )
            } else {
                imageProcessor?.setMaskImage(resource, FilterType.MASK)
            }
        }
    }

    private fun processSticker(pin: Pin) {
        loadImageFromURL(pin.image) { bitmap ->
            cropImage(
                bitmap,
                -60,
                19,
                448,
                448,
                if (pin.type == "Eye") FilterType.EYE_MANGA else FilterType.TATTOO
            )
        }
    }

    private fun cropImage(
        resource: Bitmap,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        filterType: FilterType = FilterType.ANIMAL
    ) {
        var main = resource
        if (top < 0 || left < 0 || left + width > resource.width || top + height > resource.height) {
            val bitmap = Bitmap.createBitmap(
                if (left < 0) (-left + width) else resource.width.coerceAtLeast(left + width),
                if (top < 0) (-top + height) else resource.height.coerceAtLeast(top + height),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            val paint = Paint()
            paint.isAntiAlias = true
            paint.isFilterBitmap = true
            paint.isDither = true
            canvas.drawBitmap(
                resource,
                if (left < 0) (-left * 1f) else 0f,
                if (top < 0) (-top * 1f) else 0f,
                paint
            )


            main = bitmap
        }
        val faceCrop = Bitmap.createBitmap(
            main, left.coerceAtLeast(0), top.coerceAtLeast(0), width, height
        )
        imageProcessor?.setMaskImage(faceCrop, filterType)
    }

    private fun loadImageFromURL(url: String, onLoaded: (Bitmap) -> Unit) {
        Handler(Looper.getMainLooper()).post {
            GlideApp.with(context).asBitmap()
                .load(if (url.contains("file")) Uri.parse(url) else url)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap, transition: Transition<in Bitmap>?
                    ) {
                        onLoaded.invoke(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        Toast.makeText(context, "load image error", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun setupObserver() {
        addSurfaceView(parentView, 0, 0)
        createAndStartCamera()
    }

    open fun setVideoSize(width: Int, height: Int) {
        mVideoWidth = width
        mVideoHeight = height
        if (mRenderThread != null) {
            mRenderThread?.handler?.setVideoSize(width, height)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d("thang", "surfaceCreated holder=$holder")
        mRenderer = GPUImageRenderer(GPUImageFilter())
        mRenderer!!.setScaleType(GPUImage.ScaleType.CENTER_INSIDE)
        mRenderThread = RenderThread(
            holder, ActivityHandler(this), getOutputFolder(),
            MiscUtils.getDisplayRefreshNsec(context)
        )
        mRenderThread!!.setOnMovieEncodeListener(this)
        mRenderThread!!.setVideoSize(mVideoWidth, mVideoHeight)
        mRenderThread!!.setRenderer(mRenderer)
        mRenderThread!!.setRecordMethod(mSelectedRecordMethod)
        mRenderThread!!.name = "RecordFBO GL render"
        mRenderThread!!.start()
        mRenderThread!!.waitUntilReady()

        mRenderThread!!.handler?.sendSurfaceCreated()

        // start the draw events

        // start the draw events
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(
            TAG,
            "surfaceChanged fmt=" + format + " size=" + width + "x" + height +
                    " holder=" + holder
        )
        mRenderThread?.handler?.sendSurfaceChanged(format, width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed holder=$holder")

        // We need to wait for the render thread to shut down before continuing because we
        // don't want the Surface to disappear out from under it mid-render.  The frame
        // notifications will have been stopped back in onPause(), but there might have
        // been one in progress.
        //
        // TODO: the RenderThread doesn't currently wait for the encoder / muxer to stop,
        //       so we can't use this as an indication that the .mp4 file is complete.
        val rh: RenderHandler? =
            mRenderThread?.handler
        if (rh != null) {
            rh.sendShutdown()
            try {
                mRenderThread!!.join()
            } catch (ie: InterruptedException) {
                // not expected
                throw java.lang.RuntimeException("join was interrupted", ie)
            }
        }
        mRenderThread = null
        mRecordingEnabled = false

        if (mRenderer != null) {
            mRenderer!!.deleteImage()
            mRenderer!!.clear()
            mRenderer = null
        }
        // If the callback was posted, remove it.  Without this, we could get one more
        // call on doFrame().
        // If the callback was posted, remove it.  Without this, we could get one more
        // call on doFrame().
        Choreographer.getInstance().removeFrameCallback(this)
        Log.d(TAG, "surfaceDestroyed complete")
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (mRenderThread == null) return
        val rh: RenderHandler? =
            mRenderThread?.handler
        if (rh != null) {
            Choreographer.getInstance().postFrameCallback(this)
            rh.sendDoFrame(frameTimeNanos)
        }
    }

    override fun onStopMovieEncoder(outFile: File?) {
        TODO("Not yet implemented")
    }

    /**
     * Handles messages sent from the render thread to the UI thread.
     *
     *
     * The object is created on the UI thread, and all handlers run there.
     */
    internal class ActivityHandler(context: PluginFiltersView) : Handler(Looper.getMainLooper()) {
        // Weak reference to the Activity; only access this from the UI thread.
        private val mWeakActivity: WeakReference<PluginFiltersView>

        init {
            mWeakActivity = WeakReference(context)
        }

        /**
         * Send the GLES version.
         *
         *
         * Call from non-UI thread.
         */
        fun sendGlesVersion(version: Int) {
            sendMessage(obtainMessage(MSG_GLES_VERSION, version, 0))
        }

        fun sendCapturedImage(image: Bitmap?) {
            sendMessage(obtainMessage(MSG_CAPTURED_IMAGE, image))
        }

        /**
         * Send an FPS update.  "fps" should be in thousands of frames per second
         * (i.e. fps * 1000), so we can get fractional fps even though the Handler only
         * supports passing integers.
         *
         *
         * Call from non-UI thread.
         */
        fun sendFpsUpdate(tfps: Int, dropped: Int) {
            sendMessage(obtainMessage(MSG_UPDATE_FPS, tfps, dropped))
        }

        // runs on UI thread
        override fun handleMessage(msg: Message) {
            val what = msg.what
            //Log.d(TAG, "ActivityHandler [" + this + "]: what=" + what);
            val activity = mWeakActivity.get()
            if (activity == null) {
                Log.w(
                    TAG,
                    "ActivityHandler.handleMessage: activity is null"
                )
                return
            }
            when (what) {
                MSG_GLES_VERSION -> activity.handleShowGlesVersion(msg.arg1)
                MSG_UPDATE_FPS -> activity.handleUpdateFps(msg.arg1, msg.arg2)
                MSG_CAPTURED_IMAGE -> if (msg.obj is Bitmap) activity.handleCapturedImage(msg.obj as Bitmap)
                else -> throw RuntimeException("unknown msg $what")
            }
        }

        companion object {
            private const val MSG_GLES_VERSION = 0
            private const val MSG_UPDATE_FPS = 1
            private const val MSG_CAPTURED_IMAGE = 2
        }
    }


    /**
     * This class handles all OpenGL rendering.
     *
     *
     * We use Choreographer to coordinate with the device vsync.  We deliver one frame
     * per vsync.  We can't actually know when the frame we render will be drawn, but at
     * least we get a consistent frame interval.
     *
     *
     * Start the render thread after the Surface has been created.
     */
    private class RenderThread(// may be updated by UI thread
        private val mSurfaceHolder: SurfaceHolder, // Handler we can send messages to if we want to update the app UI.
        private var mActivityHandler: ActivityHandler, outputFolder: File,
        refreshPeriodNs: Long
    ) : Thread(), AudioRecordCallback {
        /**
         * Returns the render thread's Handler.  This may be called from any thread.
         */
        // Object must be created on render thread to get correct Looper, but is used from
        // UI thread, so we need to declare it volatile to ensure the UI thread sees a fully
        // constructed object.
        @Volatile
        var handler: RenderHandler? = null
            private set

        // Used to wait for the thread to start.
        private val mStartLock = Object()
        private var mReady = false
        private var mEglCore: EglCore? = null
        private var mWindowSurface: WindowSurface? = null

        //        private FlatShadedProgram mProgram;
        // Orthographic projection matrix.
        private val mDisplayProjectionMatrix = FloatArray(16)

        // One spinning triangle, one bouncing rectangle, and four edge-boxes.
        //        private final Sprite2d mTri;
        //        private final Sprite2d mRect;
        //        private final Sprite2d[] mEdges;
        //        private final Sprite2d mRecordRect;
        private var mRectVelX = 0f
        private var mRectVelY = 0f // velocity, in viewport units per second
        private val mInnerLeft = 0f
        private val mInnerTop = 0f
        private val mInnerRight = 0f
        private val mInnerBottom = 0f
        private val mIdentityMatrix: FloatArray

        // Previous frame time.
        private var mPrevTimeNanos: Long = 0

        // FPS / drop counter.
        private val mRefreshPeriodNanos: Long
        private var mFpsCountStartNanos: Long = 0
        private var mFpsCountFrame = 0
        private var mDroppedFrames = 0
        private var mPreviousWasDropped = false

        // Used for off-screen rendering.
        private var mOffscreenTexture = 0
        private var mFramebuffer = 0
        private var mDepthBuffer = 0
        private var mFullScreen: FullFrameRect? = null

        // Used for recording.
        private var mRecordingEnabled = false
        private val mOutputFolder: File
        private var mInputWindowSurface: WindowSurface? = null
        private var mVideoEncoder: TextureMovieEncoder2? = null
        private var mRecordMethod = 0
        private var mRecordedPrevious = false
        private val mVideoRect: Rect
        private var onMovieEncodeListener: OnMovieEncodeListener? = null
        private var mRenderer: GPUImageRenderer? = null

        //        private ConfigChooser mConfigChooser;
        private var mVideoWidth = 720
        private var mVideoHeight = 1280

        //Record video with audio
        private var mRecordAudio = true
        private var mInternalAudioFile: String? = null
        private var mHWEncoder: HWEncoder? = HWEncoder()
        private var mAudioRecorder: AudioRecorder? = null
        private var mInternalAudioRecorder: InternalAudioRecorder? = null
        private var mAudioHandler: Handler? = null
        private var mVideoHandler: Handler? = null
        val capturingPicture = AtomicBoolean(false)

        private inner class AudioRunnable : Runnable {
            override fun run() {
                if (mInternalAudioRecorder != null) {
                    mInternalAudioRecorder!!.start()
                } else if (mAudioRecorder != null) {
                    if (mAudioRecorder!!.start()) {
                        Log.v("AudioRunnable", "audiorecorder succeed+" + currentThread().name)
                    } else {
                        Log.v("AudioRunnable", "audiorecorder failed+" + currentThread().name)
                    }
                }
                Looper.prepare()
                mAudioHandler = Handler(Looper.getMainLooper())
                Looper.loop()
            }
        }

        private inner class VideoRunnable : Runnable {
            override fun run() {
                Looper.prepare()
                mVideoHandler = Handler(Looper.getMainLooper())
                Looper.loop()
            }
        }

        /**
         * Pass in the SurfaceView's SurfaceHolder.  Note the Surface may not yet exist.
         */
        init {
            mOutputFolder = outputFolder
            mRefreshPeriodNanos = refreshPeriodNs
            mVideoRect = Rect()
            mIdentityMatrix = FloatArray(16)
            Matrix.setIdentityM(mIdentityMatrix, 0)

//            Drawable2d mTriDrawable = new Drawable2d(Drawable2d.Prefab.TRIANGLE);
//            mTri = new Sprite2d(mTriDrawable);
//            Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.RECTANGLE);
//            mRect = new Sprite2d(mRectDrawable);
//            mEdges = new Sprite2d[4];
//            for (int i = 0; i < mEdges.length; i++) {
//                mEdges[i] = new Sprite2d(mRectDrawable);
//            }
//            mRecordRect = new Sprite2d(mRectDrawable);
        }

        fun setOnMovieEncodeListener(onMovieEncodeListener: OnMovieEncodeListener?) {
            this.onMovieEncodeListener = onMovieEncodeListener
        }

        fun setVideoSize(videoWidth: Int, videoHeight: Int) {
            mVideoWidth = videoWidth
            mVideoHeight = videoHeight
        }

        fun setInternalAudioFile(audioFile: String?) {
            mInternalAudioFile = audioFile
        }

        fun setRecordAudio(recordAudio: Boolean) {
            mRecordAudio = recordAudio
        }

        fun setRenderer(render: GPUImageRenderer?) {
            mRenderer = render
        }

        private fun takePhoto(): Bitmap {
            val width = mWindowSurface!!.width
            val height = mWindowSurface!!.height
            val buf = ByteBuffer.allocateDirect(width * height * 4)
            buf.order(ByteOrder.LITTLE_ENDIAN)
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf)
            GlUtil.checkGlError("glReadPixels")
            buf.rewind()
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(buf)
            val matrix = android.graphics.Matrix()
            matrix.postScale(1f, -1f, bmp.width / 2f, bmp.height / 2f)
            val result = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            if (bmp != result) {
                bmp.recycle()
            }
            return result
        }

        /**
         * Thread entry point.
         *
         *
         * The thread should not be started until the Surface associated with the SurfaceHolder
         * has been created.  That way we don't have to wait for a separate "surface created"
         * message to arrive.
         */
        override fun run() {
            Looper.prepare()
            handler = RenderHandler(this)
            mEglCore = EglCore(null, EglCore.FLAG_RECORDABLE or EglCore.FLAG_TRY_GLES3)
            synchronized(mStartLock) {
                mReady = true
                mStartLock.notify() // signal waitUntilReady()
            }
            Looper.loop()
            Log.d(TAG, "looper quit")
            releaseGl()
            mEglCore!!.release()
            synchronized(mStartLock) { mReady = false }
        }

        /**
         * Waits until the render thread is ready to receive messages.
         *
         *
         * Call from the UI thread.
         */
        fun waitUntilReady() {
            synchronized(mStartLock) {
                while (!mReady) {
                    try {
                        mStartLock.wait()
                    } catch (ie: InterruptedException) { /* not expected */
                    }
                }
            }
        }

        /**
         * Shuts everything down.
         */
        fun shutdown() {
            Log.d(TAG, "shutdown")
            stopEncoder()
            Looper.myLooper()!!.quit()
        }

        /**
         * Prepares the surface.
         */
        fun surfaceCreated() {
            val surface = mSurfaceHolder.surface
            prepareGl(surface)
            if (mRenderer != null) mRenderer!!.onSurfaceCreated(null, null)
        }

        /**
         * Prepares window surface and GL state.
         */
        private fun prepareGl(surface: Surface) {
            Log.d(TAG, "prepareGl")
            mWindowSurface = WindowSurface(mEglCore, surface, false)
            mWindowSurface!!.makeCurrent()

            // Used for blitting texture to FBO.
            mFullScreen = FullFrameRect(
                Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D)
            )

            // Program used for drawing onto the screen.
            //mProgram = new FlatShadedProgram();

            // Set the background color.
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

            // Disable depth testing -- we're 2D only.
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)

            // Don't need backface culling.  (If you're feeling pedantic, you can turn it on to
            // make sure we're defining our shapes correctly.)
            GLES20.glDisable(GLES20.GL_CULL_FACE)
            mActivityHandler.sendGlesVersion(mEglCore!!.glVersion)
        }

        /**
         * Handles changes to the size of the underlying surface.  Adjusts viewport as needed.
         * Must be called before we start drawing.
         * (Called from RenderHandler.)
         */
        fun surfaceChanged(width: Int, height: Int) {
            Log.d(TAG, "surfaceChanged " + width + "x" + height)
            prepareFramebuffer(width, height)

            // Use full window.
            GLES20.glViewport(0, 0, width, height)

            // Simple orthographic projection, with (0,0) in lower-left corner.
            Matrix.orthoM(
                mDisplayProjectionMatrix,
                0,
                0f,
                width.toFloat(),
                0f,
                height.toFloat(),
                -1f,
                1f
            )
            val smallDim = Math.min(width, height)

            // Set initial shape size / position / velocity based on window size.  Movement
            // has the same "feel" on all devices, but the actual path will vary depending
            // on the screen proportions.  We do it here, rather than defining fixed values
            // and tweaking the projection matrix, so that our squares are square.
//            mTri.setColor(0.1f, 0.9f, 0.1f);
//            mTri.setScale(smallDim / 4.0f, smallDim / 4.0f);
//            mTri.setPosition(width / 2.0f, height / 2.0f);
//            mRect.setColor(0.9f, 0.1f, 0.1f);
//            mRect.setScale(smallDim / 8.0f, smallDim / 8.0f);
//            mRect.setPosition(width / 2.0f, height / 2.0f);
            mRectVelX = 1 + smallDim / 4.0f
            mRectVelY = 1 + smallDim / 5.0f

            // left edge
//            float edgeWidth = 1 + width / 64.0f;
//            mEdges[0].setScale(edgeWidth, height);
//            mEdges[0].setPosition(edgeWidth / 2.0f, height / 2.0f);
//            // right edge
//            mEdges[1].setScale(edgeWidth, height);
//            mEdges[1].setPosition(width - edgeWidth / 2.0f, height / 2.0f);
//            // top edge
//            mEdges[2].setScale(width, edgeWidth);
//            mEdges[2].setPosition(width / 2.0f, height - edgeWidth / 2.0f);
//            // bottom edge
//            mEdges[3].setScale(width, edgeWidth);
//            mEdges[3].setPosition(width / 2.0f, edgeWidth / 2.0f);
//
//            mRecordRect.setColor(1.0f, 1.0f, 1.0f);
//            mRecordRect.setScale(edgeWidth * 2f, edgeWidth * 2f);
//            mRecordRect.setPosition(edgeWidth / 2.0f, edgeWidth / 2.0f);

            // Inner bounding rect, used to bounce objects off the walls.
//            mInnerLeft = mInnerBottom = edgeWidth;
//            mInnerRight = width - 1 - edgeWidth;
//            mInnerTop = height - 1 - edgeWidth;
//
//            Log.d(TAG, "mTri: " + mTri);
//            Log.d(TAG, "mRect: " + mRect);
            if (mRenderer != null) mRenderer!!.onSurfaceChanged(null, width, height)
        }

        /**
         * Prepares the off-screen framebuffer.
         */
        private fun prepareFramebuffer(width: Int, height: Int) {
            GlUtil.checkGlError("prepareFramebuffer start")
            val values = IntArray(1)

            // Create a texture object and bind it.  This will be the color buffer.
            GLES20.glGenTextures(1, values, 0)
            GlUtil.checkGlError("glGenTextures")
            mOffscreenTexture = values[0] // expected > 0
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOffscreenTexture)
            GlUtil.checkGlError("glBindTexture $mOffscreenTexture")

            // Create texture storage.
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
            )

            // Set parameters.  We're probably using non-power-of-two dimensions, so
            // some values may not be available for use.
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST.toFloat()
            )
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR.toFloat()
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GlUtil.checkGlError("glTexParameter")

            // Create framebuffer object and bind it.
            GLES20.glGenFramebuffers(1, values, 0)
            GlUtil.checkGlError("glGenFramebuffers")
            mFramebuffer = values[0] // expected > 0
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer)
            GlUtil.checkGlError("glBindFramebuffer $mFramebuffer")

            // Create a depth buffer and bind it.
            GLES20.glGenRenderbuffers(1, values, 0)
            GlUtil.checkGlError("glGenRenderbuffers")
            mDepthBuffer = values[0] // expected > 0
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBuffer)
            GlUtil.checkGlError("glBindRenderbuffer $mDepthBuffer")

            // Allocate storage for the depth buffer.
            GLES20.glRenderbufferStorage(
                GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                width, height
            )
            GlUtil.checkGlError("glRenderbufferStorage")

            // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
            GLES20.glFramebufferRenderbuffer(
                GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, mDepthBuffer
            )
            GlUtil.checkGlError("glFramebufferRenderbuffer")
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mOffscreenTexture, 0
            )
            GlUtil.checkGlError("glFramebufferTexture2D")

            // See if GLES is happy with all this.
            val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                throw RuntimeException("Framebuffer not complete, status=$status")
            }

            // Switch back to the default framebuffer.
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            GlUtil.checkGlError("prepareFramebuffer done")
        }

        /**
         * Releases most of the GL resources we currently hold.
         *
         *
         * Does not release EglCore.
         */
        private fun releaseGl() {
            GlUtil.checkGlError("releaseGl start")
            val values = IntArray(1)
            if (mWindowSurface != null) {
                mWindowSurface!!.release()
                mWindowSurface = null
            }
            //            if (mProgram != null) {
//                mProgram.release();
//                mProgram = null;
//            }
            if (mOffscreenTexture > 0) {
                values[0] = mOffscreenTexture
                GLES20.glDeleteTextures(1, values, 0)
                mOffscreenTexture = -1
            }
            if (mFramebuffer > 0) {
                values[0] = mFramebuffer
                GLES20.glDeleteFramebuffers(1, values, 0)
                mFramebuffer = -1
            }
            if (mDepthBuffer > 0) {
                values[0] = mDepthBuffer
                GLES20.glDeleteRenderbuffers(1, values, 0)
                mDepthBuffer = -1
            }
            if (mFullScreen != null) {
                mFullScreen!!.release(false) // TODO: should be "true"; must ensure mEglCore current
                mFullScreen = null
            }
            GlUtil.checkGlError("releaseGl done")
            mEglCore!!.makeNothingCurrent()
        }

        /**
         * Updates the recording state.  Stops or starts recording as needed.
         */
        fun setRecordingEnabled(enabled: Boolean, enableRecordMic: Boolean) {
            setRecordAudio(enableRecordMic)
            if (enabled == mRecordingEnabled) {
                return
            }
            if (enabled) {
                startEncoder()
            } else {
                stopEncoder()
            }
            mRecordingEnabled = enabled
        }

        /**
         * Changes the method we use to render frames to the encoder.
         */
        fun setRecordMethod(recordMethod: Int) {
            Log.d(TAG, "RT: setRecordMethod $recordMethod")
            mRecordMethod = recordMethod
        }

        /**
         * Creates the video encoder object and starts the encoder thread.  Creates an EGL
         * surface for encoder input.
         */
        private fun startEncoder() {
            Log.d(TAG, "starting to record")
            // Record at 1280x720, regardless of the window dimensions.  The encoder may
            // explode if given "strange" dimensions, e.g. a width that is not a multiple
            // of 16.  We can box it as needed to preserve dimensions.
            val BIT_RATE = 4000000 // 4Mbps
            val windowWidth = mWindowSurface!!.width
            val windowHeight = mWindowSurface!!.height
            val windowAspect = windowHeight.toFloat() / windowWidth.toFloat()
            val outWidth: Int
            val outHeight: Int
            if (mVideoHeight > mVideoWidth * windowAspect) {
                // limited by narrow width; reduce height
                outWidth = mVideoWidth
                outHeight = (mVideoWidth * windowAspect).toInt()
            } else {
                // limited by short height; restrict width
                outHeight = mVideoHeight
                outWidth = (mVideoHeight / windowAspect).toInt()
            }
            val offX = (mVideoWidth - outWidth) / 2
            val offY = (mVideoHeight - outHeight) / 2
            mVideoRect[offX, offY, offX + outWidth] = offY + outHeight
            Log.d(
                TAG,
                "Adjusting window " + windowWidth + "x" + windowHeight +
                        " to +" + offX + ",+" + offY + " " +
                        mVideoRect.width() + "x" + mVideoRect.height()
            )
            val outVideoFile = File(mOutputFolder, System.currentTimeMillis().toString() + ".mp4")
            if (mRecordAudio) {
                try {
                    val channels = 1
                    val sampleRate = 48000
                    val mImageFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                    mHWEncoder = HWEncoder()
                    mHWEncoder!!.init(
                        mVideoWidth,
                        mVideoHeight,
                        mImageFormat,
                        BIT_RATE,
                        sampleRate,
                        channels,
                        outVideoFile.absolutePath
                    )
                    mHWEncoder!!.setOnMovieEncodeListener(onMovieEncodeListener)
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
                mInputWindowSurface = WindowSurface(mEglCore, mHWEncoder!!.inputSurface, true)
                if (TextUtils.isEmpty(mInternalAudioFile)) {
                    mAudioRecorder = AudioRecorder()
                    mAudioRecorder!!.setRecordCallback(this)
                } else {
                    try {
                        mInternalAudioRecorder = InternalAudioRecorder(mInternalAudioFile)
                        mInternalAudioRecorder!!.setRecordCallback(this)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        mInternalAudioRecorder = null
                    }
                }
                Thread(AudioRunnable()).start()
                Thread(VideoRunnable()).start()
            } else {
                val encoderCore: VideoEncoderCore = try {
                    VideoEncoderCore(mVideoWidth, mVideoHeight, BIT_RATE, outVideoFile)
                } catch (ioe: IOException) {
                    throw RuntimeException(ioe)
                }
                mInputWindowSurface = WindowSurface(mEglCore, encoderCore.inputSurface, true)
                mVideoEncoder = TextureMovieEncoder2(encoderCore)
                mVideoEncoder!!.setOnMovieEncodeListener(onMovieEncodeListener)
            }
        }

        /**
         * Stops the video encoder if it's running.
         */
        private fun stopEncoder() {
            if (mVideoEncoder != null) {
                Log.d(
                    TAG,
                    "stopping recorder, mVideoEncoder=$mVideoEncoder"
                )
                mVideoEncoder!!.stopRecording()
                // TODO: wait (briefly) until it finishes shutting down so we know file is
                //       complete, or have a callback that updates the UI
                mVideoEncoder = null
            }
            //Record video with audio
            if (mHWEncoder != null) {
                mHWEncoder!!.stop()
                mHWEncoder = null
            }
            if (mInternalAudioRecorder != null) {
                mInternalAudioRecorder!!.stop()
            }
            if (mAudioRecorder != null) mAudioRecorder!!.stop()
            if (mAudioHandler != null) {
                mAudioHandler!!.removeCallbacksAndMessages(null)
                mAudioHandler = null
            }
            if (mVideoHandler != null) {
                mVideoHandler!!.removeCallbacksAndMessages(null)
                mVideoHandler = null
            }
            //Release input window surface
            if (mInputWindowSurface != null) {
                mInputWindowSurface!!.release()
                mInputWindowSurface = null
            }
            //Show saved path
        }

        /**
         * Advance state and draw frame in response to a vsync event.
         */
        fun doFrame(timeStampNanos: Long) {
            // If we're not keeping up 60fps -- maybe something in the system is busy, maybe
            // recording is too expensive, maybe the CPU frequency governor thinks we're
            // not doing and wants to drop the clock frequencies -- we need to drop frames
            // to catch up.  The "timeStampNanos" value is based on the system monotonic
            // clock, as is System.nanoTime(), so we can compare the values directly.
            //
            // Our clumsy collision detection isn't sophisticated enough to deal with large
            // time gaps, but it's nearly cost-free, so we go ahead and do the computation
            // either way.
            //
            // We can reduce the overhead of recording, as well as the size of the movie,
            // by recording at ~30fps instead of the display refresh rate.  As a quick hack
            // we just record every-other frame, using a "recorded previous" flag.
            update(timeStampNanos)
            val diff = System.nanoTime() - timeStampNanos
            val max = mRefreshPeriodNanos - 2000000 // if we're within 2ms, don't bother
            if (diff > max) {
                // too much, drop a frame
                Log.d(
                    TAG,
                    "diff is " + diff / 1000000.0 + " ms, max " + max / 1000000.0 +
                            ", skipping render"
                )
                mRecordedPrevious = false
                mPreviousWasDropped = true
                mDroppedFrames++
                return
            }
            val swapResult: Boolean
            if (!mRecordingEnabled || mRecordedPrevious) {
                mRecordedPrevious = false
                // Render the scene, swap back to front.
                draw()
                swapResult = mWindowSurface!!.swapBuffers()
            } else {
                mRecordedPrevious = true

                // recording
                if (mRecordMethod == RECMETHOD_DRAW_TWICE) {
                    //Log.d(TAG, "MODE: draw 2x");

                    // Draw for display, swap.
                    draw()
                    swapResult = mWindowSurface!!.swapBuffers()

                    // Draw for recording, swap.
                    onFrameAvailable()
                    mInputWindowSurface!!.makeCurrent()
                    // If we don't set the scissor rect, the glClear() we use to draw the
                    // light-grey background will draw outside the viewport and muck up our
                    // letterboxing.  Might be better if we disabled the test immediately after
                    // the glClear().  Of course, if we were clearing the frame background to
                    // black it wouldn't matter.
                    //
                    // We do still need to clear the pixels outside the scissor rect, of course,
                    // or we'll get garbage at the edges of the recording.  We can either clear
                    // the whole thing and accept that there will be a lot of overdraw, or we
                    // can issue multiple scissor/clear calls.  Some GPUs may have a special
                    // optimization for zeroing out the color buffer.
                    //
                    // For now, be lazy and zero the whole thing.  At some point we need to
                    // examine the performance here.
                    GLES20.glClearColor(0f, 0f, 0f, 1f)
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                    GLES20.glViewport(
                        mVideoRect.left, mVideoRect.top,
                        mVideoRect.width(), mVideoRect.height()
                    )
                    GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
                    GLES20.glScissor(
                        mVideoRect.left, mVideoRect.top,
                        mVideoRect.width(), mVideoRect.height()
                    )
                    draw()
                    GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
                    mInputWindowSurface!!.setPresentationTime(timeStampNanos)
                    mInputWindowSurface!!.swapBuffers()

                    // Restore.
                    GLES20.glViewport(0, 0, mWindowSurface!!.width, mWindowSurface!!.height)
                    mWindowSurface!!.makeCurrent()
                } else if (mEglCore!!.glVersion >= 3 &&
                    mRecordMethod == RECMETHOD_BLIT_FRAMEBUFFER
                ) {
                    //Log.d(TAG, "MODE: blitFramebuffer");
                    // Draw the frame, but don't swap it yet.
                    draw()
                    onFrameAvailable()
                    mInputWindowSurface!!.makeCurrentReadFrom(mWindowSurface)
                    // Clear the pixels we're not going to overwrite with the blit.  Once again,
                    // this is excessive -- we don't need to clear the entire screen.
                    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                    GlUtil.checkGlError("before glBlitFramebuffer")
                    Log.v(
                        TAG,
                        "glBlitFramebuffer: 0,0," + mWindowSurface!!.width + "," +
                                mWindowSurface!!.height + "  " + mVideoRect.left + "," +
                                mVideoRect.top + "," + mVideoRect.right + "," + mVideoRect.bottom +
                                "  COLOR_BUFFER GL_NEAREST"
                    )
                    GLES30.glBlitFramebuffer(
                        0, 0, mWindowSurface!!.width, mWindowSurface!!.height,
                        mVideoRect.left, mVideoRect.top, mVideoRect.right, mVideoRect.bottom,
                        GLES30.GL_COLOR_BUFFER_BIT, GLES30.GL_NEAREST
                    )
                    var err: Int
                    if (GLES30.glGetError().also { err = it } != GLES30.GL_NO_ERROR) {
                        Log.w(
                            TAG, "ERROR: glBlitFramebuffer failed: 0x" +
                                    Integer.toHexString(err)
                        )
                    }
                    mInputWindowSurface!!.setPresentationTime(timeStampNanos)
                    mInputWindowSurface!!.swapBuffers()

                    // Now swap the display buffer.
                    mWindowSurface!!.makeCurrent()
                    swapResult = mWindowSurface!!.swapBuffers()
                } else {
                    //Log.d(TAG, "MODE: offscreen + blit 2x");
                    // Render offscreen.
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer)
                    GlUtil.checkGlError("glBindFramebuffer")
                    draw()

                    // Blit to display.
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                    GlUtil.checkGlError("glBindFramebuffer")
                    mFullScreen!!.drawFrame(mOffscreenTexture, mIdentityMatrix)
                    swapResult = mWindowSurface!!.swapBuffers()

                    // Blit to encoder.
                    onFrameAvailable()
                    mInputWindowSurface!!.makeCurrent()
                    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f) // again, only really need to
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT) //  clear pixels outside rect
                    GLES20.glViewport(
                        mVideoRect.left, mVideoRect.top,
                        mVideoRect.width(), mVideoRect.height()
                    )
                    mFullScreen!!.drawFrame(mOffscreenTexture, mIdentityMatrix)
                    mInputWindowSurface!!.setPresentationTime(timeStampNanos)
                    mInputWindowSurface!!.swapBuffers()

                    // Restore previous values.
                    GLES20.glViewport(0, 0, mWindowSurface!!.width, mWindowSurface!!.height)
                    mWindowSurface!!.makeCurrent()
                }
            }
            mPreviousWasDropped = false
            if (!swapResult) {
                // This can happen if the Activity stops without waiting for us to halt.
                Log.w(TAG, "swapBuffers failed, killing renderer thread")
                shutdown()
                return
            }

            // Update the FPS counter.
            //
            // Ideally we'd generate something approximate quickly to make the UI look
            // reasonable, then ease into longer sampling periods.
            val NUM_FRAMES = 120
            val ONE_TRILLION = 1000000000000L
            if (mFpsCountStartNanos == 0L) {
                mFpsCountStartNanos = timeStampNanos
                mFpsCountFrame = 0
            } else {
                mFpsCountFrame++
                if (mFpsCountFrame == NUM_FRAMES) {
                    // compute thousands of frames per second
                    val elapsed = timeStampNanos - mFpsCountStartNanos
                    mActivityHandler.sendFpsUpdate(
                        (NUM_FRAMES * ONE_TRILLION / elapsed).toInt(),
                        mDroppedFrames
                    )

                    // reset
                    mFpsCountStartNanos = timeStampNanos
                    mFpsCountFrame = 0
                }
            }
        }

        /**
         * We use the time delta from the previous event to determine how far everything
         * moves.  Ideally this will yield identical animation sequences regardless of
         * the device's actual refresh rate.
         */
        private fun update(timeStampNanos: Long) {
            // Compute time from previous frame.
            var intervalNanos: Long
            if (mPrevTimeNanos == 0L) {
                intervalNanos = 0
            } else {
                intervalNanos = timeStampNanos - mPrevTimeNanos
                val ONE_SECOND_NANOS = 1000000000L
                if (intervalNanos > ONE_SECOND_NANOS) {
                    // A gap this big should only happen if something paused us.  We can
                    // either cap the delta at one second, or just pretend like this is
                    // the first frame and not advance at all.
                    Log.d(
                        TAG,
                        "Time delta too large: " + intervalNanos.toDouble() / ONE_SECOND_NANOS + " sec"
                    )
                    intervalNanos = 0
                }
            }
            mPrevTimeNanos = timeStampNanos
            val ONE_BILLION_F = 1000000000.0f
            val elapsedSeconds = intervalNanos / ONE_BILLION_F

            // Spin the triangle.  We want one full 360-degree rotation every 3 seconds,
            // or 120 degrees per second.
//            final int SECS_PER_SPIN = 3;
//            float angleDelta = (360.0f / SECS_PER_SPIN) * elapsedSeconds;
//            mTri.setRotation(mTri.getRotation() + angleDelta);

            // Bounce the rect around the screen.  The rect is a 1x1 square scaled up to NxN.
            // We don't do fancy collision detection, so it's possible for the box to slightly
            // overlap the edges.  We draw the edges last, so it's not noticeable.
//            float xpos = mRect.getPositionX();
//            float ypos = mRect.getPositionY();
//            float xscale = mRect.getScaleX();
//            float yscale = mRect.getScaleY();
//            xpos += mRectVelX * elapsedSeconds;
//            ypos += mRectVelY * elapsedSeconds;
//            if ((mRectVelX < 0 && xpos - xscale / 2 < mInnerLeft) ||
//                    (mRectVelX > 0 && xpos + xscale / 2 > mInnerRight + 1)) {
//                mRectVelX = -mRectVelX;
//            }
//            if ((mRectVelY < 0 && ypos - yscale / 2 < mInnerBottom) ||
//                    (mRectVelY > 0 && ypos + yscale / 2 > mInnerTop + 1)) {
//                mRectVelY = -mRectVelY;
//            }
//            mRect.setPosition(xpos, ypos);
        }

        /**
         * Draws the scene.
         */
        private fun draw() {
            GlUtil.checkGlError("draw start")

            // Clear to a non-black color to make the content easily differentiable from
            // the pillar-/letter-boxing.
            GLES20.glClearColor(0f, 0f, 0f, 1.0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            if (mRenderer != null) {
                mRenderer!!.onDrawFrame(null)
            } else {
                Log.e(TAG, "draw(): mRenderer is null")
            }
            if (capturingPicture.get()) {
                val image = takePhoto()
                mActivityHandler.sendCapturedImage(image)
                capturingPicture.set(false)
            }
            GlUtil.checkGlError("draw done")
        }

        override fun onRecordSample(data: ByteArray) {
            if (mAudioHandler == null) {
                return
            }
            mAudioHandler!!.post {
                if (mHWEncoder != null) try {
                    mHWEncoder!!.recordSample(data)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun onFrameAvailable() {
            if (mRecordAudio && mHWEncoder != null) {
                Log.d(
                    TAG,
                    "onFrameAvailable:  " + (mVideoHandler == null)
                )
                if (mVideoHandler == null) {
                    Log.d(
                        TAG,
                        "onFrameAvailable:  new " + (mVideoHandler == null)
                    )
                    mVideoHandler = Handler(Looper.getMainLooper())
                }
                mVideoHandler!!.post {
                    try {
                        if (mHWEncoder != null) mHWEncoder!!.recordImage()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else if (mVideoEncoder != null) {
                mVideoEncoder!!.frameAvailableSoon()
            }
        }
    }


    /**
     * Handler for RenderThread.  Used for messages sent from the UI thread to the render thread.
     *
     *
     * The object is created on the render thread, and the various "send" methods are called
     * from the UI thread.
     */
    private class RenderHandler(rt: RenderThread) : Handler(Looper.getMainLooper()) {
        // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
        // but no real harm in it.
        private val mWeakRenderThread: WeakReference<RenderThread>

        /**
         * Call from render thread.
         */
        init {
            mWeakRenderThread = WeakReference(rt)
        }

        /**
         * Sends the "surface created" message.
         *
         *
         * Call from UI thread.
         */
        fun sendSurfaceCreated() {
            sendMessage(obtainMessage(MSG_SURFACE_CREATED))
        }

        /**
         * Sends the "surface changed" message, forwarding what we got from the SurfaceHolder.
         *
         *
         * Call from UI thread.
         */
        fun sendSurfaceChanged(
            @Suppress("unused") format: Int,
            width: Int, height: Int
        ) {
            // ignore format
            sendMessage(obtainMessage(MSG_SURFACE_CHANGED, width, height))
        }

        /**
         * Sends the "do frame" message, forwarding the Choreographer event.
         *
         *
         * Call from UI thread.
         */
        fun sendDoFrame(frameTimeNanos: Long) {
            sendMessage(
                obtainMessage(
                    MSG_DO_FRAME,
                    (frameTimeNanos shr 32).toInt(),
                    frameTimeNanos.toInt()
                )
            )
        }

        /**
         * Enable or disable recording.
         *
         *
         * Call from non-UI thread.
         */
        fun setRecordingEnabled(enabled: Boolean, enableRecordMic: Boolean) {
            sendMessage(
                obtainMessage(
                    MSG_RECORDING_ENABLED,
                    if (enabled) 1 else 0,
                    if (enableRecordMic) 0 else -1
                )
            )
        }

        /**
         * Set the method used to render a frame for the encoder.
         *
         *
         * Call from non-UI thread.
         */
        fun setRecordMethod(recordMethod: Int) {
            sendMessage(obtainMessage(MSG_RECORD_METHOD, recordMethod, 0))
        }

        fun setVideoSize(videoWidth: Int, videoHeight: Int) {
            sendMessage(obtainMessage(MSG_SET_VIDEO_SIZE, videoWidth, videoHeight))
        }

        fun takePhoto() {
            sendMessage(obtainMessage(MSG_TAKE_PHOTO))
        }

        /**
         * Sends the "shutdown" message, which tells the render thread to halt.
         *
         *
         * Call from UI thread.
         */
        fun sendShutdown() {
            sendMessage(obtainMessage(MSG_SHUTDOWN))
        }

        fun setInternalAudioFile(audioFile: String?) {
            sendMessage(obtainMessage(MSG_SET_INTERNAL_AUDIO_FILE, audioFile))
        }

        // runs on RenderThread
        override fun handleMessage(msg: Message) {
            val what = msg.what
            //Log.d(TAG, "RenderHandler [" + this + "]: what=" + what);
            val renderThread = mWeakRenderThread.get()
            if (renderThread == null) {
                Log.w(
                    TAG,
                    "RenderHandler.handleMessage: weak ref is null"
                )
                return
            }
            when (what) {
                MSG_SURFACE_CREATED -> renderThread.surfaceCreated()
                MSG_SURFACE_CHANGED -> renderThread.surfaceChanged(msg.arg1, msg.arg2)
                MSG_DO_FRAME -> {
                    val timestamp = msg.arg1.toLong() shl 32 or
                            (msg.arg2.toLong() and 0xffffffffL)
                    renderThread.doFrame(timestamp)
                }

                MSG_RECORDING_ENABLED -> renderThread.setRecordingEnabled(
                    msg.arg1 != 0,
                    msg.arg2 == 0
                )

                MSG_RECORD_METHOD -> renderThread.setRecordMethod(msg.arg1)
                MSG_SHUTDOWN -> renderThread.shutdown()
                MSG_SET_VIDEO_SIZE -> renderThread.setVideoSize(msg.arg1, msg.arg2)
                MSG_TAKE_PHOTO -> renderThread.capturingPicture.set(true)
                MSG_SET_INTERNAL_AUDIO_FILE -> renderThread.setInternalAudioFile(msg.obj as String)
                else -> throw RuntimeException("unknown message $what")
            }
        }

        companion object {
            private const val MSG_SURFACE_CREATED = 0
            private const val MSG_SURFACE_CHANGED = 1
            private const val MSG_DO_FRAME = 2
            private const val MSG_RECORDING_ENABLED = 3
            private const val MSG_RECORD_METHOD = 4
            private const val MSG_SHUTDOWN = 5
            private const val MSG_SET_VIDEO_SIZE = 6
            private const val MSG_TAKE_PHOTO = 7
            private const val MSG_SET_INTERNAL_AUDIO_FILE = 8
        }
    }

    override fun getView(): View? {
        return parentView
    }

    override fun dispose() {}

    override fun onMethodCall(p0: MethodCall, p1: MethodChannel.Result) {
        handleCall(p0, p1)
    }

    private fun handleCall(methodCall: MethodCall, result: MethodChannel.Result) {
        when (methodCall.method) {
            "switchCamera" -> {
                isFrontCamera = !isFrontCamera
                removeSurface()
                cameraSource?.stop()
                setupObserver()
            }

            "setFilter" -> {
                maskSelected = methodCall.argument<Int>("filter_selected")?.toInt() ?: 0
                setMask(data[maskSelected])
            }

            else -> {}
        }
    }
}