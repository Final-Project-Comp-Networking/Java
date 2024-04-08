package com.jdbc.user;

import java.io.*;
import java.net.*;
import java.sql.*;

public class GameServer {
    private static final int PORT = 8080;
    private ServerSocket serverSocket;
    private Connection dbConnection;

    public GameServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            dbConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/triviaGame", "root", "uwwhitewater");
            System.out.println("Server started. Listening on port " + PORT);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket, dbConnection);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }
}

// Used for creating seprarte threads for each user connecting.
class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Connection dbConnection;

    public ClientHandler(Socket clientSocket, Connection dbConnection) {
        this.clientSocket = clientSocket;
        this.dbConnection = dbConnection;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            // Handle client communication here
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received from client: " + inputLine);
                // Process input and interact with the database as needed
                // Example: Execute SQL queries, update game state, etc.
                out.println("Response from server: " + inputLine); // Echo back to client
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
