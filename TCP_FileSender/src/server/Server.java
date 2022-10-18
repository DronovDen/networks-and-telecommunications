package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected 1 argument : [port]." + "\nGot " + args.length);
        }
        int port = Integer.parseInt(args[0]);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                FileReceiver fileReceiver = new FileReceiver(clientSocket);
                Thread fileTransportation = new Thread(fileReceiver);
                fileTransportation.start();
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize server on port " + port);
        }
    }
}
