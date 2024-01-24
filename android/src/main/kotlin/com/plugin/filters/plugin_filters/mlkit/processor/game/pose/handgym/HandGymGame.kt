package com.plugin.filters.plugin_filters.mlkit.processor.game.pose.handgym

import android.content.Context
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameBase
import jp.co.cyberagent.android.gpuimage.ml.MaskMode


class HandGymGame(context:Context): GameBase(context) {

    override var assets = HandGymConstants.ASSETS
    override val gameFilter =
        HandGymFilter(context, MaskMode.MASK_CAM)

    init {
        val pinkStarVfx = Array(37) {
                i -> "game/vfx/pink_star/${String.format("%02d", i)}.png"
        }
        assets += pinkStarVfx
    }
}