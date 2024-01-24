package com.plugin.filters.plugin_filters.mlkit.processor.game.face.eyebrowgym

import android.content.Context
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameBase
import jp.co.cyberagent.android.gpuimage.ml.MaskMode


class EyebrowGymGame(context:Context): GameBase(context) {

    override var assets = EyebrowGymConstants.ASSETS
    override val gameFilter =
        EyebrowGymFilter(context, MaskMode.MASK)

    init {
        val pinkStarVfx = Array(37) {
                i -> "game/vfx/pink_star/${String.format("%02d", i)}.png"
        }
        assets += pinkStarVfx
    }
}