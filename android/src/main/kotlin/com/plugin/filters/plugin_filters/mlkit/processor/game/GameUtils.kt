package com.plugin.filters.plugin_filters.mlkit.processor.game

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import jp.co.cyberagent.android.gpuimage.ml.FaceShader
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt

enum class FACE_GAME_STATE(val value: Float) {
    INTRO(0f),
    CONFIRM(1f),
    READY(2f),
    PLAY_NORMAL(3f),
    PLAY_PEEK(4f),
    FINISH(5f),
}

class GameUtils {

    // Common utilities function for Face-Game logic
    companion object {

        val FONT_CHAR = arrayOf(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "!", "?", "@", "#", "$", "%", "zeta", "'", "\"",
            "(", ")", "+", "-", "=", ",", ".",

            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z",

            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
            "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
            "u", "v", "w", "x", "y", "z",

            "<", ">", "[", "]", "{", "}", "\\", "/", "`",
            "á", "ã", "à", "é", "e:", "è", "í", "ó", "õ",
            "ú", "ù", "u:", "n~", "C_,", "c_,", "i_", "?_",
            "cO", "rO", "tm^", " ", "SS", "+^", "++^", "-^", "--^",
            "P|", "divide", "dot_up", "omega_up", "c_slash",
            "beta", "depa", ":", ";", "^", "~", "male", "female",
            "heart", "single_note", "double_note", "sun"
        )

        val FONT_PATH = "game/text/font.png"
        val CHAR_W = 39
        val CHAR_H = 47

        fun fontCharTexture(): HashMap<String, FloatArray> {
            // Load position of char
            // Get texture position of char in font-image
            val charText =  HashMap<String, FloatArray>()

            val w = (1.0 / 26.0).toFloat()
            val h = (1.0 / 5.0).toFloat()
            for(row in 0 until 5) {
                for(col in 0 until 26) {
                    val pos = row * 26 + col
                    val char = FONT_CHAR[pos]
                    val x = col * w
                    val y = row * h
                    charText[char] = floatArrayOf(
                        x, y,
                        x, y + h,
                        x + w, y,
                        x + w, y + h
                    )
                }
            }

            return charText
        }



        fun calTextRatio(
            imageWidth: Float, imageHeight: Float,
            outputWidth: Float, outputHeight: Float,
        ):FloatArray {
            val ratio1: Float = outputWidth / imageWidth
            val ratio2: Float = outputHeight / imageHeight
            val ratioMax = Math.max(ratio1, ratio2)
            val imageWidthNew = (imageWidth * ratioMax).roundToInt()
            val imageHeightNew = (imageHeight * ratioMax).roundToInt()

            val textRatioW = imageWidthNew / outputWidth
            val textRatioH = imageHeightNew / outputHeight

            return floatArrayOf(textRatioW, textRatioH)
        }

        fun fetchData(urlString: String, callback: (data: String?) -> Unit) {
            Thread {
                var jsonData: String? = null
                if (!urlString.isNullOrEmpty()) {
                    try {
                        val url = URL(urlString)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connect()

                        val inputStream = connection.inputStream
                        jsonData = inputStream.bufferedReader().use { it.readText() }

                        inputStream.close()
                        connection.disconnect()
                    } catch (e: Exception) {
                        Log.e("FetchJsonData", "Error fetching data: $e")
                    }
                }
                callback(jsonData)
            }.start()
        }

        fun playMatchPose(vertex: FloatArray?, tile: GameObject?, gameFilter: GameFilter): Boolean {
            if(vertex == null || tile == null) {
                return false
            }
            when(tile!!.id) {
                "posedance/pose/1.png" -> {
                    // both hand on left | left hand up, right hand down
                    return poseHandLeft(vertex, gameFilter)
                }
                "posedance/pose/2.png" -> {
                    // 2 hand close to 2 hip | left elbow angle < right elbow angle
                    return poseHandHip(vertex)
                }

                "posedance/pose/3.png" -> {
                    // clap 2 hand on left
                    return poseHandClap(vertex)
                }
                "posedance/pose/4.png" -> {
                    // face down
                    return poseHandWide(vertex)
                }
                "posedance/pose/5.png" -> {
                    // face left
                    return poseHandHoldUp(vertex)
                }
                "posedance/pose/6.png" -> {
                    // face right
                    return poseHandZigZag(vertex)
                }

                "posedance/pose/7.png" -> {
                    // face left and mouth open --> open both up/low lip
                    return poseHandBalance(vertex)
                }
                "posedance/pose/8.png" -> {
                    // face left and face up and mouth open --> open both up/low lip
                    return poseHandSayNo(vertex)
                }
                "posedance/pose/9.png" -> {
                    // face right and mouth open --> open both up/low lip
                    return poseHandVi(vertex)
                }
                "posedance/pose/10.png" -> {
                    // face right and face up and mouth open --> open both up/low lip
                    return poseHandNe(vertex)
                }
                "posedance/pose/11.png" -> {
                    // face straight and mouth open --> open both up/low lip
                    return poseHandYu(vertex)
                }

                "posedance/pose/12.png" -> {
                    // face straight and down side both lip - sad mouth
                    return poseHandMe(vertex)
                }
                "posedance/pose/13.png" -> {
                    // face straight and up side both lip - light smile
                    return poseHandLe(vertex)
                }
                "posedance/pose/14.png" -> {
                    // face straight and width up side low lip - width smile
                    return poseHandWu(vertex)
                }

            }

            return false
        }


        private fun sameSide(p1: PointF, p2: PointF, l1: PointF, l2: PointF): Boolean {
            val crossProduct = (l2.x - l1.x) * (p1.y - l1.y) - (l2.y - l1.y) * (p1.x - l1.x)
            val crossProduct2 = (l2.x - l1.x) * (p2.y - l1.y) - (l2.y - l1.y) * (p2.x - l1.x)
            return crossProduct * crossProduct2 >= 0
        }


        private fun poseHandLeft(vertex: FloatArray, gameFilter: GameFilter): Boolean {
            val leftAngle = getAngleByIndex(vertex, 12, 16, 15)
            val rightAngle = getAngleByIndex(vertex, 12, 11, 15)

            return (leftAngle > 0 && leftAngle < 60)
                    && (rightAngle > 0 && rightAngle < 60)
        }


        private fun poseHandHip(vertex: FloatArray): Boolean {

            val left1Angle = getAngleByIndex(vertex, 12, 14, 24)
            val left2Angle = getAngleByIndex(vertex, 12, 16, 24)

            val right1Angle = getAngleByIndex(vertex, 11, 23, 13)
            val right2Angle = getAngleByIndex(vertex, 11, 23, 15)

//            Log.e("poseLogic", "" +
//                    "12:: ${vertexPointF(vertex, 12)}\n" +
//                    "14:: ${vertexPointF(vertex, 14)}\n" +
//                    "16:: ${vertexPointF(vertex, 16)}\n" +
//                    "24:: ${vertexPointF(vertex, 24)}\n" +
//                    "Left1 $left1Angle Left2 $left2Angle\n" +
//                    "11:: ${vertexPointF(vertex, 11)}\n" +
//                    "23:: ${vertexPointF(vertex, 23)}\n" +
//                    "13:: ${vertexPointF(vertex, 13)}\n" +
//                    "15:: ${vertexPointF(vertex, 15)}\n" +
//                    "Right1 $right1Angle Right2 $right2Angle\n" +
//                    "")

            return  left1Angle < 90 && left1Angle > 30
                    && left2Angle > 0 && left2Angle < 30
                    && right1Angle < 60 && right1Angle > right2Angle
                    && right2Angle > 0 && right2Angle < 30

        }

        private fun poseHandClap(vertex: FloatArray): Boolean {

            val disHand = distByIndex(vertex, 20, 19)
            val disBase = distByIndex(vertex, 16, 14)

            val leftAngle = getAngleByIndex(vertex, 14, 12, 16)
            val rightAngle = getAngleByIndex(vertex, 13, 11, 15)

//            Log.e("poseLogic", "" +
//                    "20:: ${vertexPointF(vertex, 20)}\n" +
//                    "19:: ${vertexPointF(vertex, 19)}\n" +
//                    "hand:: $disHand\n" +
//                    "16:: ${vertexPointF(vertex, 16)}\n" +
//                    "14:: ${vertexPointF(vertex, 14)}\n" +
//                    "base:: $disBase\n" +
//                    "Left1 $leftAngle\n" +
//                    "Right1 $rightAngle\n" +
//                    "")

            return (leftAngle > 0 && leftAngle < 90)
                    && (rightAngle > 50 && rightAngle < 150)
                    && (disHand < disBase)
        }

        private fun poseHandWide(vertex: FloatArray): Boolean {
            val leftAngle = getAngleByIndex(vertex, 12, 16, 24)
            val rightAngle = getAngleByIndex(vertex, 11, 23, 15)

            val leftHandAngle = getAngleByIndex(vertex, 14, 12, 20)
            val rightHandAngle = getAngleByIndex(vertex, 13, 19, 11)

//            Log.e("poseLogic", "" +
//                    "Left1 $leftAngle hand $leftHandAngle\n" +
//                    "Right1 $rightAngle hand $rightHandAngle\n" +
//                    "")


            return (leftAngle > 30 && leftAngle < 90)
                    && (rightAngle > 30 && rightAngle < 90)
                    && (leftHandAngle > 120 && leftHandAngle < 180)
                    && (rightHandAngle > 120 && rightHandAngle < 180)
        }


        private fun poseHandHoldUp(vertex: FloatArray): Boolean {
            val disHand = distByIndex(vertex, 20, 19)
            val disBase = distByIndex(vertex, 16, 14)

            val leftAngle = getAngleByIndex(vertex, 12, 11, 16)
            val rightAngle = getAngleByIndex(vertex, 11, 15, 12)

            Log.e("poseLogic", "poseHandHoldUp\n" +
                    "Left1 $leftAngle \n" +
                    "Right1 $rightAngle \n" +
                    "Dist hand $disHand :: base $disBase\n" +
                    "")


            return (leftAngle > 30 && leftAngle < 90)
                    && (rightAngle > 30 && rightAngle < 90)
                    && disHand < disBase
        }


        private fun poseHandZigZag(vertex: FloatArray): Boolean {
            val leftAngle = getAngleByIndex(vertex, 12, 16, 11)
            val rightAngle = getAngleByIndex(vertex, 13, 15, 11)
            val chestAngle = getAngleByIndex(vertex, 12, 11, 19)

            Log.e("poseLogic", "poseHandZigZag\n" +
                    "Left1 $leftAngle \n" +
                    "Right1 $rightAngle \n" +
                    "Chest $chestAngle \n" +
                    "")

            return (leftAngle > 30 && leftAngle < 80)
                    && abs(rightAngle) < 30
                    && abs(chestAngle) < 30
        }


        private fun poseHandBalance(vertex: FloatArray): Boolean {
            val leftAngle = getAngleByIndex(vertex, 12, 11, 16)
            val rightAngle = getAngleByIndex(vertex, 11, 15, 12)
            val noseAngle = getAngleByIndex(vertex, 16, 19, 0)

            Log.e("poseLogic", "poseHandBalance\n" +
                    "Left1 $leftAngle \n" +
                    "Right1 $rightAngle \n" +
                    "nose $noseAngle \n" +
                    "")

            return (leftAngle > 110 && leftAngle < 160)
                    && (rightAngle > 110 && rightAngle < 160)
                    && abs(noseAngle) < 30
        }


        private fun poseHandSayNo(vertex: FloatArray): Boolean {
            val left3Angle = getAngleByIndex(vertex, 12, 16, 24)
            val right2Angle = getAngleByIndex(vertex, 12, 11, 19)

            Log.e("poseLogic", "poseHandSayNo\n" +
                    "Left1 $left3Angle\n" +
                    "Right1 $right2Angle\n" +

                    "")

            return (left3Angle < 40 && left3Angle > 0)
                    && (right2Angle > 10 && right2Angle < 60)
        }

        private fun poseHandVi(vertex: FloatArray): Boolean {
            val left1Angle = getAngleByIndex(vertex, 12, 20, 16)
            val left2Angle = getAngleByIndex(vertex, 12, 20, 14)
            val left3Angle = getAngleByIndex(vertex, 12, 14, 24)

            val right1Angle = getAngleByIndex(vertex, 11, 15, 19)
            val right2Angle = getAngleByIndex(vertex, 11, 13, 19)
            val right3Angle = getAngleByIndex(vertex, 11, 23, 13)


            Log.e("poseLogic", "poseHandVi\n" +
                    "Left1 0 < $left1Angle < $left2Angle\n" +
                    " && 20 < $left3Angle < 60\n" +
                    "Right1 $right2Angle > $right1Angle\n" +
                    " && 0< $right3Angle < 30\n" +

                    "")

            return (left1Angle < left2Angle && left1Angle > 0
                    && left3Angle < 80 && left3Angle > 30)
                    && (right2Angle > right1Angle && right1Angle > 0 && right3Angle < 40)
        }


        private fun poseHandNe(vertex: FloatArray): Boolean {
            val left1Angle = getAngleByIndex(vertex, 12, 16, 24)
            val left2Angle = getAngleByIndex(vertex, 12, 14, 24)

            val right1Angle = getAngleByIndex(vertex, 13, 15, 11)
            val right2Angle = getAngleByIndex(vertex, 13, 15, 7)

            Log.e("poseLogic", "poseHandNe\n" +
                    "Left1 abs($left1Angle) < 30 && abs($left2Angle) < 30\n" +
                    "Right1 90 > $right1Angle > 0 && abs($right2Angle) < 60\n" +

                    "")

            return (abs(left1Angle) < 30 && abs(left2Angle) < 30)
                    && (right1Angle > 0 && right1Angle < 70 && abs(right2Angle) < 60)
        }


        private fun poseHandYu(vertex: FloatArray): Boolean {
            val left1Angle = getAngleByIndex(vertex, 12, 16, 24)

            val right1Angle = getAngleByIndex(vertex, 11, 12, 15)
            val right2Angle = getAngleByIndex(vertex, 11, 12, 13)

            Log.e("poseLogic", "poseHandYu\n" +
                    "Left1 0 < $left1Angle < 30\n" +
                    "Right1 90 > $right1Angle > 0 && abs($right2Angle) < 60\n" +

                    "")

            return (left1Angle < 30 && left1Angle > 0)
                    && (right2Angle > 0 && right2Angle < 90 && abs(right1Angle) < 30)
        }


        private fun poseHandMe(vertex: FloatArray): Boolean {
            val left1Angle = getAngleByIndex(vertex, 12, 16, 24)
            val left2Angle = getAngleByIndex(vertex, 14, 16, 12)

            val right1Angle = getAngleByIndex(vertex, 11, 23, 15)

            Log.e("poseLogic", "poseHandMe\n" +
                    "Left1 abs($left1Angle) < 30 &&  90 < $left2Angle < 150\n" +
                    "Right1 30 < $right1Angle < 80\n" +

                    "")

            return (abs(left1Angle) < 30 && left2Angle > 90 && left2Angle < 160)
                    && (right1Angle < 80 && right1Angle > 30)
        }



        private fun poseHandLe(vertex: FloatArray): Boolean {
            val left1Angle = getAngleByIndex(vertex, 12, 14, 24)
            val left2Angle = getAngleByIndex(vertex, 14, 16, 12)

            val right1Angle = getAngleByIndex(vertex, 11, 23, 15)

            Log.e("poseLogic", "poseHandLe\n" +
                    "Left1 abs($left1Angle) < 40 &&  abs($left2Angle) < 30\n" +
                    "Right1 30 < $right1Angle < 80\n" +

                    "")

            return (abs(left1Angle) < 60 && abs(left2Angle) < 30)
                    && (right1Angle < 80 && right1Angle > 30)
        }

        private fun poseHandWu(vertex: FloatArray): Boolean {
            val left1Angle = getAngleByIndex(vertex, 12, 11, 16)
            val left2Angle = getAngleByIndex(vertex, 12, 14, 16)

            val right1Angle = getAngleByIndex(vertex, 11, 23, 13)
            val right2Angle = getAngleByIndex(vertex, 13, 15, 11)

            Log.e("poseLogic", "poseHandWu\n" +
                    "Left1 abs($left2Angle) < 30 && 80 < $left1Angle < 160 \n" +
                    "Right1 abs($right1Angle) < 30 && 120 > $right2Angle > 60\n" +

                    "")

            return (left1Angle > 90 && left1Angle < 160 && abs(left2Angle) < 30)
                    && (abs(right1Angle) < 50 && right2Angle > 50 && right2Angle < 160)
        }


        private fun vertexPointF(vertex:FloatArray, index:Int): PointF {
            return PointF(
                vertex[3 * index], vertex[3 * index + 1]
            )
        }




        fun playMatchFace(faceVertex: FloatArray?, faceTile: GameObject?, isFlip: Boolean): Boolean {
            if(faceVertex == null || faceTile == null) {
                return false
            }
            val faceAngle = getFaceAngle(faceVertex, isFlip)

            when(faceTile!!.id) {
                "facedance/face/face_closelefteye.png" -> {
                    // face straight and left eye close
                    return mouthOpen(faceAngle) && leftEyeClose(faceAngle)
                }
                "facedance/face/face_closerighteye.png" -> {
                    // face straight and right eye close
                    return mouthOpen(faceAngle) && rightEyeClose(faceAngle)
                }

                "facedance/face/face_up.png" -> {
                    // face up
                    return faceUp(faceAngle)
                }
                "facedance/face/face_down.png" -> {
                    // face down
                    return faceDown(faceAngle)
                }
                "facedance/face/face_left.png" -> {
                    // face left
                    return faceLeft(faceAngle)
                }
                "facedance/face/face_right.png" -> {
                    // face right
                    return faceRight(faceAngle)
                }

                "facedance/face/face_mouthtoleft.png" -> {
                    // face left and mouth open --> open both up/low lip
                    return faceLeft(faceAngle)
                            && mouthOpen(faceAngle)
                }
                "facedance/face/face_mouthtoleft1.png" -> {
                    // face left and face up and mouth open --> open both up/low lip
                    return faceLeft(faceAngle)
                            && faceUp(faceAngle)
                            && mouthOpen(faceAngle)
                }
                "facedance/face/face_mouthtoright.png" -> {
                    // face right and mouth open --> open both up/low lip
                    return faceRight(faceAngle)
                            && mouthOpen(faceAngle)
                }
                "facedance/face/face_mouthtoright1.png" -> {
                    // face right and face up and mouth open --> open both up/low lip
                    return faceRight(faceAngle)
                            && mouthOpen(faceAngle)
                            && faceUp(faceAngle)
                }
                "facedance/face/face_openmouth.png" -> {
                    // face straight and mouth open --> open both up/low lip
                    return mouthOpen(faceAngle)
                }

                "facedance/face/face_sad.png" -> {
                    // face straight and down side both lip - sad mouth
                    return inLowLipUp1(faceAngle)
                            && inUpLipUp1(faceAngle)
                            && !faceUp(faceAngle)
                }
                "facedance/face/face_smile1.png" -> {
                    // face straight and up side both lip - light smile
                    return inLowLipDown1(faceAngle) &&
                            mouthClose(faceAngle)
                }
                "facedance/face/face_smile2.png" -> {
                    // face straight and width up side low lip - width smile
                    return inUpLipHoriz(faceAngle)
                            && mouthHalfOpen(faceAngle)
                }

            }

            return false
        }

        fun mouthOpen(faceAngle: FloatArray):Boolean {
            return faceAngle[6] > 0.6f
        }

        fun mouthHalfOpen(faceAngle: FloatArray):Boolean {
            return faceAngle[6] > 0.4f
        }

        fun mouthClose(faceAngle: FloatArray):Boolean {
            return faceAngle[6] < 0.1f
        }

        fun leftEyeClose(faceAngle: FloatArray): Boolean {
            return (faceAngle[4] < 0.3f)
        }

        fun rightEyeClose(faceAngle: FloatArray): Boolean {
            return (faceAngle[5] < 0.3f)
        }

        fun faceStraight(faceAngle: FloatArray): Boolean {
            return (abs(faceAngle[0]) < 6f)
                    && (abs(faceAngle[1]) < 20f)
                    && (faceAngle[2] < 170f && faceAngle[2] > 120f)
                    && (faceAngle[3] < 1.2f && faceAngle[3] > 0.5)
        }

        fun faceLeft(faceAngle: FloatArray): Boolean {
            return (faceAngle[1] > 40f)
        }

        fun faceRight(faceAngle: FloatArray): Boolean {
            return (faceAngle[1] < -40f)
        }

        fun faceUp(faceAngle: FloatArray): Boolean {
            return (faceAngle[2] > 163f) // nose-yz angle
            //&& (faceAngle[3] > 1.35f) // nose-ratio
        }

        fun faceDown(faceAngle: FloatArray): Boolean {
            return (faceAngle[2] < 50f) // nose-yz angle
                    && (faceAngle[3] < 0.3f) // nose-ratio
        }

        fun inLowLipUp(faceAngle: FloatArray):Boolean {
            return (faceAngle[7] > 0f)
        }

        fun inLowLipUp1(faceAngle: FloatArray):Boolean {
            return (faceAngle[7] > 0.4f)
        }


        fun inLowLipDown1(faceAngle: FloatArray):Boolean {
            return (faceAngle[7] < 0f)
        }

        fun inLowLipDown2(faceAngle: FloatArray):Boolean {
            return (faceAngle[7] < -1f)
        }

        fun inLowLipDown3(faceAngle: FloatArray):Boolean {
            return (faceAngle[7] < -2f)
        }


        fun inUpLipHoriz(faceAngle: FloatArray):Boolean {
            return (abs(faceAngle[9]) < 0.5f)
        }


        fun inUpLipDown(faceAngle: FloatArray):Boolean {
            return (faceAngle[9] < 0f)
        }

        fun inUpLipUp1(faceAngle: FloatArray):Boolean {
            return (faceAngle[9] > 0f)
        }

        fun inUpLipUp2(faceAngle: FloatArray):Boolean {
            return (faceAngle[9] > 1f)
        }




        fun confirmFace(faceVertex:FloatArray?, isFlip: Boolean): Boolean {
            return if(faceVertex == null) {
                false
            } else {
                val faceAngle = getFaceAngle(faceVertex!!, isFlip)
                val debug = "Eye-xy:: ${faceAngle[0]} \n" +
                        "Eye-xz:: ${faceAngle[1]} \n" +
                        "Nose-yz:: ${faceAngle[2]} \n" +
                        "Nose-ratio:: ${faceAngle[3]}"

                val result = (abs(faceAngle[0]) < 6f)
                        && (abs(faceAngle[1]) < 20f)
                        && (faceAngle[2] < 170f && faceAngle[2] > 120f)
                        && (faceAngle[3] < 1.2f && faceAngle[3] > 0.5)
                        && faceInMid(faceVertex!!)
                result
            }
        }

        fun faceInMid(faceVertex: FloatArray): Boolean {
            val index = 1
            val x = faceVertex[3*index]
            val y = faceVertex[3*index + 1]

            return (x>0.3f && x<0.7f) && (y>0.3f && y<0.7f)
        }

        fun getFlipPoint(i:Int, detectVtx: FloatArray, isFlip:Boolean): PointF {
            return PointF(
                    if(!isFlip) detectVtx!![3*i]
                    else 1f - detectVtx!![3*i],
                    detectVtx!![3*i+1]
            )
        }

        fun getFlipVertex(detectVtx: FloatArray, isFlip:Boolean): FloatArray {
            var inputVertex = FloatArray(detectVtx!!.size)

            for(i in 0 until (detectVtx!!.size / 3)) {
                inputVertex[3*i] =
                    if(!isFlip) detectVtx!![3*i]
                    else 1f - detectVtx!![3*i]
                inputVertex[3*i+1] = detectVtx!![3*i+1]
                inputVertex[3*i+2] = detectVtx!![3*i+2]
            }
            return inputVertex
        }

        fun getFlipPoseVertex(detectVtx: FloatArray, isFlip:Boolean): FloatArray {
            // 1. Flip coordinate value
            var inputVertex = getFlipVertex(detectVtx, isFlip)
            // 2. Flip left-right index
            val leftRight = intArrayOf(
                10,9, 8,7, 6,3, 5,2, 4,1,
                12,11, 14,13, 16,15, 18,17, 20,19, 22,21,
                23,24, 25,26, 27,28, 29,30, 31,32
            )

            // Swap point not position in array
            for(i in leftRight.indices step 2) {
                val idx1 = leftRight[i]
                val idx2 = leftRight[i+1]
                swapPointVertex(inputVertex, idx1, idx2)
//                for(i in 0..2) {
//                    val tmp = inputVertex[3 * idx1 + i]
//                    inputVertex[3 * idx1 + i] = inputVertex[3 * idx2 + i]
//                    inputVertex[3 * idx2 + i] = tmp
//                }
            }

            return inputVertex
        }

        fun swapPointVertex(vertex: FloatArray, idx1:Int, idx2:Int) {
            for(i in 0..2) {
                val tmp = vertex[3 * idx1 + i]
                vertex[3 * idx1 + i] = vertex[3 * idx2 + i]
                vertex[3 * idx2 + i] = tmp
            }
        }


        fun getPoseAngle(vertex: FloatArray, isFlip:Boolean): FloatArray {
            val flipVertex = getFlipVertex(vertex, isFlip)

            val nose = 0
            val leftWrist = 15
            val rightWrist = 16

            val leftAngle = getAngle(
                PointF(flipVertex[3 * nose], flipVertex[3 * nose + 1]),
                PointF(flipVertex[3 * leftWrist], flipVertex[3 * leftWrist + 1])
            )

            val rightAngle = getAngle(
                PointF(flipVertex[3 * nose], flipVertex[3 * nose + 1]),
                PointF(flipVertex[3 * rightWrist], flipVertex[3 * rightWrist + 1])
            )

            var angle = leftAngle - rightAngle

            return (
                    floatArrayOf(
                        angle
                    )
                    )
        }


        fun getEyebrowAngle(vertex: FloatArray, isFlip:Boolean): FloatArray {
            val faceVertex = getFlipVertex(vertex, isFlip)

            val leftEye = 105
            val rightEye = 334
            val eyeXYAngle = getAngle(
                PointF(faceVertex[3*leftEye], faceVertex[3*leftEye + 1]),
                PointF(faceVertex[3*rightEye], faceVertex[3*rightEye + 1])
            )
            val eyeXZAngle = getAngle(
                PointF(faceVertex[3*leftEye], faceVertex[3*leftEye + 2]),
                PointF(faceVertex[3*rightEye], faceVertex[3*rightEye + 2])
            )

            val foreHead = 151
            val nose = 164
            val noseYZAngle = getAngle(
                PointF(faceVertex[3 * foreHead + 2], faceVertex[3 * foreHead + 1]),
                PointF(faceVertex[3 * nose + 2], faceVertex[3 * nose + 1])
            )

            val nose1 = 1
            val nose94 = 94
            val nose4 = 4
            val nose5 = 5

            val dis1To94 = distance(
                PointF(faceVertex[3 * nose1], faceVertex[3 * nose1 + 1]),
                PointF(faceVertex[3 * nose94], faceVertex[3 * nose94 + 1]),
            )

            val dis4To5 = distance(
                PointF(faceVertex[3 * nose4], faceVertex[3 * nose4 + 1]),
                PointF(faceVertex[3 * nose5], faceVertex[3 * nose5 + 1]),
            )

            val noseRatio = dis1To94 / dis4To5


            // right eye close or open
            val dist159_145 = dist3ByIndex(faceVertex, 159, 145)
            val dist160_153 = dist3ByIndex(faceVertex, 160, 153)
            val rightEyeRatio = dist159_145 / dist160_153

            // left eye close or open
            val dist386_374 = dist3ByIndex(faceVertex, 386, 374)
            val dist380_387 = dist3ByIndex(faceVertex, 380, 387)
            val leftEyeRatio = dist386_374 / dist380_387


            // Mouth simple open / close
            val dist13_14 = dist3ByIndex(faceVertex, 13, 14)
            val dist82_13 = dist3ByIndex(faceVertex, 82, 317)
            val mouthRatio = dist13_14 / dist82_13

            // Mouth shape
            // Shape-type
            // Line
            // Curve
            // - Up curve
            // - Down curve
            val inLowLip = intArrayOf(
                78, 95, 88, 178, 87,
                14,
                317, 402, 318, 324, 308
            )
            val inLow = checkCurve(faceVertex, inLowLip)
            val inUpLip = intArrayOf(
                78, 191, 80, 81, 82,
                13,
                312, 311, 310, 415, 308
            )
            val inUp = checkCurve(faceVertex, inUpLip)
            val outLowLip = intArrayOf(
                61, 146, 91, 181, 84,
                17,
                314, 405, 321, 375, 291
            )
            val outLow = checkCurve(faceVertex, outLowLip)
            val outUpLip = intArrayOf(
                61, 185, 40, 39, 37,
                0,
                267, 269, 270, 409, 291
            )
            val outUp = checkCurve(faceVertex, outUpLip)


            // right eye close or open
            val dist221_190 = dist3ByIndex(faceVertex, 221, 190)
            val dist56_189 = dist3ByIndex(faceVertex, 56, 189)
            val rightEyeBrow = dist221_190 / dist56_189

            // left eye close or open
            val dist441_414 = dist3ByIndex(faceVertex, 386, 374)
            val dist413_286 = dist3ByIndex(faceVertex, 380, 387)
            val leftEyeBrow = dist441_414 / dist413_286


            return (
                    floatArrayOf(
                        eyeXYAngle, eyeXZAngle, noseYZAngle, noseRatio,
                        leftEyeRatio, rightEyeRatio,
                        mouthRatio,
                    )
                    + inLow + inUp + outLow + outUp
                    + floatArrayOf(leftEyeBrow, rightEyeBrow)
                    )
        }





        fun getFaceAngle(vertex: FloatArray, isFlip:Boolean): FloatArray {
            // @Todo: only check for front-camera
            // Right/Left here for the side of person in-front of camera
            //
            val faceVertex = vertex //getFlipVertex(vertex, isFlip)

            val leftEye = 105
            val rightEye = 334
            val eyeXYAngle = getAngle(
                PointF(faceVertex[3*leftEye], faceVertex[3*leftEye + 1]),
                PointF(faceVertex[3*rightEye], faceVertex[3*rightEye + 1])
            )
            val eyeXZAngle = getAngle(
                PointF(faceVertex[3*leftEye], faceVertex[3*leftEye + 2]),
                PointF(faceVertex[3*rightEye], faceVertex[3*rightEye + 2])
            )

            val foreHead = 151
            val nose = 164
            val noseYZAngle = getAngle(
                PointF(faceVertex[3 * foreHead + 2], faceVertex[3 * foreHead + 1]),
                PointF(faceVertex[3 * nose + 2], faceVertex[3 * nose + 1])
            )

            val nose1 = 1
            val nose94 = 94
            val nose4 = 4
            val nose5 = 5

            val dis1To94 = distance(
                PointF(faceVertex[3 * nose1], faceVertex[3 * nose1 + 1]),
                PointF(faceVertex[3 * nose94], faceVertex[3 * nose94 + 1]),
            )

            val dis4To5 = distance(
                PointF(faceVertex[3 * nose4], faceVertex[3 * nose4 + 1]),
                PointF(faceVertex[3 * nose5], faceVertex[3 * nose5 + 1]),
            )

            val noseRatio = dis1To94 / dis4To5


            // right eye close or open
            val dist159_145 = dist3ByIndex(faceVertex, 159, 145)
            val dist160_153 = dist3ByIndex(faceVertex, 160, 153)
            val rightEyeRatio = dist159_145 / dist160_153

            // left eye close or open
            val dist386_374 = dist3ByIndex(faceVertex, 386, 374)
            val dist380_387 = dist3ByIndex(faceVertex, 380, 387)
            val leftEyeRatio = dist386_374 / dist380_387


            // Mouth simple open / close
            val dist13_14 = dist3ByIndex(faceVertex, 13, 14)
            val dist82_13 = dist3ByIndex(faceVertex, 82, 317)
            val mouthRatio = dist13_14 / dist82_13

            // Mouth shape
            // Shape-type
            // Line
            // Curve
            // - Up curve
            // - Down curve
            val inLowLip = intArrayOf(
                78, 95, 88, 178, 87,
                14,
                317, 402, 318, 324, 308
            )
            val inLow = checkCurve(faceVertex, inLowLip)
            val inUpLip = intArrayOf(
                78, 191, 80, 81, 82,
                13,
                312, 311, 310, 415, 308
            )
            val inUp = checkCurve(faceVertex, inUpLip)
            val outLowLip = intArrayOf(
                61, 146, 91, 181, 84,
                17,
                314, 405, 321, 375, 291
            )
            val outLow = checkCurve(faceVertex, outLowLip)
            val outUpLip = intArrayOf(
                61, 185, 40, 39, 37,
                0,
                267, 269, 270, 409, 291
            )
            val outUp = checkCurve(faceVertex, outUpLip)


            return floatArrayOf(
                eyeXYAngle, eyeXZAngle, noseYZAngle, noseRatio,
                leftEyeRatio, rightEyeRatio,
                mouthRatio,
            ) + inLow + inUp + outLow + outUp
        }

        fun checkCurve(faceVertex: FloatArray, indices: IntArray): FloatArray {
            val points = java.util.ArrayList<PointF3D>()
            for(i in indices.indices) {
                points.add(
                    PointF3D(faceVertex[3 * indices[i]],
                        faceVertex[3 * indices[i] + 1],
                        faceVertex[3 * indices[i] + 2])
                )
            }

            val center = (points[0] + points[points.size-1]) / 2f
            val mid = points[points.size / 2]
            val next = points[points.size/2 - 1]
            val mid2Center = distance2(mid, center)
            val mid2Next = distance2(mid, next)
            val ratio = mid2Center / mid2Next

            val yComp = (center.y - mid.y) / mid2Next

            return floatArrayOf(yComp, ratio)
        }

        fun getCenter(points: java.util.ArrayList<PointF3D>): PointF3D {
            var x = 0f
            var y = 0f
            var z = 0f
            for(p in points) {
                x += p.x
                y += p.y
                z += p.z
            }

            if(points.size > 0) {
                x /= points.size
                y /= points.size
                z /= points.size
            }
            return PointF3D(x, y, z)
        }

//        fun mouthPoint(faceVertex: FloatArray):Array<PointF3D> {
//
//        }

        fun facePoint(faceVertex: FloatArray, index:Int): PointF3D {
            return PointF3D(
                faceVertex[3 * index],
                faceVertex[3 * index + 1],
                faceVertex[3 * index + 2])
        }

        fun facePoint(faceSlide:Int, faceVertex: FloatArray, index:Int): PointF3D {
            return PointF3D(
                faceVertex[faceSlide + 3 * index],
                faceVertex[faceSlide + 3 * index + 1],
                faceVertex[faceSlide + 3 * index + 2])
        }

        fun facePointF(faceSlide:Int, faceVertex: FloatArray, index:Int): PointF {
            return PointF(
                faceVertex[faceSlide + 3 * index],
                faceVertex[faceSlide + 3 * index + 1])
        }

        fun facePointF(faceVertex: FloatArray, index:Int): PointF {
            return PointF(
                faceVertex[3 * index],
                faceVertex[3 * index + 1])
        }

        fun validIndex(faceVertex: FloatArray, index:Int): Boolean {
            if(index < faceVertex.size && index >= 0) {
                return true
            }
            return false
        }

        fun distByIndex(faceVertex: FloatArray, start:Int, end:Int): Float {
            if(validIndex(faceVertex, start) && validIndex(faceVertex, end)) {
                return distance(
                    PointF(faceVertex[3 * start], faceVertex[3 * start + 1]),
                    PointF(faceVertex[3 * end], faceVertex[3 * end + 1]),
                )
            }
            return 0f
        }

        fun dist3ByIndex(faceVertex: FloatArray, start:Int, end:Int): Float {
            if(validIndex(faceVertex, start) && validIndex(faceVertex, end)) {
                return distance3(
                    PointF3D(faceVertex[3 * start], faceVertex[3 * start + 1], faceVertex[3 * start + 2]),
                    PointF3D(faceVertex[3 * end], faceVertex[3 * end + 1], faceVertex[3 * start + 2]),
                )
            }
            return 0f
        }

        fun distance3(x1: PointF3D, x2: PointF3D): Float {
            return Math.pow(
                Math.pow((x1.x - x2.x).toDouble(), 2.0)
                        + Math.pow((x1.y - x2.y).toDouble(), 2.0)
                        + Math.pow((x1.z - x2.z).toDouble(), 2.0)
                , 0.5
            ).toFloat()
        }

        fun distance2(x1: PointF3D, x2: PointF3D): Float {
            return Math.pow(
                Math.pow((x1.x - x2.x).toDouble(), 2.0)
                        + Math.pow((x1.y - x2.y).toDouble(), 2.0)
                , 0.5
            ).toFloat()
        }

        fun distance(x1: PointF, x2: PointF): Float {
            return Math.pow(
                Math.pow(
                    (x1.x - x2.x).toDouble(),
                    2.0
                ) + Math.pow((x1.y - x2.y).toDouble(), 2.0), 0.5
            ).toFloat()
        }

        fun getAngle(
            source: PointF,
            target: PointF
        ): Float {
            return Math.toDegrees(
                Math.atan2(
                    (target.y - source.y).toDouble(),
                    (target.x - source.x).toDouble()
                )
            ).toFloat()
        }

        fun getAngle(
            source: PointF,
            target1: PointF,
            target2: PointF,
        ): Float {
            val a1 = getAngle(source, target1)
            val a2 = getAngle(source, target2)
            var result = a1 - a2
            result =
                if(result < -180)
                    result+360
                else if(result > 360)
                    result-360
                else
                    result
            // Invert the sign because the clock in image coordinate rotate by clock by cause of y axis

            Log.e("poseLogic", "------ Angle -----" +
                    "source:: $source target1:: $target1 target2:: $target2" +
                    "angle1:: $a1 angle2:: $a2 result:: $result" +
                    "")
            return result
        }

        fun getAngleByIndex(
            vertex: FloatArray, source:Int, target:Int
        ):Float {
            return getAngle(
                PointF(vertex[3 * source], vertex[3 * source + 1]),
                PointF(vertex[3 * target], vertex[3 * target + 1]),
            )
        }

        fun getAngleByIndex(
            faceSlide: Int, vertex: FloatArray, source:Int, target:Int
        ):Float {
            return getAngle(
                PointF(vertex[faceSlide + 3 * source], vertex[faceSlide + 3 * source + 1]),
                PointF(vertex[faceSlide + 3 * target], vertex[faceSlide + 3 * target + 1]),
            )
        }

        fun getAngleByIndex(
            vertex: FloatArray, source:Int, target1:Int, target2:Int
        ):Float {
            return getAngle(
                PointF(vertex[3 * source], vertex[3 * source + 1]),
                PointF(vertex[3 * target1], vertex[3 * target1 + 1]),
                PointF(vertex[3 * target2], vertex[3 * target2 + 1]),
            )
        }

        fun getAngleByIndex(
            faceSlide: Int, vertex: FloatArray, source:Int, target1:Int, target2:Int
        ):Float {
            return getAngle(
                PointF(vertex[faceSlide + 3 * source], vertex[faceSlide + 3 * source + 1]),
                PointF(vertex[faceSlide + 3 * target1], vertex[faceSlide + 3 * target1 + 1]),
                PointF(vertex[faceSlide + 3 * target2], vertex[faceSlide + 3 * target2 + 1]),
            )
        }



        fun translateRectangle(
            rectPoints: Array<DoubleArray>,
            newCenter: DoubleArray)
        : Array<DoubleArray> {
            val oldCenter = DoubleArray(3) {0.0}
            for (i in 0 until 4) {
                for (j in 0..2) {
                    oldCenter[j] += rectPoints[i][j]
                }
            }
            for (j in 0..2) {
                oldCenter[j] /= 4.0
            }

            val translationVector = DoubleArray(3) {0.0}
            for (j in 0..2) {
                translationVector[j] = newCenter[j] - oldCenter[j]
            }

            val translatedRect = Array(4) { DoubleArray(3) }
            for (i in 0 until 4) {
                for (j in 0..2) {
                    translatedRect[i][j] = rectPoints[i][j] + translationVector[j]
                }
            }

            return translatedRect
        }



        fun rotateRectangle(
            p1: DoubleArray, p2: DoubleArray, p3: DoubleArray, p4: DoubleArray,
            axisPoints1: Pair<DoubleArray, DoubleArray>,
            axisPoints2: Pair<DoubleArray, DoubleArray>
        ): Array<DoubleArray> {
            val rectPoints = arrayOf(
                p1,p2,p3,p4
            )

//            val axisPoints1 = doubleArrayOf(0.0, 0.0, 0.0) to doubleArrayOf(1.0, 0.0, 0.0)
//            val axisPoints2 = doubleArrayOf(0.0, 0.0, 0.0) to doubleArrayOf(0.0, 1.0, 0.0)

            val xaxis = subtract(axisPoints1.second, axisPoints1.first)
            val yaxis = subtract(axisPoints2.second, axisPoints2.first)
            val zaxis = crossProduct(xaxis, yaxis)

            val rotMatrix = arrayOf(
                xaxis,
                yaxis,
                zaxis
            )

            val rotatedRect = Array(4) { doubleArrayOf(0.0, 0.0, 0.0) }
            for (i in 0 until 4) {
                for (j in 0..2) {
                    for (k in 0..2) {
                        rotatedRect[i][j] += rectPoints[i][k] * rotMatrix[k][j]
                    }
                }
            }

            return rotatedRect
        }

        fun subtract(a: DoubleArray, b: DoubleArray): DoubleArray {
            return doubleArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])
        }

        fun crossProduct(a: DoubleArray, b: DoubleArray): DoubleArray {
            return doubleArrayOf(
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
            )
        }



        fun transformOverlay(
            bottomImagePoints: List<PointF>,
            overlayImagePoints: List<PointF>
        ): List<PointF> {
            // Extract points from input lists
            val bottomImagePoint1 = bottomImagePoints[0]
            val bottomImagePoint2 = bottomImagePoints[1]
            val overlayImagePoint1 = overlayImagePoints[0]
            val overlayImagePoint2 = overlayImagePoints[1]

            // Calculate translation vector
            val translationX = bottomImagePoint1.x - overlayImagePoint1.x
            val translationY = bottomImagePoint1.y - overlayImagePoint1.y

            // Calculate scaling factor
            val bottomImageDistance = distance(bottomImagePoint1, bottomImagePoint2)
            val overlayImageDistance = distance(overlayImagePoint1, overlayImagePoint2)
            val scale = bottomImageDistance / overlayImageDistance

            // Calculate rotation angle
            val bottomImageAngle = angle(bottomImagePoint1, bottomImagePoint2)
            val overlayImageAngle = angle(overlayImagePoint1, overlayImagePoint2)
            val rotation = bottomImageAngle - overlayImageAngle

            // Create transformation matrix
            val matrix = Matrix().apply {
                postScale(scale, scale)
                postRotate(rotation)
                postTranslate(translationX, translationY)
            }


            val after = listOf(
                transform(overlayImagePoint1, matrix),
                transform(overlayImagePoint2, matrix),
            )

            Log.e("2dSticker", "after:: ${after.joinToString(" ")}")


            return listOf(
                transform(PointF(0f,0f), matrix),
                transform(PointF(1f,0f), matrix),
                transform(PointF(0f,1f), matrix),
                transform(PointF(1f,1f), matrix),

                )
        }

        fun transform(p: PointF, matrix: Matrix): PointF {
            val r = applyTransform(matrix, p.x, p.y)
            return PointF(r.first, r.second)
        }

        fun applyTransform(matrix: Matrix, x: Float, y: Float): Pair<Float, Float> {
            val points = floatArrayOf(x, y)
            matrix.mapPoints(points)
            return Pair(points[0], points[1])
        }

        fun angle(point1: PointF, point2: PointF): Float {
            val dx = point2.x - point1.x
            val dy = point2.y - point1.y

            return Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        }



        fun randomTriangle(rect: Pair<PointF, PointF>): Pair<PointF, PointF> {
            val topLeft: PointF = rect.first
            val bottomRight: PointF = rect.second
            val rand = java.util.Random()
            val b = PointF(
                rand.nextFloat() * (bottomRight.x - topLeft.x) + topLeft.x,
                rand.nextFloat() * (bottomRight.y - topLeft.y) + topLeft.y)

            val c = PointF(
                rand.nextFloat() * (bottomRight.x - topLeft.x) + topLeft.x,
                rand.nextFloat() * (bottomRight.y - topLeft.y) + topLeft.y)

            return Pair(b, c)
        }


        fun random4Triangle(root:PointF): List<Pair<PointF, PointF>> {
            val rects = listOf<Pair<PointF, PointF>>(
                Pair(PointF(0f, 0f), root),
                Pair(root, PointF(1f, 1f))
            )

            var result = listOf<Pair<PointF, PointF>>()
            for(rect in rects) {
                val r = randomTriangle(rect)
                result += listOf<Pair<PointF, PointF>>(r)
            }

            return result

        }

        fun tweak(x:Float, rand: java.util.Random, percent:Float): Float {
            return (x * (1f + percent*2f*(rand.nextFloat())-0.5f))
        }


        /**
         * Copilot code
         */
        fun readFileFromAssetOfModuleInProject(context: Context, fileName: String): String {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            return String(buffer, Charsets.UTF_8)
        }

        /**
         * ChatGPT code
         */
        fun readAssetFileFromModule(context: Context, module: String, filename: String): String? {
            return FaceShader.getShaderFromAssets(context, "$module/$filename")
        }

        /**
         * Find the intersection between 2 line define by (x0,y0,x1,y1) and (x2,y2,x3,y3)
         */
        fun lineIntersection(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): PointF? {
            val a1 = y1 - y0
            val b1 = x0 - x1
            val c1 = a1 * x0 + b1 * y0
            val a2 = y3 - y2
            val b2 = x2 - x3
            val c2 = a2 * x2 + b2 * y2
            val determinant = a1 * b2 - a2 * b1
            return if (determinant == 0f) {
                null
            } else {
                val x = (b2 * c1 - b1 * c2) / determinant
                val y = (a1 * c2 - a2 * c1) / determinant
                PointF(x, y)
            }
        }

        /**
         * Find the intersection between line define by (PointF,PointF) and segment define by (PointF,PointF)
         * if the intersection is not on the segment, return null
         */
        fun lineSegmentIntersection(p1: PointF, p2: PointF, p3: PointF, p4: PointF): PointF? {
            val intersection = lineIntersection(p1, p2, p3, p4)
            if (intersection != null) {
                val x = intersection.x
                val y = intersection.y
                if (x in Math.min(p3.x, p4.x) .. Math.max(p3.x, p4.x)
                && y in Math.min(p3.y, p4.y) .. Math.max(p3.y, p4.y)) {
                    return intersection
                }
            }
            return null
        }

        /**
         * Find the intersection between 2 line define by (PointF,PointF) and (PointF,PointF)
         */
        fun lineIntersection(p1: PointF, p2: PointF, p3: PointF, p4: PointF): PointF? {
            return lineIntersection(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y)
        }

        /**
         * Find intersection between line and rectangle edges
         */
        fun lineRectangleIntersection(p1: PointF, p2: PointF, rect: RectF): List<PointF> {
            val p3 = PointF(rect.left, rect.top)
            val p4 = PointF(rect.right, rect.top)
            val p5 = PointF(rect.right, rect.bottom)
            val p6 = PointF(rect.left, rect.bottom)
            val intersections = mutableListOf<PointF>()
            lineIntersection(p1, p2, p3, p4)?.let { intersections.add(it) }
            lineIntersection(p1, p2, p4, p5)?.let { intersections.add(it) }
            lineIntersection(p1, p2, p5, p6)?.let { intersections.add(it) }
            lineIntersection(p1, p2, p6, p3)?.let { intersections.add(it) }
            return intersections
        }

        fun lineUnitRec(p1: PointF, p2: PointF): List<PointF?> {
            val p3 = PointF(0f, 0f)
            val p4 = PointF(1f, 0f)

            val p5 = PointF(0f, 1f)
            val p6 = PointF(1f, 1f)

            val intersections = mutableListOf<PointF?>()
            lineIntersection(p1, p2, p3, p4).let { intersections.add(it) }
            lineIntersection(p1, p2, p5, p6).let { intersections.add(it) }
            return intersections
        }

        fun lineUnitRec(x1:Float, y1:Float, x2:Float, y2: Float): List<PointF?> {
            val p1 = PointF(x1, y1)
            val p2 = PointF(x2, y2)
            return lineUnitRec(p1, p2)
        }


        /**
         * input point p0 on line (0,0) -> (1,0)
         * input point p1 on line (0,1) -> (1,1)
         * find output point p2 on line (0,0) -> (0,1)
         * such that line(p1,p2) is perpendicular to line (p2, p0)
         *
         */
        fun perpendicularPoint(p0: PointF, p1: PointF): PointF? {
            val x0 = p0.x
            val y0 = p0.y
            val x1 = p1.x
            val y1 = p1.y

            val x2 = (x0 + y0 - y1) / (x1 - x0)
            val y2 = (x0 * y1 - x1 * y0) / (x1 - x0)

            if (x2 in 0f .. 1f && y2 in 0f .. 1f) {
                return PointF(x2, y2)
            }
            return null
        }


        fun findPerpendicularPoint(p1: Pair<Double, Double>, p2: Pair<Double, Double>, o1: Pair<Double, Double>, o2: Pair<Double, Double>): Pair<Double, Double> {
            // Calculate slope and y-intercept of line passing through points p1 and p2
            val slope1 = (p2.second - p1.second) / (p2.first - p1.first)
            val yIntercept1 = p1.second - slope1 * p1.first

            // Calculate slope and y-intercept of line perpendicular to line passing through points p1 and p2
            val slope2 = -1 / slope1
            val yIntercept2 = o1.second - slope2 * o1.first

            // Solve for intersection point
            val p3y = (slope2 * p2.first - yIntercept2) / (1 - slope1 * slope2)
            val p3x = (p3y - p2.second) / slope1 + p2.first

            return Pair(p3x, p3y)
        }

        fun findPerpendicularLine(p1: PointF, p2: PointF): PointF {
            val slope = (p2.y - p1.y) / (p2.x - p1.x)
            val perpSlope = -1 / slope
            val yIntercept = p2.y - perpSlope * p2.x
            return PointF(perpSlope, yIntercept)
        }

        fun findIntersection(m1: Float, b1: Float, m2: Float, b2: Float): PointF? {
            if (m1 == m2) return null // Lines are parallel
            val x = (b2 - b1) / (m1 - m2)
            val y = m1 * x + b1
            return if (x.isFinite() && y.isFinite()) PointF(x, y) else null
        }

        fun findLineEquation(p1: PointF, p2: PointF): PointF {
            val m = (p2.y - p1.y) / (p2.x - p1.x)
            val b = p1.y - m * p1.x
            return PointF(m, b)
        }

        fun findIntersection(p1: PointF, p2: PointF, m: Float, b: Float): PointF? {
            val line = findLineEquation(p1, p2)
            return findIntersection(line.x, line.y, m, b)
        }

        fun findPerpendicularPoint(p1: PointF, p2: PointF, o1: PointF, o2: PointF): PointF? {
            val perpenLine = findPerpendicularLine(p1, p2)
            val intersection = findIntersection(o1, o2, perpenLine.x, perpenLine.y)
            return intersection
        }

        fun findSuitableRect(p1: PointF, p2: PointF): List<PointF>? {
            if(p2.x < p1.x) {
                val p3 = findPerpendicularPoint(
                    p1, p2,
                    PointF(0f, 0f), PointF(0f,1f)
                ) ?: return null

                val p4 = findPerpendicularPoint(p2, p3,
                    PointF(0f, 0f), PointF(1f,0f)
                ) ?: return null

                val p5 = findPerpendicularPoint(p3, p4,
                    p1, p2
                ) ?: return null

                return listOf(p4, p3, p5, p2)
            } else {
                val p3 = findPerpendicularPoint(
                    p2, p1,
                    PointF(0f, 0f), PointF(0f,1f)
                ) ?: return null

                val p4 = findPerpendicularPoint(p1, p3,
                    PointF(0f, 1f), PointF(1f,1f)
                ) ?: return null

                val p5 = findPerpendicularPoint(p3, p4,
                    p1, p2
                ) ?: return null

                return listOf(p3, p4, p1, p5)
            }
        }

        fun findSymmetryPoint(p1: PointF, p2: PointF, p3: PointF): PointF {
            // Find slope and y-intercept of line passing through p1 and p2
            val slope1 = (p2.y - p1.y) / (p2.x - p1.x)
            val yIntercept1 = p1.y - slope1 * p1.x

            // Find slope and y-intercept of line passing through p3 and perpendicular to line p1-p2
            val slope2 = -1 / slope1
            val yIntercept2 = p3.y - slope2 * p3.x

            // Find intersection point of the two lines
            val x = (yIntercept2 - yIntercept1) / (slope1 - slope2)
            val y = slope1 * x + yIntercept1

            // Return the reflection point of p3 through line p1-p2
            return PointF(2 * x - p3.x, 2 * y - p3.y)
        }



    }


}