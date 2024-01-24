package jp.co.cyberagent.android.gpuimage.ml

import jp.co.cyberagent.android.gpuimage.filter.GPUImageTwoInputFilter

class BgOverlayFilter(fragmentShader: String?)
    : GPUImageTwoInputFilter(fragmentShader) {

    companion object {
        const val OVERLAY_BLEND_FRAGMENT_SHADER = """
                varying highp vec2 textureCoordinate;
                varying highp vec2 textureCoordinate2;
                
                uniform sampler2D inputImageTexture;
                uniform sampler2D inputImageTexture2;
                
                void main()
                {
                    mediump vec4 base = texture2D(inputImageTexture, textureCoordinate);
                    mediump vec4 overlay = texture2D(inputImageTexture2, textureCoordinate2);
                    
                    if(overlay.a == 0.0) {
                        gl_FragColor = base;
                    } else {
                        gl_FragColor = overlay;
                    }
                }
                """
    }

    constructor() : this(
        OVERLAY_BLEND_FRAGMENT_SHADER
    ) {

    }

}