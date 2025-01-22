#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform float Radius;

out vec4 fragColor;

void main() {
    vec4 maxVal = texture(DiffuseSampler, texCoord);
    float radiusSquared = Radius * Radius;

    for(float u = 0.0; u <= Radius; u += 1.0) {
        float vMax = sqrt(radiusSquared - u * u);
        for(float v = 0.0; v <= vMax; v += 1.0) {
            if(u == 0.0 && v == 0.0) continue;

            vec2 offset = vec2(u, v) * oneTexel;

            // Sample just once per position and maximize
            maxVal = max(maxVal, texture(DiffuseSampler, texCoord + offset));
            maxVal = max(maxVal, texture(DiffuseSampler, texCoord - offset));
            if(u > 0.0) {
                maxVal = max(maxVal, texture(DiffuseSampler, texCoord + vec2(-offset.x, offset.y)));
                maxVal = max(maxVal, texture(DiffuseSampler, texCoord + vec2(offset.x, -offset.y)));
            }
        }
    }

    fragColor = vec4(maxVal.rgb, 1.0);
}