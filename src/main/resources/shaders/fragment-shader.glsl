#version 150

varying vec4 f_color;
varying vec3 f_normal;
varying vec3 f_position;
varying float f_fog;
uniform vec3 fog_color;
uniform float gamma;

uniform vec3 light_position;
const vec3 light_color = vec3(1, 1, 1);
const float ambient_light_strength = 0.3;
const float diffuse_light_strength = 0.7;

void main(void) {
    float light_strength = ambient_light_strength + diffuse_light_strength * abs(dot(normalize(f_normal), normalize(light_position - f_position)));
    vec3 light = light_strength * light_color;
    vec4 color = f_color;

    color = vec4(pow(color.r, gamma), pow(color.g, gamma), pow(color.b, gamma), color.a);
    color = vec4(light, 1) * color;
    color = color * (1 - f_fog) + vec4(fog_color, 1) * f_fog;
    gl_FragColor = color;
}
