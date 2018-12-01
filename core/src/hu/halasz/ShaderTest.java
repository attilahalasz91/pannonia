package hu.halasz;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;

import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE0;

public class ShaderTest implements ApplicationListener {

    private final String VERT =
            "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
                    "attribute vec2 "+ShaderProgram.TEXCOORD_ATTRIBUTE+"0;\n" +
                    "uniform mat4 u_projTrans;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "	vTexCoord = a_position.xy;\n" +
                    "	gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
                    "}";

    /*private final String FRAG =
            "uniform sampler2D u_texture; //default GL_TEXTURE0, expected by SpriteBatch\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "" +
                    "	gl_FragColor = texture(u_texture, vTexCoord);\n" +
                    "}";*/
    private final String FRAG =
            "// Water + Terrain shader\n" +
                    "// 10/2014 Created by Frank Hugenroth /frankenburgh/\n" +
                    "// 'hash' and 'noise' function by iq\n" +
                    "// License: Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.\n" +
                    "//\n" +
                    "// V1.1  - added 'real' water height (hits terrain) and waterheight is visible on shadow\n" +
                    "//\n" +
                    "// CLICK and MOVE the MOUSE to:\n" +
                    "// X -> Change water height  /  Y -> Change water clarity.\n" +
                    "\n" +
                    "uniform sampler2D u_texture; //default GL_TEXTURE0, expected by SpriteBatch\n" +
                    "//varying vec2 vTexCoord;\n" +
                    "uniform sampler2D u_texture1;\n" +
                    "uniform float iTime;\n" +
                    "uniform vec3 iResolution;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "\n" +
                    "// some parameters....\n" +
                    "float coast2water_fadedepth = 0.10;\n" +
                    "float large_waveheight      = 0.50; // change to adjust the \"heavy\" waves\n" +
                    "float large_wavesize        = 4.;  // factor to adjust the large wave size\n" +
                    "float small_waveheight      = .6;  // change to adjust the small random waves\n" +
                    "float small_wavesize        = .5;   // factor to ajust the small wave size\n" +
                    "float water_softlight_fact  = 15.;  // range [1..200] (should be << smaller than glossy-fact)\n" +
                    "float water_glossylight_fact= 120.; // range [1..200]\n" +
                    "float particle_amount       = 70.;\n" +
                    "vec3 watercolor             = vec3(0.43, 0.60, 0.66); // 'transparent' low-water color (RGB)\n" +
                    "vec3 watercolor2            = vec3(0.06, 0.07, 0.11); // deep-water color (RGB, should be darker than the low-water color)\n" +
                    "vec3 water_specularcolor    = vec3(1.3, 1.3, 0.9);    // specular Color (RGB) of the water-highlights\n" +
                    "vec3 light;\n" +
                    "\n" +
                    "\n" +
                    "//#define USETEXTUREHEIGHT 0\n" +
                    "#define USETEXTUREHEIGHT 1\n" +
                    "\n" +
                    "\n" +
                    "// calculate random value\n" +
                    "float hash( float n )\n" +
                    "{\n" +
                    "    return fract(sin(n)*43758.5453123);\n" +
                    "}\n" +
                    "\n" +
                    "// 2d noise function\n" +
                    "float noise1( in vec2 x )\n" +
                    "{\n" +
                    "  vec2 p  = floor(x);\n" +
                    "  vec2 f  = smoothstep(0.0, 1.0, fract(x));\n" +
                    "  float n = p.x + p.y*57.0;\n" +
                    "  return mix(mix( hash(n+  0.0), hash(n+  1.0),f.x),\n" +
                    "    mix( hash(n+ 57.0), hash(n+ 58.0),f.x),f.y);\n" +
                    "}\n" +
                    "\n" +
                    "float noise(vec2 p)\n" +
                    "{\n" +
                    "  return textureLod(u_texture1,p*vec2(1./256.),0.0).x;\n" +
                    "}\n" +
                    "\n" +
                    "float height_map( vec2 p )\n" +
                    "{\n" +
                    "#if USETEXTUREHEIGHT\n" +
                    "  float f = 0.15+textureLod(u_texture, p, 0.0).r*2.;\n" +///p*0.6
                    "#else\n" +
                    "  mat2 m = mat2( 0.9563*1.4,  -0.2924*1.4,  0.2924*1.4,  0.9563*1.4 );\n" +
                    "  p = p*6.;\n" +
                    "  float f = 0.6000*noise1( p ); p = m*p*1.1;\n" +
                    "  f += 0.2500*noise1( p ); p = m*p*1.32;\n" +
                    "  f += 0.1666*noise1( p ); p = m*p*1.11;\n" +
                    "  f += 0.0834*noise( p ); p = m*p*1.12;\n" +
                    "  f += 0.0634*noise( p ); p = m*p*1.13;\n" +
                    "  f += 0.0444*noise( p ); p = m*p*1.14;\n" +
                    "  f += 0.0274*noise( p ); p = m*p*1.15;\n" +
                    "  f += 0.0134*noise( p ); p = m*p*1.16;\n" +
                    "  f += 0.0104*noise( p ); p = m*p*1.17;\n" +
                    "  f += 0.0084*noise( p );\n" +
                    "  const float FLAT_LEVEL = 0.525;\n" +
                    "  if (f<FLAT_LEVEL)\n" +
                    "      f = f;\n" +
                    "  else\n" +
                    "      f = pow((f-FLAT_LEVEL)/(1.-FLAT_LEVEL), 2.)*(1.-FLAT_LEVEL)*2.0+FLAT_LEVEL; // makes a smooth coast-increase\n" +
                    "#endif\n" +
                    "  return clamp(f, 0., 10.);\n" +
                    "}\n" +
                    "\n" +
                    "vec3 terrain_map( vec2 p )\n" +
                    "{\n" +
                    "  //return vec3(0.7, .55, .4)+texture(iChannel1, p*2.).rgb*.5; // test-terrain is simply 'sandstone'\n" +
                    "  return vec3(0.7, .55, .4);\n" +
                    "}\n" +
                    "\n" +
                    "const mat2 m = mat2( 0.72, -1.60,  1.60,  0.72 );\n" +
                    "\n" +
                    "float water_map( vec2 p, float height )\n" +
                    "{\n" +
                    "  vec2 p2 = p*large_wavesize;\n" +
                    "  vec2 shift1 = 0.001*vec2( iTime*160.0*2.0, iTime*120.0*2.0 );\n" +
                    "  vec2 shift2 = 0.001*vec2( iTime*190.0*2.0, -iTime*130.0*2.0 );\n" +
                    "\n" +
                    "  // coarse crossing 'ocean' waves...\n" +
                    "  float f = 0.6000*noise( p );\n" +
                    "  f += 0.2500*noise( p*m );\n" +
                    "  f += 0.1666*noise( p*m*m );\n" +
                    "  float wave = sin(p2.x*0.622+p2.y*0.622+shift2.x*4.269)*large_waveheight*f*height*height ;\n" +
                    "\n" +
                    "  p *= small_wavesize;\n" +
                    "  f = 0.;\n" +
                    "  float amp = 1.0, s = .5;\n" +
                    "  for (int i=0; i<9; i++)\n" +
                    "  { p = m*p*.947; f -= amp*abs(sin((noise( p+shift1*s )-.5)*2.)); amp = amp*.59; s*=-1.329; }\n" +
                    "\n" +
                    "  return wave+f*small_waveheight;\n" +
                    "}\n" +
                    "\n" +
                    "float nautic(vec2 p)\n" +
                    "{\n" +
                    "  p *= 18.;\n" +
                    "  float f = 0.;\n" +
                    "  float amp = 1.0, s = .5;\n" +
                    "  for (int i=0; i<3; i++)\n" +
                    "  { p = m*p*1.2; f += amp*abs(smoothstep(0., 1., noise( p+iTime*s ))-.5); amp = amp*.5; s*=-1.227; }\n" +
                    "  return pow(1.-f, 5.);\n" +
                    "}\n" +
                    "\n" +
                    "float particles(vec2 p)\n" +
                    "{\n" +
                    "  p *= 200.;\n" +
                    "  float f = 0.;\n" +
                    "  float amp = 1.0, s = 1.5;\n" +
                    "  for (int i=0; i<3; i++)\n" +
                    "  { p = m*p*1.2; f += amp*noise( p+iTime*s ); amp = amp*.5; s*=-1.227; }\n" +
                    "  return pow(f*.35, 7.)*particle_amount;\n" +
                    "}\n" +
                    "\n" +
                    "\n" +
                    "float test_shadow( vec2 xy, float height)\n" +
                    "{\n" +
                    "    vec3 r0 = vec3(xy, height);\n" +
                    "    vec3 rd = normalize( light - r0 );\n" +
                    "\n" +
                    "    float hit = 1.0;\n" +
                    "    float t   = 0.001;\n" +
                    "    for (int j=1; j<25; j++)\n" +
                    "    {\n" +
                    "        vec3 p = r0 + t*rd;\n" +
                    "        float h = height_map( p.xy );\n" +
                    "        float height_diff = p.z - h;\n" +
                    "        if (height_diff<0.0)\n" +
                    "        {\n" +
                    "            return 0.0;\n" +
                    "        }\n" +
                    "        t += 0.01+height_diff*.02;\n" +
                    "        hit = min(hit, 2.*height_diff/t); // soft shaddow\n" +
                    "    }\n" +
                    "    return hit;\n" +
                    "}\n" +
                    "\n" +
                    "vec3 CalcTerrain(vec2 uv, float height)\n" +
                    "{\n" +
                    "  vec3 col = terrain_map( uv );\n" +
                    "  float h1 = height_map(uv-vec2(0., 0.01));\n" +
                    "  float h2 = height_map(uv+vec2(0., 0.01));\n" +
                    "  float h3 = height_map(uv-vec2(0.01, 0.));\n" +
                    "  float h4 = height_map(uv+vec2(0.01, 0.));\n" +
                    "  vec3 norm = normalize(vec3(h3-h4, h1-h2, 1.));\n" +
                    "  vec3 r0 = vec3(uv, height);\n" +
                    "  vec3 rd = normalize( light - r0 );\n" +
                    "  float grad = dot(norm, rd);\n" +
                    "  col *= grad+pow(grad, 8.);\n" +
                    "  float terrainshade = test_shadow( uv, height );\n" +
                    "  col = mix(col*.25, col, terrainshade);\n" +
                    "  return col;\n" +
                    "}\n" +
                    "\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "    light = vec3(-0., sin(iTime*0.5)*.5 + .35, 2.8); // position of the sun\n" +
                    "\tvec2 uv = (gl_FragCoord.xy / iResolution.xy );\n" +//- vec2(-0.12, +0.25)
                    "\n" +
                    "    float WATER_LEVEL = 0.54; // Water level (range: 0.0 - 2.0)\n" +
                    "    /*if (iMouse.z>0.)\n" +
                    "\t\tWATER_LEVEL = iMouse.x*.003;*/\n" +
                    "    float deepwater_fadedepth   = 0.5 + coast2water_fadedepth;\n" +
                    "   /* if (iMouse.z>0.)\n" +
                    "\t  deepwater_fadedepth = iMouse.y*0.003 + coast2water_fadedepth;*/\n" +
                    "\n" +
                    "    float height = height_map( uv );\n" +
                    "    vec3 col;\n" +
                    "\n" +
                    "    float waveheight = clamp(WATER_LEVEL*3.-1.5, 0., 1.);\n" +
                    "    float level = WATER_LEVEL + .2*water_map(uv*15. + vec2(iTime*.1), waveheight);\n" +
                    "    if (height > level)\n" +
                    "    {\n" +
                    "        col = CalcTerrain(uv, height);\n" +
                    "    }\n" +
                    "    if (height <= level)\n" +
                    "    {\n" +
                    "        vec2 dif = vec2(.0, .01);\n" +
                    "        vec2 pos = uv*15. + vec2(iTime*.01);\n" +
                    "        float h1 = water_map(pos-dif,waveheight);\n" +
                    "        float h2 = water_map(pos+dif,waveheight);\n" +
                    "        float h3 = water_map(pos-dif.yx,waveheight);\n" +
                    "        float h4 = water_map(pos+dif.yx,waveheight);\n" +
                    "        vec3 normwater = normalize(vec3(h3-h4, h1-h2, .125)); // norm-vector of the 'bumpy' water-plane\n" +
                    "        uv += normwater.xy*.002*(level-height);\n" +
                    "\n" +
                    "        col = CalcTerrain(uv, height);\n" +
                    "\n" +
                    "        float coastfade = clamp((level-height)/coast2water_fadedepth, 0., 1.);\n" +
                    "        float coastfade2= clamp((level-height)/deepwater_fadedepth, 0., 1.);\n" +
                    "        float intensity = col.r*.2126+col.g*.7152+col.b*.0722;\n" +
                    "        watercolor = mix(watercolor*intensity, watercolor2, smoothstep(0., 1., coastfade2));\n" +
                    "\n" +
                    "        vec3 r0 = vec3(uv, WATER_LEVEL);\n" +
                    "        vec3 rd = normalize( light - r0 ); // ray-direction to the light from water-position\n" +
                    "        float grad     = dot(normwater, rd); // dot-product of norm-vector and light-direction\n" +
                    "        float specular = pow(grad, water_softlight_fact);  // used for soft highlights\n" +
                    "        float specular2= pow(grad, water_glossylight_fact); // used for glossy highlights\n" +
                    "        float gradpos  = dot(vec3(0., 0., 1.), rd);\n" +
                    "        float specular1= smoothstep(0., 1., pow(gradpos, 5.));  // used for diffusity (some darker corona around light's specular reflections...)\n" +
                    "        float watershade  = test_shadow( uv, level );\n" +
                    "        watercolor *= 2.2+watershade;\n" +
                    "   \t\twatercolor += (.2+.8*watershade) * ((grad-1.0)*.5+specular) * .25;\n" +
                    "   \t\twatercolor /= (1.+specular1*1.25);\n" +
                    "   \t\twatercolor += watershade*specular2*water_specularcolor;\n" +
                    "        watercolor += watershade*coastfade*(1.-coastfade2)*(vec3(.5, .6, .7)*nautic(uv)+vec3(1., 1., 1.)*particles(uv));\n" +
                    "\n" +
                    "        col = mix(col, watercolor, coastfade);\n" +
                    "    }\n" +
                    "\n" +
                    "\tgl_FragColor = vec4(col , 1.0);\n" +
                    "}\n";

