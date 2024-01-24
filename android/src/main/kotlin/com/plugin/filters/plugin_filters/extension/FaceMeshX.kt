package com.plugin.filters.plugin_filters.extension

import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mlkit.vision.facemesh.FaceMesh
import kotlin.math.abs

fun FaceMesh.toArray(originImageWidth: Int, originImageHeight: Int): FloatArray {
    val keyPoints = this.allPoints
    val faceVtx = FloatArray(keyPoints.size * 3)
    var zMin = Float.MAX_VALUE
    var zMax = Float.MIN_VALUE

    for (point in keyPoints) {
        zMin = zMin.coerceAtMost(point.position.z)
        zMax = zMax.coerceAtLeast(point.position.z)
    }
    for (i in 0 until keyPoints.size) {
        val p = keyPoints[i]
        faceVtx[3 * i + 0] = p.position.x / originImageWidth.toFloat()
        faceVtx[3 * i + 1] = p.position.y / originImageHeight.toFloat()
        if (p.position.z < 0) {
            faceVtx[3 * i + 2] = p.position.z / abs(zMin)
        } else {
            faceVtx[3 * i + 2] = p.position.z / zMax
        }
    }

    return faceVtx
}

fun LandmarkProto.NormalizedLandmarkList.toArray(): FloatArray {
    val keyPoints = this.landmarkList
    val faceVtx = FloatArray(keyPoints.size * 3)
    var zMin = Float.MAX_VALUE
    var zMax = Float.MIN_VALUE

    for (point in keyPoints) {
        zMin = zMin.coerceAtMost(point.z)
        zMax = zMax.coerceAtLeast(point.z)
    }
    for (i in 0 until keyPoints.size) {
        val p = keyPoints[i]
        faceVtx[3 * i + 0] = p.x
        faceVtx[3 * i + 1] = p.y
        if (p.z < 0) {
            faceVtx[3 * i + 2] = p.z / abs(zMin)
        } else {
            faceVtx[3 * i + 2] = p.z / zMax
        }
    }

    return faceVtx
}