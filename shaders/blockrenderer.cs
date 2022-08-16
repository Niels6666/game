#version 460 core

layout(local_size_x = 16, local_size_y = 16, local_size_z = 1) in;

const int blockPixelHeight = 20;
const int blocksPerChunk = 20;
const int chunksPerWorld = 128;

const vec2 worldCenter = vec2(chunksPerWorld/2.0f) * blocksPerChunk;

struct Chunk{
	int blockIDs[blocksPerChunk * blocksPerChunk];
};

struct World{
	int chunkIDs[chunksPerWorld * chunksPerWorld];
};

struct BVHNode{
	vec2 min;
	vec2 max;
	int child1;		// -1 if leaf node
	int child2;		// -1 if leaf node
	float radius;   // if leaf node, radius of the light
	uint color;      // rgba
	vec2 direction;
	float angle;
	int padding;
};

struct BlockInfo{
	bool isAnimated;
	int animationLength;
	int animationSpeed;
	int padding;
};

layout(binding = 0) uniform sampler2D TextureAtlas;
layout(binding = 1) uniform sampler2D TextureGlowAtlas;

layout(binding = 0, rgba16f) uniform restrict writeonly image2D colorOutput;
layout(binding = 1, rgba16f) uniform restrict writeonly image2D lightOutput;

layout(std430, binding = 0) restrict readonly buffer worldBuffer {
	World world;
};

layout(std430, binding = 1) restrict readonly buffer chunkBuffer {
	Chunk chunks[];
};

layout(std430, binding = 2) restrict readonly buffer LightBuffer {
	BVHNode nodes[];
};

layout(std430, binding = 3) restrict readonly buffer BlockInfoBuffer {
	BlockInfo blockInfos[];
};

uniform float zoom;
uniform vec2 cameraPos;
uniform vec2 screenSize;
uniform int time;
uniform float glowPower;

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
				
				if(dot(dir, toPixel) < cosAngleHalf*d){
					power = 0;
				}
				
               	lightPower += power * unpackUnorm4x8(n.color);
            }
        }
    }

    return lightPower;
}

void main(){
    vec2 onScreenPixelCoords = vec2(gl_GlobalInvocationID.xy) - vec2(imageSize(colorOutput)) / 2.0;
    vec2 worldCoords = onScreenPixelCoords / blockPixelHeight * zoom 
        + cameraPos;

    ivec2 blockCoords = ivec2(floor(worldCoords));
    vec2 blockLocalCoords = (worldCoords - blockCoords) * blockPixelHeight;
    
    ivec2 chunkCoords = ivec2(floor(worldCoords / blocksPerChunk));
    blockCoords = blockCoords - chunkCoords * blocksPerChunk;

    bool insideWorld = chunkCoords.x >= 0 && chunkCoords.y >= 0 && 
                chunkCoords.x < chunksPerWorld && chunkCoords.y < chunksPerWorld;
                
	vec4 out_Color = vec4(0.0);
    if(!insideWorld){
        out_Color = vec4(vec3(0.2), 1);
        imageStore(colorOutput, ivec2(gl_GlobalInvocationID.xy), out_Color);
        return;
    }
    
    int chunkID = world.chunkIDs[chunkCoords.x + chunkCoords.y * chunksPerWorld];
    if(chunkID != -1){
    	int blockID = chunks[chunkID].blockIDs[blockCoords.x + blockCoords.y * blocksPerChunk];
	    
        if(blockID < 0){
            out_Color = vec4(1.0, 0.0, 1.0, 1); // should not happen
        }else if(blockID == 0){
            out_Color = vec4(0.0, 0.0, 0.0, 1);//AIR
        }else{
            if(blockInfos[blockID].isAnimated){
            	blockID += (time / blockInfos[blockID].animationSpeed) % blockInfos[blockID].animationLength;
            }

            ivec2 atlasSize = textureSize(TextureAtlas, 0) / blockPixelHeight;
	        ivec2 atlasCoords = ivec2(blockID % atlasSize.x, blockID / atlasSize.x);
	
		    out_Color = texelFetch(TextureAtlas, ivec2(atlasCoords * blockPixelHeight + blockLocalCoords), 0);
		    
		    vec4 glowColor = texelFetch(TextureGlowAtlas, ivec2(atlasCoords * blockPixelHeight + blockLocalCoords), 0);
            glowColor.grb *= glowColor.a * glowPower;

            vec4 lightPower = illumination(worldCoords) + glowColor;
            
			imageStore(lightOutput, ivec2(gl_GlobalInvocationID.xy), lightPower);
        }
    }else{
    	out_Color = vec4(0.0, 0.0, 0.0, 1);
    }

    imageStore(colorOutput, ivec2(gl_GlobalInvocationID.xy), out_Color);
}