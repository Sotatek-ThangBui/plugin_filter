package jp.co.cyberagent.android.gpuimage.ml

import jp.co.cyberagent.android.gpuimage.Constants
import java.nio.FloatBuffer

class BeardFilter : FaceSwapFilter() {
    override fun setMode(mode: MaskMode) {
        this.maskMode = mode
        this.faceTriangIndex = Constants.s_face_tris
    }

    override fun drawMask(textureId: Int, cubeBuffer: FloatBuffer, textureBuffer: FloatBuffer) {



    }
}