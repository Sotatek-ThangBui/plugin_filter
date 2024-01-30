attribute vec4  a_Vertex;
attribute vec4  a_TexCoord;
attribute float a_vtxalpha;
attribute float a_regionColor;


uniform float u_scaleType;
uniform float u_flip;
uniform float u_flipY;
uniform float u_ratioWidth;
uniform float u_ratioHeight;


uniform highp float u_drawMode;

uniform   mat4  u_PMVMatrix;
varying   vec2  v_texcoord;
varying   vec2  v_texcoord2;
varying   float v_vtxalpha;
varying   float v_regionColor;


float flipX(float isFlip, float x) {
    if(isFlip == -1.0) {
        return 1.0 -x;
    }
    return x;
}


void main(void)
{
    v_texcoord2 = vec2(a_TexCoord.x, a_TexCoord.y);
    v_texcoord = vec2(a_Vertex.x, a_Vertex.y);
//    gl_Position = vec4(
//            u_flip * (2.0*(a_Vertex.x) - 1.0),
//            (1.0 - 2.0*(a_Vertex.y)),
//            a_Vertex.z, a_Vertex.w
//        );

//    v_texcoord2 = vec2(a_TexCoord.x, a_TexCoord.y);
    if(u_drawMode == -1.0 ) { //{MaskMode.OVERLAY_TEXT.drawMode}) {
//        v_texcoord2 = vec2(a_TexCoord.x, a_TexCoord.y);
//        v_texcoord = vec2(a_Vertex.x, a_Vertex.y);

        gl_Position = vec4(
                (2.0*(a_Vertex.x) - 1.0),
                (1.0 - 2.0*(a_Vertex.y)),
                a_Vertex.z, a_Vertex.w
            );

    } else {
//        v_texcoord2 = vec2(flipX(u_flip, a_TexCoord.x), a_TexCoord.y);
//        v_texcoord = vec2(flipX(u_flip, a_Vertex.x), a_Vertex.y);
//        v_texcoord = vec2(a_Vertex.x, a_Vertex.y);

        if(u_scaleType == 0.0 ) {
            //{ScaleType.CENTER_FIT.value}) {
            gl_Position = vec4(
            u_flip * (2.0*(a_Vertex.x) - 1.0),
                (1.0 - 2.0*(a_Vertex.y)),
                a_Vertex.z, a_Vertex.w);
        } else if(u_scaleType == 1.0 ) {
            //{ScaleType.CENTER_CROP.value}) {
            gl_Position = vec4(
            u_flip * (2.0*(a_Vertex.x) - 1.0) * u_ratioWidth,
                (1.0 - 2.0*(a_Vertex.y)) * u_ratioHeight,
                a_Vertex.z, a_Vertex.w);
        } else if(u_scaleType == 2.0 ) {
            //{ScaleType.CENTER_INSIDE.value}){
            gl_Position = vec4(
            u_flip * (2.0*(a_Vertex.x) - 1.0) / u_ratioHeight,
                (1.0 - 2.0*(a_Vertex.y)) / u_ratioWidth,
                a_Vertex.z, a_Vertex.w);
        } else {
            gl_Position = vec4(
            u_flip * (2.0*(a_Vertex.x) - 1.0),
                (1.0 - 2.0*(a_Vertex.y)),
                a_Vertex.z, a_Vertex.w);
        }
    }

    if(u_drawMode == 0.0) {
        // If draw BASE_TEXTURE
        gl_Position.y = gl_Position.y * u_flipY;
    }

    v_vtxalpha  = a_vtxalpha;
}