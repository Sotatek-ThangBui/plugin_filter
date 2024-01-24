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
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.AnimalFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.DoubleFaceFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.FacePartFilter
import com.plugin.filters.plugin_filters.mlkit.processor.game.effect.TattooFilter
import jp.co.cyberagent.android.gpuimage.ml.FaceSwapFilter
import jp.co.cyberagent.android.gpuimage.ml.MaskMode

/** Face Mesh Detector Demo. */
open class MPFaceMeshProcessor(
    context: Context
) :
    BaseProcessor(context) {
    override fun clearMask() {
        if (filter is FacePartFilter || filter is DoubleFaceFilter) {
            filter = FaceSwapFilter(context, MaskMode.MASK)
        }
        super.clearMask()
    }

    override fun prepareStickerData() {
        if (filter !is AnimalFilter) {
            filter = AnimalFilter(context)
        }
        super.prepareStickerData()
    }

    override fun prepareMaskData() {
        if (filter is FacePartFilter || filter is AnimalFilter || filter is DoubleFaceFilter || filter is TattooFilter) {
            filter = FaceSwapFilter(context, MaskMode.MASK)
        }
        super.prepareMaskData()
    }
}
