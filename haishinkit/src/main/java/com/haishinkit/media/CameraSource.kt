package com.haishinkit.media

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.MediaCodecInfo
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import com.haishinkit.BuildConfig
import com.haishinkit.codec.MediaCodec
import com.haishinkit.rtmp.RTMPStream
import org.apache.commons.lang3.builder.ToStringBuilder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A video source that captures a camera by the Camera2 API.
 */
class CameraSource(private val activity: Activity) : VideoSource {
    var device: CameraDevice? = null
        private set(value) {
            device?.close()
            field = value
            startRunning()
        }
    var cameraId: String = DEFAULT_CAMERA_ID
        private set
    var characteristics: CameraCharacteristics? = null
        private set
    var session: CameraCaptureSession? = null
        private set(value) {
            session?.close()
            field = value
            if (value == null) {
                stream?.renderer?.stopRunning()
            } else {
                stream?.renderer?.startRunning()
            }
        }
    internal var surface: Surface? = null
    override var stream: RTMPStream? = null
        set(value) {
            field = value
            stream?.videoCodec?.callback = MediaCodec.Callback()
            stream?.videoCodec?.colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        }
    override val isRunning = AtomicBoolean(false)
    override var resolution = Size(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        set(value) {
            field = value
            stream?.videoSetting?.width = value.width
            stream?.videoSetting?.height = value.height
        }
    private var request: CaptureRequest.Builder? = null
    private var isPortraitMode: Boolean = false
    private var manager: CameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val backgroundHandler by lazy {
        val thread = HandlerThread(javaClass.name)
        thread.start()
        Handler(thread.looper)
    }
    val orientation: Int
        get() {
            return when (activity.windowManager.defaultDisplay.rotation) {
                Surface.ROTATION_0 -> if (isPortraitMode) ROTATION_270 else ROTATION_0
                Surface.ROTATION_90 -> if (isPortraitMode) ROTATION_0 else ROTATION_90
                Surface.ROTATION_180 -> if (isPortraitMode) ROTATION_90 else ROTATION_180
                Surface.ROTATION_270 -> if (isPortraitMode) ROTATION_180 else ROTATION_270
                else -> 0
            }
        }

    var mCameraSize = Size(640, 480)
    val size: Size
        get() {
            val displaySize = Point()
            activity.windowManager.defaultDisplay.getSize(displaySize)
            return if (displaySize.x > displaySize.y) {
                val scale = displaySize.y.toDouble() / mCameraSize.height.toDouble()
                Size((scale * mCameraSize.width).toInt(), (scale * mCameraSize.height).toInt())
            } else {
                val scale = displaySize.x.toDouble() / mCameraSize.height.toDouble()
                Size((scale * mCameraSize.height).toInt(), (scale * mCameraSize.width).toInt())
            }
        }

    init {
        val orientation = activity.windowManager.defaultDisplay.rotation
        isPortraitMode = if (activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            (orientation == Surface.ROTATION_0 || orientation == Surface.ROTATION_180)
        } else {
            (orientation == Surface.ROTATION_90 || orientation == Surface.ROTATION_270)
        }
    }

    @SuppressLint("MissingPermission")
    fun open(cameraId: String) {
        this.cameraId = cameraId
        characteristics = manager.getCameraCharacteristics(cameraId)
        manager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    this@CameraSource.device = camera
                    this@CameraSource.setUp()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    this@CameraSource.device = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.w(TAG, error.toString())
                }
            },
            null
        )
    }

    override fun setUp() {
        stream?.renderer?.startRunning()
    }

    override fun tearDown() {
        request = null
        session = null
        device = null
    }

    override fun startRunning() {
        Log.d(TAG, "${this::startRunning.name}: $device, $surface")
        if (isRunning.get()) { return }
        val device = device ?: return
        val surface = surface ?: return
        request = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            this.addTarget(surface)
        }
        val surfaceList = mutableListOf<Surface>(surface)
        device.createCaptureSession(
            surfaceList,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    this@CameraSource.session = session
                    val request = request ?: return
                    session.setRepeatingRequest(request.build(), null, backgroundHandler)
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    this@CameraSource.session = null
                }
            },
            backgroundHandler
        )
        isRunning.set(true)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, this::startRunning.name)
        }
    }

    override fun stopRunning() {
        if (!isRunning.get()) { return }
        isRunning.set(false)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, this::startRunning.name)
        }
    }

    override fun createGLSurfaceViewRenderer(): GLSurfaceView.Renderer? {
        return CameraSourceRenderer(this)
    }

    override fun toString(): String {
        return ToStringBuilder.reflectionToString(this)
    }

    internal fun getSize(outSize: Point) {
        activity.windowManager.defaultDisplay.getSize(outSize)
    }

    companion object {
        const val DEFAULT_WIDTH: Int = 640
        const val DEFAULT_HEIGHT: Int = 480

        const val ROTATION_0 = 0
        const val ROTATION_90 = 1
        const val ROTATION_180 = 2
        const val ROTATION_270 = 3

        private const val DEFAULT_CAMERA_ID: String = "0"
        private const val MAX_PREVIEW_WIDTH: Int = 1920
        private const val MAX_PREVIEW_HEIGHT: Int = 1080

        private val TAG = CameraSource::class.java.simpleName
    }
}
