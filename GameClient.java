package com.jdbc;

import java.io.*;
import java.net.*;

public class GameClient {
    private static final String SERVER_ADDRESS = "192.168.1.5"; // update to match server host
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
    	System.out.println("Successfully connected to Trivia Game server.\n");
    	try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

            // Thread for reading input from the user and sending it to the server
            Thread inputThread = new Thread(() -> {
                try {
                    BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    String userInputLine;
                    while ((userInputLine = userInput.readLine()) != null) {
                        out.println(userInputLine);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            inputThread.start();

            // Thread for receiving responses from the server and printing them to the console
            Thread responseThread = new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            responseThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
