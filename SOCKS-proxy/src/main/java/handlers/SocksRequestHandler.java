package handlers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import models.DnsService;
import models.Connection;
import socks.SocksParser;
import socks.SocksRequest;
import socks.SocksResponse;

public final class SocksRequestHandler extends SocksHandler {
    private static final byte DOMAIN_NAME_TYPE = 0x03;
    private static final int NO_ERROR = 0;

    public SocksRequestHandler(Connection connection) {
        super(connection);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        ByteBuffer outputBuffer = getConnection().getOutputBuffer().getByteBuffer();

        read(selectionKey);
        SocksRequest request = SocksParser.parseRequest(outputBuffer);

        if (null == request) {
            return;
        }

        byte parseError = request.getParseError();

        if (NO_ERROR != parseError) {
            onError(selectionKey, parseError);
            return;
        }

        if (DOMAIN_NAME_TYPE == request.getAddressType()) {
            DnsService dnsService = DnsService.getInstance();
            dnsService.resolveName(request, selectionKey);
            return;
        }

        ConnectHandler.connectToTarget(selectionKey, request.getAddress());
    }

    public static void onError(SelectionKey selectionKey, byte error) {
        Handler handler = (Handler) selectionKey.attachment();
        Connection connection = handler.getConnection();
        putErrorResponseIntoBuf(selectionKey, connection, error);
        selectionKey.attach(new SocksErrorHandler(connection));
    }

    public static void putErrorResponseIntoBuf(SelectionKey selectionKey, Connection connection, byte error) {
        SocksResponse response = new SocksResponse();
        response.setReply(error);
        ByteBuffer inputBuff = connection.getInputBuffer().getByteBuffer();
        inputBuff.put(response.toByteBufferWithoutAddress());
        connection.getOutputBuffer().getByteBuffer().clear();
        selectionKey.interestOpsOr(SelectionKey.OP_WRITE);
    }
}
