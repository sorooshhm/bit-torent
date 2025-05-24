package Tracker;

import Common.models.Request;
import Common.models.Tracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.google.gson.Gson;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final Tracker tracker;

    public ClientHandler(Socket socket, Tracker tracker) {
        this.socket = socket;
        this.tracker = tracker;
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(
                     socket.getOutputStream(), true)) {
            Gson gson = new Gson();
            Request req = tracker.getHandler().getStatusRequest();
            out.println(gson.toJson(req));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                this.tracker.getHandler().handle(inputLine , out ,socket);
            }
        } catch (IOException e) {
            System.err.println("Client handling error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Socket close error: " + e.getMessage());
            }
        }
    }
}
