package com.minhld.supports;

/**
 * Created by minhld on 10/30/2015.
 */
public class SocketManager {
    public enum SocketType{
        SERVER,
        CLIENT
    }

    private SocketType socketType;
    private String hostName;

    public SocketManager(SocketType sType) {
        this.socketType = sType;
    }

    public SocketManager(SocketType sType, String hostName) {
        this.socketType = sType;
        this.hostName = hostName;
    }
}
