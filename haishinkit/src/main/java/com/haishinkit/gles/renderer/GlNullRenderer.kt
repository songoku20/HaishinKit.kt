package com.haishinkit.gles.renderer

internal class GlNullRenderer: GlRenderer {
    override fun setUp() {
    }

    override fun tearDown() {
    }

    companion object {
        val instance = GlNullRenderer()
    }
}
