#version 460 core

in vec2 position;

out vec2 texCoords;

uniform mat4 transform;

void main(){
	gl_Position = transform * vec4(position, 0.0, 1.0);
	texCoords = vec2(0.5*position.x+0.5, 0.5+0.5*position.y);
}