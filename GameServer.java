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

public class GameServer {
    private static final int PORT = 8080;
    private static final int MAX_PLAYERS = 1; //update total players playing here
    private static final List<ClientHandler2> clients = new ArrayList<>(); // Client's put into List to ensure synchronoization
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
                ClientHandler2 clientHandler = new ClientHandler2(clientSocket, questions);
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
            for (ClientHandler2 client : clients) {
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
        for (ClientHandler2 client : clients) {
            client.sendCountdown();
        }

        for (int i = 0; i < 10; i++) { // Number of questions in the game
            int rq = getQuestions.getRandomQuestion(questions);

            // Send question to all clients
            for (ClientHandler2 client : clients) {
                client.sendQuestion(questions, i);
            }

            // Wait for all clients to answer
            for (ClientHandler2 client : clients) {
                client.waitForAnswer();
            }

            // Prompt for "ok" input to continue to the next question
            System.out.println("Enter 'ok' to continue: ");
            String wait = scanner.nextLine().trim();
        }

        // Send game results to clients and display leaderboard at game end
        HashMap<String, Integer> leaderboard = new HashMap<>();
        for (ClientHandler2 client : clients) {
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

        scanner.close(); // Close the scanner
    }

}

class ClientHandler2 implements Runnable {
    public final Socket clientSocket;
    private final String[][] questions;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Map<String, Integer> answers = new HashMap<>();
    private int num = 0;

    // Handler each client connection as own object
    public ClientHandler2(Socket clientSocket, String[][] questions) throws IOException {
        this.clientSocket = clientSocket;
        this.questions = questions;
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println("Welcome to the Trivia Game, please wait for the game to begin.");
        out.println("");
        out.println("You are client: " + clientSocket.getInetAddress().toString());
        answers.put(clientSocket.getInetAddress().toString(), 0);
    }

