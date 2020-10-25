package com.haishinkit.media

import android.opengl.GLSurfaceView
import android.util.Size

/**
 * An interface that captures a video source.
 */
interface VideoSource : Source {
    var resolution: Size

    fun createGLSurfaceViewRenderer(): GLSurfaceView.Renderer? {
        return null
    }
}
