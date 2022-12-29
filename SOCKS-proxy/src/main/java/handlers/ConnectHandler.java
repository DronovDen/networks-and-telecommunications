package handlers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import models.Connection;
import socks.SocksResponse;

public final class ConnectHandler extends Handler {
    private static final int ANY_PORT = 0;

    public ConnectHandler(Connection connection) {
        super(connection);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        Handler handler = (Handler) selectionKey.attachment();
        Connection connection = handler.getConnection();
        socketChannel.finishConnect();
        selectionKey.attach(new ForwardHandler(connection));
        selectionKey.interestOpsAnd(~SelectionKey.OP_CONNECT);
        selectionKey.interestOpsOr(SelectionKey.OP_READ);
    }

    public static SocketChannel initTargetSocket(Connection clientConnection, SelectionKey selectionKey, InetSocketAddress targetAddress) throws IOException {
        SocketChannel targetSocket = SocketChannel.open();
        targetSocket.bind(new InetSocketAddress(ANY_PORT));
        targetSocket.configureBlocking(false);
        Connection targetConnection = new Connection(clientConnection.getOutputBuffer(), clientConnection.getInputBuffer());
        targetSocket.connect(targetAddress);
        ConnectHandler connectHandler = new ConnectHandler(targetConnection);
        clientConnection.setAssociate(targetSocket);
        targetConnection.setAssociate((SocketChannel) selectionKey.channel());
        SelectionKey key = targetSocket.register(selectionKey.selector(), SelectionKey.OP_CONNECT, connectHandler);
        targetConnection.registerBufferListener(() -> key.interestOpsOr(SelectionKey.OP_WRITE));
        return targetSocket;
    }

    public static void connectToTarget(SelectionKey clientKey, InetSocketAddress targetAddress) throws IOException {
        Handler handler = (Handler) clientKey.attachment();
        Connection clientConnection = handler.getConnection();
        SocketChannel targetSocketChannel = initTargetSocket(clientConnection, clientKey, targetAddress);
        putResponseIntoBuf(clientConnection, targetSocketChannel);
        clientKey.interestOpsOr(SelectionKey.OP_WRITE);
        clientKey.attach(new ForwardHandler(clientConnection));
        clientConnection.getOutputBuffer().getByteBuffer().clear();
    }

    private static void putResponseIntoBuf(Connection connection, SocketChannel socketChannel) throws IOException {
        InetSocketAddress socketAddress = (InetSocketAddress) socketChannel.getLocalAddress();
        SocksResponse response = new SocksResponse();
        byte[] address = InetAddress.getLocalHost().getAddress();
        response.setBoundIp4Address(address);
        response.setBoundPort((short) socketAddress.getPort());
        ByteBuffer inputBuff = connection.getInputBuffer().getByteBuffer();
        inputBuff.put(response.toByteBuffer());
    }
}
