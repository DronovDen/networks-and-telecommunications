package handlers;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;
import models.Connection;

public final class AcceptHandler extends Handler {
    private static final Logger logger = Logger.getLogger(AcceptHandler.class);

    private final ServerSocketChannel serverSocketChannel;

    public AcceptHandler(ServerSocketChannel serverSocketChannel) {
        super(null);
        this.serverSocketChannel = serverSocketChannel;
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = this.serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        Connection connection = new Connection(getBufSize());
        SocksConnectHandler connectHandler = new SocksConnectHandler(connection);
        // Регаем на том же старом селекторе новый ключ, на котором будем готовы читать запрос на connect
        // и обрабатываем его соответствующим handler-ом
        SelectionKey key = socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ, connectHandler);
        connection.registerBufferListener(() -> key.interestOpsOr(SelectionKey.OP_WRITE));
        logger.debug("New connection: " + socketChannel.getRemoteAddress());
    }
}
