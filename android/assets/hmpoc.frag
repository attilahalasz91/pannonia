#version 130

#ifdef GL_ES
precision highp float;
#endif

uniform float iTime;
uniform vec3 iResolution;
//uniform sampler2D iChannel0;
varying vec4 vColor;
//our camera matrix
uniform mat4 u_projTrans;

void main() {
    gl_FragColor = vColor;
}
