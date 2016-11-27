package com.haishinkit.rtmp.messages;

import com.haishinkit.amf.AMF0Deserializer;
import com.haishinkit.rtmp.RTMPConnection;
import com.haishinkit.rtmp.RTMPObjectEncoding;
import com.haishinkit.rtmp.RTMPSocket;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class RTMPDataMessage extends RTMPMessage {
    private final RTMPObjectEncoding objectEncoding;
    private String handlerName = null;
    private List<Object> arguments = new ArrayList<Object>();

    public RTMPDataMessage(final RTMPObjectEncoding objectEncoding) {
        super(objectEncoding.getDataType());
        this.objectEncoding = objectEncoding;
    }

    public RTMPObjectEncoding getObjectEncoding() {
        return objectEncoding;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public RTMPDataMessage setHandlerName(final String handlerName) {
        this.handlerName = handlerName;
        return this;
    }

    public List<Object> getArguments() {
        return arguments;
    }

    public RTMPDataMessage setArguments(final List<Object> arguments) {
        this.arguments = arguments;
        return this;
    }

    @Override
    public RTMPMessage decode(final RTMPSocket socket, final ByteBuffer buffer) {
        int eom = buffer.position() + getLength();
        AMF0Deserializer deserializer = new AMF0Deserializer(buffer);
        setHandlerName(deserializer.getString());
        List<Object> arguments = getArguments();
        while (buffer.position() < eom) {
            arguments.add(deserializer.getObject());
        }
        return this;
    }

    @Override
    public RTMPMessage execute(final RTMPConnection connection) {
        return this;
    }
}
