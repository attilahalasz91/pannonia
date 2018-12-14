#version 130

//our attributes
attribute vec2 a_position;

//hightmap
uniform sampler2D u_texture;
//our camera matrix
uniform mat4 u_projTrans;

//send the color out to the fragment shader
varying vec4 vColor;

void main() {
    vColor = texture(u_texture, a_position / vec2(10) /vec2(100));

    gl_Position = u_projTrans * vec4(a_position.xy, 0.0, 1.0);
}