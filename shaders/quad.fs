#version 460

layout(binding = 0) uniform sampler2D img;

in vec2 texCoords;

out vec4 out_Color; 

void main(){

    out_Color = texelFetch(img, ivec2(texCoords * (textureSize(img, 0)-1) + 0.5), 0);

}