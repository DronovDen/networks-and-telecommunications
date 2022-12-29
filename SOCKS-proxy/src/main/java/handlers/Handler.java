package handlers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;
import models.Connection;

@RequiredArgsConstructor
public abstract class Handler {
    private static final Logger logger = Logger.getLogger(Handler.class);

    private static final int BUF_SIZE = 65536;
    private static final int NO_REMAINING = 0;

    private final @Getter Connection connection;

    public static int getBufSize() {
        return BUF_SIZE;
    }

    public abstract void handle(SelectionKey selectionKey) throws IOException;

    public int read(SelectionKey selectionKey) throws IOException {
        Handler handler = (Handler) selectionKey.attachment();
        SocketChannel socket = (SocketChannel) selectionKey.channel();
        Connection connection = handler.getConnection();
        ByteBuffer outputBuffer = connection.getOutputBuffer().getByteBuffer();

        if (!isReadyToRead(outputBuffer, connection)) {
            return 0;
        }

        int readCount = socket.read(outputBuffer);

        if (readCount <= 0) {
            connection.shutdown();
            selectionKey.interestOps(0);
            checkConnectionClose(socket);
        }

        return readCount;
    }

    public int write(SelectionKey selectionKey) throws IOException {
        ByteBuffer inputBuffer = this.connection.getInputBuffer().getByteBuffer();
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        this.connection.prepareToWrite();
        socketChannel.write(inputBuffer);
        int remaining = inputBuffer.remaining();

        if (NO_REMAINING == remaining) {
            selectionKey.interestOps(SelectionKey.OP_READ);
            checkAssociate(socketChannel, inputBuffer);
        }
        else {
            this.connection.setWriteStartPosition();
        }

        return remaining;
    }

    private boolean isReadyToRead(ByteBuffer buffer, Connection connection) {
        return (buffer.position() < BUF_SIZE / 2) || connection.isAssociateShutDown();
    }

    private void checkConnectionClose(SocketChannel socketChannel) throws IOException {
        if (this.connection.isReadyToClose()) {
            logger.debug("Socket closed: " + socketChannel.getRemoteAddress());
            socketChannel.close();
            this.connection.closeAssociate();
        }
    }

    private void checkAssociate(SocketChannel socketChannel, ByteBuffer buffer) throws IOException {
        if (this.connection.isAssociateShutDown()) {
            socketChannel.shutdownOutput();
            return;
        }

        buffer.clear();
        this.connection.resetWriteStartPosition();
    }
}
