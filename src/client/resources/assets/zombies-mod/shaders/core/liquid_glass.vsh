#version 330
#extension GL_ARB_separate_shader_objects : require

// 顶点直接给 NDC 坐标（CPU 端按面板矩形算好），这里透传即可，不需要投影矩阵。
layout(location = 0) in vec3 Position;

void main() {
    gl_Position = vec4(Position, 1.0);
}
