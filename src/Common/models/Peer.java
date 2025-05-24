package Common.models;

import java.io.*;
import java.net.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import Common.utilities.MD5Hasher;
import Peer.PeerHandler;
import Peer.ClientHandler;
import com.google.gson.JsonObject;

public class Peer {
    private final String ip;
    private final int port;
    private String folder;
    private Socket socket;
    private Socket trackerSocket;
    private final PeerHandler handler = new PeerHandler(this);
    private final HashMap<String, String> files = new HashMap<>();
    private final ArrayList<PeerFileReceiver> receiverPeers = new ArrayList<>();
    private final ArrayList<PeerFileSender> senderPeers = new ArrayList<>();

    public Peer(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public Peer(String ip, int port, String folder) {
        this.ip = ip;
        this.port = port;
        this.folder = folder;
        initializeFiles();
    }

    public void initializeFiles() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("./" + folder))) {
            for (Path filePath : stream) {
                if (Files.isRegularFile(filePath)) {
                    String fileName = filePath.getFileName().toString();
                    String content = Files.readString(filePath);
                    files.put(fileName, MD5Hasher.hash(content));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public HashMap<String, String> getFiles() {
        return files;
    }

    public void connect() {
        try (Socket socket = new Socket()) {
            trackerSocket = socket;
            socket.connect(new InetSocketAddress("127.0.0.1", 8080));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                String input = in.readLine();
                if (input != null)
                    handler.handle(input, out, socket);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void requestTracker(String name) {
        try {
            PrintWriter out = new PrintWriter(trackerSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(trackerSocket.getInputStream()));
            handler.getFileRequestRequest(name, out, in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void request(String ip, int port, String md5, String name) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            JsonObject request = new JsonObject();
            request.addProperty("type", "download_request");
            request.addProperty("name", name);
            request.addProperty("md5", md5);
            out.println(request.toString());
            String inputLine;
            String content = "";
            while ((inputLine = in.readLine()) != null) {
                content += inputLine;
            }

            if (MD5Hasher.hash(content).equals(md5)) {
                Files.writeString(Paths.get("./", folder, name), content);
                files.put(name, md5);
                PeerFileSender ps = null;
                for (PeerFileSender p : senderPeers) {
                    if(p.getAddress().equals(ip+":"+port)) {
                        ps = p;
                    }
                }
                if(ps == null) {
                    ps = new PeerFileSender(ip+":"+port);
                    ps.getFiles().put(name, md5);
                    senderPeers.add(ps);
                }else {
                    ps.getFiles().put(name,md5);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public PeerHandler getHandler() {
        return handler;
    }

    @Override
    public String toString() {
        return "Peer : " + ip + ":" + port;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public ArrayList<PeerFileReceiver> getReceiverPeers() {
        return receiverPeers;
    }

    public ArrayList<PeerFileSender> getSenderPeers() {
        return senderPeers;
    }

    public String address() {
        return ip + ":" + port;
    }

    public Socket getTrackerSocket() {
        return trackerSocket;
    }
}
