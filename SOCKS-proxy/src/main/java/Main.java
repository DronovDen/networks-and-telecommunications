import org.apache.log4j.Logger;

public final class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 0xFFFF;

    public static void main(String[] args) {
        if (1 != args.length) {
            logger.info("Usage: port");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
            if (!isPortValid(port)) {
                logger.error(String.format("Port is out of range [%d, %d], got %d", MIN_PORT, MAX_PORT, port));
                return;
            }
        }
        catch (NumberFormatException exception) {
            logger.error("Unable to parse port {" + args[0] + "}", exception);
            return;
        }

        Proxy proxy = new Proxy(port);
        proxy.start();
    }

    private static boolean isPortValid(int port) {
        return MIN_PORT <= port && port <= MAX_PORT;
    }
}
