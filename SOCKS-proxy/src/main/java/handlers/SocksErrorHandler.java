package handlers;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import models.Connection;

public final class SocksErrorHandler extends Handler {
    public SocksErrorHandler(Connection connection) {
        super(connection);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {

    }

    @Override
    public int write(SelectionKey selectionKey) throws IOException {
        int remaining = super.write(selectionKey);
        if (0 == remaining) {
            SocketChannel socket = (SocketChannel) selectionKey.channel();
            socket.close();
        }
        return remaining;
    }
}
