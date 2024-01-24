precision mediump float;
varying vec2 v_texcoord;
varying vec2 v_texcoord2;
varying float v_vtxalpha;
varying float v_regionColor;


uniform highp float u_texelWidth;
uniform highp float u_texelHeight;

uniform sampler2D u_sampler;
uniform sampler2D u_sampler2;
uniform highp float u_drawMode;
const highp float u_filter = -1.0;//19.0;
uniform vec3 u_skinColor;
const vec2 EPSILON2 = vec2(0.001, 0.001);

// 1. Saturation
// Values from \"Graphics Shaders: Theory and Practice\" by Bailey and Cunningham
const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);


// 2. Toon
//const highp float intensity;
const highp float threshold = 0.2;
const highp float quantizationLevels = 10.0;
const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);




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

vec4 vibrance(vec4 color) {
    float vibrance = 1.2;
    lowp float average = (color.r + color.g + color.b) / 3.0;
    lowp float mx = max(color.r, max(color.g, color.b));
    lowp float amt = (mx - average) * (-vibrance * 3.0);
    color.rgb = mix(color.rgb, vec3(mx), amt);
    return color;
}

vec4 rise_filter(vec4 color) {
    // Apply Rise filter
    color.rgb = mix(color.rgb, vec3(0.99, 0.87, 0.73), 0.4);
    color.rgb = mix(color.rgb, vec3(0.99, 0.91, 0.76), 0.3);
    color.rgb = mix(color.rgb, vec3(0.93, 0.63, 0.50), 0.2);
    color.rgb = mix(color.rgb, vec3(0.78, 0.33, 0.26), 0.1);

    // Output the final color
    return color;
}

vec4 brannan_filter(vec4 color) {
    // Apply Rise filter
    color.rgb = mix(color.rgb, vec3(0.5, 0.4, 0.2), 0.3);
    color.rgb = mix(color.rgb, vec3(0.9, 0.6, 0.3), 0.3);
    color.r = mix(color.r, color.g, 0.5);
    color.g = mix(color.g, color.b, 0.5);

    // Output the final color
    return color;
}

vec4 inkwell_filter(vec4 color) {
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    color.rgb = vec3(gray);

    // Apply Inkwell filter
    color.r = mix(1.0, color.r, 0.7);
    color.g = mix(1.0, color.g, 0.7);
    color.b = mix(1.0, color.b, 0.6);

    // Output the final color
    return color;
}

vec4 toaster_filter(vec4 color) {
    // Apply Toaster filter
    color.r = mix(color.r, color.g, 0.5);
    color.g = mix(color.g, color.b, 0.5);
    color.b = color.b * 0.5;
    color.r = color.r * 1.5 - 0.2;
    color.g = color.g * 1.5 - 0.2;
    color.b = color.b * 1.5 - 0.2;
    color.rgb = mix(color.rgb, vec3(0.8, 0.725, 0.45), 0.2);
    color.rgb = mix(color.rgb, vec3(0.0, 0.0, 0.0), 0.2);

    // Output the final color
    return color;
}

vec4 sutro_filter(vec4 color) {
    // Apply Sutro filter
    color.rgb = mix(color.rgb, vec3(0.4, 0.32, 0.26), 0.5);
    color.r = color.r * 0.9 + color.g * 0.1;
    color.g = color.g * 0.9 + color.b * 0.1;
    color.b = color.b * 0.9 + color.r * 0.1;
    color.rgb = mix(color.rgb, vec3(0.94, 0.75, 0.45), 0.5);
    color.rgb = mix(color.rgb, vec3(0.0, 0.0, 0.0), 0.2);

    // Output the final color
    return color;
}

vec4 earlybird_filter(vec4 color) {
    // Apply Earlybird filter
    color.r = color.r * 0.9 + color.g * 0.1;
    color.g = color.g * 0.9 + color.b * 0.1;
    color.b = color.b * 0.9 + color.r * 0.1;
    color.rgb = mix(color.rgb, vec3(0.96, 0.91, 0.81), 0.2);
    color.rgb = mix(color.rgb, vec3(0.54, 0.29, 0.14), 0.3);
    color.rgb = mix(color.rgb, vec3(0.47, 0.35, 0.31), 0.4);
    color.rgb = mix(color.rgb, vec3(0.16, 0.16, 0.2), 0.4);

    // Output the final color
    return color;
}

