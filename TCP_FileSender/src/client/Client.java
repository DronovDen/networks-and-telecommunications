package client;

import java.io.IOException;
import java.net.InetAddress;

public class Client {
    public static void main(String[] args) throws IOException {

        if (args.length != 3) {
            throw new IllegalArgumentException("Expected 3 arguments: [filename] [hostname] [port]" + "\nGot: " + args.length);
        }
        String fileName = args[0];
        String hostName = args[1];
        int port = Integer.parseInt(args[2]);
        InetAddress serverAddress = InetAddress.getByName(hostName);
        FileSender fileSender = new FileSender(fileName, serverAddress, port);
        fileSender.run();
    }
}
