package com.skymeta.arface.mlkit.processor.game.face.facedance

import android.content.Context
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameBase
import jp.co.cyberagent.android.gpuimage.ml.MaskMode


class FaceDanceGame(context:Context): GameBase(context) {

    override var assets = FaceDanceConstants.ASSETS
    override val gameFilter =
        FaceDanceFilter(context, MaskMode.MASK_CAM)
}