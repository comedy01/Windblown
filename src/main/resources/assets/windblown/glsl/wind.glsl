#ifndef WINDBLOWN_WIND_GLSL
#define WINDBLOWN_WIND_GLSL

float windblown_code(float a) {
    return 254.0 - floor(a * 255.0 + 0.5);
}

float windblown_alpha(float a) {
    float code = windblown_code(a);
    return (code >= 0.0 && code < 64.0) ? 1.0 : a;
}

#ifdef WINDBLOWN_GLOBALS

vec2 windblown_limit(vec2 xz, float most) {
    float len = length(xz);
    return len > most ? xz * (most / len) : xz;
}

vec3 windblown_offset(vec3 pos, float a, float sky) {
    float code = windblown_code(a);
    if (WindblownInfo.z != 4321.5 || code < 0.0 || code >= 64.0) {
        return vec3(0.0);
    }
    float kind = floor(code / 16.0);
    float w = (code - kind * 16.0) / 15.0;
    vec3 world = pos + WindblownCamera.xyz;
    float t = WindblownInfo.x;
    vec2 dir = WindblownWind.xy;
    vec2 side = vec2(-dir.y, dir.x);
    float along = dot(world.xz, dir);
    float across = dot(world.xz, side);

    if (kind > 2.5) {
        float phase = t * 0.9 - along * 0.21 + across * 0.13 + world.y * 0.35;
        vec2 sway = vec2(sin(phase), 0.55 * sin(phase * 0.63 + 1.7 + across * 0.2));
        vec2 xz = (dir * sway.x + side * sway.y) * pow(w, 1.5) * 0.16 * WindblownInfo.w;
        xz = windblown_limit(xz, 0.5);
        return vec3(xz.x, -0.4 * dot(xz, xz), xz.y);
    }

    float s = WindblownInfo.y * mix(0.18, 1.0, smoothstep(0.25, 0.8, sky));
    if (s <= 0.0) {
        return vec3(0.0);
    }
    float front = sin(along * 0.09 - t * 1.25 + sin(across * 0.06 + t * 0.15) * 2.2);
    float gust = mix(0.65, mix(0.35, 1.0, smoothstep(-0.2, 0.9, front) * (0.55 + 0.45 * WindblownWind.z)), WindblownWind.w);
    float wave = sin(t * 2.1 - along * 0.55 + across * 0.17) * 0.6
            + sin(t * 3.4 + world.x * 0.83 + world.z * 0.61) * 0.28
            + sin(t * 5.7 + world.x * 1.9 - world.z * 1.3) * 0.12;

    if (kind < 0.5) {
        float lean = 0.06 * gust;
        float swing = 0.09 * wave * (0.6 + 0.4 * gust);
        float flutter = 0.025 * sin(t * 4.3 + across * 0.9 + world.x * 1.3);
        vec2 xz = windblown_limit((dir * (lean + swing) + side * flutter) * s * w * w, 0.6);
        return vec3(xz.x, -0.5 * dot(xz, xz), xz.y);
    }
    if (kind < 1.5) {
        vec3 jitter = vec3(sin(t * 2.6 + world.x * 1.9 + world.y * 1.3),
                0.5 * sin(t * 2.1 + world.y * 1.7 + world.z * 1.1),
                sin(t * 3.1 + world.z * 2.1 + world.x * 0.9));
        vec3 drift = vec3(dir.x, 0.0, dir.y) * (wave * 0.02 + 0.012 * gust);
        return clamp((jitter * 0.022 * gust + drift) * s * w, vec3(-0.12), vec3(0.12));
    }
    float swing = 0.05 * gust + 0.08 * wave;
    vec2 xz = windblown_limit((dir * swing + side * 0.04 * sin(t * 1.7 + world.y * 0.9 + across * 0.4)) * s * w, 0.5);
    return vec3(xz.x, 0.0, xz.y);
}

#else

vec3 windblown_offset(vec3 pos, float a, float sky) {
    return vec3(0.0);
}

#endif

#endif
