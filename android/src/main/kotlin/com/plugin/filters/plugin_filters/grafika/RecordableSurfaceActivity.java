/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.plugin.filters.plugin_filters.grafika;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.MediaCodecInfo;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Choreographer;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.RadioButton;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageRenderer;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import com.plugin.filters.plugin_filters.BaseActivity;
import com.plugin.filters.plugin_filters.grafika.gles.EglCore;
import com.plugin.filters.plugin_filters.grafika.gles.FullFrameRect;
import com.plugin.filters.plugin_filters.grafika.gles.GlUtil;
import com.plugin.filters.plugin_filters.grafika.gles.Texture2dProgram;
import com.plugin.filters.plugin_filters.grafika.gles.WindowSurface;

/**
 * Demonstrates efficient display + recording of OpenGL rendering using an FBO.  This
 * records only the GL surface (i.e. not the app UI, nav bar, status bar, or alert dialog).
 * <p>
 * This uses a plain SurfaceView, rather than GLSurfaceView, so we have full control
 * over the EGL config and rendering.  When available, we use GLES 3, which allows us
 * to do recording with one extra copy instead of two.
 * <p>
 * We use Choreographer so our animation matches vsync, and a separate rendering
 * thread to keep the heavy lifting off of the UI thread.  Ideally we'd let the render
 * thread receive the Choreographer events directly, but that appears to be creating
 * a permanent JNI global reference to the render thread object, preventing it from
 * being garbage collected (which, in turn, causes the Activity to be retained).  So
 * instead we receive the vsync on the UI thread and forward it.
 * <p>
 * If the rendering is fairly simple, it may be more efficient to just render the scene
 * twice (i.e. configure for display, call draw(), configure for video, call draw()).  If
 * the video being created is at a lower resolution than the display, rendering at the lower
 * resolution may produce better-looking results than a downscaling blit.
 * <p>
 * To reduce the impact of recording on rendering (which is probably a fancy-looking game),
 * we want to perform the recording tasks on a separate thread.  The actual video encoding
 * is performed in a separate process by the hardware H.264 encoder, so feeding input into
 * the encoder requires little effort.  The MediaMuxer step runs on the CPU and performs
 * disk I/O, so we really want to drain the encoder on a separate thread.
 * <p>
 * Some other examples use a pair of EGL contexts, configured to share state.  We don't want
 * to do that here, because GLES3 allows us to improve performance by using glBlitFramebuffer(),
 * and framebuffer objects aren't shared.  So we use a single EGL context for rendering to
 * both the display and the video encoder.
 * <p>
 * It might appear that shifting the rendering for the encoder input to a different thread
 * would be advantageous, but in practice all of the work is done by the GPU, and submitting
 * the requests from different CPU cores isn't going to matter.
 * <p>
 * As always, we have to be careful about sharing state across threads.  By fully configuring
 * the encoder before starting the encoder thread, we ensure that the new thread sees a
 * fully-constructed object.  The encoder object then "lives" in the encoder thread.  The main
 * thread doesn't need to talk to it directly, because all of the input goes through Surface.
 * <p>
 * TODO: add another bouncing rect that uses decoded video as a texture.  Useful for
 * evaluating simultaneous video playback and recording.
 * <p>
 * TODO: show the MP4 file name somewhere in the UI so people can find it in the player
 * <p>
 * * (c) 2023 HungNV
 * * (c) 2023 HungNV Inc.
 * *
 * * @author HungNV
 * * @since 06/01/2023
 */
