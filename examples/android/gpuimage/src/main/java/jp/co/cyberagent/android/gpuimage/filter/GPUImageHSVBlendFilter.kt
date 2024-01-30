package jp.co.cyberagent.android.gpuimage.filter


class GPUImageHSVBlendFilter : GPUImageTwoInputFilter(HSV_BLEND_FRAGMENT_SHADER) {
    companion object {
        const val HSV_BLEND_FRAGMENT_SHADER = """
            varying highp vec2 textureCoordinate;
            varying highp vec2 textureCoordinate2;
            
            uniform sampler2D inputImageTexture;
            uniform sampler2D inputImageTexture2;
            
            vec3 rgb2hsv(vec3 c) { // from http://lolengine.net/blog/2013/07/27/rgb-to-hsv-in-glsl
                vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
                vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
                vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
                float d = q.x - min(q.w, q.y);
                float e = 1.0e-10;
                return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
            }
           
            vec3 hsv2rgb(vec3 c) { //from https://github.com/hughsk/glsl-hsv2rgb
                vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
                vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
                return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
                //return c.z * normalize(mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y));
            }
            
            void main() {
                mediump vec4 base = texture2D(inputImageTexture, textureCoordinate);
                mediump vec4 overlay = texture2D(inputImageTexture2, textureCoordinate2);
            
                vec3 baseHSV = rgb2hsv(base.rgb);
                vec3 colorHSV = rgb2hsv(overlay.rgb);
                
                vec3 result = vec3(
                                colorHSV.x, 
                                colorHSV.y, 
                                baseHSV.z
                                );
                result = hsv2rgb(result);
                if(overlay.a > 0.0) {
                    gl_FragColor = vec4(result, overlay.a);
                } else {
                    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
                }
            }
            
        """

    }


}
