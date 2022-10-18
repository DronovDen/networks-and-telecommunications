package client;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static util.Protocol.*;

public class FileSender implements Runnable {
    private final Path path;
    private final File file;
    private final InetAddress serverIP;
    private final int serverPort;

    public FileSender(String filename, InetAddress serverIP, int serverPort) throws FileNotFoundException {
        this.path = Paths.get(filename);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Can't find such file: " + path);
        }
        //file = new File(path.toUri());
        file = new File(path.toAbsolutePath().toString());

        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    private void UploadFile(FileInputStream fileInputStream, DataOutputStream socketOutputStream) throws IOException, InterruptedException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int segmentSize; // size of an individual piece of data that can be less than size of the buffer
        while ((segmentSize = fileInputStream.read(buffer, 0, BUFFER_SIZE)) != -1) {

            Thread.sleep(10);
            socketOutputStream.write(buffer, 0, segmentSize);

            //socketOutputStream.sendInt(segmentSize);
            //socketOutputStream.send(buffer, segmentSize);
            //socketOutputStream.flush();
        }
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(serverIP, serverPort);
             DataOutputStream outputSteam = new DataOutputStream(socket.getOutputStream());
             DataInputStream inputStream = new DataInputStream(socket.getInputStream());
             FileInputStream fileInputStream = new FileInputStream(file)) {

            //send filename length and then the name itself
            sendFileNameLength(outputSteam);
            sendFileName(outputSteam);

            String fileName = file.getName();
            System.out.println("Uploading: " + fileName);

            // if length that we send differs from the one we've got, it's an error
            if (inputStream.readInt() != SUCCESSFUL_FILENAME_TRANSFER) {
                System.err.println("Error transferring filename");
                fileInputStream.close();
                outputSteam.close();
                socket.close();
                return;
            }

            // send file size to the server
            sendFileSize(outputSteam);

            if (inputStream.readInt() != FILE_CREATED_SUCCESSFULLY) {
                System.err.println("Error transferring file");
                fileInputStream.close();
                outputSteam.close();
                socket.close();
                return;
            }

            try {
                UploadFile(fileInputStream, outputSteam);
            } catch (IOException | InterruptedException ex) {
                System.err.println("Client was interrupted with an exception: " + ex.getMessage());
            }

            fileInputStream.close();

            // get server's response whether the transfer was successful or not
            int transferStatus = inputStream.readInt();
            if (transferStatus == FILE_TRANSFER_FAILURE) {
                System.out.println("File transfer failed");
            } else if (transferStatus == SUCCESSFUL_FILE_TRANSFER) {
                System.out.println("File transferred successfully");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFileNameLength(DataOutputStream outputStream) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        //getting array of bytes from integer and putting it to bytes buffer
        buffer.putInt(file.getName().getBytes(StandardCharsets.UTF_8).length);
        outputStream.write(buffer.array());
    }

    private void sendFileName(DataOutputStream outputStream) throws IOException {
        outputStream.write(file.getName().getBytes(StandardCharsets.UTF_8));
    }

    private void sendFileSize(DataOutputStream outputStream) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(Files.size(path));
        outputStream.write(buffer.array());
    }
}

