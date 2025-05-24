package Common.models;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import Tracker.ClientHandler;
import Tracker.TrackerHandler;

public class Tracker {
    private final String ip;
    private final int port;
    private final ArrayList<Peer> peers = new ArrayList<>();
    private final TrackerHandler handler = new TrackerHandler(this);

    public Tracker(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void listen() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on " + ip + ":" + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket, this).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }

    }

    public void addPeer(Peer peer) {
        peers.add(peer);
    }

    public ArrayList<Peer> getPeers() {
        return peers;
    }

    public TrackerHandler getHandler() {
        return handler;
    }

    public Peer findPeerBySocket(Socket socket) {
        for (Peer peer : peers) {
            if (peer.getSocket().equals(socket)) {
                return peer;
            }
        }
        return null;
    }
}
