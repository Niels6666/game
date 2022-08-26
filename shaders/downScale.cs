#version 460 core

layout(local_size_x = 16, local_size_y = 16, local_size_z = 1) in;

layout(binding = 0) uniform sampler2D inputImg;
layout(binding = 0, rgba8) uniform restrict writeonly image2D outputImg;
/*
const int N = 20;

shared vec4 elements[N*N];

void loadElements(){
	//Size of one texel
    vec2 texel_size = 1.0 / textureSize(inputImg, 0);
    
	for(int k=0; k<N*N; k++){
		int y = k / N;
		int x = k - y * N;

		ivec2 texel = ivec2(gl_WorkGroupID.xy * 16) + ivec2(x, y) - ivec2(2);
		vec2 texCoords = (vec2(texel) + vec2(0.5)) * texel_size;

		elements[k] = texture(inputImg, texCoords);
	}

	memoryBarrierShared();
	barrier();
}

vec3 getElement(ivec2 offset){
	int k = (int(gl_LocalInvocationID.y) + offset.y + 2) * N 
				+ (int(gl_LocalInvocationID.x) + offset.x + 2);

	return vec3(elements[k].xyz);
}


vec3 filter13tap() {
	loadElements();

    //Sampling pattern:
    //
    //   c27   c28   c21
    //      c14   c11
    //   c26   c00   c22   
    //      c13   c12
    //   c25   c24   c23
    //

	vec3 c00 = getElement(ivec2(0, 0));
	
	vec3 c11 = getElement(ivec2(+1, +1));
	vec3 c12 = getElement(ivec2(+1, -1));
	vec3 c13 = getElement(ivec2(-1, +1));
	vec3 c14 = getElement(ivec2(-1, -1));
	
	vec3 c21 = getElement(ivec2(+2, +2));
	vec3 c22 = getElement(ivec2(+2, +0));
	vec3 c23 = getElement(ivec2(+2, -2));
	vec3 c24 = getElement(ivec2(+0, -2));
	vec3 c25 = getElement(ivec2(-2, -2));
	vec3 c26 = getElement(ivec2(-2, +0));
	vec3 c27 = getElement(ivec2(-2, +2));
	vec3 c28 = getElement(ivec2(+0, +2));
	
	vec3 box0 = (c11 + c12 + c13 + c14) * 0.25;
	vec3 box1 = (c21 + c22 + c00 + c28) * 0.25;
	vec3 box2 = (c00 + c22 + c23 + c24) * 0.25;
	vec3 box3 = (c26 + c00 + c24 + c25) * 0.25;
	vec3 box4 = (c27 + c28 + c00 + c26) * 0.25;
	
	return box0 * 0.5 + (box1 + box2 + box3 + box4) * 0.125;
}
*/

vec4 filter13tap(vec2 texCoords) {
	//Size of one texel
    vec2 tex_offset = 1.0 / textureSize(inputImg, 0);
    
    //Sampling pattern:
    //
    //   c27   c28   c21
    //      c14   c11
    //   c26   c00   c22   
    //      c13   c12
    //   c25   c24   c23
    //

	vec4 c00 = texture(inputImg, texCoords);
	
	vec4 c11 = texture(inputImg, texCoords + tex_offset * ivec2(+1, +1));
	vec4 c12 = texture(inputImg, texCoords + tex_offset * ivec2(+1, -1));
	vec4 c13 = texture(inputImg, texCoords + tex_offset * ivec2(-1, +1));
	vec4 c14 = texture(inputImg, texCoords + tex_offset * ivec2(-1, -1));
	
	vec4 c21 = texture(inputImg, texCoords + tex_offset * ivec2(+2, +2));
	vec4 c22 = texture(inputImg, texCoords + tex_offset * ivec2(+2, +0));
	vec4 c23 = texture(inputImg, texCoords + tex_offset * ivec2(+2, -2));
	vec4 c24 = texture(inputImg, texCoords + tex_offset * ivec2(+0, -2));
	vec4 c25 = texture(inputImg, texCoords + tex_offset * ivec2(-2, -2));
	vec4 c26 = texture(inputImg, texCoords + tex_offset * ivec2(-2, +0));
	vec4 c27 = texture(inputImg, texCoords + tex_offset * ivec2(-2, +2));
	vec4 c28 = texture(inputImg, texCoords + tex_offset * ivec2(+0, +2));
	
	vec4 box0 = (c11 + c12 + c13 + c14) * 0.25;
	vec4 box1 = (c21 + c22 + c00 + c28) * 0.25;
	vec4 box2 = (c00 + c22 + c23 + c24) * 0.25;
	vec4 box3 = (c26 + c00 + c24 + c25) * 0.25;
	vec4 box4 = (c27 + c28 + c00 + c26) * 0.25;
	
	return box0 * 0.5 + (box1 + box2 + box3 + box4) * 0.125;
}


void main(){

    vec2 texCoords = (vec2(gl_GlobalInvocationID.xy) + 0.5) / imageSize(outputImg);
    vec4 c = filter13tap(texCoords);

	if(c.a == 0){
		c = vec4(0);
	}

    imageStore(outputImg, ivec2(gl_GlobalInvocationID.xy), c);

}