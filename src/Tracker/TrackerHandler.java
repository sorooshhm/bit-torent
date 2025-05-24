package Tracker;

import Common.models.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;

public class TrackerHandler {
    private Tracker tracker;
    Gson gson = new Gson();

    public TrackerHandler(Tracker tracker) {
        this.tracker = tracker;
    }

    public void handle(String inputLine, PrintWriter out, Socket socket) {
        String type = gson.fromJson(inputLine, JsonObject.class).get("type").getAsString();
        if (type.equals("file_request")) {
            handleFileRequest(inputLine, out, socket);

        } else {
            String command = gson.fromJson(inputLine, JsonObject.class).get("command").getAsString();
            if (command.equals("status")) {
                handleStatusResponse(inputLine, out, socket);
            } else if (command.equals("get_files_list")) {
                handleGetFilesResponse(inputLine, out, socket);
            } else if (command.equals("get_sends")) {
                handleGetSendsRequest(inputLine, out, socket);
            } else if (command.equals("get_receives")) {
                handleGetReceivesRequest(inputLine, out, socket);
            }
        }
    }

    public Request getStatusRequest() {
        Request request = new Request();
        request.setType("command").setCommand("status");
        return request;
    }

    public Request getFilesRequest() {
        Request request = new Request();
        request.setType("command").setCommand("get_files_list");
        return request;
    }

    public void handleStatusResponse(String inputLine, PrintWriter out, Socket socket) {
        String response = gson.fromJson(inputLine, JsonObject.class).get("response").getAsString();
        if (response.equals("ok")) {
            String peer = gson.fromJson(inputLine, JsonObject.class).get("peer").getAsString();
            String port = gson.fromJson(inputLine, JsonObject.class).get("listen_port").getAsString();
            Peer p = new Peer(peer, Integer.parseInt(port));
            p.setSocket(socket);
            tracker.addPeer(p);
            out.println(gson.toJson(getFilesRequest()));
        }
    }

    public void handleGetSendsRequest(String inputLine, PrintWriter out, Socket socket) {
        String response = gson.fromJson(inputLine, JsonObject.class).get("response").getAsString();
        if (response.equals("ok")) {
            JsonObject obj = gson.fromJson(inputLine, JsonObject.class).getAsJsonObject("sent_files");
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                for (Map.Entry<String, JsonElement> peerEntry : entry.getValue().getAsJsonObject().entrySet()) {
                    System.out.println(peerEntry.getKey() + " - " + peerEntry.getValue().getAsString() + " - " + entry.getKey());
                }
            }
        }
    }

    public void handleGetReceivesRequest(String inputLine, PrintWriter out, Socket socket) {
        String response = gson.fromJson(inputLine, JsonObject.class).get("response").getAsString();
        if (response.equals("ok")) {
            JsonObject obj = gson.fromJson(inputLine, JsonObject.class).getAsJsonObject("received_files");
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                for (Map.Entry<String, JsonElement> peerEntry : entry.getValue().getAsJsonObject().entrySet()) {
                    System.out.println(peerEntry.getKey() + " - " + peerEntry.getValue().getAsString() + " - " + entry.getKey());
                }
            }
        }
    }

    public void handleGetFilesResponse(String inputLine, PrintWriter out, Socket socket) {
        String response = gson.fromJson(inputLine, JsonObject.class).get("response").getAsString();
        if (response.equals("ok")) {
            Peer p = this.tracker.findPeerBySocket(socket);
            JsonObject files = gson.fromJson(inputLine, JsonObject.class).get("files").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : files.entrySet()) {
                String filename = entry.getKey();
                String md5Hash = entry.getValue().getAsString();
                p.getFiles().put(filename, md5Hash);
            }
        }
    }

    public void handleFileRequest(String inputLine, PrintWriter out, Socket socket) {
        String name = gson.fromJson(inputLine, JsonObject.class).get("name").getAsString();
        ArrayList<Peer> peers = new ArrayList<>();
        ArrayList<String> hashes = new ArrayList<>();
        boolean notFound = false;
        boolean multipleHash = false;
        for (Peer p : tracker.getPeers()) {
            if (p.getFiles().containsKey(name)) {
                if (hashes.contains(p.getFiles().get(name))) {
                    multipleHash = true;
                    break;
                }
                peers.add(p);
                hashes.add(p.getFiles().get(name));
            }
        }
        if (peers.isEmpty()) {
            notFound = true;
        }
        if (notFound) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", "response");
            obj.addProperty("response", "error");
            obj.addProperty("error", "not_found");
            out.println(obj);
            return;
        }
        if (multipleHash) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", "response");
            obj.addProperty("response", "error");
            obj.addProperty("error", "multiple_hash");
            out.println(obj);
            return;
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "response");
        obj.addProperty("response", "peer_found");
        obj.addProperty("md5", hashes.get(0));
        obj.addProperty("peer_have", peers.get(0).getIp());
        obj.addProperty("peer_port", peers.get(0).getPort());
        out.println(obj);
    }
}
