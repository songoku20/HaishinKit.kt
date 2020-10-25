package com.haishinkit.gles

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLExt
import android.opengl.EGLExt.EGL_RECORDABLE_ANDROID
import android.util.Log
import android.view.Surface

internal class GlWindowSurface {
    private var initilized: Boolean = false
    private var display = EGL14.EGL_NO_DISPLAY
    private var context = EGL14.EGL_NO_CONTEXT
    private var surface = EGL14.EGL_NO_SURFACE

    fun makeCurrent() {
        if (!initilized) {
            return
        }
        if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
            Log.e(TAG, "eglMakeCurrent failed.")
        }
    }

    fun swapBuffers() {
        if (!initilized) {
            return
        }
        EGL14.eglSwapBuffers(display, surface)
    }

    fun setPresentationTime(timestamp: Long) {
        if (!initilized) {
            return
        }
        EGLExt.eglPresentationTimeANDROID(display, surface, timestamp)
        GlUtil.checkGlError("eglPresentationTimeANDROID")
    }

    fun setUp(width: Int, height: Int, surface: Surface?, eglSharedContext: EGLContext?) {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException()
        }

        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
            throw RuntimeException()
        }

        val attribList: IntArray = if (eglSharedContext != null) {
            intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            )
        } else {
            intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
            )
        }

        val configs: Array<EGLConfig?> = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(display, attribList, 0, configs, 0, configs.size, numConfigs, 0)
        GlUtil.checkGlError("eglCreateContext RGB888+recordable ES2")

        context = EGL14.eglCreateContext(
            display, configs[0],
            eglSharedContext ?: EGL14.EGL_NO_CONTEXT, CONTEXT_ATTRIBUTES, 0
        )
        GlUtil.checkGlError("eglCreateContext")

        this.surface = EGL14.eglCreateWindowSurface(display, configs[0], surface, SURFACE_ATTRIBUTES, 0)
        GlUtil.checkGlError("eglCreateWindowSurface")

        initilized = true
    }

    fun tearDown() {
        if (!initilized) {
            return
        }
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(display, surface)
        EGL14.eglDestroyContext(display, context)
        EGL14.eglReleaseThread()
        EGL14.eglTerminate(display)
        display = EGL14.EGL_NO_DISPLAY
        context = EGL14.EGL_NO_CONTEXT
        surface = EGL14.EGL_NO_SURFACE
        initilized = false
    }

    companion object {
        private val TAG = GlWindowSurface::class.java.simpleName
        private val CONTEXT_ATTRIBUTES = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        private val SURFACE_ATTRIBUTES = intArrayOf(EGL14.EGL_NONE)
        private const val EGL_RECORDABLE_ANDROID: Int = 0x3142
    }
}
