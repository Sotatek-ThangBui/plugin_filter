package jp.co.cyberagent.android.gpuimage.ml

import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class FaceShader {

    companion object {
        /**
         * Read the Shader string from the ASSETS folder
         * @param context
         * @param path      Shader relative path
         * @return
         */
        fun getShaderFromAssets(context: Context, path: String?): String? {
            var inputStream: InputStream? = null
            try {
                inputStream = context.resources.assets.open(path!!)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return getShaderStringFromStream(inputStream)
        }

        /**
         * Read shader characters from the input stream
         * @param inputStream
         * @return
         */
        fun getShaderStringFromStream(inputStream: InputStream?): String? {
            if (inputStream == null) {
                return null
            }
            try {
                val reader = BufferedReader(InputStreamReader(inputStream))
                val builder = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    builder.append(line).append("\n")
                }
                reader.close()
                return builder.toString()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }


        val VERTEX_SHADER = """
            attribute vec4  a_Vertex;
            attribute vec4  a_TexCoord;
            attribute float a_vtxalpha;
            attribute float a_regionColor;
            
            
            uniform float u_scaleType;
            uniform float u_flip;
            uniform float u_ratioWidth;
            uniform float u_ratioHeight;
            
            
            uniform highp float u_drawMode;
            
            uniform   mat4  u_PMVMatrix;
            varying   vec2  v_texcoord;
            varying   vec2  v_texcoord2;
            varying   float v_vtxalpha;
            varying   float v_regionColor;
            
    
            void main(void)
            {
                v_texcoord2 = vec2(a_TexCoord.x, a_TexCoord.y);
                v_texcoord = vec2(a_Vertex.x, a_Vertex.y); 
                
                gl_Position = vec4(
                        u_flip * (2.0*(a_Vertex.x) - 1.0),
                        (1.0 - 2.0*(a_Vertex.y)), 
                        a_Vertex.z, a_Vertex.w);

                if(u_drawMode == ${MaskMode.OVERLAY_TEXT.drawMode}) {
                    gl_Position = vec4(
                            (2.0*(a_Vertex.x) - 1.0),
                            (1.0 - 2.0*(a_Vertex.y)), 
                            a_Vertex.z, a_Vertex.w);
                } else {
                    if(u_scaleType == ${ScaleType.CENTER_FIT.value}) {
                        gl_Position = vec4(
                                    u_flip * (2.0*(a_Vertex.x) - 1.0),
                                    (1.0 - 2.0*(a_Vertex.y)), 
                                    a_Vertex.z, a_Vertex.w);

                    } else if(u_scaleType == ${ScaleType.CENTER_CROP.value}) {
                        gl_Position = vec4(
                                    u_flip * (2.0*(a_Vertex.x) - 1.0) * u_ratioWidth,
                                    (1.0 - 2.0*(a_Vertex.y)) * u_ratioHeight, 
                                    a_Vertex.z, a_Vertex.w);

                    } else if(u_scaleType == ${ScaleType.CENTER_INSIDE.value}){
                        gl_Position = vec4(
                                    u_flip * (2.0*(a_Vertex.x) - 1.0) / u_ratioHeight, // u_ratioWidth, // 
                                    (1.0 - 2.0*(a_Vertex.y)) / u_ratioWidth, // u_ratioHeight, //  
                                    a_Vertex.z, a_Vertex.w);
                    } else {
                        gl_Position = vec4(
                                    (2.0*(a_Vertex.x) - 1.0),
                                    (1.0 - 2.0*(a_Vertex.y)), 
                                    a_Vertex.z, a_Vertex.w);

                    } 
                }

                v_vtxalpha  = a_vtxalpha;
            }
            """

        val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2    v_texcoord;
            varying vec2    v_texcoord2;
            varying float   v_vtxalpha;
            varying float   v_regionColor;
            
            
            uniform sampler2D u_sampler;
            uniform sampler2D u_sampler2;
            uniform highp float u_drawMode;
            uniform vec3 u_skinColor;
            const vec2 EPSILON2 = vec2(0.001, 0.001);
   
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
    
            void main(void)
            {
                // 1. Base texture
                lowp vec4 base = texture2D(u_sampler, v_texcoord);
                // 2. Mask texture
                lowp vec4 color = texture2D(u_sampler2, v_texcoord2);
                
                // u_drawMode = 0
                if(u_drawMode == ${MaskMode.BASE_TEXT.drawMode}) { // Draw base-texture only
                    lowp vec4 base = texture2D(u_sampler, v_texcoord);
                    gl_FragColor = base;
                    
                // u_drawMode = -1
                } else if(u_drawMode == ${MaskMode.OVERLAY_TEXT.drawMode}) { // Draw game-object with v_textcoord2 AT v_textcoord
                    
                    lowp vec4 sprite = texture2D(u_sampler2, v_texcoord2);
                    gl_FragColor = sprite;
                    
                    
                // u_drawMode = 1    
                } else if(u_drawMode == ${MaskMode.TATTOO_CANON.drawMode}) { // Blend tattoo
                    float a = color.a * v_vtxalpha;
                    float ax = 1.0-a;
                    
                    if(v_vtxalpha < 1.0) {
                        gl_FragColor = vec4(base.r*ax + color.r*a, base.g*ax + color.g*a, base.b*ax + color.b*a, 1.0);
                    } else {
                        gl_FragColor = vec4(color.r, color.g, color.b, v_vtxalpha);
                    }
               
                // u_drawMode = 4
                } else if(u_drawMode == ${MaskMode.MASK_HSV.drawMode}) { // HSV color correcting
                    float a = v_vtxalpha;
                    float ax = 1.0-v_vtxalpha;
                    
                    vec3 baseHSV = rgb2hsv(base.rgb);
                    vec3 colorHSV = rgb2hsv(color.rgb);
                    
                    vec3 result = vec3(
                                baseHSV.x, 
                                baseHSV.y, 
                                colorHSV.z
                                );
//                                colorHSV.x, 
//                                colorHSV.y, 
//                                baseHSV.z
//                                );
//                                baseHSV.x*ax + colorHSV.x*a, 
//                                baseHSV.y*ax + colorHSV.y*a, 
//                                baseHSV.z*ax + colorHSV.z*a);
                                
                    result = hsv2rgb(result);
                    color = vec4(result, 1.0);
                    
                    if(v_vtxalpha < 1.0) {
                        gl_FragColor = vec4(base.r*ax + color.r*a, base.g*ax + color.g*a, base.b*ax + color.b*a, 1.0);
                    } else {
                        gl_FragColor = vec4(color.r, color.g, color.b, v_vtxalpha);
                    }

                
                } else if(u_drawMode == 3.0) { // Swap Frame
                    // 1. Mix with skinColor 
                    float a = 0.5;
                    float ax = 0.5;
                    
                    a = v_vtxalpha;
                    ax = 1.0-v_vtxalpha;
                    
                    base = vec4(base.r*ax + u_skinColor.r*a, 
                                base.g*ax + u_skinColor.g*a, 
                                base.b*ax + u_skinColor.b*a, 
                                1.0);
                    
                    
                    // 2. Mix 2 faces
                    gl_FragColor = base;
                    

                // u_drawMode = 5
                } else if(u_drawMode ==  ${MaskMode.MASK.drawMode}) { // Draw mask with alpha - without mixing base
                    gl_FragColor = vec4(color.r, color.g, color.b, v_vtxalpha);
                    
                    
                // u_drawMode = 2
                } else { // Blend swap mask
                    float a = v_vtxalpha;
                    float ax = 1.0-v_vtxalpha;
                    
                    gl_FragColor = vec4(base.r*ax + color.r*a, base.g*ax + color.g*a, base.b*ax + color.b*a, 1.0);
                    
                }
            }
            """


    }
}