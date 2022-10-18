package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static util.Protocol.*;


public class FileReceiver implements Runnable {
    private final Socket socket;

    public FileReceiver(Socket socket) {
        this.socket = socket;
    }

    private Path createFile(String filename, DataOutputStream output) throws IOException {
        //Path filenamePath = Paths.get(filename);
        //filename = filenamePath.getFileName().toString();
        Path uploadDir = Paths.get("uploads");
        if (!Files.exists(uploadDir)) {
            Files.createDirectory(uploadDir);
        }
        Path path = Paths.get(uploadDir + System.getProperty("file.separator") + filename);
        if (Files.exists(path)) {
            output.writeInt(FILE_ALREADY_EXISTS);
            throw new FileAlreadyExistsException("Such file is already uploaded!");
        }
        System.out.println("Creating file: " + path);
        Files.createFile(path);
        return path;
    }

    @Override
    public void run() {
        try (DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
             DataInputStream inputStream = new DataInputStream(socket.getInputStream())) {

            // read filename length and file name
            int fileNameSize = inputStream.readInt();
            String filename = receiveFilename(inputStream, fileNameSize);
            System.out.println("Requested to upload new file: " + filename);

            // if length that we send differs from the one we've got, it's an error
            if (filename.length() != fileNameSize) {
                outputStream.writeInt(FILENAME_SIZE_NOT_MATCH);
                outputStream.flush();
            }

            outputStream.writeInt(SUCCESSFUL_FILENAME_TRANSFER);

            // receive file size
            long fileSize = inputStream.readLong();

            //create file in Upload directory
            Path newFile = createFile(filename, outputStream);
            outputStream.writeInt(FILE_CREATED_SUCCESSFULLY);

            // read data from the socket and write to the file
            try (FileOutputStream fileOutputStream = new FileOutputStream(newFile.toFile())) {
                long totalBytesCount = 0;
                long uploadStartedTime = System.currentTimeMillis();
                long lastSpeedMeasureTime = uploadStartedTime;

                long currentTime = uploadStartedTime;
                long readSinceLastTimeMeasure = 0;

                byte[] buffer = new byte[BUFFER_SIZE];

                int segmentSize = 0;

                while (totalBytesCount < fileSize && (segmentSize = inputStream.read(buffer)) != -1) {

                    readSinceLastTimeMeasure += segmentSize;
                    totalBytesCount += segmentSize;

                    if (segmentSize > 0) {
                        fileOutputStream.write(buffer, 0, segmentSize);
                        fileOutputStream.flush();
                    }

                    currentTime = System.currentTimeMillis();
                    if (currentTime - lastSpeedMeasureTime > TIME_MEASURE_INTERVAL) {
                        System.out.printf("Uploading " + newFile.getFileName() + " from SOCKET " + socket.getInetAddress()
                                        + " " + socket.getPort() + ":\n"
                                        + "Instant speed: %.3f KB/sec\n",
                                (double) readSinceLastTimeMeasure * 1000 / (KILOBYTE * (currentTime - lastSpeedMeasureTime)));
                        System.out.printf("Average speed: %.3f KB/sec\n",
                                (double) totalBytesCount * 1000 / (KILOBYTE * (currentTime - uploadStartedTime)));

                        readSinceLastTimeMeasure = 0;
                        lastSpeedMeasureTime = currentTime;
                    }
                }
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    outputStream.writeInt(FILE_TRANSFER_FAILURE);
                    e.printStackTrace(System.err);
                }
                if (totalBytesCount == fileSize) {
                    outputStream.writeInt(SUCCESSFUL_FILE_TRANSFER);
                    System.out.println("Finished uploading " + newFile.getFileName());
                } else {
                    outputStream.writeInt(FILE_TRANSFER_FAILURE);
                }
                outputStream.close();
                inputStream.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            System.err.println("Connection error:");
            e.printStackTrace(System.err);
        }
    }

    private String receiveFilename(DataInputStream inputStream, int fileNameLength) {
        byte[] buffer = new byte[fileNameLength];
        try {
            inputStream.readNBytes(buffer, 0, fileNameLength);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String fileName = new String(buffer, StandardCharsets.UTF_8);
        return fileName;
    }
}
