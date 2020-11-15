package com.haishinkit.studio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.support.v4.app.Fragment
import android.widget.Button
import com.haishinkit.rtmp.RTMPConnection
import com.haishinkit.rtmp.RTMPStream
import com.haishinkit.events.IEventListener
import com.haishinkit.media.CameraSource
import com.haishinkit.events.Event
import com.haishinkit.events.EventUtils
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.support.v4.content.ContextCompat
import android.util.Log
import com.haishinkit.media.AudioRecordSource
import com.haishinkit.view.GlHkView

class CameraTabFragment: Fragment(), IEventListener {
    private lateinit var connection: RTMPConnection
    private lateinit var stream: RTMPStream
    private lateinit var cameraView: GlHkView

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(javaClass.name, "onCreate")
        super.onCreate(savedInstanceState)
        val permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), 1)
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
        connection = RTMPConnection()
        stream = RTMPStream(connection)
        stream.attachAudio(AudioRecordSource())

        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val camera = CameraSource(activity).apply {
            this.open(cameraId)
        }
        stream.attachCamera(camera)
        connection.addEventListener(Event.RTMP_STATUS, this)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_camera, container, false)
        val button = v.findViewById<Button>(R.id.button)
        button.setOnClickListener {
            if (button.text == "Publish") {
                connection.connect(Preference.shared.rtmpURL)
                button.text = "Stop"
            } else {
                connection.close()
                button.text = "Publish"
            }
        }
        cameraView = v.findViewById<GlHkView>(R.id.camera)
        cameraView.attachStream(stream)
        return v
    }

    override fun onDestroy() {
        super.onDestroy()
        connection.dispose()
    }

    override fun handleEvent(event: Event) {
        Log.i(javaClass.name + "#handleEvent", event.toString())
        val data = EventUtils.toMap(event)
        val code = data["code"].toString()
        if (code == RTMPConnection.Code.CONNECT_SUCCESS.rawValue) {
            stream.publish(Preference.shared.streamName)
        }
    }

    companion object {
        fun newInstance(): CameraTabFragment {
            return CameraTabFragment()
        }
    }
}
