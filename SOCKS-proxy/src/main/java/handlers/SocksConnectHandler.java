package handlers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import models.Connection;
import socks.SocksConnectRequest;
import socks.SocksConnectResponse;
import socks.SocksParser;

public final class SocksConnectHandler extends SocksHandler {
    private static final byte NO_AUTHENTICATION = 0x00;
    private static final int SOCKS_VERSION = 0x05;
    private static final byte NO_COMPARABLE_METHOD = (byte) 0xFF;

    public SocksConnectHandler(Connection connection) {
        super(connection);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        Connection connection = getConnection();
        ByteBuffer outputBuffer = connection.getOutputBuffer().getByteBuffer();
        read(selectionKey);
        SocksConnectRequest connectRequest = SocksParser.parseConnect(outputBuffer);

        if (null == connectRequest) {
            return;
        }

        SocksConnectResponse connectResponse = new SocksConnectResponse();

        if (!checkRequest(connectRequest)) {
            connectResponse.setMethod(NO_COMPARABLE_METHOD);
        }

        ByteBuffer inputBuffer = connection.getInputBuffer().getByteBuffer();
        inputBuffer.put(connectResponse.toByteArray());
        selectionKey.interestOpsOr(SelectionKey.OP_WRITE);
        selectionKey.attach(new SocksRequestHandler(connection));
        connection.getOutputBuffer().getByteBuffer().clear();
    }

    private boolean checkRequest(SocksConnectRequest connectRequest) {
        return (SOCKS_VERSION == connectRequest.getVersion()) && checkMethods(connectRequest.getMethods());
    }

    private static boolean checkMethods(byte[] methods) {
        for (byte method : methods) {
            if (NO_AUTHENTICATION == method) {
                return true;
            }
        }
        return false;
    }
}