vec4 hudson_filter(vec4 color) {
    // Apply Hudson filter
    color.rgb = mix(color.rgb, vec3(0.6, 0.7, 0.8), 0.2);
    color.r = color.r * 0.9 + color.g * 0.1;
    color.g = color.g * 0.9 + color.b * 0.1;
    color.b = color.b * 0.9 + color.r * 0.1;
    color.rgb = mix(color.rgb, vec3(0.1, 0.1, 0.1), 0.2);

    // Output the final color
    return color;
}

vec4 aden_filter(vec4 color) {
    // Apply Aden filter
    color.r = color.r * 0.8 + color.g * 0.2;
    color.g = color.g * 0.85 + color.b * 0.15;
    color.b = color.b * 0.9 + color.r * 0.1;
    color.rgb = mix(color.rgb, vec3(0.7, 0.9, 1.0), 0.2);
    color.rgb = mix(vec3(1.0), color.rgb, 0.15);

    // Output the final color
    return color;
}

vec4 lofi_filter(vec4 color) {
    // Apply Lo-Fi filter
    color.r = color.r * 1.1 - color.g * 0.1;
    color.g = color.g * 0.9 - color.b * 0.1;
    color.b = color.b * 0.9 - color.r * 0.05;
    color.rgb = mix(color.rgb, vec3(0.6, 0.4, 0.2), 0.4);
    color.rgb = mix(vec3(1.0), color.rgb, 0.4);

    // Output the final color
    return color;
}

vec4 xproii_filter(vec4 color) {
    // Apply X-Pro II filter
    color.r = color.r * 0.98 + color.g * 0.01 + color.b * 0.01;
    color.g = color.r * 0.25 + color.g * 0.75;
    color.b = color.r * 0.25 + color.b * 0.75;
    color.rgb = mix(color.rgb, vec3(0.7, 0.4, 0.1), 0.4);
    color.rgb = mix(vec3(0.95, 0.9, 0.6), color.rgb, 0.3);

    // Output the final color
    return color;
}

vec4 willow_filter(vec4 color) {
    // Apply Willow filter
    color.rgb = mix(color.rgb, vec3(0.46, 0.36, 0.21), 0.3);
    color.rgb = mix(color.rgb, vec3(0.92, 0.9, 0.79), 0.5);
    // Output the final color
    return color;
}

vec4 sierra_filter(vec4 color) {
    // Apply Sierra filter
    color.rgb = mix(color.rgb, vec3(0.35, 0.45, 0.6), 0.4);
    color.rgb = mix(color.rgb, vec3(0.75, 0.85, 1.0), 0.3);
    color.rgb = mix(color.rgb, vec3(0.3, 0.2, 0.1), 0.1);

    // Output the final color
    return color;
}

vec4 nashville_filter(vec4 color) {
    // Apply Nashville filter
    color.r = color.r * 0.4 + color.g * 0.3 + color.b * 0.3;
    color.g = color.r * 0.4 + color.g * 0.4 + color.b * 0.2;
    color.b = color.r * 0.2 + color.g * 0.3 + color.b * 0.5;
    color.rgb = mix(vec3(0.9, 0.6, 0.3), color.rgb, 0.4);
    color.rgb = mix(vec3(1.0), color.rgb, 0.2);

    // Output the final color
    return color;
}

vec4 valencia_filter(vec4 color) {
    // Apply Valencia filter
    color.rgb = vec3(
    color.r * 0.9 + color.g * 0.1,
    color.g * 0.85 + color.b * 0.15,
    color.b * 0.7 + color.r * 0.3
    );
    color.rgb = mix(vec3(0.93, 0.52, 0.47), color.rgb, 0.5);

    // Output the final color
    return color;
}

vec4 lofi2_filter(vec4 color) {
    // Apply Lofi filter
    color.rgb = vec3(
        (color.r + color.g + color.b) / 3.0,
        (color.r + color.g + color.b) / 3.0 + 0.1,
        (color.r + color.g + color.b) / 3.0 - 0.1
        );

    // Output the final color
    return color;
}


vec4 juno_filter(vec4 color) {
    // Apply Juno filter
    color.r = color.r * 0.85 + color.g * 0.05 + color.b * 0.15;
    color.g = color.r * 0.15 + color.g * 0.8 + color.b * 0.05;
    color.b = color.r * 0.05 + color.g * 0.15 + color.b * 0.8;
    color.rgb = mix(vec3(1.0, 0.93, 0.83), color.rgb, 0.5);

    // Output the final color
    return color;
}

vec4 lark_filter(vec4 color) {
    // Apply Lark filter
    color.r = color.r * 0.7 + color.g * 0.2 + color.b * 0.1;
    color.g = color.r * 0.3 + color.g * 0.7 + color.b * 0.1;
    color.b = color.r * 0.1 + color.g * 0.2 + color.b * 0.7;

    // Output the final color
    return color;
}

