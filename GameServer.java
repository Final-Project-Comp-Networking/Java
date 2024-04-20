
public class GameServerTest_1 {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // Creates a new thread to handle the client
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Connection connection;
    private String loggedInUser;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        setupDatabaseConnection();
    }

    @Override
    public void run() {
        try (
            // Input and output streams for communication with the client
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            // Send a welcome message to the client
            out.println("Welcome to Trivia Game.");
            out.flush();
            sendGameMenu(out);

            // Process the client's input
            processClientInput(in, out);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }
    
    private void setupDatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Use lowercase "localhost" here
            String dbURL = "jdbc:mysql://localhost:3306/triviaGame";
            String username = "root";
            String password = "uwwhitewater";
            
            connection = DriverManager.getConnection(dbURL, username, password);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Failed to connect to database.", e);
        }
    }
    
    // Display the game menu to the client
    private void sendGameMenu(PrintWriter out) {
        // Game menu
        out.println("Trivia Game");
        out.println("1. Log-In");
        out.println("2. Create Account");
        out.println("3. Leaderboards (WIP)");
        out.println("4. Play Game (WIP)");
        out.println("9. Log-Out");
        out.println("Please choose an option by typing the corresponding number.");
        out.flush();
    }
    
 // Display the game menu to the client
    private void sendPlayGameMenu(PrintWriter out) {
        // Game menu
        out.println("Play Game Menu");
        out.println("1. Create Lobby");
        out.println("2. Join Lobby");
        out.println("9. Return the main menu");
        out.println("Please choose an option by typing the corresponding number.");
        out.flush();
    }

    // A simple method to demonstrate the game starting for the client
    private void processClientInput(BufferedReader in, PrintWriter out) throws IOException, SQLException {
        while (true) {
            // Read client input
            String clientInput = in.readLine();
            System.out.println("Client " + clientSocket.getInetAddress() + " selected option " + clientInput + ".");

            if (clientInput == null) {
                break; // Client disconnected
            }

            int choice;
            try {
                choice = Integer.parseInt(clientInput);
            } catch (NumberFormatException e) {
                out.println("Invalid choice, please try again.");
                continue;
            }

            // Switch-case to handle user input choice
            switch (choice) {
                case 1:
                    Login(in, out);
                    break;
                case 2:
                    createAccount(in, out);
                    break;
                /*
                case 3:
                    leaderboards(in, out);
                    break;
				*/
                case 4:    
                    playGame(in, out);
                    break;
                case 9:
                	out.println("Logging " + loggedInUser + " out.");
                	loggedInUser = null;
                default:
                    out.println("Invalid choice, please try again.");
                    break;
            }
            out.flush();
            // Return to game menu
            sendGameMenu(out);
        }
    }
    
    private void playGame(BufferedReader in, PrintWriter out) throws SQLException, IOException {
        if (loggedInUser != null) {
            out.println("Hello " + loggedInUser + ".\n"); // Accessing the logged-in user's information
        } else {
            out.println("Please log in first.\n");
            return;
        }
        sendPlayGameMenu(out);
        while (true) {
            // Read client input
            String clientInput = in.readLine();
            System.out.println("Client " + clientSocket.getInetAddress() + " selected option " + clientInput + ".");

            if (clientInput == null) {
                break; // Client disconnected
            }

            int choice;
            try {
                choice = Integer.parseInt(clientInput);
            } catch (NumberFormatException e) {
                out.println("Invalid choice, please try again.");
                continue;
            }

            // Switch-case to handle user input choice
            switch (choice) {
                case 1:
                    createLobby(in, out);
                    break;
                case 2:
                    joinLobby(in, out);
                    break;
                case 9:
                	return;
                default:
                    out.println("Invalid choice, please try again.");
                    break;
            }
            out.flush();
            // Return to game menu
            sendPlayGameMenu(out);
        }
    }

    private void createLobby(BufferedReader in, PrintWriter out) throws IOException, SQLException {
        
    	out.println("Create a lobby:");
    	out.flush();
    	
    	//Get information from client in creating lobby
    	out.println("Name of the lobby: ");
    	out.flush();
    	String lobbyName = in.readLine();
    	System.out.println(clientSocket.getInetAddress() + " created a new lobby named \"" + lobbyName + "\".");
    	
    	out.println("Enter max players able to join lobby: ");
    	out.flush();
    	int maxPlayers = Integer.parseInt(in.readLine());
    	System.out.println(clientSocket.getInetAddress() + " set max players in \" " + lobbyName + "\" to " + maxPlayers + ".");
    	out.flush();
    	
    	// Inserting data in database
        String sql = "INSERT INTO lobbies (name, max_players, current_players) VALUES (?, ?, ?)";
        
        int currentPlayers = 1;
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, lobbyName);
            preparedStatement.setInt(2,maxPlayers);
            preparedStatement.setInt(3, currentPlayers);
            
            int rowsAffected = preparedStatement.executeUpdate();

            // Inform the client if the account creation was successful
            if (rowsAffected > 0) {
                out.println(lobbyName + " created successfully.\n");
            } 
            else {
                out.println("Failed to create lobby.\n");
            }
            out.flush();
        }  	
    }

    private void joinLobby(BufferedReader in, PrintWriter out) throws IOException, SQLException {
        // Query the database to retrieve all available lobbies
        String sql = "SELECT name, current_players, max_players FROM lobbies";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                // Display available lobbies to the client with an index
                out.println("Available Lobbies:");
                int index = 1;
                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    int currentPlayers = resultSet.getInt("current_players");
                    int maxPlayers = resultSet.getInt("max_players");
                    out.println(index + ". " + name + " (" + currentPlayers + "/" + maxPlayers + ") players");
                    index++;
                }
                out.println("Choose a lobby to join (enter the corresponding number):");
                out.flush();
                
                // Read client input to select a lobby
                String input = in.readLine();
                int lobbyChoice = Integer.parseInt(input);
                
                // Process the lobby choice
                if (lobbyChoice >= 1 && lobbyChoice < index) {
                    // Get the selected lobby name based on the index
                    resultSet.absolute(lobbyChoice);
                    String selectedLobbyName = resultSet.getString("name");
                    
                    // Update the database to reflect the new player joining
                    sql = "UPDATE lobbies SET current_players = current_players + 1 WHERE name = ?";
                    try (PreparedStatement updateStatement = connection.prepareStatement(sql)) {
                        updateStatement.setString(1, selectedLobbyName);
                        int rowsUpdated = updateStatement.executeUpdate();
                        if (rowsUpdated > 0) {
                            out.println("You joined " + selectedLobbyName + ". Waiting for other players...\n");
                        } else {
                            out.println("Failed to join the lobby. Please try again.\n");
                        }
                    }
                } else {
                    out.println("Invalid lobby choice. Please enter a valid number.\n");
                }
            }
        }
    }



    
    private void createAccount(BufferedReader in, PrintWriter out) throws SQLException, IOException {
        out.println("Creating Account");
        out.flush();

        // Getting username, email, and password from user
        // Username
        out.println("Enter username: ");
        out.flush();
        String username = in.readLine();
        System.out.println("Client " + clientSocket.getInetAddress() + " entered username: " + username + ".");
        
        out.println("Username entered successfully.");
        out.println("Enter email: ");
        out.flush();
        String email = in.readLine();
        System.out.println("Client " + clientSocket.getInetAddress() + " entered email: " + email + ".");
        
        out.println("Email entered successfully.");
        out.println("Enter password: ");
        out.flush();
        String password = in.readLine();
        System.out.println("Client " + clientSocket.getInetAddress() + " entered password: " + password + ".");
        
        out.println("Password entered successfully.");
        out.println("Account created successfully.");
        out.flush();
        
        

        // Inserting data in database
        String sql = "INSERT INTO players(username, email, password) VALUES (?, ?, ?)";
        
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, email);
            preparedStatement.setString(3, password);
            
            int rowsAffected = preparedStatement.executeUpdate();

            // Inform the client if the account creation was successful
            if (rowsAffected > 0) {
                out.println("Account created successfully for " + username + ".\n");
            } 
            else {
                out.println("Failed to create account.\n");
            }
            out.flush();
        }
    }

    private void Login(BufferedReader in, PrintWriter out) throws SQLException, IOException {
        out.println("Log-In");
        out.flush();

        // Get email and password from client
        out.println("Enter email: ");
        out.flush();
        String email = in.readLine();
        if (email != null) {
        	System.out.println(clientSocket.getInetAddress() + " entered email successfully.");
        }
        else {
        	System.out.println(clientSocket.getInetAddress() + " failed to receive email.");
        }
        out.flush();

        out.println("Enter password: ");
        out.flush();
        String password = in.readLine();
        if (password != null) {
        	System.out.println(clientSocket.getInetAddress() + " entered password successfully.");
        }
        else {
        	System.out.println(clientSocket.getInetAddress() + " failed to receive password.");
        }
        out.flush();

        // Verification of email and password
        String sql = "SELECT username, password FROM players WHERE email = ?";
        
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, email);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String DBpassword = resultSet.getString("password");
                    String username = resultSet.getString("username");

                    if (DBpassword.equals(password)) {
                        out.println("Login successful. Welcome back " + username + ".\n");
                        loggedInUser = username;
                        //System.out.println(clientSocket.getInetAddress() + " is now known as " + username + ".");
                    } 
                    else {
                        out.println("Invalid email or password, try again.\n");
                    }
                } 
                else {
                    out.println("Email does not exist.\n");
                }
                out.flush();
            }
        }
    }

    private void closeResources() {
        try {
            if (connection != null) {
                connection.close();
            }
            clientSocket.close();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}

