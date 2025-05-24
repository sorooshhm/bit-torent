package Common.models;

import java.util.HashMap;

public class PeerFileReceiver {
    private final String address;
    private final HashMap<String, String> files = new HashMap<>();

    public PeerFileReceiver(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public HashMap<String, String> getFiles() {
        return files;
    }
}
