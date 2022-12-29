package socks;

import lombok.Setter;

import java.nio.ByteBuffer;

public final class SocksResponse {
    private static final int RESPONSE_LENGTH = 10;
    private static final byte VERSION = 0x05;
    private static final byte ADDRESS_TYPE = 0x01;
    private @Setter byte reply = 0x00;
    private @Setter byte[] boundIp4Address;
    private @Setter short boundPort;

    public ByteBuffer toByteBuffer(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(RESPONSE_LENGTH);
        byteBuffer
                .put(VERSION)
                .put(reply)
                .put((byte) 0x00)
                .put(ADDRESS_TYPE)
                .put(boundIp4Address)
                .putShort(boundPort);
        byteBuffer.flip();
        return byteBuffer;
    }

    public ByteBuffer toByteBufferWithoutAddress(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(RESPONSE_LENGTH);
        byteBuffer
                .put(VERSION)
                .put(reply)
                .put((byte) 0x00);
        byteBuffer.flip();
        return byteBuffer;
    }
}
