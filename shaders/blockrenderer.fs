#version 460 core

const int blockPixelHeight = 20;
const int blocksPerChunk = 20;
const int chunksPerWorld = 128;
const float sunRadius = 15.0f;

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
	int child1;		   // -1 if leaf node
	int child2;		   // -1 if leaf node
	float radius;      // if leaf node, radius of the light
	float innerRadius; // if leaf node, innerRadius of the light
	vec2 direction;
	uint color;        // rgba
	float angle;
};

struct BlockInfo{
	bool isAnimated;
	int animationLength;
	int animationSpeed;
	int padding;
};

layout(binding = 0) uniform sampler2D TextureAtlas;
layout(binding = 1) uniform sampler2D TextureGlowAtlas;

layout(location = 0) out vec4 colorOutput;
layout(location = 1) out vec4 lightOutput;
layout(location = 2) out vec4 glowOutput;

////////////
//foreground
////////////
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

/////////////
//background
/////////////
layout(std430, binding = 4) restrict readonly buffer bgWorldBuffer {
	World BGworld;
};

layout(std430, binding = 5) restrict readonly buffer bgChunkBuffer {
	Chunk BGchunks[];
};

/////////////
//altitudes
/////////////
layout(std430, binding = 6) restrict readonly buffer altitudesBuffer {
	int altitudes[];
};

//////////
//uniforms
//////////
uniform vec2 cameraPos;
uniform vec2 screenSize;
uniform int time;
uniform float zoom;
uniform float ambientLight;
uniform vec4 skyColor;
uniform vec4 sunColor;
uniform vec2 sunPos;

in vec2 texCoords;

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
                float r = n.radius;
				
                float power = max(1.0f - d / r, 0.0);
                float innerPower = max(1.0f - d / n.innerRadius, 0.0);

				vec2 dir = n.direction;
				float cosAngleHalf = n.angle;
				
				power *= smoothstep(cosAngleHalf*d*0.8, cosAngleHalf*d, dot(dir, toPixel));
				
				power = max(power, innerPower);
				
               	lightPower += power * unpackUnorm4x8(n.color);
            }
        }
    }

    return lightPower;
}

void main(){
    
    vec2 onScreenPixelCoords = gl_FragCoord.xy - screenSize / 2.0;
    vec2 worldCoords = onScreenPixelCoords / blockPixelHeight * zoom 
        + cameraPos;

    ivec2 blockGlobalCoords = ivec2(floor(worldCoords));
    vec2 pixelLocalCoords = (worldCoords - blockGlobalCoords) * blockPixelHeight;
    
    ivec2 chunkCoords = ivec2(floor(worldCoords / blocksPerChunk));
    ivec2 blockLocalCoords = blockGlobalCoords - chunkCoords * blocksPerChunk;

    bool insideWorld = chunkCoords.x >= 0 && chunkCoords.y >= 0 && 
                chunkCoords.x < chunksPerWorld && chunkCoords.y < chunksPerWorld;
                
    lightOutput = vec4(0, 0, 0, 0);
    if(!insideWorld){
        colorOutput = vec4(vec3(0.2), 1);
        return;
    }
    
    int chunkID = world.chunkIDs[chunkCoords.x + chunkCoords.y * chunksPerWorld];
    if(chunkID != -1){
    	int blockID = chunks[chunkID].blockIDs[blockLocalCoords.x + blockLocalCoords.y * blocksPerChunk];
	    
        if(blockID == 0){
            colorOutput = vec4(0.0, 0.0, 0.0, 0);//AIR
        }else{
            if(blockInfos[blockID].isAnimated){
            	blockID += (time / blockInfos[blockID].animationSpeed) % blockInfos[blockID].animationLength;
            }

            ivec2 atlasSize = textureSize(TextureAtlas, 0) / blockPixelHeight;
	        ivec2 atlasCoords = ivec2(blockID % atlasSize.x, blockID / atlasSize.x);
	
		    colorOutput = texelFetch(TextureAtlas, ivec2(atlasCoords * blockPixelHeight + pixelLocalCoords), 0);
		    glowOutput = texelFetch(TextureGlowAtlas, ivec2(atlasCoords * blockPixelHeight + pixelLocalCoords), 0);
            glowOutput.grb *= glowOutput.a;
        }
        
    }else{
    	colorOutput = vec4(0.0, 0.0, 0.0, 1);
        glowOutput = vec4(0.0, 0.0, 0.0, 1);
    }
    
    lightOutput = illumination(worldCoords);
    
    if(colorOutput.a < 1){
		// do almost the same for the background
		int BGchunkID = BGworld.chunkIDs[chunkCoords.x + chunkCoords.y * chunksPerWorld];
		if(BGchunkID != -1){
			int BGblockID = BGchunks[chunkID].blockIDs[blockLocalCoords.x + blockLocalCoords.y * blocksPerChunk];
		    
		    if(BGblockID == 0){
		        colorOutput = vec4(0.0, 0.0, 0.0, 0);//AIR
		    }else{
		        if(blockInfos[BGblockID].isAnimated){
		        	BGblockID += (time / blockInfos[BGblockID].animationSpeed) % blockInfos[BGblockID].animationLength;
		        }
		
		        ivec2 atlasSize = textureSize(TextureAtlas, 0) / blockPixelHeight;
		        ivec2 atlasCoords = ivec2(BGblockID % atlasSize.x, BGblockID / atlasSize.x);
		
			    colorOutput = texelFetch(TextureAtlas, ivec2(atlasCoords * blockPixelHeight + pixelLocalCoords), 0);
			    //darken it to give the 'background' impression
			    colorOutput.grb *= 0.4;
		    }
		}
    }
    
    //test again for the sky
    if(colorOutput.a < 1){
     	//test if it is the sun
     	vec2 toPixel = worldCoords - sunPos;
        float d2 = dot(toPixel, toPixel);
     	if(d2 <= sunRadius*sunRadius){
     		colorOutput = sunColor;
     		glowOutput = sunColor;
     	}else{
	    	colorOutput = skyColor;
     	}
    }
    
	lightOutput += max(ambientLight, 0.2f) * skyColor * 
						max(smoothstep(
								blockGlobalCoords.y - 20, 
								blockGlobalCoords.y,
								altitudes[blockGlobalCoords.x]
							), 
							0.2f);
    
    
}