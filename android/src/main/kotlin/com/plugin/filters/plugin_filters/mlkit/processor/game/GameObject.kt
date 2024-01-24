package com.plugin.filters.plugin_filters.mlkit.processor.game

import android.graphics.Bitmap
import android.util.Log
import jp.co.cyberagent.android.gpuimage.ml.ScaleType
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils
import java.util.ArrayList
import kotlin.math.roundToInt

enum class SizeType(
    val value: Float
) {
    WIDTH(0f), HEIGHT(1f), SCALE(2f)
}

open class GameObject(
    var id:String,
    var assetPath:String,
    var textureId:Int = OpenGlUtils.NO_TEXTURE,
    // Origin width and height of image texture
    var width:Int = 0,
    var height:Int = 0,
    var image:Bitmap? = null,

    var x:Float = 0f,
    var y:Float = 0f,
    // Expected width and height in openGL coordinate
    var w:Float = 0f,
    var h:Float = 0f,
    var sizeType: SizeType = SizeType.WIDTH,


    ){

    constructor(): this(
        id = "",
        assetPath = "",
        textureId = OpenGlUtils.NO_TEXTURE,
        width = 0,
        height = 0,
        image = null,

        x = 0f,
        y = 0f,
        w = 0f,
        h = 0f,
        sizeType = SizeType.WIDTH,
    ) {

    }

    constructor(textureId:Int = OpenGlUtils.NO_TEXTURE,
                width:Int = 0, height:Int = 0) : this (
        "",
        "",
        textureId,
        width,
        height,
        null,
        0f, 0f, 0f, 0f, SizeType.WIDTH
    ) {

    }

    constructor(other: GameObject) : this(
        other.id,
        other.assetPath,
        other.textureId,
        other.width,
        other.height,
        other.image,
        other.x, other.y, other.w, other.h,
        other.sizeType
    ) {

    }


    companion object {
        fun relativeWBottomCenter(
            relativeW: Float, endY: Float,
            originW: Float, originH: Float
        ): FloatArray {
            val relativeH = relativeW * originH / originW
            val startX = (1 - relativeW) / 2f
            val endX = startX + relativeW
            val startY = endY - relativeH

            return floatArrayOf(
                startX, startY,
                endX - startX, endY - startY,
            )
        }

        fun relativeWMiddleY(
            relativeW: Float, startX:Float,
            originW: Float, originH: Float
        ): FloatArray {
            val relativeH = relativeW * originH / originW

            val endX = startX + relativeW
            val startY = 0.5f - relativeH/2
            val endY = 0.5f + relativeH/2

            return floatArrayOf(
                startX, startY,
                endX - startX, endY - startY,
            )
        }

        fun relativeHMiddleY(
            relativeH: Float, startX:Float,
            originW: Float, originH: Float
        ): FloatArray {
            val relativeW = relativeH * originW / originH

            val endX = startX + relativeW
            val startY = 0.5f - relativeH/2
            val endY = 0.5f + relativeH/2

            return floatArrayOf(
                startX, startY,
                endX - startX, endY - startY,
            )
        }
    }

    var textScale: ScaleType = ScaleType.CENTER_FIT
    private var textRatioW: Float = 1f
    private var textRatioH: Float = 1f
    private var scaleWidth: Float = 1f
    private var scaleHeight: Float = 1f

    var drawW = -1f
    var drawH = -1f


    // List of animation texture
    var animTextIndex = 0
    var animTexts:java.util.ArrayList<Int> = ArrayList()
    var animRate:Float = 500f // Play next frame after 100f millisecond
    var animTimer:Float = 0f // To count time each update and reset when update animation

    
    fun updateTimer(timeDelta: Float) {
        if(this.animTexts.size > 0) {
            // Update timer
            this.animTimer += timeDelta
            if (this.animTimer >= this.animRate) {
                this.animTimer = 0f
                this.animTextIndex = (this.animTextIndex + 1) % (this.animTexts.size)
                this.textureId = this.animTexts[this.animTextIndex]
            }
        }
    }

    fun drawGLSize(outputWidth: Float, outputHeight: Float) {
        // 1. Draw relative size

        // Consider width as standard - scale by outputWidth
        val textRatio = GameUtils.calTextRatio(
            this.width.toFloat(), this.height.toFloat(),
            this.w * outputWidth, // absolute width
            this.h * outputHeight // absolute height
        )
        val textRatioW = textRatio[0]
        val textRatioH = textRatio[1]


        drawW = when (textScale) {
            ScaleType.CENTER_CROP -> this.w * textRatioW
            ScaleType.CENTER_INSIDE -> this.w / textRatioH
            else -> this.w
        }

        drawH = when(textScale) {
            ScaleType.CENTER_CROP -> this.h * textRatioH
            ScaleType.CENTER_INSIDE -> this.h / textRatioW
            else -> this.h
        }
    }

    private fun setTextScale(scaleType: ScaleType,
                     outputWidth: Float, outputHeight: Float,
    ) {
        setTextScale(scaleType,
            this.width.toFloat(), this.height.toFloat(),
            outputWidth, outputHeight)
    }

    fun setTextScale(scaleType: ScaleType,
                     imageWidth: Float, imageHeight: Float,
                     outputWidth: Float, outputHeight: Float,
    ) {
        this.textScale = scaleType
        val ratio1: Float = outputWidth / imageWidth
        val ratio2: Float = outputHeight / imageHeight
        val ratioMax = Math.max(ratio1, ratio2)
        scaleWidth = (imageWidth * ratioMax).roundToInt().toFloat()
        scaleHeight = (imageHeight * ratioMax).roundToInt().toFloat()

        textRatioW = scaleWidth / outputWidth
        textRatioH = scaleHeight / outputHeight

        Log.e("FaceDance", "Text-Scale:: $textScale $textRatioW $textRatioH" +
                "\n" +
                "Origin-Image: $imageWidth $imageHeight" +
                "\n" +
                "Origin-Box: $outputWidth $outputHeight")

    }

    override fun toString(): String {
        return "(" +
                "id: " + id +
                " , asset: " + assetPath +
                " , textureId: " + textureId +
                " , width: " + width +
                " , height: " + height +
                " , image: " + image
                ")"
    }


}