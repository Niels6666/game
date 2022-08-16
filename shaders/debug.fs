#version 460 core

out vec4 out_Color;
uniform float lightDepth;
uniform int isLeaf;

void main(){
	out_Color = vec4(lightDepth, 1.0-lightDepth, lightDepth+1.0, 1.0);
	if(isLeaf == 1){ // green LOGIC
		out_Color = vec4(0.0, 0.6, 0.0, 1.0);
	}
}
