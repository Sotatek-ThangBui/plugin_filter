package jp.co.cyberagent.android.gpuimage.ml

import android.util.Log
import jp.co.cyberagent.android.gpuimage.Constants
import jp.co.cyberagent.android.gpuimage.VtxIndex
import jp.co.cyberagent.android.gpuimage.VtxIndex.Companion.border2Index
import jp.co.cyberagent.android.gpuimage.VtxIndex.Companion.border3Index
import jp.co.cyberagent.android.gpuimage.VtxIndex.Companion.border45Index
import jp.co.cyberagent.android.gpuimage.VtxIndex.Companion.border4Index
import jp.co.cyberagent.android.gpuimage.VtxIndex.Companion.borderIndex
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class FaceUtils {
    companion object {
        fun getVtxAlpha(
            alphaMode: Float,
            faceIndex: Int,
            skinPercent: Float,
            eyePercent: Float,
            nosePercent: Float,
            mouthPercent: Float,
        ): FloatBuffer {
            return when (alphaMode) {
                -1f -> noVtxAlpha(faceIndex)
                0f -> borderVtxAlpha(
                    faceIndex,
                    skinPercent, eyePercent, nosePercent, mouthPercent
                )
                1f -> decorateVtxAlpha(
                    faceIndex,
                    skinPercent, eyePercent, nosePercent, mouthPercent
                )
                2f -> noForeheadVtxAlpha(
                    faceIndex,
                    skinPercent, eyePercent, nosePercent, mouthPercent
                )
                6f -> decorateSwapVtxAlpha(
                    faceIndex,
                    skinPercent, eyePercent, nosePercent, mouthPercent
                )
                5f -> borderVtxAlphaSwap(
                    faceIndex,
                    1f, eyePercent, nosePercent, mouthPercent
                )
                4f -> noForeheadVtxAlphaBeauty(
                    faceIndex,
                    skinPercent, eyePercent, nosePercent, mouthPercent
                )
                7f -> animalVtxAlpha(faceIndex)
                else -> tattooVtxAlpha(faceIndex)
            }
        }

         fun animalVtxAlpha(faceIndex: Int): FloatBuffer {
            val data = FloatArray(faceIndex) { 0.7f }
            val customIndexBorder3 = intArrayOf(187, 207, 50, 117, 111, 35).toSet()
            val customIndexBorder4 = intArrayOf(200, 201, 421, 194, 418).toSet()
            val bottomMouth =
                intArrayOf(106, 182, 83, 18, 313, 406, 335, 273, 432, 422, 202, 43).toSet()
            val border3IndexTop = intArrayOf(
                143, 156, 70, 63, 105, 66, 107, 9,
                336, 296, 334, 293, 300, 383, 372,
                411,
                187
            ).toSet()
            val test = intArrayOf(345, 372, 383, 300, 116, 143, 156, 70)
            val border3Index = intArrayOf(
                199, 428, 208, 32, 211, 210, 262, 424, 204, 431, 430,
                430, 431, 262, 428, 199,
                208, 32, 211, 210, 213, 433,
                416, 434, 376, 345, 352,
                214, 192, 147, 123, 116,
                227, 34
            ).toSet()
            for (i in data.indices) {
                when (i) {
                    in test -> data[i] = .1f
                    in border3Index -> data[i] = 0f
                    in borderIndex -> data[i] = 0f
                    in border2Index -> data[i] = 0.0f
                    in border3IndexTop -> data[i] = .36f
                    in customIndexBorder3 -> data[i] = 0.6f
                    in customIndexBorder4 -> data[i] = 0f
                    in bottomMouth -> data[i] = 0.36f
                }
            }
            val result = ByteBuffer
                .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            result.put(data)
            return result
        }


        fun borderVtxAlpha(size:Int): FloatBuffer {
            val data = FloatArray(size) { 1f }
            for (i in data.indices) {
                if (i in VtxIndex.borderIndex) {
                    data[i] = 0f
                } else if (i in VtxIndex.border2Index) {
                    data[i] = 0.5f
                }
            }
            val result = ByteBuffer
                .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            result.put(data)
            return result
        }

        private fun tattooVtxAlpha(faceIndex:Int): FloatBuffer {
            val data = FloatArray(faceIndex) { 0.7f }
            val borderIndex = intArrayOf(
                // @todo: additional facemesh for full face cover
                // 468, 469, 470, 471, 472, 473, 474, 475,
                10, 338, 297, 332,
                284, 251, 389, 356, 454, 323, 361, 288,
                397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136,
                172, 58, 132, 93, 234, 127, 162, 21, 54,
                103, 67, 109,
            ).toSet()


            for (i in data.indices) {
                if (i in borderIndex) {
                    data[i] = 0f
                } else if (i in border2Index) {
                    data[i] = 0.0f
                }
            }
            val result = ByteBuffer
                .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            result.put(data)
            return result
        }

        fun decorateVtxAlpha(
            faceIndex: Int,
            skinPercent: Float, eyePercent: Float,
            nosePercent: Float, mouthPercent: Float
        ): FloatBuffer {
            val data = FloatArray(faceIndex) { skinPercent } //0.6f

            for (i in data.indices) {
                when (i) {
//                in eyeOutIndex -> {
//                    data[i] = 0.5f
//                }
                    in VtxIndex.eyeIndex -> data[i] = eyePercent//0.1f
//                in mouthOutIndex -> {
//                    data[i] = 0.3f
//                }
                    in VtxIndex.mouthIndex -> data[i] = mouthPercent//0.15f
//                in noseUpIndex -> {
//                    data[i] = 0.5f
//                }
                    in VtxIndex.noseIndex -> data[i] = nosePercent//0.2f
                }
            }
            //            result.put(data)
            return ByteBuffer
                .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer().put(data)
        }

        fun decorateSwapVtxAlpha(
            faceIndex: Int,
            skinPercent: Float, eyePercent: Float,
            nosePercent: Float, mouthPercent: Float
        ): FloatBuffer {
            val data = FloatArray(faceIndex) { skinPercent } //0.6f

            for (i in data.indices) {
                when (i) {
                    in VtxIndex.eyeOutIndex -> {
                        data[i] = 1f
                    }
                    in VtxIndex.eyeIndex -> {
                        data[i] = eyePercent//0.1f
                    }
                    in VtxIndex.mouthOutIndex -> {
                        data[i] = mouthPercent * 0.8f
                    }
                    in VtxIndex.mouthIndex -> {
                        data[i] = mouthPercent//0.15f
                    }
                    in VtxIndex.noseIndex -> {
                        data[i] = nosePercent//0.2f
                    }
                }
            }
            //            result.put(data)
            return ByteBuffer
                .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer().put(data)
        }

        fun swapVtxAlpha(faceIndex: Int): FloatBuffer {
//            val faceIndex = faceTriangIndex
            val data = FloatArray(faceIndex) { 1f }
            val borderIndex = intArrayOf(
                // @Todo: additional facemesh
                //            468, 469, 470, 471, 472, 473, 474, 475,
                10, 338, 297, 332,
                284, 251, 389, 356, 454, 323, 361, 288,
                397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136,
                172, 58, 132, 93, 234, 127, 162, 21, 54,
                103, 67, 109,
            ).toSet()


            for (i in data.indices) {
                if (i in borderIndex) {
                    data[i] = 0.0f
                } else if (i in border2Index) {
                    data[i] = 0.9f
                }
            }
            val result = ByteBuffer
                .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            result.put(data)
            return result
        }

        // For swapping face
        fun border1VtxAlpha(faceIndex: Int): FloatBuffer {
//            val faceIndex = faceTriangIndex
            val data = FloatArray(faceIndex) { 0.9f } //0.6f
            val eyeOutIndex = intArrayOf(
                130, 247, 30, 29, 27, 28,
                56, 190, 243,
                112, 26, 22, 23, 24,
                110, 25,
                463, 414, 286, 258, 257, 259, 260,
                467, 359, 255, 339, 254, 253, 252,
                256, 341,
            ).toSet()
            val eyeIndex = intArrayOf(
                33, 246, 161, 160, 159, 158,
                157, 173, 133,
                155, 154, 153, 145, 144,
                163, 7,
                362, 398, 384, 385, 386, 387, 388,
                466, 263, 249, 390, 373, 374, 380,
                381, 382,
            ).toSet()
            val mouthOutIndex = intArrayOf(
                61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 91,
                409, 270, 269, 267, 0, 27, 39, 40, 185,
            ).toSet()
            val mouthInIndex = intArrayOf(
                78, 308, 324, 318, 402, 317, 14, 87, 178, 88, 95,
                415, 310, 311, 312, 13, 82, 81, 80, 191
            ).toSet()
            val mouthIndex = intArrayOf(
                //61,
                76, 62, 78,
                //291,
                306, 292, 308,
                //375,
                307, 325, 324,
                //321,
                320, 319, 318,
                //405,
                404, 403, 402,
                //314,
                315, 316, 317,
                //17,
                16, 15, 14,
                //84,
                85, 86, 87,
                //181,
                180, 179, 178,
                //91,
                90, 89, 88,
                //146,
                77, 96, 95,
                //409,
                408, 407, 415,
                //270,
                304, 272, 310,
                //269,
                303, 271, 311,
                //267,
                302, 268, 312,
                //0,
                11, 12, 13,
                //37,
                72, 38, 82,
                //39,
                73, 41, 81,
                //40,
                74, 42, 80,
                //185,
                184, 183, 191,
//            57, 43, 106, 182, 83, 18, 313,
//            406, 335, 273, 287,
            ).toSet()
            val noseUpIndex = intArrayOf(
                55, 8, 285, 9,
                193, 168, 417,
                188, 122, 6, 351, 412,
                217, 174, 196, 197, 419, 399, 437,
            ).toSet()
            val noseIndex = intArrayOf(
                209,
                198,
                236,
                3,
                195,
                248,
                456,
                420,
                429,
                //129,
                49,
                131,
                134,
                51,
                5,
                281,
                363,
                360,
                279,
                //358,
                102,
                48,
                115,
                220,
                45,
                4,
                275,
                440,
                344,
                278,
                331,
                64,
                219,
                218,
                237,
                44,
                1,
                274,
                457,
                438,
                439,
                294,
                235,
                59,
                166,
                79,
                239,
                238,
                241,
                125,
                19,
                354,
                461,
                468,
                459,
                309,
                392,
                289,
                455,
                //98,
                //240,
                75,
                60,
                20,
                //99,
                242,
                97,
                141,
                94,
                //2,
                370,
                326,
                462,
                //328,
                250,
                290,
                305,
                //460,
                //327,
            ).toSet()
            val noseCenterIndex = intArrayOf(
                168, 6, 197, 195, 5, 4, 1, 19, 94, 2,
                122, 236, 115, 166,
                351, 456, 344, 392,
            ).toSet()







            for (i in data.indices) {
                when (i) {
//                in eyeOutIndex -> {
//                    data[i] = 0.5f
//                }
                    in eyeOutIndex -> {
                        data[i] = 0.9f//eyePercent//0.1f
                    }
                    in eyeIndex -> {
                        data[i] = 0.9f//eyePercent//0.1f
                    }
//                in mouthOutIndex -> {
//                    data[i] = 0.3f
//                }
                    in mouthIndex -> {
                        data[i] = 1f//mouthPercent//0.15f
                    }
                    in mouthInIndex -> {
                        data[i] = 0.6f
                    }
                    in noseIndex -> {
                        data[i] = 1f//nosePercent//0.2f
                    }
                    in borderIndex -> {
                        data[i] = 0f
                    }
                    in border2Index -> {
                        data[i] = 0.2f //0.4f //0.1f //
                    }
                    in border3Index -> {
                        data[i] = 0.4f // 0.6f //0.2f //
                    }
                    in border4Index -> {
                        data[i] = 0.7f //0.75f //0.3f //
                    }
                    in noseUpIndex -> {
                        data[i] = 0.75f
                    }
                    in border45Index -> {
                        data[i] = 0.85f //0.85f //0.4f //
                    }
                }
            }
            val result = ByteBuffer
                .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            result.put(data)
            return result
        }

        fun noVtxAlpha(
            faceIndex: Int
        ): FloatBuffer {
            val data = FloatArray(faceIndex) { 1f } //0.6f
            val result = ByteBuffer
                .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            result.put(data)
            return result
        }

        // For swapping face
        fun borderVtxAlpha(
            faceIndex: Int,
            skinPercent: Float, eyePercent: Float,
            nosePercent: Float, mouthPercent: Float
        ): FloatBuffer {
            val data = FloatArray(faceIndex) { skinPercent } //0.6f
            val border2Index = intArrayOf(
                151, 337, 299, 333, 298, 301, 368, 264, 447, 366,
                401, 435, 367, 364, 394, 395, 369, 396, 175, 171,
                140, 170, 169,
                135, 138, 215, 177, 137, 227, 34,
                139, 71, 68, 104, 69, 108,
            ).toSet()
            val defaultPercent = 1f

            for (i in data.indices) {
                when (i) {
//                in eyeOutIndex -> {
//                    data[i] = 0.5f
//                }
                    in VtxIndex.eyeOutIndex -> {
                        data[i] = defaultPercent * eyePercent//0.1f//0.9f//
                    }
                    in VtxIndex.eyeIndex -> {
                        data[i] = eyePercent//0.1f//0.9f//
                    }
                    in VtxIndex.mouthOutIndex -> {
                        data[i] = defaultPercent * mouthPercent
                    }
                    in VtxIndex.mouthIndex -> {
                        data[i] = mouthPercent//0.15f//1f//
                    }
                    in VtxIndex.mouthInIndex -> {
                        data[i] = defaultPercent * mouthPercent//0.6f
                    }
                    in VtxIndex.noseIndex -> {
                        data[i] = nosePercent//0.2f//1f//
                    }
                    in VtxIndex.noseUpIndex -> {
                        data[i] = defaultPercent * skinPercent
                    }
                    in border2Index -> {
                        data[i] = 0.2f * skinPercent //0.4f //0.1f //
                    }
                    in border3Index -> {
                        data[i] = 0.5f * skinPercent // 0.6f //0.2f //
                    }
                    in border4Index -> {
                        data[i] = defaultPercent * skinPercent //0.75f //0.3f //
                    }
                    in border45Index -> {
                        data[i] = defaultPercent * skinPercent //0.85f //0.4f //
                    }
                    in borderIndex -> {
                        data[i] = 0f
                    }
                }
            }
            val result = ByteBuffer
                .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            result.put(data)
            return result
        }

        private fun noForeheadVtxAlphaBeauty(
            faceIndex: Int,
            skinPercent: Float, eyePercent: Float,
            nosePercent: Float, mouthPercent: Float
        ): FloatBuffer {
            val data = FloatArray(faceIndex) { 0f }
            for (i in data.indices) {
                when (i) {
                    in VtxIndex.eyeIndex -> data[i] = eyePercent
                    in VtxIndex.eyeOutIndex -> data[i] = eyePercent * 0.4f
                    in VtxIndex.mouthIndex -> data[i] = mouthPercent
                    in VtxIndex.mouthOutIndex -> data[i] = mouthPercent * 0.2f
                }
            }
            val result = ByteBuffer
                .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            result.put(data)
            return result
        }

        // For swapping face
        fun borderVtxAlphaSwap(
            faceIndex: Int,
            skinPercent: Float, eyePercent: Float,
            nosePercent: Float, mouthPercent: Float
        ): FloatBuffer {
            val data = FloatArray(faceIndex) { skinPercent } //0.6f
            val border2Index = intArrayOf(
                151, 337, 299, 333, 298, 301, 368, 264, 447, 366,
                401, 435, 367, 364, 394, 395, 369, 396, 175, 171,
                140, 170, 169,
                135, 138, 215, 177, 137, 227, 34,
                139, 71, 68, 104, 69, 108,
            ).toSet()
            val breadIndex = intArrayOf(
                57, 186, 92, 165, 167, 164, 393, 391, 322, 410,
                287, 273, 335, 406, 313, 18, 83, 182, 106,
                43, 212, 432
            ).toSet()
            val otherIndex = intArrayOf(
                423, 327, 358, 371,
                98, 129, 36, 205, 206,
                101, 2, 436, 434, 97, 219, 235, 166, 59, 75,
                326, 426, 203, 50, 187, 207,
            )

            for (i in data.indices) {
                when (i) {
                    in VtxIndex.eyeOutIndex -> data[i] = 0f//0.1f//0.9f//
                    in VtxIndex.mouthOutIndex -> data[i] = 0f
                    in VtxIndex.noseIndex -> data[i] = 0f//0.2f//1f//
                    in VtxIndex.noseCenterIndex -> data[i] = 0f
                    in border2Index -> data[i] = 0f //0.4f //0.1f //
                    in border3Index -> data[i] = 0f // 0.6f //0.2f //
                    in border4Index -> data[i] = 0f//0.75f //0.3f //
                    in VtxIndex.noseUpIndex -> data[i] = 0f
                    in border45Index -> data[i] = 0f //0.85f //0.4f //
                    in borderIndex -> data[i] = 0f
                    in breadIndex -> data[i] = 0f
                    in otherIndex -> data[i] = 0f
                }
            }
            val result = ByteBuffer
                .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            result.put(data)
            return result
        }

        private fun noForeheadVtxAlpha(
            faceIndex: Int,
            skinPercent: Float, eyePercent: Float,
            nosePercent: Float, mouthPercent: Float
        ): FloatBuffer {
//            val faceIndex = faceTriangIndex
            val data = FloatArray(faceIndex) { skinPercent } //0.6f
            val borderIndex = intArrayOf(
                10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288,
                397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136,
                172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109,
                // border 2
                151, 337, 299, 333, 298, 301, 368, 264,
                34, 139, 71, 68, 104, 69, 108,
            ).toSet()
            val borderMidIndex = intArrayOf(
                // Middle 2
                364, 394, 395, 369, 396, 175, 171,
                140, 170, 169, 135,
                // Middle 3
                433, 416, 434, 430, 431, 262, 428, 199,
                208, 32, 211, 210, 214, 192, 213,
            ).toSet()
            val border2Index = intArrayOf(
                // 151, 337, 299, 333, 298, 301, 368, 264,
                447, 366,
                401, 435, 367,
                // Middle face - keep the same
//                364, 394, 395, 369, 396, 175, 171,
//                140, 170, 169, 135,
                138, 215, 177, 137, 227,
                //34, 139, 71, 68, 104, 69, 108,
            ).toSet()
            val border3Index = intArrayOf(
                9, 336, 296, 334, 293, 300, 383, 372, 345, 352,
                376,
                // Midle face - keep the same
//                433, 416, 434, 430, 431, 262, 428, 199,
//                208, 32, 211, 210, 214, 192, 213,
                147, 123,
                116, 143, 156, 70, 63, 105, 66, 107,
            ).toSet()
            val border4Index = intArrayOf(
                8, 55,
                65, 52, 53, 46,
                124, 35,
                11, 117, 50, 187, 207,
                212, 202, 204, 194, 201, 200,
                421, 418, 424, 422, 432,
                427, 411, 280, 346,
                340, 265,
                446, 353,
                276, 283, 282, 295, 285
            ).toSet()
            val border5Index = intArrayOf(
                8, 55,
                65, 52, 53, 46,
                124, 35, 226,
                111, 117, 118, 119,
                120, 121, 114, 217, 47, 126,
                100, 142, 101, 36, 205, 50,
                187, 207, 206, 216,
                212, 202, 204, 194, 201, 200,
                421, 418, 424, 422, 432,
                436, 426, 427, 411,
                280, 425, 266, 330,
                329, 348, 347, 346, 340,
                371, 353, 437, 399, 412, 343,
                277, 350, 349, 450, 449, 448,
                261, 265, 446, 353,
                276, 283, 282, 295, 285
            ).toSet()





            for (i in data.indices) {
                when (i) {
                    in borderIndex -> {
                        data[i] = 0f
                    }
                    in border2Index -> {
                        data[i] = 0.05f * skinPercent
                    }
                    in border3Index -> {
                        data[i] = 0.4f * skinPercent // 0.6f //0.2f //
                    }
                    in borderMidIndex -> {
                        data[i] = 0.55f * skinPercent // 0.6f //0.2f //
                    }
                    in border4Index -> {
                        data[i] = 0.7f * skinPercent //0.75f //0.3f //
                    }
                    in border5Index -> {
                        data[i] = 0.85f * skinPercent //0.75f //0.3f //
                    }
                }
            }

            Log.e(
                "MaskMode", "No-forehead alpha:: " +
                        "${data[156]} ${data[139]} ${data[143]} ${data[34]}"
            )
            val result = ByteBuffer
                .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            result.put(data)
            return result
        }
    }
}