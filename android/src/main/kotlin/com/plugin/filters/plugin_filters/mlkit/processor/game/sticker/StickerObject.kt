package com.plugin.filters.plugin_filters.mlkit.processor.game.sticker

import android.graphics.Bitmap
import com.plugin.filters.plugin_filters.mlkit.processor.game.GameObject
import com.plugin.filters.plugin_filters.mlkit.processor.game.sticker.BlendConfig

/**
 * Create by NghiaNV on 12/01/2022 22:47
 */
class StickerObject : GameObject {

    var blendConfig: BlendConfig? = null
    var type: String = "Hair"
    var blendData: Bitmap? = null
    var iconData: Bitmap? = null


    constructor(): super() {

    }

    constructor(other: GameObject) : super(other) {

    }


}