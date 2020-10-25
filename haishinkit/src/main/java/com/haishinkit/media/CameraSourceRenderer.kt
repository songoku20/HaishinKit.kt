package com.haishinkit.media

import android.graphics.Point
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.Surface
import com.haishinkit.gles.GlUtil
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal class CameraSourceRenderer(private val source: CameraSource) : GLSurfaceView.Renderer {
    var texture: SurfaceTexture? = null
    var textureId: Int = -1

    private var invalidateSurface: Boolean = false
    private val vertexBuffer = GlUtil.createFloatBuffer(VERTECES)
    private val texCoordBuffer = GlUtil.createFloatBuffer(TEX_COORDS_ROTATION_0)
    private var program = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureHandle = 0

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        val textures = intArrayOf(0)
        gl.glGenTextures(1, textures, 0)
        textureId = textures[0]
        texture = SurfaceTexture(textures[0]).apply {
            setDefaultBufferSize(640, 480)
        }
        source.surface = Surface(texture)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE)

        program = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(program, "position")
        GLES20.glEnableVertexAttribArray(positionHandle)
        texCoordHandle = GLES20.glGetAttribLocation(program, "texcoord")
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        textureHandle = GLES20.glGetAttribLocation(program, "texture")

        source.startRunning()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        invalidateSurface = true
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        configuration()
        texture?.updateTexImage()

        GLES20.glUseProgram(program)

        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GlUtil.checkGlError("glVertexAttribPointer")

        GLES20.glUniform1i(textureHandle, 0)
        GlUtil.checkGlError("glUniform1i")

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GlUtil.checkGlError("glBindTexture")

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glUseProgram(0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    private fun configuration() {
        if (!invalidateSurface) {
            return
        }
        when (source.orientation) {
            CameraSource.ROTATION_0 -> texCoordBuffer.put(TEX_COORDS_ROTATION_0)
            CameraSource.ROTATION_90 -> texCoordBuffer.put(TEX_COORDS_ROTATION_90)
            CameraSource.ROTATION_180 -> texCoordBuffer.put(TEX_COORDS_ROTATION_180)
            CameraSource.ROTATION_270 -> texCoordBuffer.put(TEX_COORDS_ROTATION_270)
        }
        texCoordBuffer.position(0)
        val point = Point()
        source.getSize(point)
        val textureSize = source.size
        val origin = Point(
            (point.x - textureSize.width) / 2,
            (point.y - textureSize.height) / 2
        )
        GLES20.glViewport(origin.x, origin.y, textureSize.width, textureSize.height)
        invalidateSurface = false
    }

    companion object {
        private val VERTECES = floatArrayOf(
            -1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
            1.0f, -1.0f, 0.0f
        )
        private val TEX_COORDS_ROTATION_0 = floatArrayOf(
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
        )
        private val TEX_COORDS_ROTATION_90 = floatArrayOf(
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        )
        private val TEX_COORDS_ROTATION_180 = floatArrayOf(
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            0.0f, 0.0f
        )
        private val TEX_COORDS_ROTATION_270 = floatArrayOf(
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
        )
        private const val VERTEX_SHADER = "attribute vec4 position;\n" +
            "attribute vec2 texcoord;\n" +
            "varying vec2 texcoordVarying;\n" +
            "void main() {\n" +
            "    gl_Position = position;\n" +
            "    texcoordVarying = texcoord;\n" +
            "}\n"
        private const val FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 texcoordVarying;\n" +
            "uniform samplerExternalOES texture;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(texture, texcoordVarying);\n" +
            "}\n"
        private val TAG = CameraSourceRenderer::class.java.simpleName
    }
}
