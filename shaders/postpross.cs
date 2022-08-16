#version 460 core

layout(local_size_x = 16, local_size_y = 16, local_size_z = 1) in;

layout(binding = 0, rgba16f) uniform restrict readonly image2D colorOutput;
layout(binding = 1, rgba16f) uniform restrict readonly image2D lightOutput;
layout(binding = 2, rgba8) uniform restrict writeonly image2D postprocessOutput;

uniform float exposure;

void main(){

    vec4 color = imageLoad(colorOutput, ivec2(gl_GlobalInvocationID.xy));
    vec4 bloomLight = imageLoad(lightOutput, ivec2(gl_GlobalInvocationID.xy));

    bloomLight.a = 1.0;
    color = color * bloomLight;

    color = color / (color + exposure);

    imageStore(postprocessOutput, ivec2(gl_GlobalInvocationID.xy), color);

}