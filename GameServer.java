package com.jdbc;

package com.jdbc;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import com.jdbc.getQuestions;
import java.util.Timer;
import java.util.TimerTask;

public class GameServer {
    private static final int PORT = 8080;
    private static final int MAX_PLAYERS = 1; //update total players playing here
    private static final List<ClientHandler> clients = new ArrayList<>(); // Client's put into List to ensure synchronoization
    static Connection connection; // Database connection
    
    public static void main(String[] args) throws Exception {
        String[][] questions = getQuestions.questionList();

        // Setup database connection
        setupDatabaseConnection();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (clients.size() < MAX_PLAYERS) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket, questions);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
                System.out.println("Lobby Has: " + clients.size() + " Players");
            }

            // Start the game
            startGame(questions);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close database connection when no longer needed
            if (connection != null) {
                try {
                    connection.close();
                    System.out.println("Database connection closed.");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void setupDatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String dbURL = "jdbc:mysql://localhost:3306/triviaGame";
            String username = "root";
            String password = "uwwhitewater";

            connection = DriverManager.getConnection(dbURL, username, password);
            System.out.println("Database connection established.");
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Failed to connect to database.", e);
        }
    }

    // Method to start game as well as display leaderboard to all users
    private static void startGame(String[][] questions) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // Prompt for "start" input
        System.out.println("Type 'Start' to start the game or 'Leaderboard' to display leaderboard: ");
        String input = scanner.nextLine().trim();
        
        if (input.equalsIgnoreCase("leaderboard")) {    
            for (ClientHandler client : clients) {
                PrintWriter out = new PrintWriter(client.clientSocket.getOutputStream(), true);
                client.displayLeaderboard(out); // Pass PrintWriter to display leaderboard
            }
        }
        
        // Continue prompting until valid "start" or "leaderboard" input is received
        while (!input.equalsIgnoreCase("start")) {
            System.out.println("Please type 'Start' to begin the game: ");
            input = scanner.nextLine().trim();
        }
        
        // Notify clients with countdown before sending questions
        for (ClientHandler client : clients) {
            client.sendCountdown();
        }

        for (int i = 0; i < 4; i++) { // Number of questions in the game
            int rq = getQuestions.getRandomQuestion(questions);

            // Send question to all clients
            for (ClientHandler client : clients) {
                client.sendQuestion(questions, i);
            }

            // Wait for all clients to answer
            for (ClientHandler client : clients) {
                client.waitForAnswer();
            }

            // Prompt for "ok" input to continue to the next question
            System.out.println("Enter 'ok' to continue: ");
            String wait = scanner.nextLine().trim();
        }
        
        // Notify clients that game is over
        for (ClientHandler client : clients) {
            client.sendGameOver();
        }

        // Send game results to clients and display leaderboard at game end
        HashMap<String, Integer> leaderboard = new HashMap<>();
        for (ClientHandler client : clients) {
            leaderboard.put(client.clientSocket.getInetAddress().toString(), client.sendGameResults());
        }

        // Sort the leaderboard by scores in descending order
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(leaderboard.entrySet());
        entryList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Print the sorted leaderboard
        System.out.println("Game Leaderboard:");
        for (Map.Entry<String, Integer> entry : entryList) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        scanner.close();
    }
}
