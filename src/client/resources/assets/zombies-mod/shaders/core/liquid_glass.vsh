#version 150

// 顶点直接给 NDC 坐标（CPU 端按面板矩形算好），这里透传即可，不需要投影矩阵。
in vec3 Position;

void main() {
    gl_Position = vec4(Position, 1.0);
}