	@Override
    public void run() {
        try {
        	addPlayerIfNotExists(clientSocket.getInetAddress().toString()); // add new player to DB if they don't exist
            while (true) {
                String answer = in.readLine();
                if (answer != null && !answer.isEmpty()) {
                    System.out.println("Received input from " + clientSocket.getInetAddress() + ": " + answer); // Server side display to see response of each client
                    answers.put(clientSocket.getInetAddress().toString(), checkAnswer(questions, answer));
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void addPlayerIfNotExists(String ipAddress) throws SQLException {
        // Check if player already exists in the database
        if (playerExists(ipAddress)) {
            System.out.println("Welcome back, " + ipAddress + ".");
        } else {
            // Player doesn't exist, add them to the database
            addNewPlayer(ipAddress);
            System.out.println("Welcome, new player " + ipAddress + "! You've been registered.");
        }
    }
    
    private boolean playerExists(String ipAddress) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        boolean exists = false;

        try {
            // Get database connection
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/triviaGame", "root", "uwwhitewater");

            // Check if player with the given IP address exists in the players table
            String sql = "SELECT id FROM players WHERE id = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, ipAddress);
            exists = preparedStatement.executeQuery().next(); // Check if result set has next row
        } finally {
            // Close the prepared statement and connection
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }

        return exists;
    }
    
    public void addNewPlayer(String ipAddress) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/triviaGame", "root", "uwwhitewater");

            // Insert new player into the players table with initial score and games count
            String sql = "INSERT INTO players (id, total_score, total_games) VALUES (?, 0, 0)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, ipAddress);
            preparedStatement.executeUpdate(); // Execute the insert statement
        } finally {
            // Close the prepared statement and connection
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }
    
    // Sent to client's right before game starts so everyone is ready
    public void sendCountdown() throws InterruptedException {
    	out.println("Game starting in 3...");
    	Thread.sleep(1000);
    	out.println("2...");
    	Thread.sleep(1000);
    	out.println("1...");
    	Thread.sleep(1000);
    	out.println("Go");
    	Thread.sleep(1000);
    }
    

    public void sendQuestion(String[][] questions, int num) {
        out.println("Question " + (num + 1) + ": " + questions[num][0]);
        out.println("Answers: \r1. " + questions[num][1] + "\r2. " + questions[num][2] + "\r3. " + questions[num][3] + "\r4. " + questions[num][4]);
        out.println("Your Answer: ");
    }

    public void waitForAnswer() {
        while (!answers.containsKey(clientSocket.getInetAddress().toString())) {
            // Wait for the client to send their answer
            try {
                Thread.sleep(100); // Add a short delay to avoid busy-waiting
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Display game results once game has ended
    public int sendGameResults() {
    	out.println("Your Score: " + answers.get(clientSocket.getInetAddress().toString()));
    	return answers.get(clientSocket.getInetAddress().toString());
    }

    // DB query to update client's score after each game
    public void addScore(String ipAddress, int scoreIncrement) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/triviaGame", "root", "uwwhitewater");

            // Check if the player exists in the database
            if (!playerExists(ipAddress)) {
                // If player does not exist, add them to the database
                addNewPlayer(ipAddress);
            }

            // Update player's total_score in the players table
            String sql = "UPDATE players SET total_score = total_score + ? WHERE id = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, scoreIncrement); // Set the amount by which to increment total_score
            preparedStatement.setString(2, ipAddress); // Set the player ID (IP address)
            int rowsUpdated = preparedStatement.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Total score updated successfully for player with ID: " + ipAddress);
            } else {
                System.out.println("Player with ID " + ipAddress + " not found.");
            }
        } finally {
            // Close the prepared statement and connection
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }
    
    // DB query to update client's total games played after each game
    public void addGame(String ipAddress, int gameIncrement) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            // Establish database connection
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/triviaGame", "root", "uwwhitewater");

            // Check if the player exists in the database; add if not found
            if (!playerExists(ipAddress)) {
                addNewPlayer(ipAddress);
            }

            // Prepare SQL statement to update total_games
            String sql = "UPDATE players SET total_games = total_games + 1 WHERE id = ?";
            preparedStatement = connection.prepareStatement(sql);

            // Set the player ID as a parameter
            preparedStatement.setString(1, ipAddress);

            // Execute the update query
            int rowsUpdated = preparedStatement.executeUpdate();

            // Check if update was successful
            if (rowsUpdated > 0) {
                System.out.println("Total games updated successfully for player with ID: " + ipAddress);
            } else {
                System.out.println("Player with ID " + ipAddress + " not found or update failed.");
            }
        } catch (SQLException e) {
            // Handle database-related exceptions
            System.err.println("Error updating total games for player: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close PreparedStatement and database connection
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    // Method to retrieve player's ip as their id.
    private String getPlayerId(String ipAddress) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        String playerId = null; // Use String type to store player ID

        try {
            // Get database connection
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/triviaGame", "root", "uwwhitewater");

            // Retrieve playerId based on the id (IP address)
            String sql = "SELECT id FROM players WHERE id = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, ipAddress);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                playerId = resultSet.getString("id"); // Retrieve player ID (IP address) as String
            }
        } finally {
            // Close the prepared statement and connection
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }

        return playerId; // Return player ID (IP address) as String
    }
    
    // Displays all user's in the database when "leaderboard" is called by server
    public void displayLeaderboard(PrintWriter out) throws SQLException {
        String query = "SELECT id, total_score, total_games FROM players ORDER BY total_score DESC";

        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/triviaGame", "root", "uwwhitewater");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
        	out.println("");
            out.println("Global Leaderboard:");

            int position = 1; // Counter to increment leaderboard

            while (resultSet.next()) {
                String id = resultSet.getString("id");
                int totalScore = resultSet.getInt("total_score");
                int totalGames = resultSet.getInt("total_games");

                // Send the player information to the client
                out.println(position + ") " + id + " Score: " + totalScore + ", Games Played: " + totalGames);
                position++; // Increment for the next player
            }
        }
    }

    // Checks client's answer each round as well as update their total score and total games in DB
    public int checkAnswer(String[][] questions, String answer) throws SQLException {
        String correctAnswer = questions[num][5]; // Accessing the correct answer for the current question
        String id = clientSocket.getInetAddress().toString();
        
        // Check if the answer is correct
        if (correctAnswer.equals(answer)) {
            int scoreIncrement = 100; // Increment amount for correct answer
            try {
                addScore(id, scoreIncrement); // Update player's score
            } catch (SQLException e) {
                e.printStackTrace();
            }
            answers.put(id, answers.get(id) + scoreIncrement); // Update the player's score in the map
        }
        // Move to the next question
        num++;

        // Check if all questions have been answered
        if (num == 1) {
        	
            // All questions have been answered, increment total games for the player
            int gameIncrement = 1;
            addGame(id, gameIncrement);
        }

        return answers.get(id); // Return the updated score
        
    }
}
