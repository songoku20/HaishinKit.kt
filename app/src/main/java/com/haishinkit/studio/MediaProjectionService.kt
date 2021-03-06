package com.haishinkit.studio

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.*
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.util.DisplayMetrics
import android.util.Log
import com.haishinkit.events.Event
import com.haishinkit.events.EventUtils
import com.haishinkit.events.IEventListener
import com.haishinkit.media.AudioRecordSource
import com.haishinkit.media.MediaProjectionSource
import com.haishinkit.rtmp.RTMPConnection
import com.haishinkit.rtmp.RTMPStream

class MediaProjectionService : Service(), IEventListener {
    private lateinit var stream: RTMPStream
    private lateinit var connection: RTMPConnection

    private var messenger: Messenger? = null
    private var handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0 -> connection.connect(Preference.shared.rtmpURL)
                1 -> connection.close()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return messenger?.binder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(javaClass.name, "onStartCommand")
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            channel.description = CHANNEL_DESC
            channel.setSound(null, null)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setColorized(true)
            setSmallIcon(R.mipmap.ic_launcher)
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
            setContentTitle(NOTIFY_TITLE)
        }.build()
        if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(ID, notification)
        }
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        stream.attachAudio(AudioRecordSource())
        stream.listener = listener
        data?.let {
            stream.attachCamera(MediaProjectionSource(mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, it), metrics))
        }
        connection.connect(Preference.shared.rtmpURL)
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        messenger = Messenger(handler)
        connection = RTMPConnection()
        connection.addEventListener(Event.RTMP_STATUS, this)
        stream = RTMPStream(connection)
    }

    override fun onDestroy() {
        super.onDestroy()
        connection.dispose()
    }

    override fun handleEvent(event: Event) {
        Log.i(javaClass.name, event.toString())
        val data = EventUtils.toMap(event)
        val code = data["code"].toString()
        if (code == RTMPConnection.Code.CONNECT_SUCCESS.rawValue) {
            stream.publish(Preference.shared.streamName)
        }
    }

    companion object {
        const val ID = 1
        const val CHANNEL_ID = "MediaProjectionID"
        const val CHANNEL_NAME = "MediaProjectionService"
        const val CHANNEL_DESC = ""
        const val NOTIFY_TITLE = "Recording."

        var metrics = DisplayMetrics()
        var data: Intent? = null
        var listener: RTMPStream.Listener? = null
    }
}
