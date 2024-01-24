package com.plugin.filters.plugin_filters.mlkit.processor.game.effect

import android.graphics.PointF
import jp.co.cyberagent.android.gpuimage.Constants
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class EffectUtils {
    companion object {

        // 1. Initialize vertex and texture-coordinates
        val RIGHT_EYE_INDICES = shortArrayOf(
            // Outer
            359, 255, 339, 254,253, 252, 256, 341, 463,
            414, 286, 258, 257, 259, 260, 467,
            // Inner
            263, 249, 390, 373, 374, 380, 381, 382, 362,
            398, 384, 385, 386, 387, 388, 466
        )

        val RIGHT_EYE_TRIANGLE = shortArrayOf(
            26, 25, 23, 29, 30, 14, 10, 9, 25, 19, 20, 4, 28, 12, 11, 31, 14, 30, 23, 7, 6, 23, 25, 24, 22, 6, 5, 11, 10, 26, 29, 13, 12, 20, 21, 5, 19, 3, 2, 16, 31, 17, 18, 17, 31, 16, 1, 0, 16, 15, 31, 21, 20, 28, 19, 18, 30, 20, 19, 29, 27, 26, 22, 7, 23, 24, 9, 8, 24, 2, 1, 17, 25, 26, 10, 24, 25, 9, 24, 8, 7, 16, 0, 15, 16, 17, 1, 31, 15, 14, 22, 26, 23, 13, 29, 14, 3, 19, 4, 27, 28, 11, 22, 23, 6, 21, 22, 5, 27, 11, 26, 28, 29, 12, 4, 20, 5, 18, 19, 2, 30, 18, 31, 27, 21, 28, 29, 19, 30, 28, 20, 29, 21, 27, 22, 18, 2, 17
        )

        // 1. Initialize vertex and texture-coordinates
        val LEFT_EYE_INDICES = shortArrayOf(
            // Outer
            130, 25, 110, 24, 23, 22, 26, 112, 243,
            190, 56, 28, 27, 29, 30, 247,
            // Inner
            33, 7, 163, 144, 145, 153, 154, 155, 133,
            173, 157, 158, 159, 160, 161, 246
        )

        // 2. Set triangle-index
        val LEFT_EYE_TRIANGLE = shortArrayOf(
            26, 22, 23, 29, 13, 14, 10, 26, 25, 19, 3, 4, 28, 27, 11, 15, 31, 30, 23, 22, 6,
            25, 23, 24, 22, 21, 5, 11, 27, 26, 29, 28, 12, 20, 4, 5, 19, 18, 2, 31, 16, 17,
            18, 30, 31, 1, 17, 16, 15, 0, 16, 21, 27, 28, 19, 29, 30, 20, 28, 29, 27, 21, 22,
            24, 7, 8, 9, 25, 24, 2, 18, 17, 24, 8, 9, 24, 23, 7, 16, 31, 15, 16, 0, 1,
            25, 26, 23, 30, 29, 14, 9, 10, 25, 20, 19, 4, 12, 28, 11, 14, 15, 30, 7, 23, 6,
            6, 22, 5, 10, 11, 26, 13, 29, 12, 21, 20, 5, 3, 19, 2, 17, 18, 31, 20, 21, 28,
            18, 19, 30, 19, 20, 29, 26, 27, 22, 1, 2, 17
        )



        fun getSubVertices(faceVertex: FloatArray, indices: ShortArray): FloatArray {
            val subVertices = FloatArray(indices.size * 3)
            for (i in indices.indices) {
                subVertices[i * 3] = faceVertex[indices[i] * 3]
                subVertices[i * 3 + 1] = faceVertex[indices[i] * 3 + 1]
                subVertices[i * 3 + 2] = faceVertex[indices[i] * 3 + 2]
            }
            return subVertices
        }

        fun getSubMaskVertices(faceVertex: FloatArray, indices: ShortArray): FloatArray {
            val subVertices = FloatArray(indices.size * 2)
            for (i in indices.indices) {
                subVertices[i * 2] = faceVertex[indices[i] * 3]
                subVertices[i * 2 + 1] = faceVertex[indices[i] * 3 + 1]
            }
            return subVertices
        }

        fun getSubMaskUVVertices(faceVertex: FloatArray, indices: ShortArray): FloatArray {
            val subVertices = FloatArray(indices.size * 2)
            for (i in indices.indices) {
                subVertices[i * 2] = faceVertex[indices[i] * 2]
                subVertices[i * 2 + 1] = faceVertex[indices[i] * 2 + 1]
            }
            return subVertices
        }


        fun translateVertex(
            vertex: FloatArray, source:PointF, target:PointF
        ): FloatArray {

            val moveX = target.x - source.x
            val moveY = target.y - source.y

            for (i in vertex.indices step 3) {
                vertex[i] += moveX
                vertex[i + 1] += moveY
            }

            return vertex
        }


        fun translateVertex(
            vertex: FloatArray, target:PointF
        ): FloatArray {
            // 2. Initialize vertex and texture-coordinates
            var center = PointF(0f, 0f)
            for (i in vertex.indices step 3) {
                center.x += vertex[i]
                center.y += vertex[i + 1]
            }
            center.x /= vertex.size / 3
            center.y /= vertex.size / 3

            val moveX = target.x - center.x
            val moveY = target.y - center.y

            for (i in vertex.indices step 3) {
                vertex[i] += moveX
                vertex[i + 1] += moveY
            }

            return vertex
        }


        fun scaleVertex(
            vertex: FloatArray, ratio: Float
        ): FloatArray {
            // 2. Initialize vertex and texture-coordinates
            var center = PointF(0f, 0f)
            for (i in vertex.indices step 3) {
                center.x += vertex[i]
                center.y += vertex[i + 1]
            }
            center.x /= vertex.size / 3
            center.y /= vertex.size / 3


            for (i in vertex.indices step 3) {
                vertex[i] += (center.x - vertex[i]) * (1-ratio)
                vertex[i + 1] += (center.y - vertex[i + 1]) * (1-ratio)
            }

            return vertex
        }

        /**
         * Given root PointF O, rotate point P around O by angle
         */
        fun rotatePoint(root: PointF, p: PointF, angle: Float): PointF {
            val x = (p.x - root.x) * cos(angle) - (p.y - root.y) * sin(angle) + root.x
            val y = (p.x - root.x) * sin(angle) + (p.y - root.y) * cos(angle) + root.y
            return PointF(x, y)
        }


        fun rotateVertex(
            vertex: FloatArray, angle: Float
        ): FloatArray {
            // 2. Initialize vertex and texture-coordinates
            var center = PointF(0f, 0f)
            for (i in vertex.indices step 3) {
                center.x += vertex[i]
                center.y += vertex[i + 1]
            }
            center.x /= vertex.size / 3
            center.y /= vertex.size / 3


            for (i in vertex.indices step 3) {
                val p = rotatePoint(center, PointF(vertex[i], vertex[i+1]), angle)
                vertex[i] = p.x
                vertex[i + 1] = p.y
            }

            return vertex
        }


        fun eyeVtxAlpha(triangleIndices: ShortArray): FloatBuffer {
            val data = FloatArray(triangleIndices.size) { 0.8f }

            for (i in data.indices) {
                if(i < 16) {
                    data[i] = 0f
                }
            }
            val result = ByteBuffer
                .allocateDirect(data.size * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            result.put(data)
            return result
        }
        
        
        



        fun generateCirclePoints(numCircle:Int, numPoint:Int, center: PointF, radius:Float): FloatArray {
            val points = FloatArray(2*(numCircle*numPoint+1))
            var index = 0
            points[index++] = center.x
            points[index++] = center.y
            for (i in 0 until numCircle) {
                val r = radius*(i+1)/numCircle
                for (j in 0 until numPoint) {
                    val angle = 2*Math.PI*j/numPoint
                    val x = center.x + r * cos(angle).toFloat()
                    val y = center.y + r * sin(angle).toFloat()
                    points[index++] = x
                    points[index++] = y
                }
            }
            return points
        }

        fun generateCircleTriangles(numCircle: Int, numPoint: Int): ShortArray {
            val triangles = ShortArray(3*(2*numCircle-1)*numPoint)
            var index = 0

            // Smallest circle triangles
            for (j in 0 until numPoint) {
                val p1 = 1 + j
                val p2 = 1 + (j+1)%numPoint
                triangles[index++] = 0
                triangles[index++] = p1.toShort()
                triangles[index++] = p2.toShort()
            }

            // Pair of next circles triangles
            for (i in 0 until numCircle-1) {
                for (j in 0 until numPoint) {
                    val p1 = 1 + i*numPoint + j
                    val p2 = 1 + (i+1)*numPoint + j
                    val p3 = 1 + i*numPoint + (j+1)%numPoint
                    val p4 = 1 + (i+1)*numPoint + (j+1)%numPoint
                    triangles[index++] = p1.toShort()
                    triangles[index++] = p2.toShort()
                    triangles[index++] = p3.toShort()
                    triangles[index++] = p2.toShort()
                    triangles[index++] = p4.toShort()
                    triangles[index++] = p3.toShort()
                }
            }
            return triangles
        }

        fun warpCirclePoints(numCircle: Int, numPoint: Int, points: FloatArray, center: PointF, radius: Float, step: Float): FloatArray {

            val points = FloatArray(2*(numCircle*numPoint+1))
            var index = 0
            points[index++] = center.x
            points[index++] = center.y
            // Only warp the points of the inner circles - exclude the biggest circle
            for (i in 0 until numCircle) {
                val r = radius*(i+1)/numCircle
                for (j in 0 until numPoint) {
                    val angle = 2*Math.PI*j/numPoint
                    var x = 0f
                    var y = 0f
                    if(i < numCircle - 1) {
                        val percent = 1f - i.toFloat()/numCircle
                        x = center.x + (r + step * percent) * cos(angle).toFloat()
                        y = center.y + (r + step * percent) * sin(angle).toFloat()
                    } else {
                        x = center.x + r * cos(angle).toFloat()
                        y = center.y + r * sin(angle).toFloat()
                    }
                    points[index++] = x
                    points[index++] = y
                }
            }
            return points
        }






        /**
         * input - grid points
         * input - center point
         * input - radius
         * output - warped grid points by moving points inside the circle backward the center
         */
        fun warpGridPoints(points: FloatArray, center: PointF, radius: Float, step:Float): FloatArray {
            val result = FloatArray(points.size)
            val n = points.size / 2
            // Exclude center point - not move it
            for (i in 1 until n) {
                val x = points[2 * i]
                val y = points[2 * i + 1]
                val dx = x - center.x
                val dy = y - center.y
                val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (d < radius) {
                    val angle = Math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
                    val scale = 1.0f - d / radius
                    val x1 = center.x + (d +step*0.2f) * cos(angle)
                    val y1 = center.y + (d +step*0.2f) * sin(angle)
                    result[2 * i] = x1
                    result[2 * i + 1] = y1
                } else {
                    result[2 * i] = x
                    result[2 * i + 1] = y
                }
            }

            return result
        }

        fun generateGridPoints(n:Int): FloatArray {
            val points = FloatArray(2*n*n)
            val step = 1.0f/n
            var index = 0
            for (i in 0 until n) {
                for (j in 0 until n) {
                    points[index++] = i*step
                    points[index++] = j*step
                }
            }
            return points
        }

        fun generateGridTriangles(n:Int) : ShortArray {
            val triangles = ShortArray(6*(n-1)*(n-1))
            var index = 0
            for (i in 0 until n-1) {
                for (j in 0 until n-1) {
                    val p1 = i*n + j
                    val p2 = i*n + j + 1
                    val p3 = (i+1)*n + j
                    val p4 = (i+1)*n + j + 1
                    triangles[index++] = p1.toShort()
                    triangles[index++] = p2.toShort()
                    triangles[index++] = p3.toShort()
                    triangles[index++] = p2.toShort()
                    triangles[index++] = p3.toShort()
                    triangles[index++] = p4.toShort()
                }
            }
            return triangles
        }


        /**
         * Write function to get symetric point of PoinF p by the line defined by point a and b
         */
        private fun symetricPoint(p: PointF, a: PointF, b: PointF): PointF {
            val x1 = a.x
            val y1 = a.y
            val x2 = b.x
            val y2 = b.y
            val x3 = p.x
            val y3 = p.y
            val x4 =
                (x1 * x2 * y3 + x1 * x3 * y2 - x2 * x3 * y1 + x2 * y1 * y3 - x3 * y1 * y2 + y1 * y2 * y3) /
                        (x1 * x2 - x1 * x3 - x2 * x3 + y1 * y2 - y1 * y3 - y2 * y3)
            val y4 =
                (x1 * x2 * y3 + x1 * x3 * y2 - x2 * x3 * y1 + x2 * y1 * y3 - x3 * y1 * y2 + y1 * y2 * y3) /
                        (x1 * x2 - x1 * x3 - x2 * x3 + y1 * y2 - y1 * y3 - y2 * y3)

            return PointF(x4, y4)
        }

        fun reflectPoint(p: PointF, a: PointF, b: PointF): PointF {
            val v = PointF(b.x - a.x, b.y - a.y)
            val w = PointF(p.x - a.x, p.y - a.y)
            val dot = w.x * v.x + w.y * v.y
            val vLen = sqrt(v.x * v.x + v.y * v.y)
            val proj = PointF(dot / (vLen * vLen) * v.x, dot / (vLen * vLen) * v.y)
            val r = PointF(2 * proj.x - w.x, 2 * proj.y - w.y)
            return PointF(a.x + r.x, a.y + r.y)
        }

    }
}