vec4 gingham_filter(vec4 color) {
    vec3 red = vec3(0.756, 0.325, 0.298);
    vec3 green = vec3(0.486, 0.722, 0.541);
    vec3 blue = vec3(0.392, 0.365, 0.569);
    vec3 mixColor = vec3(0.0);
    float luminance = dot(color.rgb, vec3(0.2125, 0.7154, 0.0721));

    if (luminance < 0.5) {
        mixColor = blue;
    } else {
        mixColor = red;
    }

    color.rgb = mix(mixColor, green, sqrt(luminance));
    return color;
}

vec4 gingham2_filter(vec4 color) {
    // Apply Gingham filter
    vec3 gray = vec3(0.299, 0.587, 0.114);
    vec3 lum = vec3(dot(color.rgb, gray));
    color.rgb = mix(vec3(0.8, 0.8, 0.8), vec3(1.0, 0.9, 0.7), lum);


    // Output the final color
    return color;
}

vec4 clarendon_filter(vec4 color) {
    // Apply Clarendon effect
    color.r = clamp(color.r * 1.2 - 0.1, 0.0, 1.0);
    color.g = clamp(color.g * 1.1 - 0.1, 0.0, 1.0);
    color.b = clamp(color.b * 1.1 - 0.1, 0.0, 1.0);

    // Output the final color
    return color;
}

vec4 hefe_filter(vec4 color) {
    // Apply Rise filter
    color.rgb = mix(color.rgb, vec3(1.0, 0.9, 0.6), 0.5);
    color.rgb = 1.0 - color.rgb;
    color.rgb = mix(color.rgb, vec3(0.9, 0.2, 0.0), 0.6);

    // Output the final color
    return color;
}

vec4 color_filter(vec4 color) {
    if(u_filter == -1.0) {
        // No filter
    } else if(u_filter == 0.0) {
        color = clarendon_filter(color);
    } else if(u_filter == 1.0) {
        color = gingham_filter(color);
    } else if(u_filter == 2.0) {
        color = lark_filter(color);
    } else if(u_filter == 3.0) {
        color = juno_filter(color);
    } else if(u_filter == 4.0) {
        color = lofi2_filter(color);
    } else if(u_filter == 5.0) {
        color = valencia_filter(color);
    } else if(u_filter == 6.0) {
        color = nashville_filter(color);
    } else if(u_filter == 7.0) {
        color = xproii_filter(color);
    } else if(u_filter == 8.0) {
        color = lofi_filter(color);
    } else if(u_filter == 9.0) {
        color = aden_filter(color);
    } else if(u_filter == 10.0) {
        color = hudson_filter(color);
    } else if(u_filter == 11.0) {
        color = earlybird_filter(color);
    } else if(u_filter == 12.0) {
        color = sutro_filter(color);
    } else if(u_filter == 13.0) {
        color = toaster_filter(color);
    } else if(u_filter == 14.0) {
        color = brannan_filter(color);
    } else if(u_filter == 15.0) {
        color = inkwell_filter(color);
    } else if(u_filter == 16.0) {
        color = hefe_filter(color);
    } else if(u_filter == 17.0) {
        color = rise_filter(color);
    } else if(u_filter == 18.0) {
        color = sierra_filter(color);
    } else if(u_filter == 19.0) {
        color = willow_filter(color);
    }

    // Output the final color
    return color;
}

