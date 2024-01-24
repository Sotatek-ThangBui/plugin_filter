package com.plugin.filters.plugin_filters.mlkit.processor

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions
import com.google.mediapipe.solutions.facemesh.FaceMeshResult

class MPFaceMeshDetector(context: Context, maxFace: Int = 2) {
    private var faceMesh: com.google.mediapipe.solutions.facemesh.FaceMesh =
        com.google.mediapipe.solutions.facemesh.FaceMesh(
            context,
            FaceMeshOptions.builder().setStaticImageMode(true).setRefineLandmarks(true)
                .setMaxNumFaces(maxFace).setRunOnGpu(false).build()
        )

    fun process(inputImage: Bitmap?): Task<FaceMeshResult> {
        val t = TaskCompletionSource<FaceMeshResult>()
        faceMesh.setResultListener { t.setResult(it) }

        faceMesh.setErrorListener { message, e -> t.setException(Exception(message, e)) }

        if (inputImage != null) {
            faceMesh.send(inputImage)
        } else {
            t.setException(Exception("Input Image is null"))
        }

        return t.task
    }

    fun close() {
        faceMesh.close()
    }
}