package com.jdbc.user;

import java.io.*;
import java.net.*;

public class GameClientTest_1 {
    private static final String SERVER_ADDRESS = "192.168.1.5";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        try (
            // Create socket connection to the server
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            // Input stream to read server responses
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Output stream to send client inputs
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // Input stream to read user inputs
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Successfully connected to Trivia Game server.\n");

            // Read and process initial server responses
            String line;
            while ((line = in.readLine()) != null ) {
                // Print server responses
                System.out.println("Server: " + line);

                // If the server asks for user input (e.g. for choosing a menu option)
                if (line.contains("Please choose an option by typing the corresponding number") ||
                    line.contains("Enter") || line.equals("Account created successfully.") ||
                    line.equals("Create Lobby") || line.equals("Join Lobby") || line.contains("Name") || line.contains("Enter")) {
                    
                    // Read user input from the console
                    String userInputLine = userInput.readLine();
                    // Send user input to the server
                    out.println(userInputLine);
                } else {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            // Handle any I/O exceptions
            e.printStackTrace();
        }
    }
}
