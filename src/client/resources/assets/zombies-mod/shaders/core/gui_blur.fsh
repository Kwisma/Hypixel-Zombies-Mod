#version 330

#moj_import <minecraft:projection.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColor;
out vec4 fragColor;

#ifndef BLUR_RADIUS
#define BLUR_RADIUS 1
#endif

void main() {
    vec2 textureSizePx = vec2(textureSize(Sampler0, 0));
    vec2 uv = gl_FragCoord.xy / textureSizePx;

    // ProjMat[0][0] = 2/guiWidth。由此把 API 的 GUI 像素半径换算成物理像素。
    float guiWidth = 2.0 / max(abs(ProjMat[0][0]), 0.00001);
    float guiScale = textureSizePx.x / max(guiWidth, 1.0);
    vec2 oneGuiPixel = vec2(guiScale) / textureSizePx;

    // 完整二维高斯卷积。sigma 取 radius/2.5，使核边缘仍有轻微权重，
    // radius=10 时连续采样 21x21 像素，不再出现稀疏采样造成的重影/分层。
    const int radius = BLUR_RADIUS;
    float sigma = max(float(radius) / 2.5, 0.5);
    float denominator = 2.0 * sigma * sigma;

    vec3 accumulated = vec3(0.0);
    float totalWeight = 0.0;

    for (int offsetY = -radius; offsetY <= radius; ++offsetY) {
        for (int offsetX = -radius; offsetX <= radius; ++offsetX) {
            vec2 offset = vec2(float(offsetX), float(offsetY));
            float weight = exp(-dot(offset, offset) / denominator);
            accumulated += texture(Sampler0, uv + offset * oneGuiPixel).rgb * weight;
            totalWeight += weight;
        }
    }

    fragColor = vec4(accumulated / max(totalWeight, 0.00001), 1.0);
}
