package jp.co.cyberagent.android.gpuimage.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGaussianBlurFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHSVBlendFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import kotlin.math.floor

class GpuSwapProcessor {
    companion object {
        fun smooth3dMask(
            context: Context,
            maskImage: Bitmap,
            originImage: Bitmap,
            maskUV: FloatArray,
            faceVertex: FloatArray,
            skinPercent: Float, eyePercent: Float, nosePercent: Float, mouthPercent: Float
        ): Bitmap {
            val filter = FaceSwapFilter(context, MaskMode.DECORATE)
            filter.setPercent(skinPercent, eyePercent, nosePercent, mouthPercent)
            filter.setData(faceVertex, maskUV)
            filter.setMaskBitmap(maskImage)
            val gpuImage = GPUImage(context)
            gpuImage.setFilter(filter)
            return gpuImage.getBitmapWithFilterApplied(originImage)
        }

        fun smooth3dMaskOnLyMouthAndEye(
            context: Context,
            maskImage: Bitmap,
            originImage: Bitmap,
            maskUV: FloatArray,
            faceVertex: FloatArray,
            skinPercent: Float, eyePercent: Float, nosePercent: Float, mouthPercent: Float
        ): Bitmap {
            val filter = FaceSwapFilter(context, MaskMode.DECORATE_MOUTH_EYE)
            filter.setPercent(skinPercent, eyePercent, nosePercent, mouthPercent)
            filter.setData(faceVertex, maskUV)
            filter.setMaskBitmap(maskImage)
            val gpuImage = GPUImage(context)
            gpuImage.setFilter(filter)
            return gpuImage.getBitmapWithFilterApplied(originImage)
        }

        fun make3dMask(
            context: Context,
            maskImage: Bitmap,
            originImage: Bitmap,
            maskUV: FloatArray,
            faceVertex: FloatArray,
        ): Bitmap {
            val filter = FaceSwapFilter(context, MaskMode.SWAP)
            filter.setData(faceVertex, maskUV)
            filter.setMaskBitmap(maskImage)
            val gpuImage = GPUImage(context)
            gpuImage.setFilter(filter)
            return gpuImage.getBitmapWithFilterApplied(originImage)
        }

        fun frame3dMask(
            context: Context,
            maskImage: Bitmap,
            originImage: Bitmap,
            maskUV: FloatArray,
            faceVertex: FloatArray,
        ): Bitmap {
            val filter = FaceSwapFilter(context, MaskMode.SWAP_FRAME)
            filter.setData(faceVertex, maskUV)
            filter.setMaskBitmap(maskImage)
            val gpuImage = GPUImage(context)
            gpuImage.setFilter(filter)
            return gpuImage.getBitmapWithFilterApplied(originImage)
        }

        fun frame3dMaskMouthAndEye(
            context: Context,
            maskImage: Bitmap,
            originImage: Bitmap,
            maskUV: FloatArray,
            faceVertex: FloatArray,
        ): Bitmap {
            val filter = FaceSwapFilter(context, MaskMode.SWAP_ONLY_MOUTH_EYE)
            filter.setData(faceVertex, maskUV)
            filter.setMaskBitmap(maskImage)
            val gpuImage = GPUImage(context)
            gpuImage.setFilter(filter)
            return gpuImage.getBitmapWithFilterApplied(originImage)
        }

        fun tattoo3dMask(
            context: Context,
            maskImage: Bitmap,
            originImage: Bitmap,
            maskUV: FloatArray,
            faceVertex: FloatArray,
            skinPercent: Float, eyePercent: Float, nosePercent: Float, mouthPercent: Float
        ): Bitmap {
            val filter = FaceSwapFilter(context, MaskMode.MASK)
            filter.setPercent(skinPercent, eyePercent, nosePercent, mouthPercent)
            filter.setData(faceVertex, maskUV)
            filter.setMaskBitmap(maskImage)
            val gpuImage = GPUImage(context)
            gpuImage.setFilter(filter)
            return gpuImage.getBitmapWithFilterApplied(originImage)
        }

        fun beauty(
            context: Context,
            maskImage: Bitmap,
            originImage: Bitmap,
            maskUV: FloatArray,
            faceVertex: FloatArray,
            skinPercent: Float, eyePercent: Float, nosePercent: Float, mouthPercent: Float
        ): Bitmap {
            val filter = FaceSwapFilter(context, MaskMode.BEAUTY)
            filter.setPercent(skinPercent, eyePercent, nosePercent, mouthPercent)
            filter.setData(faceVertex, maskUV)
            filter.setMaskBitmap(maskImage)
            val gpuImage = GPUImage(context)
            gpuImage.setFilter(filter)
            return gpuImage.getBitmapWithFilterApplied(originImage)
        }

        fun gaussSmooth(
            context: Context,
            image: Bitmap
        ): Bitmap {
            val gpuImage = GPUImage(context)
            val filter = GPUImageGaussianBlurFilter(4f)
            gpuImage.setFilter(filter)
            var result = image
            for (i in 0..2) {
                result = gpuImage.getBitmapWithFilterApplied(result)
            }
            return result
        }

        fun hsvBlend(
            context: Context,
            image: Bitmap,
            origin: Bitmap
        ): Bitmap {
            val gpuImage = GPUImage(context)
            val filter = GPUImageHSVBlendFilter()
            filter.bitmap = image
            gpuImage.setFilter(filter)
            return gpuImage.getBitmapWithFilterApplied(origin)
        }

        fun rgbSharpen(
            context: Context,
            image: Bitmap
        ): Bitmap {
            val gpuImage = GPUImage(context)
            val filter = GPUImageSharpenFilter()
            gpuImage.setFilter(filter)
            return gpuImage.getBitmapWithFilterApplied(image)
        }

        fun skin3dMask(
            context: Context,
            maskImage: Bitmap,
            originImage: Bitmap,
            maskUV: FloatArray,
            faceVertex: FloatArray,
        ): Bitmap {
            val filter = FaceSwapFilter(context, MaskMode.SWAP_FRAME)
            val index = 4
            val x = floor(faceVertex[index * 3 + 0] * originImage.width).toInt()
            val y = floor(faceVertex[index * 3 + 1] * originImage.height).toInt()
            val pixel = originImage.getPixel(x, y)
            val r = Color.red(pixel) / 255.0;
            val b = Color.blue(pixel) / 255.0;
            val g = Color.green(pixel) / 255.0;
            Log.e("GPUSwap", "Skin-color $r $g $b")

            filter.skinColor = floatArrayOf(r.toFloat(), g.toFloat(), b.toFloat())
            filter.setData(faceVertex, maskUV)
            filter.setMaskBitmap(maskImage)
            val gpuImage = GPUImage(context)
            gpuImage.setFilter(filter)
            return gpuImage.getBitmapWithFilterApplied(originImage)
        }
    }
}