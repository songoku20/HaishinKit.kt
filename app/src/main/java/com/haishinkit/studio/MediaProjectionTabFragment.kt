package com.haishinkit.studio

import android.annotation.SuppressLint
import android.util.Log
import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.haishinkit.rtmp.RTMPConnection
import com.haishinkit.rtmp.RTMPStream

class MediaProjectionTabFragment : Fragment(), ServiceConnection {
    private var messenger: Messenger? = null

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_mediaprojection, container, false)
        val button = v.findViewById<Button>(R.id.button)
        MediaProjectionService.listener = object : RTMPStream.Listener {
            override fun onStatics(stream: RTMPStream, connection: RTMPConnection) {
                activity.runOnUiThread {
                    v.findViewById<TextView>(R.id.fps).text = "${stream.currentFPS}FPS"
                }
            }
        }
        button.setOnClickListener {
            if (button.text == "Publish") {
                if (messenger == null) {
                    if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
                        val mediaProjectionManager = activity.getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CAPTURE)
                    }
                } else {
                    messenger?.send(Message.obtain(null, 0))
                }
                button.text = "Stop"
            } else {
                messenger?.send(Message.obtain(null, 1))
                button.text = "Publish"
            }
        }
        return v
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        activity.windowManager.defaultDisplay.getMetrics(MediaProjectionService.metrics)
        if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
            if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
                MediaProjectionService.data = data
                Intent(activity, MediaProjectionService::class.java).also { intent ->
                    if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
                        activity.startForegroundService(intent)
                    } else {
                        activity.startService(intent)
                    }
                    activity.bindService(intent, this@MediaProjectionTabFragment, Context.BIND_AUTO_CREATE)
                }
                Log.i(toString(), "mediaProjectionManager success")
            }
        }
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        messenger = Messenger(binder)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        messenger = null
    }

    companion object {
        private const val REQUEST_CAPTURE = 1

        fun newInstance(): MediaProjectionTabFragment {
            return MediaProjectionTabFragment()
        }
    }
}
