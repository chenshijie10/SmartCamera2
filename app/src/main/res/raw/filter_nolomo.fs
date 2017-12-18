/**
 * Copyright (C) 2010,2011 Thundersoft Corporation
 * All rights Reserved
 */
precision mediump float;

varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform sampler2D inputImageTexture2;
uniform float type;
uniform float saturation;
uniform float strength;

const vec2 center = vec2(0.5);
vec4 doVignette (vec4 orig) {
    vec3 rgb=orig.xyz;
    float d=distance(textureCoordinate, center);
    float total = distance(vec2(0.0),center);
    rgb*=smoothstep(0.0,0.5,(total - d)/total);

    return vec4(vec3(rgb),1.0);

}

vec4 doColor (vec4 orig) {
    vec4 r=texture2D (inputImageTexture2,vec2(orig.r,type));
    vec4 g=texture2D (inputImageTexture2,vec2(orig.g,type));
    vec4 b=texture2D (inputImageTexture2,vec2(orig.b,type));
    return vec4 (r.r,g.g,b.b,1.0);
}
vec3 rgb2hsv(vec3 c){
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c){
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec4 changeProperty(vec3 rgbColor, float saturation, float strength){
    vec3 hsv = rgb2hsv(rgbColor);
    hsv.g=clamp(hsv.g*saturation, 0.0, 1.0);
    hsv.b=clamp(hsv.b*strength, 0.0, 1.0);
    return vec4(hsv2rgb(hsv), 1.0);
}
void main() {

    vec4 orig=texture2D(inputImageTexture, textureCoordinate);
    gl_FragColor=changeProperty(doColor(orig).rgb, saturation, strength);

}
