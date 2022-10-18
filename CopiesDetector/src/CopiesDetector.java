import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

public class CopiesDetector {

    private static final int PORT = 8888, DELAY = 0, TIMEOUT = 1000, BUF_SIZE = 0;

    private static final byte[] buffer = new byte[BUF_SIZE];

    private static final Timer sendTimer = new Timer(true);

    static void detectCopies(InetAddress multicastAddress, NetworkInterface networkInterface) {
        try (MulticastSocket recvSocket = new MulticastSocket(PORT);
             DatagramSocket sendSocket = new DatagramSocket()) {

            SocketAddress recvSocketAddress = new InetSocketAddress(multicastAddress, PORT);

            recvSocket.joinGroup(recvSocketAddress, networkInterface);
            recvSocket.setSoTimeout(TIMEOUT);

            DatagramPacket recvPack = new DatagramPacket(buffer, BUF_SIZE);

            HashMap<SocketAddress, Long> activeCopies = new HashMap<>();

            setTimer(sendSocket, multicastAddress);


            while (true) {
                boolean isActiveCopiesChanged = false;
                try {
                    recvSocket.receive(recvPack);
                    SocketAddress recvAddress = recvPack.getSocketAddress(); //socket address == IP + port number
                    if (activeCopies.put(recvAddress, System.currentTimeMillis()) == null) {
                        System.out.println("\nNew copy joined with address " + recvAddress);
                        isActiveCopiesChanged = true;
                    }
                } catch (SocketTimeoutException exc) {
                }

                Iterator<Entry<SocketAddress, Long>> iterator = activeCopies.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry<SocketAddress, Long> copy = iterator.next();
                    if (System.currentTimeMillis() - copy.getValue() > TIMEOUT) {
                        System.out.println("\nCopy with address " + copy.getKey() + " disconnected");
                        iterator.remove();
                        isActiveCopiesChanged = true;
                    }
                }

                if (isActiveCopiesChanged) {
                    printActiveCopies(activeCopies);
                }

            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void printActiveCopies(HashMap<SocketAddress, Long> activeCopies) {
        System.out.println("\n" + activeCopies.size() + " copies are alive:");
        for (SocketAddress address : activeCopies.keySet()) {
            System.out.println("Copy with address " + address);
        }
    }


    private static void setTimer(DatagramSocket socket, InetAddress multicastAddress) {
        sendTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    socket.send(new DatagramPacket(buffer, BUF_SIZE, multicastAddress, PORT));
                } catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
        }, DELAY, TIMEOUT);
    }
}