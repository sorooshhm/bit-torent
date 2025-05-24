package Peer;

import Common.models.Peer;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

public class PeerMain {
    public static void main(String[] args) {
        String ip = args[0].split(":")[0];
        String port = args[0].split(":")[1];
        String folder = args[1];
        Peer peer = new Peer(ip, Integer.parseInt(port), folder);
        Scanner scanner = new Scanner(System.in);
        new Thread(() -> {
            try {
                peer.connect();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
        new Thread(() -> {
            try {
                peer.listen();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        while (true) {
            String line = scanner.nextLine().trim();
            if (line.equals("list")) {
                if (peer.getFiles().isEmpty()) {
                    System.out.println("No files found");
                    return;
                }
                for (Map.Entry<String, String> entry : peer.getFiles().entrySet()) {
                    System.out.println(entry.getKey() + " " + entry.getValue());
                }
            } else if (line.contains("download")) {
                String name = line.split(" ")[1];
                peer.requestTracker(name);
            }
        }
    }
}
