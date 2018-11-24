// Simple Water shader. (c) Victor Korsun, bitekas@gmail.com; 2012.
//
// Attribution-ShareAlike CC License.
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
//a_position from vertex shader - varying to interpolate between vertex coords
varying vec2 a_position2;

const float PI = 3.1415926535897932;

// play with these parameters to custimize the effect
// ===================================================

//speed
const float speed = 0.05;//0.1
const float speed_x = 0.1;//0.2
const float speed_y = 0.1;//0.2

// refraction
const float emboss = 0.50;
const float intensity = 2.4;
const int steps = 8;
const float frequency = 6.0;
const int angle = 7; // better when a prime

// reflection
const float delta = 60.;
const float intence = 700.;

const float reflectionCutOff = 0.012;
const float reflectionIntence = 200000.;

// ===================================================

float col(vec2 coord,float time) {
    float delta_theta = 2.0 * PI / float(angle);
    float col = 0.0;
    float theta = 0.0;
    for (int i = 0; i < steps; i++) {
        vec2 adjc = coord;
        theta = delta_theta*float(i);
        adjc.x += cos(theta)*time*speed + time * speed_x;
        adjc.y -= sin(theta)*time*speed - time * speed_y;
        col = col + cos( (adjc.x*cos(theta) - adjc.y*sin(theta))*frequency)*intensity;
    }

    return cos(col);
}

//---------- main

void main() {
    float time = iTime*1.3;

    //vec2 p = (gl_FragCoord.xy) / iResolution.xy, c1 = p, c2 = p;
    vec2 p = (a_position2.xy) / iResolution.xy*200.0, c1 = p, c2 = p;
    float cc1 = col(c1,time);

    c2.x += iResolution.x/delta;
    float dx = emboss*(cc1-col(c2,time))/delta;

    c2.x = p.x;
    c2.y += iResolution.y/delta;
    float dy = emboss*(cc1-col(c2,time))/delta;

    c1.x += dx*2.;
    c1.y = -(c1.y+dy*2.);

    float alpha = 1.+dot(dx,dy)*intence;

    float ddx = dx - reflectionCutOff;
    float ddy = dy - reflectionCutOff;
    if (ddx > 0. && ddy > 0.)
        alpha = pow(alpha, ddx*ddy*reflectionIntence);

    //vec4 col = texture(iChannel0,c1)*(alpha);
    vec4 col = vColor*(alpha);
    gl_FragColor = col;
}
