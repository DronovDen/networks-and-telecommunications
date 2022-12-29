package socks;

import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public final class SocksRequest {
    private @Getter @Setter String domainName;
    private @Getter @Setter byte parseError = 0x00;
    private @Getter @Setter byte version;
    private @Getter @Setter byte command;
    private @Getter @Setter byte addressType;
    private @Getter @Setter short targetPort;
    private final @Getter byte[] ip4Address = new byte[4];

    public InetSocketAddress getAddress() throws UnknownHostException {
        return new InetSocketAddress(InetAddress.getByAddress(this.ip4Address), this.targetPort);
    }
}
