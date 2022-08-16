#version 460

layout(binding = 0) uniform sampler2D img;

in vec2 texCoords;

out vec4 out_Color; 

void main(){

    out_Color = texture(img, texCoords);

}