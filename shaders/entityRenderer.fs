#version 460 core

const int blockPixelHeight = 20;
const int blocksPerChunk = 20;
const int chunksPerWorld = 128;

const vec2 worldCenter = vec2(chunksPerWorld/2.0f) * blocksPerChunk;

struct BVHNode{
	vec2 min;
	vec2 max;
	int child1;		   // -1 if leaf node
	int child2;		   // -1 if leaf node
	float radius;      // if leaf node, radius of the light
	float innerRadius; // if leaf node, innerRadius of the light
	vec2 direction;
	uint color;        // rgba
	float angle;
};

layout(binding = 0) uniform sampler2D TextureAtlas;
layout(binding = 1) uniform sampler2D TextureGlowAtlas;

layout(location = 0) out vec4 colorOutput;
layout(location = 1) out vec4 lightOutput;
layout(location = 2) out vec4 glowOutput;

layout(std430, binding = 2) restrict readonly buffer LightBuffer {
	BVHNode nodes[];
};

uniform float zoom;
uniform vec2 cameraPos;
uniform vec2 screenSize;
uniform int time;
uniform float ambientLight;

in vec2 texCoords;

float getDynamicRadius(float radius) {
	//float s = time + cos(time/60.0 * 0.12) + sin(time/60.0 * 4.456);
	//s = 0.8f + 0.2f * cos(s);
	return radius;// * s;
	//Random r = new Random(System.nanoTime() / 50000);
	//dynamicRadius = radius - r.nextFloat();
}

vec4 illumination(vec2 worldPos){
    if(nodes.length() == 0){
        return vec4(0.0);
    }
    
    int stackPtr = 0;
    int stack[32];
    stack[stackPtr] = 0;

    vec4 lightPower = vec4(0.0f);
    while(stackPtr >= 0){
        //pop the last node
        BVHNode n = nodes[stack[stackPtr--]];

        if(all(greaterThan(worldPos, n.min)) && all(lessThan(worldPos, n.max))){
            //inside the node

            if(n.child1 >= 0){//n is not a leaf
                stack[++stackPtr] = n.child2;
                stack[++stackPtr] = n.child1;
            } else {
                vec2 center = (n.min + n.max) * 0.5f;
                vec2 toPixel = worldPos - center;
                float d = length(toPixel);
                float r = getDynamicRadius(n.radius);
				
                float power = max(1.0f - d / r, 0.0);

				vec2 dir = n.direction;
				float cosAngleHalf = n.angle;
				
				if(d > n.innerRadius){
					if(dot(dir, toPixel) < cosAngleHalf*d){
						power = 0;
					}
				}
				
               	lightPower += power * unpackUnorm4x8(n.color);
            }
        }
    }

    return lightPower;
}

void main(){
    
    vec2 onScreenPixelCoords = gl_FragCoord.xy - screenSize / 2.0;
    vec2 worldCoords = onScreenPixelCoords / blockPixelHeight * zoom + cameraPos;

    colorOutput = texelFetch(TextureAtlas, ivec2(texCoords * textureSize(TextureAtlas, 0)), 0);

    if(colorOutput.a == 0){
        discard;
    }
    
    vec4 glowColor = texelFetch(TextureGlowAtlas, ivec2(texCoords * textureSize(TextureGlowAtlas, 0)), 0);
    glowColor *= glowColor.a;

    lightOutput = illumination(worldCoords) + ambientLight;
    glowOutput = glowColor;
}