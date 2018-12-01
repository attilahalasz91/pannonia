uniform sampler2D u_texture; //default GL_TEXTURE0, expected by SpriteBatch\n" +
varying vec2 vTexCoord;

uniform sampler2D spectralColorPalette;

vec4 interpolate(vec4 terrainColor) {
    float xn = terrainColor.r + terrainColor.g + terrainColor.b;

    float y1 = 0.0;
    float y2 = 888.0;
    float x1 = 0.0;
    float x2 = 3.0;
    int yn = int( (((xn - x1) / (x2 - x1)) * (y2 - y1) + y1) ) ;

    return texture(spectralColorPalette, vec2(1.0 - yn/y2, 0.0)); //yn/y2 normalize to 0..1
}

void main() {
    vec4 terrainColor = texture(u_texture, vTexCoord);

    gl_FragColor = interpolate(terrainColor);
}