    private Texture tex;
    private Texture noise;
    //private Texture tex;
    private OrthographicCamera cam;
    private ShaderProgram shaderProgram;
    private static float iTime = 0;
    private SpriteBatch batch;

    @Override
    public void create() {

        tex = new Texture(Gdx.files.internal("ireland.png"));
        noise = new Texture(Gdx.files.internal("noise.png"));

        //important since we aren't using some uniforms and attributes that SpriteBatch expects
        ShaderProgram.pedantic = false;

        shaderProgram = new ShaderProgram(VERT, FRAG);
        if (!shaderProgram.isCompiled()) {
            System.err.println(shaderProgram.getLog());
            System.exit(0);
        }

        if (shaderProgram.getLog().length() != 0)
            System.out.println(shaderProgram.getLog());


        cam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.setToOrtho(false);



        noise.bind(1);
        //now we need to reset glActiveTexture to zero!!!! since sprite batch does not do this for us
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);


        batch = new SpriteBatch(1000, shaderProgram);
        batch.setShader(shaderProgram);

        shaderProgram.begin();
        shaderProgram.setUniformf("iResolution", new Vector3(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 1.0f));
        //set our sampler2D uniforms
        shaderProgram.setUniformi("u_texture1", 1);
        //shaderProgram.setUniformi("u_mask", 2);
        shaderProgram.end();

    }

    @Override
    public void resize(int width, int height) {
        cam.setToOrtho(false, width, height);
        //batch.setProjectionMatrix(cam.combined);
    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.setProjectionMatrix(cam.combined);
        iTime += 0.01;

        shaderProgram.setUniformf("iTime", iTime);

        batch.draw(tex, 0, 0);
        batch.end();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {
        //batch.dispose();
        shaderProgram.dispose();
        tex.dispose();
    }
}
