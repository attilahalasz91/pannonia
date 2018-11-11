#version 130

//our attributes
attribute vec2 a_position;
attribute float a_color;

//our camera matrix
uniform mat4 u_projTrans;

//send the color out to the fragment shader
varying vec4 vColor;

void main() {
    int value = int(a_color);
    float rValue = ((value & 0x00ff0000) >> 16) / 255.0;
    float gValue = ((value & 0x0000ff00) >> 8) / 255.0;
    float bValue = ((value & 0x000000ff)) / 255.0;
    vColor = vec4(rValue, gValue, bValue, 1.0);

    gl_Position = u_projTrans * vec4(a_position.xy, 0.0, 1.0);
}