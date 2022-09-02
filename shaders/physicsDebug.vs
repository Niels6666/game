#version 460 core

const int blockPixelHeight = 20;

layout(location = 0) in vec2 position;

uniform float zoom;
uniform vec2 cameraPos;
uniform vec2 screenSize;

void main(){
	vec2 onScreenPixelCoords = (position - cameraPos) / zoom * blockPixelHeight; // in [0, screenSize]
	vec2 p = onScreenPixelCoords / screenSize * 2.0;

	p = vec2(p.x, -p.y); //reverse ??

	gl_Position = vec4(p, 0.0, 1.0);// in [-1, 1]
}



