// SCP Session
// Author: Jordan Maddock
// Date: 01/09/2018
// This object keeps track of all attributes of a session of the SCP server or client

public class ScpSession {
    private String remoteHostname;
    private int remotePort;
    private String localHostname;
    private int localPort;
    private long timeConnectInitiated;
    private String clientUsername;
    private String latestMsg;

    public String latest() {
        return latestMsg;
    }

    public void setLatest(String latestLine) {
        this.latestMsg = latestLine;
    }

    public String getRemoteHostname() {
        return remoteHostname;
    }

    public void setRemoteHostname(String remoteHostname) {
        this.remoteHostname = remoteHostname;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public String getLocalHostname() {
        return localHostname;
    }

    public void setLocalHostname(String localHostname) {
        this.localHostname = localHostname;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public long getTimeConnectInitiated() {
        return timeConnectInitiated;
    }

    public void setTimeConnectInitiated(long timeConnectInitiated) {
        this.timeConnectInitiated = timeConnectInitiated;
    }

    public String getLocalHost() {
        return clientUsername;
    }

    public void setClientUsername(String clientUsername) {
        this.clientUsername = clientUsername;
    }

    public String getClientUsername() {
        return clientUsername;
    }
}
