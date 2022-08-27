#version 460 core

layout(local_size_x = 16, local_size_y = 16, local_size_z = 1) in;

const int blockPixelHeight = 20;

layout(binding = 0, rgba8) uniform restrict readonly image2D colorOutput;
layout(binding = 1, r11f_g11f_b10f) uniform restrict readonly image2D lightOutput;
layout(binding = 2, rgba8) uniform restrict readonly image2D glowOutput;
layout(binding = 3, rgba8) uniform restrict writeonly image2D postprocessOutput;

/////////////
//altitudes
/////////////
layout(std430, binding = 6) restrict readonly buffer altitudesBuffer {
	float altitudes[];
};

uniform float exposure;
uniform float glowPower;

uniform vec4 skyColor;
uniform vec4 depthColor;

uniform vec2 cameraPos;
uniform vec2 screenSize;
uniform float zoom;

void main(){

    vec4 color = imageLoad(colorOutput, ivec2(gl_GlobalInvocationID.xy));
    vec4 light = imageLoad(lightOutput, ivec2(gl_GlobalInvocationID.xy));
    vec4 glow = imageLoad(glowOutput, ivec2(gl_GlobalInvocationID.xy));

	
	const vec2 onScreenPixelCoords = ivec2(gl_GlobalInvocationID.xy) - screenSize / 2.0;
    const vec2 worldCoords = onScreenPixelCoords / blockPixelHeight * zoom 
       								 + cameraPos;

    const ivec2 blockGlobalCoords = ivec2(floor(worldCoords));
	
    if(color.a == 0){
        //draw the sky

        float surfaceHeight = mix(
        		altitudes[blockGlobalCoords.x],
                altitudes[blockGlobalCoords.x+1],
                fract(worldCoords.x) 
            );

        float surfaceGradient = smoothstep(
        							worldCoords.y - 20,
                                    worldCoords.y,
                                    surfaceHeight
                                );

        color = mix(depthColor, skyColor, surfaceGradient);
    }
    color *= (light + glow * glowPower); // add the light

    color = color / (color + exposure);  //tone mapping
    color.a = 1.0f;
    imageStore(postprocessOutput, ivec2(gl_GlobalInvocationID.xy), color);

}