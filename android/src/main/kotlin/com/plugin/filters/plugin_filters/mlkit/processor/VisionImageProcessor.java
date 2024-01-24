/*
 * Copyright 2020 Google LLC. All rights reserved.
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

package com.plugin.filters.plugin_filters.mlkit.processor;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.plugin.filters.plugin_filters.enums.FilterType;
import com.google.mlkit.common.MlKitException;
import com.plugin.filters.plugin_filters.mlkit.FrameMetadata;
import com.plugin.filters.plugin_filters.mlkit.graphic.GraphicOverlay;

import java.nio.ByteBuffer;

/**
 * An interface to process the images with different vision detectors and custom image models.
 */
public interface VisionImageProcessor {
    void processBitmap(Bitmap bitmap, GraphicOverlay graphicOverlay);

    /**
     * Processes ByteBuffer image data, e.g. used for Camera1 live preview case.
     */
    void processByteBuffer(
            ByteBuffer data, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay)
            throws MlKitException;

    /**
     * Processes ImageProxy image data, e.g. used for CameraX live preview case.
     */
    void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay) throws MlKitException;

    /**
     * Stops the underlying machine learning model and release resources.
     */
    void stop();

    void setDebugView(DebugView debug);

    void setMusicControl(MusicControl musicControl);

    void setLensFacing(int facing);

    void setMaskImage(Bitmap image, FilterType filterType);

    void setMaskImage(Bitmap image, Boolean isHumanFace);

    void clearMask();

    void resume();

    void changeFilter(@NonNull FilterType filterType);

}