public abstract class RecordableSurfaceActivity extends BaseActivity implements SurfaceHolder.Callback,
        Choreographer.FrameCallback, TextureMovieEncoder2.OnMovieEncodeListener {
    private static final String TAG = RecordableSurfaceActivity.class.getSimpleName();

    // See the (lengthy) notes at the top of HardwareScalerActivity for thoughts about
    // Activity / Surface lifecycle management.

    private static final int RECMETHOD_DRAW_TWICE = 0;
    private static final int RECMETHOD_FBO = 1;
    private static final int RECMETHOD_BLIT_FRAMEBUFFER = 2;

    private boolean mRecordingEnabled = false;          // controls button state
    private boolean mBlitFramebufferAllowed = false;    // requires GLES3
    private int mSelectedRecordMethod;                  // current radio button

    private RenderThread mRenderThread;
    private GPUImageRenderer mRenderer;
    private int mVideoWidth = 720;
    private int mVideoHeight = 1280;
    //Record video with audio
    private boolean mRecordAudio = true;
    private String mInternalAudioFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelectedRecordMethod = RECMETHOD_DRAW_TWICE;
//        setContentView(R.layout.activity_record_fbo);
//        updateControls();
//        SurfaceView sv = findViewById(R.id.fboActivity_surfaceView);
//        sv.getHolder().addCallback(this);
    }

    public GPUImageRenderer getRender() {
        return mRenderer;
    }

    @Override
    protected void onPause() {
        super.onPause();

        // TODO: we might want to stop recording here.  As it is, we continue "recording",
        //       which is pretty boring since we're not outputting any frames (test this
        //       by blanking the screen with the power button).

        // If the callback was posted, remove it.  This stops the notifications.  Ideally we
        // would send a message to the thread letting it know, so when it wakes up it can
        // reset its notion of when the previous Choreographer event arrived.
        Log.d(TAG, "onPause unhooking choreographer");
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If we already have a Surface, we just need to resume the frame notifications.
        if (mRenderThread != null) {
            Log.d(TAG, "onResume re-hooking choreographer");
            Choreographer.getInstance().postFrameCallback(this);
        }

        updateControls();
    }

    public void setRecordAudio(boolean recordAudio) {
        this.mRecordAudio = recordAudio;
    }

    /**
     * Called before surfaceCreated
     *
     * @return created movie folder
     */
    protected File getOutputFolder() {
        File file = new File(getFilesDir(), "Movies");//new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "ArFaces");
        if (!file.exists())
            file.mkdirs();
        return file;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated holder=" + holder);
        mRenderer = new GPUImageRenderer(new GPUImageFilter());
        mRenderer.setScaleType(GPUImage.ScaleType.CENTER_INSIDE);
        mRenderThread = new RenderThread(holder, new ActivityHandler(this), getOutputFolder(),
                MiscUtils.getDisplayRefreshNsec(this));
        mRenderThread.setRecordAudio(mRecordAudio);
        mRenderThread.setOnMovieEncodeListener(this);
        mRenderThread.setVideoSize(mVideoWidth, mVideoHeight);
        mRenderThread.setRenderer(mRenderer);
        mRenderThread.setRecordMethod(mSelectedRecordMethod);
        mRenderThread.setInternalAudioFile(mInternalAudioFile);
        mRenderThread.setName("RecordFBO GL render");
        mRenderThread.start();
        mRenderThread.waitUntilReady();

        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            rh.sendSurfaceCreated();
        }

        // start the draw events
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged fmt=" + format + " size=" + width + "x" + height +
                " holder=" + holder);
        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            rh.sendSurfaceChanged(format, width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed holder=" + holder);

        // We need to wait for the render thread to shut down before continuing because we
        // don't want the Surface to disappear out from under it mid-render.  The frame
        // notifications will have been stopped back in onPause(), but there might have
        // been one in progress.
        //
        // TODO: the RenderThread doesn't currently wait for the encoder / muxer to stop,
        //       so we can't use this as an indication that the .mp4 file is complete.

        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            rh.sendShutdown();
            try {
                mRenderThread.join();
            } catch (InterruptedException ie) {
                // not expected
                throw new RuntimeException("join was interrupted", ie);
            }
        }
        mRenderThread = null;
        mRecordingEnabled = false;

        if (mRenderer != null) {
            mRenderer.deleteImage();
            mRenderer.clear();
            mRenderer = null;
        }
        // If the callback was posted, remove it.  Without this, we could get one more
        // call on doFrame().
        Choreographer.getInstance().removeFrameCallback(this);
        Log.d(TAG, "surfaceDestroyed complete");
    }

    /*
     * Choreographer callback, called near vsync.
     *
     * @see android.view.Choreographer.FrameCallback#doFrame(long)
     */
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mRenderThread == null) return;
        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            Choreographer.getInstance().postFrameCallback(this);
            rh.sendDoFrame(frameTimeNanos);
        }
    }

    /**
     * Updates the GLES version string.
     * <p>
     * Called from the render thread (via ActivityHandler) after the EGL context is created.
     */
    void handleShowGlesVersion(int version) {
        if (version >= 3) {
            mSelectedRecordMethod = RECMETHOD_BLIT_FRAMEBUFFER;
            mBlitFramebufferAllowed = true;
            updateControls();
        }

//        TextView tv = findViewById(R.id.glesVersionValue_text);
//        if (tv != null) {
//            tv.setText("" + version);
//        }
    }

    /**
     * Updates the FPS counter.
     * <p>
     * Called periodically from the render thread (via ActivityHandler).
     */
    void handleUpdateFps(int tfps, int dropped) {
//        String str = getString(R.string.frameRateFormat, tfps / 1000.0f, dropped);
//        TextView tv = findViewById(R.id.frameRateValue_text);
//        if (tv != null)
//            tv.setText(str);
    }

    protected void handleCapturedImage(Bitmap image) {

    }

    /**
     * onClick handler for "record" button.
     * <p>
     * Ideally we'd grey out the button while in a state of transition, e.g. while the
     * MediaMuxer finishes creating the file, and in the (very brief) period before the
     * SurfaceView's surface is created.
     */
    public void startRecording(boolean isRecording) {
        if (mRenderThread == null) return;
        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            mRecordingEnabled = isRecording;
            updateControls();
            rh.setRecordingEnabled(mRecordingEnabled, mRecordAudio);
        }
    }

    public void setInternalAudioFile(String audioFile) {
        mInternalAudioFile = audioFile;
        if (mRenderThread == null) return;
        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            rh.setInternalAudioFile(audioFile);
        }
    }

    public void takePhoto() {
        if (mRenderThread == null) return;
        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            rh.takePhoto();
        }
    }

    public boolean isRecording() {
        return mRecordingEnabled;
    }

    /**
     * onClick handler for radio buttons.
     */
    public void onRadioButtonClicked(View view) {
        RadioButton rb = (RadioButton) view;
        if (rb == null || !rb.isChecked()) {
            Log.d(TAG, "Got click on non-checked radio button");
            return;
        }

//        int id = rb.getId();
//        if (id == R.id.recDrawTwice_radio) {
//            mSelectedRecordMethod = RECMETHOD_DRAW_TWICE;
//        } else if (id == R.id.recFbo_radio) {
//            mSelectedRecordMethod = RECMETHOD_FBO;
//        } else if (id == R.id.recFramebuffer_radio) {
//            mSelectedRecordMethod = RECMETHOD_BLIT_FRAMEBUFFER;
//        } else {
//            throw new RuntimeException("Click from unknown id " + rb.getId());
//        }

        Log.d(TAG, "Selected rec mode " + mSelectedRecordMethod);
        if (mRenderThread != null) {
            RenderHandler rh = mRenderThread.getHandler();
            if (rh != null) {
                rh.setRecordMethod(mSelectedRecordMethod);
            }
        }
    }

    public void setVideoSize(int width, int height) {
        this.mVideoWidth = width;
        this.mVideoHeight = height;
        if (mRenderThread != null) {
            RenderHandler rh = mRenderThread.getHandler();
            if (rh != null) {
                rh.setVideoSize(width, height);
            }
        }
    }

    /**
     * Updates the on-screen controls to reflect the current state of the app.
     */
    protected void updateControls() {
//        Button toggleRelease = findViewById(R.id.fboRecord_button);
//        if (toggleRelease != null) {
//            int id = mRecordingEnabled ?
//                    R.string.toggleRecordingOff : R.string.toggleRecordingOn;
//            toggleRelease.setText(id);
//        }
//
//        RadioButton rb;
//        rb = findViewById(R.id.recDrawTwice_radio);
//        if (rb != null)
//            rb.setChecked(mSelectedRecordMethod == RECMETHOD_DRAW_TWICE);
//
//        rb = findViewById(R.id.recFbo_radio);
//        if (rb != null)
//            rb.setChecked(mSelectedRecordMethod == RECMETHOD_FBO);
//
//        rb = findViewById(R.id.recFramebuffer_radio);
//        if (rb != null) {
//            rb.setChecked(mSelectedRecordMethod == RECMETHOD_BLIT_FRAMEBUFFER);
//            rb.setEnabled(mBlitFramebufferAllowed);
//        }
//
//        TextView tv = findViewById(R.id.nowRecording_text);
//        if (tv != null) {
//            if (mRecordingEnabled) {
//                tv.setText(getString(R.string.nowRecording));
//            } else {
//                tv.setText("");
//            }
//        }
    }


    /**
     * Handles messages sent from the render thread to the UI thread.
     * <p>
     * The object is created on the UI thread, and all handlers run there.
     */
    static class ActivityHandler extends Handler {
        private static final int MSG_GLES_VERSION = 0;
        private static final int MSG_UPDATE_FPS = 1;

        private static final int MSG_CAPTURED_IMAGE = 2;
        // Weak reference to the Activity; only access this from the UI thread.
        private final WeakReference<RecordableSurfaceActivity> mWeakActivity;

        public ActivityHandler(RecordableSurfaceActivity activity) {
            mWeakActivity = new WeakReference<>(activity);
        }

        /**
         * Send the GLES version.
         * <p>
         * Call from non-UI thread.
         */
        public void sendGlesVersion(int version) {
            sendMessage(obtainMessage(MSG_GLES_VERSION, version, 0));
        }

        public void sendCapturedImage(Bitmap image) {
            sendMessage(obtainMessage(MSG_CAPTURED_IMAGE, image));
        }

        /**
         * Send an FPS update.  "fps" should be in thousands of frames per second
         * (i.e. fps * 1000), so we can get fractional fps even though the Handler only
         * supports passing integers.
         * <p>
         * Call from non-UI thread.
         */
        public void sendFpsUpdate(int tfps, int dropped) {
            sendMessage(obtainMessage(MSG_UPDATE_FPS, tfps, dropped));
        }

        @Override  // runs on UI thread
        public void handleMessage(Message msg) {
            int what = msg.what;
            //Log.d(TAG, "ActivityHandler [" + this + "]: what=" + what);

            RecordableSurfaceActivity activity = mWeakActivity.get();
            if (activity == null) {
                Log.w(TAG, "ActivityHandler.handleMessage: activity is null");
                return;
            }

            switch (what) {
                case MSG_GLES_VERSION:
                    activity.handleShowGlesVersion(msg.arg1);
                    break;
                case MSG_UPDATE_FPS:
                    activity.handleUpdateFps(msg.arg1, msg.arg2);
                    break;
                case MSG_CAPTURED_IMAGE:
                    if (msg.obj instanceof Bitmap)
                        activity.handleCapturedImage((Bitmap) msg.obj);
                    break;
                default:
                    throw new RuntimeException("unknown msg " + what);
            }
        }
    }


    /**
     * This class handles all OpenGL rendering.
     * <p>
     * We use Choreographer to coordinate with the device vsync.  We deliver one frame
     * per vsync.  We can't actually know when the frame we render will be drawn, but at
     * least we get a consistent frame interval.
     * <p>
     * Start the render thread after the Surface has been created.
     */
    private static class RenderThread extends Thread implements AudioRecorder.AudioRecordCallback {
        // Object must be created on render thread to get correct Looper, but is used from
        // UI thread, so we need to declare it volatile to ensure the UI thread sees a fully
        // constructed object.
        private volatile RenderHandler mHandler;

        // Handler we can send messages to if we want to update the app UI.
        private final ActivityHandler mActivityHandler;

        // Used to wait for the thread to start.
        private final Object mStartLock = new Object();
        private boolean mReady = false;

        private final SurfaceHolder mSurfaceHolder;  // may be updated by UI thread

        Camera.PictureCallback jpegCallback;
        private EglCore mEglCore;
        private WindowSurface mWindowSurface;
//        private FlatShadedProgram mProgram;

        // Orthographic projection matrix.
        private final float[] mDisplayProjectionMatrix = new float[16];

        // One spinning triangle, one bouncing rectangle, and four edge-boxes.
//        private final Sprite2d mTri;
//        private final Sprite2d mRect;
//        private final Sprite2d[] mEdges;
//        private final Sprite2d mRecordRect;
        private float mRectVelX, mRectVelY;     // velocity, in viewport units per second
        private float mInnerLeft, mInnerTop, mInnerRight, mInnerBottom;

        private final float[] mIdentityMatrix;

        // Previous frame time.
        private long mPrevTimeNanos;

        // FPS / drop counter.
        private final long mRefreshPeriodNanos;
        private long mFpsCountStartNanos;
        private int mFpsCountFrame;
        private int mDroppedFrames;
        private boolean mPreviousWasDropped;

        // Used for off-screen rendering.
        private int mOffscreenTexture;
        private int mFramebuffer;
        private int mDepthBuffer;
        private FullFrameRect mFullScreen;

        // Used for recording.
        private boolean mRecordingEnabled;
        private final File mOutputFolder;
        private WindowSurface mInputWindowSurface;
        private TextureMovieEncoder2 mVideoEncoder;
        private int mRecordMethod;
        private boolean mRecordedPrevious;
        private final Rect mVideoRect;
        private TextureMovieEncoder2.OnMovieEncodeListener onMovieEncodeListener;
        private GPUImageRenderer mRenderer;
        //        private ConfigChooser mConfigChooser;
        private int mVideoWidth = 720;
        private int mVideoHeight = 1280;
        //Record video with audio
        private boolean mRecordAudio = true;

        private String mInternalAudioFile;
        private HWEncoder mHWEncoder = new HWEncoder();
        private AudioRecorder mAudioRecorder;
        private InternalAudioRecorder mInternalAudioRecorder;
        private Handler mAudioHandler;
        private Handler mVideoHandler;

        private final AtomicBoolean capturingPicture = new AtomicBoolean(false);

        private class AudioRunnable implements Runnable {

            @Override
            public void run() {
                if (mInternalAudioRecorder != null) {
                    mInternalAudioRecorder.start();
                } else if (mAudioRecorder != null) {
                    if (mAudioRecorder.start()) {
                        Log.v("AudioRunnable", "audiorecorder succeed+" + Thread.currentThread().getName());
                    } else {
                        Log.v("AudioRunnable", "audiorecorder failed+" + Thread.currentThread().getName());
                    }
                }

                Looper.prepare();
                mAudioHandler = new Handler();
                Looper.loop();

            }
        }

        private class VideoRunnable implements Runnable {

            @Override
            public void run() {
                Looper.prepare();
                mVideoHandler = new Handler();
                Looper.loop();
            }
        }

        /**
         * Pass in the SurfaceView's SurfaceHolder.  Note the Surface may not yet exist.
         */
        public RenderThread(SurfaceHolder holder, ActivityHandler aHandler, File outputFolder,
                            long refreshPeriodNs) {
            mSurfaceHolder = holder;
            mActivityHandler = aHandler;
            mOutputFolder = outputFolder;
            mRefreshPeriodNanos = refreshPeriodNs;

            mVideoRect = new Rect();

            mIdentityMatrix = new float[16];
            Matrix.setIdentityM(mIdentityMatrix, 0);

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

        public void setOnMovieEncodeListener(TextureMovieEncoder2.OnMovieEncodeListener onMovieEncodeListener) {
            this.onMovieEncodeListener = onMovieEncodeListener;
        }

        public void setVideoSize(int videoWidth, int videoHeight) {
            this.mVideoWidth = videoWidth;
            this.mVideoHeight = videoHeight;
        }

        public void setInternalAudioFile(String audioFile) {
            this.mInternalAudioFile = audioFile;
        }

        public void setRecordAudio(boolean recordAudio) {
            this.mRecordAudio = recordAudio;
        }

        public void setRenderer(GPUImageRenderer render) {
            this.mRenderer = render;
        }

        private Bitmap takePhoto() {
            int width = mWindowSurface.getWidth();
            int height = mWindowSurface.getHeight();

            ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
            GlUtil.checkGlError("glReadPixels");
            buf.rewind();

            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buf);
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postScale(1, -1, bmp.getWidth() / 2f, bmp.getHeight() / 2f);
            Bitmap result = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            if (bmp != result) {
                bmp.recycle();
            }

            return result;
        }

        /**
         * Thread entry point.
         * <p>
         * The thread should not be started until the Surface associated with the SurfaceHolder
         * has been created.  That way we don't have to wait for a separate "surface created"
         * message to arrive.
         */
        @Override
        public void run() {
            Looper.prepare();
            mHandler = new RenderHandler(this);
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
            synchronized (mStartLock) {
                mReady = true;
                mStartLock.notify();    // signal waitUntilReady()
            }

            Looper.loop();

            Log.d(TAG, "looper quit");
            releaseGl();
            mEglCore.release();

            synchronized (mStartLock) {
                mReady = false;
            }
        }

        /**
         * Waits until the render thread is ready to receive messages.
         * <p>
         * Call from the UI thread.
         */
        public void waitUntilReady() {
            synchronized (mStartLock) {
                while (!mReady) {
                    try {
                        mStartLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        /**
         * Shuts everything down.
         */
        private void shutdown() {
            Log.d(TAG, "shutdown");
            stopEncoder();
            Looper.myLooper().quit();
        }

        /**
         * Returns the render thread's Handler.  This may be called from any thread.
         */
        public RenderHandler getHandler() {
            return mHandler;
        }

        /**
         * Prepares the surface.
         */
        private void surfaceCreated() {
            Surface surface = mSurfaceHolder.getSurface();
            prepareGl(surface);
            if (mRenderer != null) mRenderer.onSurfaceCreated(null, null);
        }

        /**
         * Prepares window surface and GL state.
         */
        private void prepareGl(Surface surface) {
            Log.d(TAG, "prepareGl");

            mWindowSurface = new WindowSurface(mEglCore, surface, false);
            mWindowSurface.makeCurrent();

            // Used for blitting texture to FBO.
            mFullScreen = new FullFrameRect(
                    new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D));

            // Program used for drawing onto the screen.
            //mProgram = new FlatShadedProgram();

            // Set the background color.
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

            // Disable depth testing -- we're 2D only.
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);

            // Don't need backface culling.  (If you're feeling pedantic, you can turn it on to
            // make sure we're defining our shapes correctly.)
            GLES20.glDisable(GLES20.GL_CULL_FACE);

            mActivityHandler.sendGlesVersion(mEglCore.getGlVersion());
        }

        /**
         * Handles changes to the size of the underlying surface.  Adjusts viewport as needed.
         * Must be called before we start drawing.
         * (Called from RenderHandler.)
         */
        private void surfaceChanged(int width, int height) {
            Log.d(TAG, "surfaceChanged " + width + "x" + height);

            prepareFramebuffer(width, height);

            // Use full window.
            GLES20.glViewport(0, 0, width, height);

            // Simple orthographic projection, with (0,0) in lower-left corner.
            Matrix.orthoM(mDisplayProjectionMatrix, 0, 0, width, 0, height, -1, 1);

            int smallDim = Math.min(width, height);

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
            mRectVelX = 1 + smallDim / 4.0f;
            mRectVelY = 1 + smallDim / 5.0f;

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

            if (mRenderer != null)
                mRenderer.onSurfaceChanged(null, width, height);
        }

        /**
         * Prepares the off-screen framebuffer.
         */
        private void prepareFramebuffer(int width, int height) {
            GlUtil.checkGlError("prepareFramebuffer start");

            int[] values = new int[1];

            // Create a texture object and bind it.  This will be the color buffer.
            GLES20.glGenTextures(1, values, 0);
            GlUtil.checkGlError("glGenTextures");
            mOffscreenTexture = values[0];   // expected > 0
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOffscreenTexture);
            GlUtil.checkGlError("glBindTexture " + mOffscreenTexture);

            // Create texture storage.
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

            // Set parameters.  We're probably using non-power-of-two dimensions, so
            // some values may not be available for use.
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
            GlUtil.checkGlError("glTexParameter");

            // Create framebuffer object and bind it.
            GLES20.glGenFramebuffers(1, values, 0);
            GlUtil.checkGlError("glGenFramebuffers");
            mFramebuffer = values[0];    // expected > 0
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
            GlUtil.checkGlError("glBindFramebuffer " + mFramebuffer);

            // Create a depth buffer and bind it.
            GLES20.glGenRenderbuffers(1, values, 0);
            GlUtil.checkGlError("glGenRenderbuffers");
            mDepthBuffer = values[0];    // expected > 0
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBuffer);
            GlUtil.checkGlError("glBindRenderbuffer " + mDepthBuffer);

            // Allocate storage for the depth buffer.
            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                    width, height);
            GlUtil.checkGlError("glRenderbufferStorage");

            // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                    GLES20.GL_RENDERBUFFER, mDepthBuffer);
            GlUtil.checkGlError("glFramebufferRenderbuffer");
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, mOffscreenTexture, 0);
            GlUtil.checkGlError("glFramebufferTexture2D");

            // See if GLES is happy with all this.
            int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Framebuffer not complete, status=" + status);
            }

            // Switch back to the default framebuffer.
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            GlUtil.checkGlError("prepareFramebuffer done");
        }

        /**
         * Releases most of the GL resources we currently hold.
         * <p>
         * Does not release EglCore.
         */
        private void releaseGl() {
            GlUtil.checkGlError("releaseGl start");

            int[] values = new int[1];

            if (mWindowSurface != null) {
                mWindowSurface.release();
                mWindowSurface = null;
            }
//            if (mProgram != null) {
//                mProgram.release();
//                mProgram = null;
//            }
            if (mOffscreenTexture > 0) {
                values[0] = mOffscreenTexture;
                GLES20.glDeleteTextures(1, values, 0);
                mOffscreenTexture = -1;
            }
            if (mFramebuffer > 0) {
                values[0] = mFramebuffer;
                GLES20.glDeleteFramebuffers(1, values, 0);
                mFramebuffer = -1;
            }
            if (mDepthBuffer > 0) {
                values[0] = mDepthBuffer;
                GLES20.glDeleteRenderbuffers(1, values, 0);
                mDepthBuffer = -1;
            }
            if (mFullScreen != null) {
                mFullScreen.release(false); // TODO: should be "true"; must ensure mEglCore current
                mFullScreen = null;
            }

            GlUtil.checkGlError("releaseGl done");

            mEglCore.makeNothingCurrent();
        }

        /**
         * Updates the recording state.  Stops or starts recording as needed.
         */
        private void setRecordingEnabled(boolean enabled, boolean enableRecordMic) {
            setRecordAudio(enableRecordMic);
            if (enabled == mRecordingEnabled) {
                return;
            }
            if (enabled) {
                startEncoder();
            } else {
                stopEncoder();
            }
            mRecordingEnabled = enabled;
        }

        /**
         * Changes the method we use to render frames to the encoder.
         */
        private void setRecordMethod(int recordMethod) {
            Log.d(TAG, "RT: setRecordMethod " + recordMethod);
            mRecordMethod = recordMethod;
        }

        /**
         * Creates the video encoder object and starts the encoder thread.  Creates an EGL
         * surface for encoder input.
         */
        private void startEncoder() {
            Log.d(TAG, "starting to record");
            // Record at 1280x720, regardless of the window dimensions.  The encoder may
            // explode if given "strange" dimensions, e.g. a width that is not a multiple
            // of 16.  We can box it as needed to preserve dimensions.
            final int BIT_RATE = 4000000;   // 4Mbps
            int windowWidth = mWindowSurface.getWidth();
            int windowHeight = mWindowSurface.getHeight();
            float windowAspect = (float) windowHeight / (float) windowWidth;
            int outWidth, outHeight;
            if (mVideoHeight > mVideoWidth * windowAspect) {
                // limited by narrow width; reduce height
                outWidth = mVideoWidth;
                outHeight = (int) (mVideoWidth * windowAspect);
            } else {
                // limited by short height; restrict width
                outHeight = mVideoHeight;
                outWidth = (int) (mVideoHeight / windowAspect);
            }
            int offX = (mVideoWidth - outWidth) / 2;
            int offY = (mVideoHeight - outHeight) / 2;
            mVideoRect.set(offX, offY, offX + outWidth, offY + outHeight);
            Log.d(TAG, "Adjusting window " + windowWidth + "x" + windowHeight +
                    " to +" + offX + ",+" + offY + " " +
                    mVideoRect.width() + "x" + mVideoRect.height());
            final File outVideoFile = new File(mOutputFolder, System.currentTimeMillis() + ".mp4");
            if (mRecordAudio) {
                try {
                    int channels = 1;
                    int sampleRate = 48000;
                    int mImageFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
                    mHWEncoder = new HWEncoder();
                    mHWEncoder.init(mVideoWidth, mVideoHeight, mImageFormat, BIT_RATE, sampleRate, channels, outVideoFile.getAbsolutePath());
                    mHWEncoder.setOnMovieEncodeListener(onMovieEncodeListener);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                mInputWindowSurface = new WindowSurface(mEglCore, mHWEncoder.getInputSurface(), true);
                if (TextUtils.isEmpty(mInternalAudioFile)) {
                    mAudioRecorder = new AudioRecorder();
                    mAudioRecorder.setRecordCallback(this);
                } else {
                    try {
                        mInternalAudioRecorder = new InternalAudioRecorder(mInternalAudioFile);
                        mInternalAudioRecorder.setRecordCallback(this);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        mInternalAudioRecorder = null;
                    }
                }

                new Thread(new AudioRunnable()).start();
                new Thread(new VideoRunnable()).start();
            } else {
                VideoEncoderCore encoderCore;
                try {
                    encoderCore = new VideoEncoderCore(mVideoWidth, mVideoHeight, BIT_RATE, outVideoFile);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }

                mInputWindowSurface = new WindowSurface(mEglCore, encoderCore.getInputSurface(), true);
                mVideoEncoder = new TextureMovieEncoder2(encoderCore);
                mVideoEncoder.setOnMovieEncodeListener(onMovieEncodeListener);
            }
        }

        /**
         * Stops the video encoder if it's running.
         */
        private void stopEncoder() {
            if (mVideoEncoder != null) {
                Log.d(TAG, "stopping recorder, mVideoEncoder=" + mVideoEncoder);
                mVideoEncoder.stopRecording();
                // TODO: wait (briefly) until it finishes shutting down so we know file is
                //       complete, or have a callback that updates the UI
                mVideoEncoder = null;
            }
            //Record video with audio
            if (mHWEncoder != null) {
                mHWEncoder.stop();
                mHWEncoder = null;
            }

            if (mInternalAudioRecorder != null) {
                mInternalAudioRecorder.stop();
            }

            if (mAudioRecorder != null)
                mAudioRecorder.stop();

            if (mAudioHandler != null) {
                mAudioHandler.removeCallbacksAndMessages(null);
                mAudioHandler = null;
            }

            if (mVideoHandler != null) {
                mVideoHandler.removeCallbacksAndMessages(null);
                mVideoHandler = null;
            }
            //Release input window surface
            if (mInputWindowSurface != null) {
                mInputWindowSurface.release();
                mInputWindowSurface = null;
            }
            //Show saved path

        }

        /**
         * Advance state and draw frame in response to a vsync event.
         */
        private void doFrame(long timeStampNanos) {
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

            update(timeStampNanos);

            long diff = System.nanoTime() - timeStampNanos;
            long max = mRefreshPeriodNanos - 2000000;   // if we're within 2ms, don't bother
            if (diff > max) {
                // too much, drop a frame
                Log.d(TAG, "diff is " + (diff / 1000000.0) + " ms, max " + (max / 1000000.0) +
                        ", skipping render");
                mRecordedPrevious = false;
                mPreviousWasDropped = true;
                mDroppedFrames++;
                return;
            }

            boolean swapResult;

            if (!mRecordingEnabled || mRecordedPrevious) {
                mRecordedPrevious = false;
                // Render the scene, swap back to front.
                draw();
                swapResult = mWindowSurface.swapBuffers();
            } else {
                mRecordedPrevious = true;

                // recording
                if (mRecordMethod == RECMETHOD_DRAW_TWICE) {
                    //Log.d(TAG, "MODE: draw 2x");

                    // Draw for display, swap.
                    draw();
                    swapResult = mWindowSurface.swapBuffers();

                    // Draw for recording, swap.
                    onFrameAvailable();

                    mInputWindowSurface.makeCurrent();
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
                    GLES20.glClearColor(0f, 0f, 0f, 1f);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                    GLES20.glViewport(mVideoRect.left, mVideoRect.top,
                            mVideoRect.width(), mVideoRect.height());
                    GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
                    GLES20.glScissor(mVideoRect.left, mVideoRect.top,
                            mVideoRect.width(), mVideoRect.height());
                    draw();
                    GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
                    mInputWindowSurface.setPresentationTime(timeStampNanos);
                    mInputWindowSurface.swapBuffers();

                    // Restore.
                    GLES20.glViewport(0, 0, mWindowSurface.getWidth(), mWindowSurface.getHeight());
                    mWindowSurface.makeCurrent();

                } else if (mEglCore.getGlVersion() >= 3 &&
                        mRecordMethod == RECMETHOD_BLIT_FRAMEBUFFER) {
                    //Log.d(TAG, "MODE: blitFramebuffer");
                    // Draw the frame, but don't swap it yet.
                    draw();

                    onFrameAvailable();

                    mInputWindowSurface.makeCurrentReadFrom(mWindowSurface);
                    // Clear the pixels we're not going to overwrite with the blit.  Once again,
                    // this is excessive -- we don't need to clear the entire screen.
                    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                    GlUtil.checkGlError("before glBlitFramebuffer");
                    Log.v(TAG, "glBlitFramebuffer: 0,0," + mWindowSurface.getWidth() + "," +
                            mWindowSurface.getHeight() + "  " + mVideoRect.left + "," +
                            mVideoRect.top + "," + mVideoRect.right + "," + mVideoRect.bottom +
                            "  COLOR_BUFFER GL_NEAREST");
                    GLES30.glBlitFramebuffer(
                            0, 0, mWindowSurface.getWidth(), mWindowSurface.getHeight(),
                            mVideoRect.left, mVideoRect.top, mVideoRect.right, mVideoRect.bottom,
                            GLES30.GL_COLOR_BUFFER_BIT, GLES30.GL_NEAREST);
                    int err;
                    if ((err = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
                        Log.w(TAG, "ERROR: glBlitFramebuffer failed: 0x" +
                                Integer.toHexString(err));
                    }
                    mInputWindowSurface.setPresentationTime(timeStampNanos);
                    mInputWindowSurface.swapBuffers();

                    // Now swap the display buffer.
                    mWindowSurface.makeCurrent();
                    swapResult = mWindowSurface.swapBuffers();

                } else {
                    //Log.d(TAG, "MODE: offscreen + blit 2x");
                    // Render offscreen.
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
                    GlUtil.checkGlError("glBindFramebuffer");
                    draw();

                    // Blit to display.
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                    GlUtil.checkGlError("glBindFramebuffer");
                    mFullScreen.drawFrame(mOffscreenTexture, mIdentityMatrix);
                    swapResult = mWindowSurface.swapBuffers();

                    // Blit to encoder.
                    onFrameAvailable();

                    mInputWindowSurface.makeCurrent();
                    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);    // again, only really need to
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);     //  clear pixels outside rect
                    GLES20.glViewport(mVideoRect.left, mVideoRect.top,
                            mVideoRect.width(), mVideoRect.height());
                    mFullScreen.drawFrame(mOffscreenTexture, mIdentityMatrix);
                    mInputWindowSurface.setPresentationTime(timeStampNanos);
                    mInputWindowSurface.swapBuffers();

                    // Restore previous values.
                    GLES20.glViewport(0, 0, mWindowSurface.getWidth(), mWindowSurface.getHeight());
                    mWindowSurface.makeCurrent();
                }
            }

            mPreviousWasDropped = false;

            if (!swapResult) {
                // This can happen if the Activity stops without waiting for us to halt.
                Log.w(TAG, "swapBuffers failed, killing renderer thread");
                shutdown();
                return;
            }

            // Update the FPS counter.
            //
            // Ideally we'd generate something approximate quickly to make the UI look
            // reasonable, then ease into longer sampling periods.
            final int NUM_FRAMES = 120;
            final long ONE_TRILLION = 1000000000000L;
            if (mFpsCountStartNanos == 0) {
                mFpsCountStartNanos = timeStampNanos;
                mFpsCountFrame = 0;
            } else {
                mFpsCountFrame++;
                if (mFpsCountFrame == NUM_FRAMES) {
                    // compute thousands of frames per second
                    long elapsed = timeStampNanos - mFpsCountStartNanos;
                    mActivityHandler.sendFpsUpdate((int) (NUM_FRAMES * ONE_TRILLION / elapsed),
                            mDroppedFrames);

                    // reset
                    mFpsCountStartNanos = timeStampNanos;
                    mFpsCountFrame = 0;
                }
            }
        }

        /**
         * We use the time delta from the previous event to determine how far everything
         * moves.  Ideally this will yield identical animation sequences regardless of
         * the device's actual refresh rate.
         */
        private void update(long timeStampNanos) {
            // Compute time from previous frame.
            long intervalNanos;
            if (mPrevTimeNanos == 0) {
                intervalNanos = 0;
            } else {
                intervalNanos = timeStampNanos - mPrevTimeNanos;

                final long ONE_SECOND_NANOS = 1000000000L;
                if (intervalNanos > ONE_SECOND_NANOS) {
                    // A gap this big should only happen if something paused us.  We can
                    // either cap the delta at one second, or just pretend like this is
                    // the first frame and not advance at all.
                    Log.d(TAG, "Time delta too large: " +
                            (double) intervalNanos / ONE_SECOND_NANOS + " sec");
                    intervalNanos = 0;
                }
            }
            mPrevTimeNanos = timeStampNanos;

            final float ONE_BILLION_F = 1000000000.0f;
            final float elapsedSeconds = intervalNanos / ONE_BILLION_F;

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
        private void draw() {
            GlUtil.checkGlError("draw start");

            // Clear to a non-black color to make the content easily differentiable from
            // the pillar-/letter-boxing.
            GLES20.glClearColor(0, 0, 0, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

//            mTri.draw(mProgram, mDisplayProjectionMatrix);
//            mRect.draw(mProgram, mDisplayProjectionMatrix);
//            for (int i = 0; i < 4; i++) {
//                if (false && mPreviousWasDropped) {
//                    mEdges[i].setColor(1.0f, 0.0f, 0.0f);
//                } else {
//                    mEdges[i].setColor(0.5f, 0.5f, 0.5f);
//                }
//                mEdges[i].draw(mProgram, mDisplayProjectionMatrix);
//            }

            // Give a visual indication of the recording method.
//            switch (mRecordMethod) {
//                case RECMETHOD_DRAW_TWICE:
//                    mRecordRect.setColor(1.0f, 0.0f, 0.0f);
//                    break;
//                case RECMETHOD_FBO:
//                    mRecordRect.setColor(0.0f, 1.0f, 0.0f);
//                    break;
//                case RECMETHOD_BLIT_FRAMEBUFFER:
//                    mRecordRect.setColor(0.0f, 0.0f, 1.0f);
//                    break;
//                default:
//            }
//            mRecordRect.draw(mProgram, mDisplayProjectionMatrix);

            if (mRenderer != null) {
                mRenderer.onDrawFrame(null);
            } else {
                Log.e(TAG, "draw(): mRenderer is null");
            }

            if (capturingPicture.get()) {
                Bitmap image = takePhoto();
                mActivityHandler.sendCapturedImage(image);
                capturingPicture.set(false);
            }

            GlUtil.checkGlError("draw done");
        }

        @Override
        public void onRecordSample(byte[] data) {
            if (mAudioHandler == null) {
                return;
            }

            mAudioHandler.post(() -> {
                if (mHWEncoder != null)
                    try {
                        mHWEncoder.recordSample(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            });

        }

        private void onFrameAvailable() {
            if (mRecordAudio && mHWEncoder != null) {
                Log.d(TAG, "onFrameAvailable:  " + (mVideoHandler == null));
                if (mVideoHandler == null) {
                    Log.d(TAG, "onFrameAvailable:  new " + (mVideoHandler == null));
                    mVideoHandler = new Handler();
                }
                mVideoHandler.post(() -> {
                    try {
                        if (mHWEncoder != null)
                            mHWEncoder.recordImage();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else if (mVideoEncoder != null) {
                mVideoEncoder.frameAvailableSoon();
            }
        }
    }

    /**
     * Handler for RenderThread.  Used for messages sent from the UI thread to the render thread.
     * <p>
     * The object is created on the render thread, and the various "send" methods are called
     * from the UI thread.
     */
    private static class RenderHandler extends Handler {
        private static final int MSG_SURFACE_CREATED = 0;
        private static final int MSG_SURFACE_CHANGED = 1;
        private static final int MSG_DO_FRAME = 2;
        private static final int MSG_RECORDING_ENABLED = 3;
        private static final int MSG_RECORD_METHOD = 4;
        private static final int MSG_SHUTDOWN = 5;
        private static final int MSG_SET_VIDEO_SIZE = 6;

        private static final int MSG_TAKE_PHOTO = 7;

        private static final int MSG_SET_INTERNAL_AUDIO_FILE = 8;
        // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
        // but no real harm in it.
        private final WeakReference<RenderThread> mWeakRenderThread;

        /**
         * Call from render thread.
         */
        public RenderHandler(RenderThread rt) {
            mWeakRenderThread = new WeakReference<>(rt);
        }

        /**
         * Sends the "surface created" message.
         * <p>
         * Call from UI thread.
         */
        public void sendSurfaceCreated() {
            sendMessage(obtainMessage(RenderHandler.MSG_SURFACE_CREATED));
        }

        /**
         * Sends the "surface changed" message, forwarding what we got from the SurfaceHolder.
         * <p>
         * Call from UI thread.
         */
        public void sendSurfaceChanged(@SuppressWarnings("unused") int format,
                                       int width, int height) {
            // ignore format
            sendMessage(obtainMessage(RenderHandler.MSG_SURFACE_CHANGED, width, height));
        }

        /**
         * Sends the "do frame" message, forwarding the Choreographer event.
         * <p>
         * Call from UI thread.
         */
        public void sendDoFrame(long frameTimeNanos) {
            sendMessage(obtainMessage(RenderHandler.MSG_DO_FRAME,
                    (int) (frameTimeNanos >> 32), (int) frameTimeNanos));
        }

        /**
         * Enable or disable recording.
         * <p>
         * Call from non-UI thread.
         */
        public void setRecordingEnabled(boolean enabled, boolean enableRecordMic) {
            sendMessage(obtainMessage(MSG_RECORDING_ENABLED, enabled ? 1 : 0, enableRecordMic ? 0 : -1));
        }

        /**
         * Set the method used to render a frame for the encoder.
         * <p>
         * Call from non-UI thread.
         */
        public void setRecordMethod(int recordMethod) {
            sendMessage(obtainMessage(MSG_RECORD_METHOD, recordMethod, 0));
        }

        public void setVideoSize(int videoWidth, int videoHeight) {
            sendMessage(obtainMessage(MSG_SET_VIDEO_SIZE, videoWidth, videoHeight));
        }

        public void takePhoto() {
            sendMessage(obtainMessage(MSG_TAKE_PHOTO));
        }

        /**
         * Sends the "shutdown" message, which tells the render thread to halt.
         * <p>
         * Call from UI thread.
         */
        public void sendShutdown() {
            sendMessage(obtainMessage(RenderHandler.MSG_SHUTDOWN));
        }

        public void setInternalAudioFile(String audioFile) {
            sendMessage(obtainMessage(MSG_SET_INTERNAL_AUDIO_FILE, audioFile));
        }

        @Override  // runs on RenderThread
        public void handleMessage(Message msg) {
            int what = msg.what;
            //Log.d(TAG, "RenderHandler [" + this + "]: what=" + what);

            RenderThread renderThread = mWeakRenderThread.get();
            if (renderThread == null) {
                Log.w(TAG, "RenderHandler.handleMessage: weak ref is null");
                return;
            }

            switch (what) {
                case MSG_SURFACE_CREATED:
                    renderThread.surfaceCreated();
                    break;
                case MSG_SURFACE_CHANGED:
                    renderThread.surfaceChanged(msg.arg1, msg.arg2);
                    break;
                case MSG_DO_FRAME:
                    long timestamp = (((long) msg.arg1) << 32) |
                            (((long) msg.arg2) & 0xffffffffL);
                    renderThread.doFrame(timestamp);
                    break;
                case MSG_RECORDING_ENABLED:
                    renderThread.setRecordingEnabled(msg.arg1 != 0, msg.arg2 == 0);
                    break;
                case MSG_RECORD_METHOD:
                    renderThread.setRecordMethod(msg.arg1);
                    break;
                case MSG_SHUTDOWN:
                    renderThread.shutdown();
                    break;
                case MSG_SET_VIDEO_SIZE:
                    renderThread.setVideoSize(msg.arg1, msg.arg2);
                    break;
                case MSG_TAKE_PHOTO:
                    renderThread.capturingPicture.set(true);
                    break;
                case MSG_SET_INTERNAL_AUDIO_FILE:
                    renderThread.setInternalAudioFile((String) msg.obj);
                    break;
                default:
                    throw new RuntimeException("unknown message " + what);
            }
        }
    }
}
