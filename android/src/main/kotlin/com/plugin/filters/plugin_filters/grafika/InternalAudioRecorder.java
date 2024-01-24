package com.plugin.filters.plugin_filters.grafika;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class InternalAudioRecorder {
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();
    private static final int TIMEOUT_US = -1;
    private final MediaCodec codec;
    private final MediaExtractor extractor;

    private final ByteBuffer[] codecInputBuffers;
    private ByteBuffer[] codecOutputBuffers;
    private Boolean sawInputEOS = false;
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private final AudioTrack mAudioTrack;
    private final MediaCodec.BufferInfo info;
    private AudioRecorder.AudioRecordCallback mRecordCallback;

    public void setRecordCallback(AudioRecorder.AudioRecordCallback recordCallback) {
        mRecordCallback = recordCallback;
    }

    public InternalAudioRecorder(String filePath) throws IOException {
        extractor = new MediaExtractor();
        extractor.setDataSource(filePath);
        MediaFormat format = extractor.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null, null, 0);
        codec.start();

        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        extractor.selectTrack(0);

        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT
                ),
                AudioTrack.MODE_STREAM
        );

        info = new MediaCodec.BufferInfo();
    }

    public void start() {
        stopFlag.set(false);
        mExecutor.execute(() -> {
            mAudioTrack.play();
            do {
                input();
                output();
            } while (!sawInputEOS);
        });
    }

    public void stop() {
        stopFlag.set(true);
        if (mAudioTrack != null && mAudioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
            if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                try {
                    mAudioTrack.stop();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }

            }

            mAudioTrack.release();
        }

        mExecutor.shutdown();
    }

    protected void onPlayingFinish() {

    }

    private void output() {
        final int res = codec.dequeueOutputBuffer(info, TIMEOUT_US);
        if (res >= 0) {
            int outputBufIndex = res;
            ByteBuffer buf = codecOutputBuffers[outputBufIndex];

            final byte[] chunk = new byte[info.size];
            buf.get(chunk); // Read the buffer all at once
            buf.clear(); // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN

            if (chunk.length > 0) {
                mAudioTrack.write(chunk, 0, chunk.length);
                if (mRecordCallback != null) mRecordCallback.onRecordSample(chunk);
            }

            codec.releaseOutputBuffer(outputBufIndex, false);

            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Boolean sawOutputEOS = true;
                onPlayingFinish();
            }
        } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            codecOutputBuffers = codec.getOutputBuffers();
        } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            final MediaFormat format = codec.getOutputFormat();
            mAudioTrack.setPlaybackRate(format.getInteger(MediaFormat.KEY_SAMPLE_RATE));
        }

    }

    private void input() {
        int inputBufIndex = codec.dequeueInputBuffer(TIMEOUT_US);
        if (inputBufIndex >= 0) {
            ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

            int sampleSize = extractor.readSampleData(dstBuf, 0);
            long presentationTimeUs = 0;
            if (sampleSize < 0) {
                sawInputEOS = true;
                sampleSize = 0;
            } else {
                presentationTimeUs = extractor.getSampleTime();
            }

            if (stopFlag.get()) {
                codec.queueInputBuffer(inputBufIndex,
                        0,
                        sampleSize,
                        presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                sawInputEOS = true;
            } else {
                codec.queueInputBuffer(inputBufIndex,
                        0,
                        sampleSize,
                        presentationTimeUs,
                        sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
            }

            if (!sawInputEOS && !stopFlag.get()) {
                extractor.advance();
            }
        }

    }
}
