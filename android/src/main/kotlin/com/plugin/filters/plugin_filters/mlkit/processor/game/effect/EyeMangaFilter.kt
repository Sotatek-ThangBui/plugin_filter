package com.plugin.filters.plugin_filters.mlkit.processor.game.effect

import android.content.Context
import jp.co.cyberagent.android.gpuimage.Constants
import jp.co.cyberagent.android.gpuimage.ml.MaskMode

class EyeMangaFilter(context: Context) : TattooFilter(context) {
    companion object {
        val s_face_tris_optimize_eye =
            shortArrayOf(
                127, 34, 139, 11, 0, 37, 72, 37, 39, 128, 121, 47, 104, 69, 67, 175,
                171, 148, 118, 50, 101, 73, 39, 40, 9, 151, 108, 48, 115, 131, 194, 204, 211,
                74, 40, 185, 80, 42, 183, 40, 92, 186, 202, 212, 214, 83, 18, 17, 76, 61, 146,
                106, 204, 194, 135, 214, 192, 203, 165, 98, 21, 71, 68, 51, 45, 4, 77, 146, 91,
                205, 50, 187, 201, 200, 18, 91, 106, 182, 90, 91, 181, 85, 84, 17, 206, 203, 36,
                148, 171, 140, 92, 40, 39, 236, 3, 196, 54, 68, 104, 193, 168, 8, 189, 193, 55,
                98, 97, 99, 126, 47, 100, 166, 79, 218, 209, 49, 131, 135, 136, 150, 47, 126, 217,
                45, 51, 134, 211, 170, 140, 67, 69, 108, 43, 106, 91, 63, 53, 52, 238, 20, 242, 46,
                70, 156, 78, 62, 96, 46, 53, 63, 143, 34, 227, 123, 117, 111, 44, 125, 19, 236, 134,
                51, 216, 206, 205, 39, 37, 167, 200, 201, 208, 36, 142, 100, 57, 212, 202, 20,
                60, 99, 204, 202, 210, 43, 202, 204, 62, 76, 77, 137, 123, 116, 41, 38, 72, 203,
                129, 142, 64, 98, 240, 49, 102, 64, 41, 73, 74, 212, 216, 207, 42, 74, 184, 169,
                170, 211, 170, 149, 176, 105, 66, 69, 122, 6, 168, 123, 147, 187, 96, 77, 90, 65,
                55, 107, 89, 90, 180, 101, 100, 120, 63, 105, 104, 93, 137, 227, 15, 86, 85, 129,
                102, 49, 14, 87, 86, 55, 8, 9, 100, 47, 121, 88, 89, 179, 6, 122, 196, 88, 95, 96,
                138, 172, 136, 215, 58, 172, 115, 48, 219, 42, 80, 81, 195, 3, 51, 43, 146, 61, 171,
                175, 199, 81, 82, 38, 52, 65, 66, 34, 127, 234, 107, 108, 69, 109, 108, 151, 48, 64,
                235, 62, 78, 191, 129, 209, 126, 111, 35, 143, 117, 123, 50, 19, 125, 141, 3, 195,
                197, 220, 237, 44, 70, 71, 139, 122, 193, 245, 71, 21, 162, 170, 169, 150, 188,
                174, 196, 216, 186, 92, 2, 97, 167, 141, 125, 241, 164, 167, 37, 72, 38, 12, 38,
                82, 13, 63, 68, 71, 101, 50, 205, 206, 92, 165, 209, 198, 217, 165, 167, 97, 220,
                115, 218, 239, 238, 241, 214, 135, 169, 171, 208, 32, 125, 44, 237, 86, 87, 178,
                85, 86, 179, 84, 85, 180, 83, 84, 181, 201, 83, 182, 137, 93, 132, 76, 62, 183, 61,
                76, 184, 57, 61, 185, 212, 57, 186, 214, 207, 187, 34, 143, 156, 79, 239, 237, 123,
                137, 177, 44, 1, 4, 201, 194, 32, 64, 102, 129, 213, 215, 138, 59, 166, 219, 242,
                99, 97, 2, 94, 141, 75, 59, 235, 238, 79, 20, 166, 59, 75, 60, 75, 240, 147, 177,
                215, 20, 79, 166, 187, 147, 213, 128, 114, 188, 114, 217, 174, 131, 115, 220, 217,
                198, 236, 198, 131, 134, 177, 132, 58, 143, 35, 124, 356, 389, 368, 11, 302, 267,
                302, 303, 269, 357, 343, 277, 333, 332, 297, 175, 152, 377, 347, 348, 330, 303,
                304, 270, 9, 336, 337, 278, 279, 360, 418, 262, 431, 304, 408, 409, 310, 415, 407,
                270, 409, 410, 422, 430, 434, 313, 314, 17, 306, 307, 375, 335, 406, 418, 364, 367,
                416, 423, 358, 327, 251, 284, 298, 281, 5, 4, 307, 320, 321, 425, 427, 411, 421,
                313, 18, 321, 405, 406, 320, 404, 405, 315, 16, 17, 426, 425, 266, 377, 400, 369,
                322, 391, 269, 456, 399, 419, 284, 332, 333, 417, 285, 8, 327, 460, 328, 355, 371,
                329, 392, 439, 438, 429, 420, 360, 364, 394, 379, 277, 343, 437, 275, 440, 363, 431,
                262, 369, 297, 338, 337, 273, 375, 321, 293, 334, 282, 458, 461, 462, 276, 353, 383,
                308, 324, 325, 276, 300, 293, 372, 345, 447, 352, 345, 340, 274, 1, 19, 456, 248,
                281, 436, 427, 425, 269, 391, 393, 200, 199, 428, 266, 330, 329, 287, 273, 422, 250,
                462, 328, 424, 431, 430, 273, 335, 424, 292, 325, 307, 366, 447, 345, 271, 303,
                302, 423, 266, 371, 294, 455, 460, 279, 278, 294, 271, 272, 304, 432, 434, 427,
                272, 407, 408, 394, 430, 431, 395, 369, 400, 334, 333, 299, 351, 417, 168, 352,
                280, 411, 325, 319, 320, 295, 296, 336, 319, 403, 404, 330, 348, 349, 293, 298,
                333, 323, 454, 447, 15, 16, 315, 358, 429, 279, 14, 15, 316, 285, 336, 9, 329,
                349, 350, 318, 402, 403, 6, 197, 419, 318, 319, 325, 367, 364, 365, 435, 367,
                397, 344, 438, 439, 272, 271, 311, 195, 5, 281, 273, 287, 291, 396, 428, 199,
                311, 271, 268, 282, 334, 296, 264, 447, 454, 336, 296, 299, 338, 10, 151, 278,
                439, 455, 292, 407, 415, 358, 371, 355, 340, 345, 372, 346, 347, 280, 19, 94,
                370, 248, 419, 197, 440, 275, 274, 300, 383, 368, 351, 412, 465, 301, 368, 389,
                395, 378, 379, 412, 351, 419, 436, 426, 322, 2, 164, 393, 370, 462, 461, 164,
                0, 267, 302, 11, 12, 268, 12, 13, 293, 300, 301, 330, 266, 425, 426, 423, 391,
                429, 355, 437, 391, 327, 326, 440, 457, 438, 459, 457, 461, 434, 430, 394, 396,
                369, 262, 354, 461, 457, 316, 403, 402, 315, 404, 403, 314, 405, 404, 313, 406,
                405, 421, 418, 406, 366, 401, 361, 306, 408, 407, 291, 409, 408, 287, 410, 409,
                432, 436, 410, 434, 416, 411, 264, 368, 383, 309, 438, 457, 352, 376, 401, 274,
                275, 4, 421, 428, 262, 294, 327, 358, 433, 416, 367, 289, 455, 439, 462, 370,
                326, 2, 326, 370, 305, 460, 455, 459, 458, 250, 289, 392, 290, 290, 328, 460,
                376, 433, 435, 250, 290, 392, 411, 416, 433, 357, 465, 412, 343, 412, 399, 360,
                363, 440, 437, 399, 456, 420, 456, 363, 401, 435, 288, 372, 383, 353, 75, 60, 166,
                238, 239, 79, 162, 127, 139, 72, 11, 37, 73, 72, 39, 114, 128, 47, 103, 104, 67,
                152, 175, 148, 119, 118, 101, 74, 73, 40, 107, 9, 108, 49, 48, 131, 32, 194, 211,
                184, 74, 185, 191, 80, 183, 185, 40, 186, 210, 202, 214, 84, 83, 17, 77, 76, 146,
                182, 106, 194, 138, 135, 192, 129, 203, 98, 54, 21, 68, 5, 51, 4, 90, 77, 91, 207,
                205, 187, 83, 201, 18, 181, 91, 182, 180, 90, 181, 16, 85, 17, 205, 206, 36, 176,
                148, 140, 165, 92, 39, 174, 236, 196, 103, 54, 104, 55, 193, 8, 240, 98, 99, 142,
                126, 100, 219, 166, 218, 198, 209, 131, 169, 135, 150, 114, 47, 217, 220, 45, 134,
                32, 211, 140, 109, 67, 108, 146, 43, 91, 105, 63, 52, 241, 238, 242, 124, 46, 156,
                95, 78, 96, 70, 46, 63, 116, 143, 227, 116, 123, 111, 1, 44, 19, 3, 236, 51, 207,
                216, 205, 165, 39, 167, 199, 200, 208, 101, 36, 100, 43, 57, 202, 242, 20, 99, 211,
                204, 210, 106, 43, 204, 96, 62, 77, 227, 137, 116, 73, 41, 72, 36, 203, 142, 235,
                64, 240, 48, 49, 64, 42, 41, 74, 214, 212, 207, 183, 42, 184, 210, 169, 211, 140,
                170, 176, 104, 105, 69, 193, 122, 168, 50, 123, 187, 89, 96, 90, 66, 65, 107, 179,
                89, 180, 119, 101, 120, 68, 63, 104, 234, 93, 227, 16, 15, 85, 209, 129, 49, 15,
                14, 86, 107, 55, 9, 120, 100, 121, 178, 88, 179, 197, 6, 196, 89, 88, 96, 135, 138,
                136, 138, 215, 172, 218, 115, 219, 41, 42, 81, 5, 195, 51, 57, 43, 61, 208, 171,
                199, 41, 81, 38, 105, 52, 66, 227, 34, 234, 66, 107, 69, 10, 109, 151, 219, 48,
                235, 183, 62, 191, 142, 129, 126, 116, 111, 143, 118, 117, 50, 94, 19, 141, 196,
                3, 197, 45, 220, 44, 156, 70, 139, 188, 122, 245, 139, 71, 162, 149, 170, 150,
                122, 188, 196, 206, 216, 92, 164, 2, 167, 242, 141, 241, 0, 164, 37, 11, 72, 12,
                12, 38, 13, 70, 63, 71, 36, 101, 205, 203, 206, 165, 126, 209, 217, 98, 165, 97,
                237, 220, 218, 237, 239, 241, 210, 214, 169, 140, 171, 32, 241, 125, 237, 179,
                86, 178, 180, 85, 179, 181, 84, 180, 182, 83, 181, 194, 201, 182, 177, 137, 132,
                184, 76, 183, 185, 61, 184, 186, 57, 185, 216, 212, 186, 192, 214, 187, 139, 34,
                156, 218, 79, 237, 147, 123, 177, 45, 44, 4, 208, 201, 32, 98, 64, 129, 192, 213,
                138, 235, 59, 219, 141, 242, 97, 97, 2, 141, 240, 75, 235, 99, 60, 240, 213, 147,
                215, 60, 20, 166, 192, 187, 213, 245, 128, 188, 188, 114, 174, 134, 131, 220, 174,
                217, 236, 236, 198, 134, 215, 177, 58, 156, 143, 124, 264, 356, 368, 0, 11, 267,
                267, 302, 269, 350, 357, 277, 299, 333, 297, 396, 175, 377, 280, 347, 330, 269,
                303, 270, 151, 9, 337, 344, 278, 360, 424, 418, 431, 270, 304, 409, 272, 310, 407,
                322, 270, 410, 432, 422, 434, 18, 313, 17, 291, 306, 375, 424, 335, 418, 434, 364,
                416, 391, 423, 327, 301, 251, 298, 275, 281, 4, 375, 307, 321, 280, 425, 411, 200,
                421, 18, 335, 321, 406, 321, 320, 405, 314, 315, 17, 423, 426, 266, 396, 377, 369,
                270, 322, 269, 248, 456, 419, 298, 284, 333, 168, 417, 8, 326, 327, 328, 277, 355,
                329, 309, 392, 438, 279, 429, 360, 365, 364, 379, 355, 277, 437, 281, 275, 363, 395,
                431, 369, 299, 297, 337, 335, 273, 321, 283, 293, 282, 250, 458, 462, 300, 276, 383,
                292, 308, 325, 283, 276, 293, 264, 372, 447, 346, 352, 340, 354, 274, 19, 363, 456,
                281, 426, 436, 425, 267, 269, 393, 421, 200, 428, 371, 266, 329, 432, 287, 422, 290,
                250, 328, 422, 424, 430, 422, 273, 424, 306, 292, 307, 352, 366, 345, 268, 271, 302,
                358, 423, 371, 327, 294, 460, 331, 279, 294, 303, 271, 304, 436, 432, 427, 304, 272,
                408, 395, 394, 431, 378, 395, 400, 296, 334, 299, 6, 351, 168, 376, 352, 411, 307,
                325, 320, 285, 295, 336, 320, 319, 404, 329, 330, 349, 334, 293, 333, 366, 323, 447,
                316, 15, 315, 331, 358, 279, 317, 14, 316, 8, 285, 9, 277, 329, 350, 319, 318, 403,
                351, 6, 419, 324, 318, 325, 397, 367, 365, 288, 435, 397, 278, 344, 439, 310, 272,
                311, 248, 195, 281, 375, 273, 291, 175, 396, 199, 312, 311, 268, 295, 282, 296, 356,
                264, 454, 337, 336, 299, 337, 338, 151, 294, 278, 455, 308, 292, 415, 429, 358, 355, 265, 340, 372, 352, 346, 280, 354, 19, 370, 195, 248, 197, 457, 440, 274, 301, 300, 368, 417, 351, 465, 251, 301, 389, 394, 395, 379, 399, 412, 419, 410, 436, 322, 326, 2, 393, 354, 370, 461, 393, 164, 267, 268, 302, 12, 312, 268, 13, 298, 293, 301, 280, 330, 425, 322, 426, 391, 420, 429, 437, 393, 391, 326, 344, 440, 438, 458, 459, 461, 364, 434, 394, 428, 396, 262, 274, 354, 457, 317, 316, 402, 316, 315, 403, 315, 314, 404, 314, 313, 405, 313, 421, 406, 323, 366, 361, 292, 306, 407, 306, 291, 408, 291, 287, 409, 287, 432, 410, 427, 434, 411, 372, 264, 383, 459, 309, 457, 366, 352, 401, 1, 274, 4, 418, 421, 262, 331, 294, 358, 435, 433, 367, 392, 289, 439, 328, 462, 326, 94, 2, 370, 289, 305, 455, 309, 459, 250, 305, 289, 290, 305, 290, 460, 401, 376, 435, 309, 250, 392, 376, 411, 433, 343, 357, 412, 437, 343, 399, 344, 360, 440, 420, 437, 456, 360, 420, 363, 361, 401, 288, 265, 372, 353, 193, 55, 159, 55, 65, 159, 65, 52, 159, 52, 53, 159, 53, 46, 159, 46, 124, 159, 124, 35, 159, 159, 145, 193, 193, 145, 245, 111, 117, 145, 117, 118, 145, 118, 119, 145, 119, 120, 145, 120, 121, 145, 121, 128, 145, 128, 245, 145, 145, 159, 35, 35, 111, 145, 417, 285, 386, 285, 295, 386, 295, 282, 386, 282, 283, 386, 283, 276, 386, 276, 353, 386, 353, 265, 386, 417, 465, 386, 465, 386, 374, 265, 340, 374, 374, 386, 265, 465, 357, 374, 357, 350, 374, 350, 349, 374, 349, 348, 374, 348, 347, 374, 347, 346, 374, 346, 340, 374
            )
    }

    override fun setMode(mode: MaskMode) {
        this.maskMode = mode
        faceTriangIndex = s_face_tris_optimize_eye
    }
}