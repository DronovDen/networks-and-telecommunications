package socks;

import lombok.Getter;
import lombok.Setter;

public final class SocksConnectRequest {
    private @Getter @Setter byte version;
    private @Getter byte[] methods;

    public void setNumOfMethods(byte numOfMethods) {
        this.methods = new byte[numOfMethods];
    }
}
