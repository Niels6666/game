#version 460 core

layout(local_size_x = 16, local_size_y = 16, local_size_z = 1) in;

layout(binding = 0, rgba8) uniform restrict readonly image2D colorOutput;
layout(binding = 1, r11f_g11f_b10f) uniform restrict readonly image2D lightOutput;
layout(binding = 2, rgba8) uniform restrict readonly image2D glowOutput;
layout(binding = 3, rgba8) uniform restrict writeonly image2D postprocessOutput;

uniform float exposure;
uniform float glowPower;

void main(){

    vec4 color = imageLoad(colorOutput, ivec2(gl_GlobalInvocationID.xy));
    vec4 light = imageLoad(lightOutput, ivec2(gl_GlobalInvocationID.xy));
    vec4 glow = imageLoad(glowOutput, ivec2(gl_GlobalInvocationID.xy));

    color *= (light + glow * glowPower); // add the light
    color = color / (color + exposure);
    color.a = 1.0f;

    imageStore(postprocessOutput, ivec2(gl_GlobalInvocationID.xy), color);

}