#version 150

// 移植自 Jacquesqwq/LiquidGlassShader 的 liquidGlassV2Clear.frag（+tinted 的色散）。
// 关键：超椭圆 SDF + “朝面板中心收缩”的透镜折射（中心 1:1 通透，边缘放大/挤压）。

uniform sampler2D Sampler0;   // 屏幕快照

layout(std140) uniform LiquidGlass {
    vec2  uScreenSize;
    vec2  _pad0;
    vec4  uRect;             // x, y, w, h（像素，左上原点）
    vec4  uTint;             // rgb + 强度
    float uPower;            // 超椭圆指数 n（≈4 方圆，越大越方）   —— Java 的 radius 槽
    float uNoise;            // grain 噪点                          —— Java 的 blur 槽
    float uRefractionPower;  // 折射幂（越大边缘放大越强）          —— Java 的 refraction 槽
    float uChroma;           // 色散（uv 单位，很小）               —— Java 的 dispersion 槽
    float uGlow;             // 描边光强                            —— Java 的 edge 槽
    float uAlpha;
    float _pad1;
    float _pad2;
};

out vec4 fragColor;

const float M_E = 2.718281828459045;

float rand(vec2 co) { return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453); }

// 折射曲线：dist=0(边缘)→约0.646，dist 大(中心)→1.0
float f(float x) { return 1.0 - 2.3 * pow(5.2 * M_E, -6.9 * x - 0.7); }

// 超椭圆有符号距离（梯度归一化）：内部<0，边界≈0
float sdSuperellipse(vec2 p, float n, float r) {
    vec2 a = abs(p);
    float num = pow(a.x, n) + pow(a.y, n) - pow(r, n);
    float den = n * sqrt(pow(a.x, 2.0 * n - 2.0) + pow(a.y, 2.0 * n - 2.0)) + 0.00001;
    return num / den;
}

// 方向性描边光（按角度给一侧更亮）
float Glow(vec2 uv) {
    vec2 g = uv * 2.0 - 1.0;
    return sin(atan(g.y, g.x) - 0.5);
}

void main() {
    vec2 px = vec2(gl_FragCoord.x, uScreenSize.y - gl_FragCoord.y); // 左上原点
    vec2 localUV = (px - uRect.xy) / uRect.zw;   // 0..1 跨面板
    vec2 p = (localUV - 0.5) * 2.0;               // -1..1

    float d = sdSuperellipse(p, uPower, 1.0);
    float edge = 1.0 - smoothstep(-0.012, 0.012, d);  // 抗锯齿覆盖
    if (edge <= 0.0) discard;

    float dist = max(-d, 0.0);
    float refraction = pow(f(dist), uRefractionPower);

    // 透镜折射：采样朝面板中心收缩（中心 refraction≈1 → 1:1；边缘 <1 → 放大/挤压）
    vec2 midUV = vec2(uRect.x + uRect.z * 0.5,
                      uScreenSize.y - (uRect.y + uRect.w * 0.5)) / uScreenSize;
    vec2 baseUv = gl_FragCoord.xy / uScreenSize;
    vec2 offset = baseUv - midUV;
    vec2 sampleUV = midUV + offset * refraction;

    // 轻微色散（边缘更明显）
    float fres = pow(1.0 - clamp(dist, 0.0, 1.0), 3.0);
    vec2 chromaDir = normalize(offset + 1e-5);
    vec2 chroma = chromaDir * fres * uChroma;
    vec3 col = vec3(
        texture(Sampler0, sampleUV + chroma).r,
        texture(Sampler0, sampleUV).g,
        texture(Sampler0, sampleUV - chroma).b
    );

    // grain 噪点
    float noise = (rand(gl_FragCoord.xy * 1e-3) - 0.5) * uNoise;
    col += vec3(noise);

    // 染色（clear 时强度给 0）
    col = mix(col, uTint.rgb, clamp(uTint.a, 0.0, 1.0));

    // 方向性描边光：集中在边缘
    float glow = Glow(localUV);
    float rimBand = 1.0 - smoothstep(0.0, 0.25, dist);  // 1=边缘, 0=中心
    float glowStrength = glow * uGlow * rimBand + 1.0 + 0.02;
    col *= glowStrength;

    fragColor = vec4(col, uAlpha * edge);
}
