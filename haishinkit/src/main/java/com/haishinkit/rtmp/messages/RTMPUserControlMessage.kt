package com.haishinkit.rtmp.messages

import com.haishinkit.rtmp.RTMPChunk
import com.haishinkit.rtmp.RTMPConnection
import com.haishinkit.rtmp.RTMPSocket
import com.haishinkit.rtmp.RTMPStream
import java.nio.ByteBuffer

/**
 * 7.1.5. Video Message (9)
 */
internal class RTMPUserControlMessage : RTMPMessage(RTMPMessage.Type.USER) {
    enum class Event(val rawValue: Short) {
        STREAM_BEGIN(0x00),
        STREAM_EOF(0x01),
        STREAM_DRY(0x02),
        SET_BUFFER(0x03),
        RECORDED(0x04),
        PING(0x06),
        PONG(0x07),
        BUFFER_EMPTY(0x1F),
        BUFFER_FULL(0x20),
        UNKNOWN(Short.MAX_VALUE);
    }

    var event: Event = Event.UNKNOWN
        private set
    var value = 0
        private set

    override fun encode(socket: RTMPSocket): ByteBuffer {
        val buffer = ByteBuffer.allocate(CAPACITY)
        buffer.putShort(event.rawValue)
        buffer.putInt(value)
        return buffer
    }

    override fun decode(buffer: ByteBuffer): RTMPMessage {
        val e = buffer.short
        event = Event.values().first { n -> n.rawValue == e }
        value = buffer.int
        return this
    }

    override fun execute(connection: RTMPConnection): RTMPMessage {
        when (event) {
            RTMPUserControlMessage.Event.PING -> {
                var message = connection.messageFactory.createRTMPUserControlMessage()
                message.event = Event.PONG
                message.chunkStreamID = RTMPChunk.CONTROL
                connection.doOutput(RTMPChunk.ZERO, message)
            }
            RTMPUserControlMessage.Event.BUFFER_FULL,
            RTMPUserControlMessage.Event.BUFFER_EMPTY -> {
                val stream = connection.streams[value]
                if (stream != null) {
                    val data = if (event == Event.BUFFER_FULL)
                        RTMPStream.Code.BUFFER_FLUSH.data("")
                    else
                        RTMPStream.Code.BUFFER_EMPTY.data("")
                    stream.dispatchEventWith(com.haishinkit.events.Event.RTMP_STATUS, false, data)
                }
            }
            else -> {
            }
        }
        return this
    }

    companion object {
        private const val CAPACITY = 6
    }
}
