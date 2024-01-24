package com.plugin.filters.plugin_filters.mlkit.processor.game.pose.posedance

import android.content.Context
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameBase
import jp.co.cyberagent.android.gpuimage.ml.MaskMode


class PoseDanceGame(context:Context): GameBase(context) {

    override var assets = PoseDanceConstants.ASSETS
    override val gameFilter =
        PoseDanceFilter(context, MaskMode.MASK_CAM)
}