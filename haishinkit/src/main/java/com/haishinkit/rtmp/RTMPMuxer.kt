package com.haishinkit.rtmp

import android.media.MediaFormat
import com.haishinkit.codec.MediaCodec
import com.haishinkit.flv.AacPacketType
import com.haishinkit.flv.AvcPacketType
import com.haishinkit.flv.FlameType
import com.haishinkit.flv.VideoCodec
import com.haishinkit.iso.AVCConfigurationRecord
import com.haishinkit.iso.AVCFormatUtils
import com.haishinkit.iso.AudioSpecificConfig
import com.haishinkit.rtmp.messages.RTMPAACAudioMessage
import com.haishinkit.rtmp.messages.RTMPAVCVideoMessage
import com.haishinkit.rtmp.messages.RTMPMessage
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

internal class RTMPMuxer(private val stream: RTMPStream) : MediaCodec.Listener {
    private val timestamps = ConcurrentHashMap<String, Long>()
    private var audioConfig: AudioSpecificConfig? = null
    private var videoConfig: AVCConfigurationRecord? = null

    override fun onFormatChanged(mime: String, mediaFormat: MediaFormat) {
        var message: RTMPMessage? = null
        when (mime) {
            MediaCodec.MIME_VIDEO_AVC -> {
                videoConfig = AVCConfigurationRecord.create(mediaFormat)
                val video = stream.connection.messageFactory.createRTMPVideoMessage() as RTMPAVCVideoMessage
                video.packetType = AvcPacketType.SEQ.toByte()
                video.frame = FlameType.KEY
                video.codec = VideoCodec.AVC
                video.payload = videoConfig!!.toByteBuffer()
                video.chunkStreamID = RTMPChunk.VIDEO
                video.streamID = stream.id
                message = video
            }
            MediaCodec.MIME_AUDIO_MP4A -> {
                val buffer = mediaFormat.getByteBuffer("csd-0") ?: return
                audioConfig = AudioSpecificConfig.create(buffer)
                val audio = stream.connection.messageFactory.createRTMPAudioMessage() as RTMPAACAudioMessage
                audio.config = audioConfig
                audio.aacPacketType = AacPacketType.SEQ.toByte()
                audio.payload = buffer
                audio.chunkStreamID = RTMPChunk.AUDIO
                audio.streamID = stream.id
                message = audio
            }
        }
        if (message != null) {
            stream.connection.doOutput(RTMPChunk.ZERO, message)
        }
    }

    override fun onSampleOutput(mime: String, info: android.media.MediaCodec.BufferInfo, buffer: ByteBuffer) {
        if (info.flags and android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            return
        }
        var timestamp = 0
        var message: RTMPMessage? = null
        if (timestamps.containsKey(mime)) {
            timestamp = (info.presentationTimeUs - timestamps[mime]!!.toLong()).toInt()
        }
        when (mime) {
            MediaCodec.MIME_VIDEO_AVC -> {
                val keyframe = info.flags and android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME != 0
                val video = stream.connection.messageFactory.createRTMPVideoMessage() as RTMPAVCVideoMessage
                video.packetType = AvcPacketType.NAL
                video.frame = if (keyframe) FlameType.KEY else FlameType.INTER
                video.codec = VideoCodec.AVC
                video.payload = AVCFormatUtils.toNALFileFormat(buffer)
                video.chunkStreamID = RTMPChunk.VIDEO
                video.timestamp = timestamp / 1000
                video.streamID = stream.id
                message = video
                stream.frameCount.incrementAndGet()
            }
            MediaCodec.MIME_AUDIO_MP4A -> {
                val audio = stream.connection.messageFactory.createRTMPAudioMessage() as RTMPAACAudioMessage
                audio.aacPacketType = AacPacketType.RAW
                audio.config = audioConfig
                audio.payload = buffer
                audio.chunkStreamID = RTMPChunk.AUDIO
                audio.timestamp = timestamp / 1000
                audio.streamID = stream.id
                message = audio
            }
        }
        if (message != null) {
            stream.connection.doOutput(RTMPChunk.ONE, message)
        }
        timestamps[mime] = info.presentationTimeUs
    }

    fun clear() {
        timestamps.clear()
    }
}
