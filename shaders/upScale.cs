#version 460 core

layout(local_size_x = 16, local_size_y = 16, local_size_z = 1) in;

layout(binding = 0) uniform sampler2D inputImg;
layout(binding = 0, rgba16f) uniform restrict image2D outputImg;

uniform float weight;

vec3 filterTent(vec2 texCoords){
	//Size of one texel
    vec2 tex_offset = 1.0 / textureSize(inputImg, 0);
    
    //Sampling pattern:
    //
    //   c11   c12   c13
    //   c21   c22   c23   
    //   c31   c32   c33
    //

	vec3 c11 = texture(inputImg, texCoords + tex_offset * ivec2(-1, +1)).rgb;
	vec3 c12 = texture(inputImg, texCoords + tex_offset * ivec2(+0, +1)).rgb;
	vec3 c13 = texture(inputImg, texCoords + tex_offset * ivec2(+1, +1)).rgb;
	                   
	vec3 c21 = texture(inputImg, texCoords + tex_offset * ivec2(-1, +0)).rgb;
	vec3 c22 = texture(inputImg, texCoords + tex_offset * ivec2(+0, +0)).rgb;
	vec3 c23 = texture(inputImg, texCoords + tex_offset * ivec2(+1, +0)).rgb;
	                   
	vec3 c31 = texture(inputImg, texCoords + tex_offset * ivec2(-1, -1)).rgb;
	vec3 c32 = texture(inputImg, texCoords + tex_offset * ivec2(+0, -1)).rgb;
	vec3 c33 = texture(inputImg, texCoords + tex_offset * ivec2(+1, -1)).rgb;
	
	return (1.0/16.0) * ((c11 + c13 + c31 + c33) + (c12 + c23 + c21 + c32) * 2.0 + c22 * 4.0);
}


void main(){

    vec3 previous_c = imageLoad(outputImg, ivec2(gl_GlobalInvocationID.xy)).rgb;

    vec2 texCoords = (vec2(gl_GlobalInvocationID.xy) + 0.5) / imageSize(outputImg);
    vec3 c = filterTent(texCoords);

    previous_c += c*weight;

    imageStore(outputImg, ivec2(gl_GlobalInvocationID.xy), vec4(previous_c, 1.0));


}