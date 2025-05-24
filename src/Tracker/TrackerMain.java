package Tracker;

import Common.models.*;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;

public class TrackerMain {
    public static void main(String[] args) {
        String ip = "127.0.0.1";
        String port = args[0];
        Tracker tracker = new Tracker(ip, Integer.parseInt(port));
        new Thread(() -> {
            try {
                tracker.listen();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String inputLine = scanner.nextLine();
            if (inputLine.equals("peers")) {
                System.out.println("Peers:" + tracker.getPeers().toString());
            } else if (inputLine.equals("refresh_files")) {
                for (Peer p : tracker.getPeers()) {
                    Socket socket = p.getSocket();
                    try {
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        Request req = tracker.getHandler().getFilesRequest();
                        Gson gson = new Gson();
                        out.println(gson.toJson(req));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            } else if (inputLine.contains("list_files")) {
                String address = inputLine.split(" ")[1];
                Peer p = null;
                for (Peer p1 : tracker.getPeers()) {
                    if (p1.address().equals(address)) {
                        p = p1;
                    }
                }
                if (p != null) {
                    for (Map.Entry<String, String> entry : p.getFiles().entrySet()) {
                        System.out.println(entry.getKey() + " " + entry.getValue());
                    }
                }
            } else if (inputLine.contains("get_sends")) {
                System.out.println("sends:");
                String address = inputLine.split(" ")[1];
                Peer p = null;
                for (Peer p1 : tracker.getPeers()) {
                    if (p1.address().equals(address)) {
                        p = p1;
                    }
                }
                Socket socket = p.getSocket();
                try {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    Request req = new Request();
                    req.setType("command").setCommand("get_sends");
                    Gson gson = new Gson();
                    out.println(gson.toJson(req));
                    for (PeerFileReceiver pr : p.getReceiverPeers()) {
                        for (Map.Entry<String, String> entry : pr.getFiles().entrySet()) {
                            System.out.println(entry.getKey() + " " + entry.getValue());
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (inputLine.contains("get_receives")) {
                System.out.println("receives:");
                String address = inputLine.split(" ")[1];
                Peer p = null;
                for (Peer p1 : tracker.getPeers()) {
                    if (p1.address().equals(address)) {
                        p = p1;
                    }
                }
                try {
                    Socket socket = p.getSocket();
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    Request req = new Request();
                    req.setType("command").setCommand("get_receives");
                    Gson gson = new Gson();
                    out.println(gson.toJson(req));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }
}
