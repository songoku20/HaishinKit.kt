package com.haishinkit.rtmp.messages

import com.haishinkit.rtmp.RTMPConnection
import com.haishinkit.rtmp.RTMPSocket
import java.nio.ByteBuffer

/**
 * 5.4.1. Set Chunk Size (1)
 */
internal class RTMPSetChunkSizeMessage : RTMPMessage(RTMPMessage.Type.CHUNK_SIZE) {
    var size: Int = 0

    override fun encode(socket: RTMPSocket): ByteBuffer {
        val buffer = ByteBuffer.allocate(CAPACITY)
        buffer.putInt(size)
        return buffer
    }

    override fun decode(buffer: ByteBuffer): RTMPMessage {
        size = buffer.int
        return this
    }

    override fun execute(connection: RTMPConnection): RTMPMessage {
        connection.socket.chunkSizeC = size
        return this
    }

    companion object {
        private const val CAPACITY = 4
    }
}
