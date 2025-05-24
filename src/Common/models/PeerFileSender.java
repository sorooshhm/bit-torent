package Common.models;

import java.util.HashMap;

public class PeerFileSender {
    private final String address;
    private final HashMap<String, String> files = new HashMap<>();

    public PeerFileSender(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public HashMap<String, String> getFiles() {
        return files;
    }
}