vec4 drawMask(vec4 base, vec4 color) {
    vec4 fragResult;
    float a = v_vtxalpha;
    float ax = 1.0 - v_vtxalpha;
    if (u_drawMode == 0.0) { //${MaskMode.BASE_TEXT.drawMode}) {
        // Draw base-texture only
        // lowp vec4 base = texture2D(u_sampler, v_texcoord);
        fragResult = base;
    } else if (u_drawMode == -1.0) { //${MaskMode.OVERLAY_TEXT.drawMode}) {
        // Draw game-object with v_textcoord2 AT v_textcoord
        // lowp vec4 color = texture2D(u_sampler2, v_texcoord2);
        fragResult = color;
    } else if (u_drawMode == 1.0) { //${MaskMode.TATTOO_CANON.drawMode}) {
        // Draw tattoo with more alpha
        fragResult = vec4(
        color.r, color.g, color.b,
        color.a * v_vtxalpha);
    } else if (u_drawMode == 4.0) { //${MaskMode.MASK_HSV.drawMode}) {
        // HSV color correcting
        vec3 baseHSV = rgb2hsv(base.rgb);
        vec3 colorHSV = rgb2hsv(color.rgb);
        vec3 result = vec3(
        baseHSV.x, // Hue from base
        baseHSV.y, // Saturation from base
        colorHSV.z // Value-Light from color
        );
        result = hsv2rgb(result);
        color = vec4(result, 1.0);
        fragResult = vec4(
        base.r * ax + color.r * a,
        base.g * ax + color.g * a,
        base.b * ax + color.b * a,
        1.0
        );
    } else if (u_drawMode == 3.0) {
        // Exp sync mask color with base color by skinColor
        // 1. Mix with skinColor
        color = vec4(
        color.r * ax + u_skinColor.r * a,
        color.g * ax + u_skinColor.g * a,
        color.b * ax + u_skinColor.b * a,
        1.0);
        // 2. Mix 2 faces
        fragResult = vec4(
        base.r * ax + color.r * a,
        base.g * ax + color.g * a,
        base.b * ax + color.b * a,
        1.0);
    } else if (u_drawMode == 5.0) { //${MaskMode.MASK.drawMode}) {
        // Draw mask with alpha - without mixing base
        fragResult = vec4(color.r, color.g, color.b, v_vtxalpha);
    } else {
        // Mix mask with base by alpha
        fragResult = vec4(
        base.r * ax + color.r * a,
        base.g * ax + color.g * a,
        base.b * ax + color.b * a,
        1.0);
    }

    return fragResult;
}


/**
 * saturation:
 The degree of saturation or desaturation to apply to the image
 (0.0 - 2.0, with 1.0 as the default)
 */
vec4 saturation_func(vec4 textureColor) {
    lowp float luminance = dot(textureColor.rgb, luminanceWeighting);
    lowp vec3 greyScaleColor = vec3(luminance);

    float saturation = 2.0;
    return vec4(mix(greyScaleColor, textureColor.rgb, saturation), textureColor.w);
}

vec4 toon_func() {

    vec2 widthStep = vec2(u_texelWidth, 0.0);
    vec2 heightStep = vec2(0.0, u_texelHeight);
    vec2 widthHeightStep = vec2(u_texelWidth, u_texelHeight);
    vec2 widthNegativeHeightStep = vec2(u_texelWidth, -u_texelHeight);

    vec2 textureCoordinate = v_texcoord;
    vec2 leftTextureCoordinate = v_texcoord - widthStep;
    vec2 rightTextureCoordinate = v_texcoord + widthStep;

    vec2 topTextureCoordinate = v_texcoord - heightStep;
    vec2 topLeftTextureCoordinate = v_texcoord - widthHeightStep;
    vec2 topRightTextureCoordinate = v_texcoord + widthNegativeHeightStep;

    vec2 bottomTextureCoordinate = v_texcoord + heightStep;
    vec2 bottomLeftTextureCoordinate = v_texcoord - widthNegativeHeightStep;
    vec2 bottomRightTextureCoordinate = v_texcoord + widthHeightStep;

    vec4 textureColor = texture2D(u_sampler, textureCoordinate);

    float bottomLeftIntensity = texture2D(u_sampler, bottomLeftTextureCoordinate).r;
    float topRightIntensity = texture2D(u_sampler, topRightTextureCoordinate).r;
    float topLeftIntensity = texture2D(u_sampler, topLeftTextureCoordinate).r;
    float bottomRightIntensity = texture2D(u_sampler, bottomRightTextureCoordinate).r;
    float leftIntensity = texture2D(u_sampler, leftTextureCoordinate).r;
    float rightIntensity = texture2D(u_sampler, rightTextureCoordinate).r;
    float bottomIntensity = texture2D(u_sampler, bottomTextureCoordinate).r;
    float topIntensity = texture2D(u_sampler, topTextureCoordinate).r;
    float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;
    float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;

    float mag = length(vec2(h, v));

    vec3 posterizedImageColor = floor((textureColor.rgb * quantizationLevels) + 0.5) / quantizationLevels;

    float thresholdTest = 1.0 - step(threshold, mag);

    return vec4(posterizedImageColor * thresholdTest, textureColor.a);

}

void main(void) {
    // 1. Base texture
    lowp vec4 base =
        // toon_func();
        texture2D(u_sampler, v_texcoord);
    // 2. Mask texture
    lowp vec4 color = texture2D(u_sampler2, v_texcoord2);

    vec4 maskResult = drawMask(base, color);

    gl_FragColor = maskResult;
}