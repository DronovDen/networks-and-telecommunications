import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;
import handlers.AcceptHandler;
import handlers.Handler;
import models.Connection;
import models.DnsService;

@RequiredArgsConstructor
public final class Proxy {
    private static final Logger logger = Logger.getLogger(Proxy.class);

    private final int port;

    public void start() {
        try (Selector selector = Selector.open(); ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, new AcceptHandler(serverSocketChannel));

            DatagramChannel dnsChannel = DatagramChannel.open();
            dnsChannel.configureBlocking(false);

            DnsService dnsService = DnsService.getInstance();
            dnsService.setDatagramChannel(dnsChannel);
            dnsService.registerSelector(selector);

            run(selector);
        }
        catch (IOException exception) {
            logger.error(exception);
        }
    }

    private void run(Selector selector) throws IOException {
        while (!Thread.interrupted()) {
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey readyKey = iterator.next();
                try {
                    iterator.remove();
                    if (readyKey.isValid()) {
                        handleSelectionKey(readyKey);
                    }
                }
                catch (IOException exception) {
                    logger.error(exception);
                    closeConnection(readyKey);
                }
                catch (CancelledKeyException ignored) {

                }
            }
        }
    }

    private void handleSelectionKey(SelectionKey selectionKey) throws IOException {
        Handler handler = (Handler) selectionKey.attachment();
        if (selectionKey.isWritable()) {
            handler.write(selectionKey);
        }
        if (selectionKey.readyOps() != SelectionKey.OP_WRITE) {
            handler.handle(selectionKey);
        }
        /*if (selectionKey.isValid() && selectionKey.readyOps() != SelectionKey.OP_WRITE) {
            handler.handle(selectionKey);
        }*/
    }

    private void closeConnection(SelectionKey selectionKey) throws IOException {
        Handler handler = (Handler) selectionKey.attachment();
        Connection connection = handler.getConnection();
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        try {
            logger.debug("Socket closed: " + socketChannel.getRemoteAddress());
            socketChannel.close();
            connection.closeAssociate();
        }
        catch (ClosedChannelException exception) {
            logger.error(exception);
        }
    }
}
