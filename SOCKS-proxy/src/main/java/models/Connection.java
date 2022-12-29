package models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@RequiredArgsConstructor
public final class Connection {
    private static final Logger logger = Logger.getLogger(Connection.class);

    private final @Getter ObservableByteBuffer inputBuffer;
    private final @Getter ObservableByteBuffer outputBuffer;

    private @Setter SocketChannel associate;
    private int writeStartPosition = 0;

    public Connection(int bufSize) {
        this.inputBuffer = new ObservableByteBuffer(ByteBuffer.allocate(bufSize));
        this.outputBuffer = new ObservableByteBuffer(ByteBuffer.allocate(bufSize));
    }

    public void registerBufferListener(ObservableByteBuffer.BufferListener bufferListener) {
        this.inputBuffer.setBufferListener(bufferListener);
    }

    public void notifyBufferListener() {
        this.outputBuffer.notifyListener();
    }

    public void closeAssociate() throws IOException {
        if (null != this.associate) {
            logger.debug("Socket closed: " + this.associate.getRemoteAddress());
            this.associate.close();
        }
    }

    public void shutdown() {
        this.outputBuffer.shutdown();
    }

    public boolean isAssociateShutDown() {
        return this.inputBuffer.isReadyToClose();
    }

    public void prepareToWrite() {
        ByteBuffer inputBuffer = getInputBuffer().getByteBuffer();
        inputBuffer.flip();
        inputBuffer.position(this.writeStartPosition);
    }

    public boolean isReadyToClose() {
        return this.inputBuffer.isReadyToClose() && this.outputBuffer.isReadyToClose();
    }

    public void resetWriteStartPosition() {
        this.writeStartPosition = 0;
    }

    public void setWriteStartPosition() {
        ByteBuffer inputBuffer = getInputBuffer().getByteBuffer();
        this.writeStartPosition = inputBuffer.position();
        int newStartPosition = inputBuffer.limit();
        inputBuffer.clear();
        inputBuffer.position(newStartPosition);
    }

    @RequiredArgsConstructor
    public static final class ObservableByteBuffer {
        private final @Getter ByteBuffer byteBuffer;

        private @Setter BufferListener bufferListener;
        private boolean shouldShutdown = false;

        public void notifyListener(){
            this.bufferListener.onUpdate();
        }

        public void shutdown() {
            this.shouldShutdown = true;
        }

        public boolean isReadyToClose(){
            return (0 == this.byteBuffer.remaining()) && this.shouldShutdown;
        }

        public interface BufferListener {
            void onUpdate();
        }
    }
}
