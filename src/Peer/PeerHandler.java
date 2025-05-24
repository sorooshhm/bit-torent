package Peer;

import Common.models.Peer;
import Common.models.PeerFileReceiver;
import Common.models.PeerFileSender;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class PeerHandler {
    private final Peer peer;
    Gson gson = new Gson();

    public PeerHandler(Peer peer) {
        this.peer = peer;
    }

    public void getFileRequestRequest(String filename, PrintWriter out, BufferedReader in) {
        JsonObject request = new JsonObject();
        request.addProperty("type", "file_request");
        request.addProperty("name", filename);
        out.println(gson.toJson(request));
        try {
            String inputLine = in.readLine();
            System.out.println(inputLine);
            handleFileRequestResponse(inputLine, filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void handle(String inputLine, PrintWriter out, Socket socket) {
        String type = gson.fromJson(inputLine, JsonObject.class).get("type").getAsString();
        if (type.equals("command")) {
            String command = gson.fromJson(inputLine, JsonObject.class).get("command").getAsString();
            if (command.equals("status")) {
                handleStatusRequest(inputLine, out);
            } else if (command.equals("get_files_list")) {
                handleGetFilesRequest(inputLine, out);
            } else if (command.equals("get_sends")) {
                handleGetSends(inputLine, out);
            } else if (command.equals("get_receives")) {
                handleGetReceived(inputLine, out);
            }
        } else if (type.equals("download_request")) {
            handleDownloadFile(inputLine, out, socket);
        }
    }

    public void handleDownloadFile(String inputLine, PrintWriter out, Socket socket) {
        String name = gson.fromJson(inputLine, JsonObject.class).get("name").getAsString();
        String md5 = gson.fromJson(inputLine, JsonObject.class).get("md5").getAsString();
        if (!peer.getFiles().containsKey(name)) {
            return;
        }
        if (!md5.equals(peer.getFiles().get(name))) {
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader("./" + peer.getFolder() + "/" + name))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.println(line);
            }
            PeerFileReceiver pr = null;
            String clientAddress = socket.getInetAddress().getHostAddress();
            int clientPort = socket.getPort();
            for (PeerFileReceiver p : peer.getReceiverPeers()) {
                if (p.getAddress().equals(clientAddress+":"+clientPort)) {
                    pr = p;
                }
            }
            if (pr == null) {
                pr = new PeerFileReceiver(clientAddress+":"+clientPort);
                pr.getFiles().put(name, md5);
                peer.getReceiverPeers().add(pr);
            } else {
                pr.getFiles().put(name, md5);
            }
            socket.close();
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

    }

    public void handleStatusRequest(String inputLine, PrintWriter out) {
        JsonObject json = gson.fromJson("{}", JsonObject.class);
        json.addProperty("type", "response");
        json.addProperty("command", "status");
        json.addProperty("response", "ok");
        json.addProperty("peer", peer.getIp());
        json.addProperty("listen_port", peer.getPort());
        out.println(json.toString());
    }

    public void handleGetFilesRequest(String inputLine, PrintWriter out) {
        JsonObject json = gson.fromJson("{}", JsonObject.class);
        json.addProperty("type", "response");
        json.addProperty("command", "get_files_list");
        json.addProperty("response", "ok");
        JsonObject files = new JsonObject();
        for (Map.Entry<String, String> entry : this.peer.getFiles().entrySet()) {
            files.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("files", files);
        out.println(json.toString());
    }

    public void handleGetSends(String inputLine, PrintWriter out) {
        JsonObject json = gson.fromJson("{}", JsonObject.class);
        json.addProperty("type", "response");
        json.addProperty("command", "get_sends");
        json.addProperty("response", "ok");
        JsonObject sends = new JsonObject();
        for (PeerFileReceiver rp : peer.getReceiverPeers()) {
            JsonObject files = new JsonObject();
            for (Map.Entry<String, String> entry : rp.getFiles().entrySet()) {
                files.addProperty(entry.getKey(), entry.getValue());
            }
            sends.add(peer.address(), files);
        }
        json.add("sent_files", sends);
        out.println(json.toString());
    }

    public void handleGetReceived(String inputLine, PrintWriter out) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "response");
        json.addProperty("command", "get_receives");
        json.addProperty("response", "ok");
        JsonObject sends = new JsonObject();
        for (PeerFileSender rp : peer.getSenderPeers()) {
            JsonObject files = new JsonObject();
            for (Map.Entry<String, String> entry : rp.getFiles().entrySet()) {
                files.addProperty(entry.getKey(), entry.getValue());
            }
            sends.add(peer.address(), files);
        }
        json.add("received_files", sends);
        System.out.println(inputLine);
        out.println(json.toString());
    }

    public void handleFileRequestResponse(String inputLine, String name) {
        String response = gson.fromJson(inputLine, JsonObject.class).get("response").getAsString();
        if (response.equals("peer_found")) {
            String md5 = gson.fromJson(inputLine, JsonObject.class).get("md5").getAsString();
            String peer_have = gson.fromJson(inputLine, JsonObject.class).get("peer_have").getAsString();
            String peer_port = gson.fromJson(inputLine, JsonObject.class).get("peer_port").getAsString();
            peer.request(peer_have, Integer.parseInt(peer_port), md5, name);
        } else if (response.equals("error")) {
            String error = gson.fromJson(inputLine, JsonObject.class).get("error").getAsString();
            if (error.equals("not_found")) {

            } else if (error.equals("multiple_hash")) {

            }
        }
    }
}
