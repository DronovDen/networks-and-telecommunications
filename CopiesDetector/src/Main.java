import java.io.IOException;
import java.net.*;

public class Main {
    public static void main(String[] args) {
        try {
            InetAddress address = InetAddress.getByName(args[0]);
            NetworkInterface networkInterface = NetworkInterface.getByName(args[1]);
            CopiesDetector.detectCopies(address, networkInterface);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
