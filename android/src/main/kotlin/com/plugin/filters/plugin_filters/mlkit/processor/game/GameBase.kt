package com.plugin.filters.plugin_filters.mlkit.processor.game

import android.content.Context
import com.plugin.filters.plugin_filters.mlkit.BitmapUtils
import com.skymeta.arface.mlkit.processor.game.face.facedance.FaceDanceConstants
import jp.co.cyberagent.android.gpuimage.ml.MaskMode
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils


open class GameBase(var context:Context) {

    var gameAssets: HashMap<String, GameObject?> = HashMap()
    open var assets = FaceDanceConstants.ASSETS

    open val gameFilter: GameFilter = GameFilter(context, MaskMode.MASK_CAM)

    fun init() {

        this.assets.forEach { path ->
            val img = BitmapUtils.getBitmapFromAsset("$path", context.assets)
            gameAssets[path] = GameObject(
                id = path,
                assetPath = "$path",
                textureId = OpenGlUtils.NO_TEXTURE, // unloaded texture - init to be loaded
                width = if(img != null) img!!.width else 0,
                height = if(img != null) img!!.height else 0,
                image = img,

                x=0f,y=0f,w=0f,h=0f,
                sizeType = SizeType.HEIGHT,
            )
        }

        gameFilter.initAssets(gameAssets!!)

    }
}