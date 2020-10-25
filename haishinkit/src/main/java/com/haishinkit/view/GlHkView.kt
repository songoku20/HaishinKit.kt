package com.haishinkit.view

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import com.haishinkit.BuildConfig
import com.haishinkit.rtmp.RTMPStream
import org.apache.commons.lang3.builder.ToStringBuilder
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * A view that displays a video content of a NetStream object which uses OpenGL api.
 */
class GlHkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : GLSurfaceView(context, attrs), NetStreamView {
    internal class StrategyRenderer : GLSurfaceView.Renderer {
        var stream: RTMPStream? = null
        var strategy: GLSurfaceView.Renderer? = null

        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "${this::onSurfaceCreated.name}: $strategy")
            }
            strategy?.onSurfaceCreated(gl, config)
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            strategy?.onSurfaceChanged(gl, width, height)
        }

        override fun onDrawFrame(gl: GL10) {
            strategy?.onDrawFrame(gl)
            stream?.videoCodec?.onAvailableFrame()
        }

        companion object {
            private val TAG = StrategyRenderer::class.java.simpleName
        }
    }

    override val isRunning: AtomicBoolean = AtomicBoolean(false)
    override var stream: RTMPStream? = null
    private val renderer: StrategyRenderer by lazy {
        StrategyRenderer()
    }

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
    }

    override fun startRunning() {
        if (isRunning.get()) { return }
        renderer.stream = stream
        renderer.strategy = stream?.video?.createGLSurfaceViewRenderer()
        isRunning.set(true)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, this::startRunning.name)
        }
    }

    override fun stopRunning() {
        if (!isRunning.get()) { return }
        renderer.strategy = null
        isRunning.set(false)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, this::stopRunning.name)
        }
    }

    override fun toString(): String {
        return ToStringBuilder.reflectionToString(this)
    }

    companion object {
        private val TAG = GlHkView::class.java.simpleName
    }
}
