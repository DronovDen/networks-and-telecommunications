package socks;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class SocksParser {
    private static final byte WRONG_ADDRESS_TYPE = 0x08;
    private static final byte WRONG_COMMAND = 0x07;
    private static final int IPV4 = 0x01;
    private static final int DOMAIN_NAME = 0x03;
    private static final int CONNECT_COMMAND = 0x01;

    public static SocksConnectRequest parseConnect(ByteBuffer byteBuffer) {
        try {
            byteBuffer.flip();
            SocksConnectRequest connect = new SocksConnectRequest();
            connect.setVersion(byteBuffer.get());
            connect.setNumOfMethods(byteBuffer.get());
            byteBuffer.get(connect.getMethods());
            return connect;
        }
        catch (BufferUnderflowException exception) {
            prepareBufferToWrite(byteBuffer);
            return null;
        }
    }

    public static SocksRequest parseRequest(ByteBuffer byteBuffer) {
        try {
            SocksRequest request = new SocksRequest();
            byteBuffer.flip();
            request.setVersion(byteBuffer.get());
            byte command = byteBuffer.get();

            if (CONNECT_COMMAND != command) {
                request.setParseError(WRONG_COMMAND);
            }

            request.setCommand(command);
            byteBuffer.get();
            checkAddressType(byteBuffer.get(), byteBuffer, request);
            request.setTargetPort(byteBuffer.getShort());
            return request;
        }
        catch (BufferUnderflowException exception) {
            prepareBufferToWrite(byteBuffer);
            return null;
        }
    }

    private static void prepareBufferToWrite(ByteBuffer byteBuffer) {
        int newStartPos = byteBuffer.limit();
        byteBuffer.clear();
        byteBuffer.position(newStartPos);
    }

    private static void checkAddressType(byte addressType, ByteBuffer byteBuffer, SocksRequest request) {
        request.setAddressType(addressType);
        switch (addressType) {
            case IPV4 -> byteBuffer.get(request.getIp4Address());
            case DOMAIN_NAME -> request.setDomainName(getDomainName(byteBuffer));
            default -> request.setParseError(WRONG_ADDRESS_TYPE);
        }
    }

    private static String getDomainName(ByteBuffer byteBuffer) {
        byte nameLength = byteBuffer.get();
        byte[] nameBytes = new byte[nameLength];
        byteBuffer.get(nameBytes);
        return new String(nameBytes, StandardCharsets.UTF_8);
    }
}
