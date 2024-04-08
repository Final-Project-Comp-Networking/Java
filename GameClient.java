package com.jdbc.user;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Scanner;

public class GameClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;
    
    private static Connection connection = null;
	private static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try (
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))
        ) {
            String userInputLine;
            while ((userInputLine = userInput.readLine()) != null) {
                out.println(userInputLine);
                System.out.println("Server response: " + in.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        GameClient userDatabase = new GameClient();
		
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			String dbURL = "jdbc:mysql://localhost:3306/triviaGame";
			String username = "root";
			String password = "uwwhitewater";
			
			connection = DriverManager.getConnection(dbURL, username, password);
			
			System.out.print("Trivia Game\n");
			System.out.println("1. Log-In \n2. Insert new user record\n3. Leaderboard");
			int choice = Integer.parseInt(sc.nextLine());
			
			// Switch-case used as a menu system.
			switch (choice) {
			case 1:
				userDatabase.Login();
				break;
				
			case 2:
				userDatabase.insertUserRecord();
			/*
			case3:
				userDatabase.Leaderboards();
			*/
				
			default:
				break;
			}
			
		} catch (SQLException e) {
			// Custom exception message for connection failure
			throw new RuntimeException("MySQL connection failed.");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("An error has occurred.");
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}       
    }
    
    private void insertUserRecord() throws SQLException {
		
	    System.out.println("Creating Account.");
	    
	    int games_played = 0;
	    int total_pts = 0;
	    int num_correct_ans = 0;
	    int num_incorrect_ans = 0;
	    int highest_pts_achvd = 0;
	    int avg_pts = 0;
	    String password;
	    
	    System.out.print("Username: ");
	    String username = sc.nextLine();
	    
	    System.out.print("Email: ");
	    String email = sc.nextLine();
	    
	    while (true) {
	    	
	    	System.out.print("Password:");
	 	    password = sc.nextLine();
	 	    
	 	    System.out.println("Confirm Password: ");
	 	    String password2 = sc.nextLine();
	 	    
	 	    if (password.equals(password2)) {
	 	    	break;
	 	    }
	 	    else {
	 	    	System.out.print("passwords did not match, try again.\n");
	 	    	continue;
	 	    }
	    }
	    
	    System.out.print("What's your favorite color: ");
	    String theme_color = sc.nextLine();
	    
	    /*
	    System.out.println("Select a profile picture:");
	    String profile_pic = sc.nextLine();
	    */ // Will be implemented later, to have a set of profile pictures for a user to select from when creating an account.
	    
	    String profile_pic = "profile_pic_1.jpg";

	    String sql = "INSERT INTO user (username, games_played, profile_pic, theme_color, total_pts, num_correct_ans, num_incorrect_ans,"
	    		+ "highest_pts_achvd, avg_pts, email, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	    
	    
	    PreparedStatement preparedStatement = connection.prepareStatement(sql);
	    
	    preparedStatement.setString(1, username);
	    preparedStatement.setInt(2, games_played);
	    preparedStatement.setString(3, profile_pic);
	    preparedStatement.setString(4, theme_color);
	    preparedStatement.setInt(5, total_pts);
	    preparedStatement.setInt(6, num_correct_ans);
	    preparedStatement.setInt(7, num_incorrect_ans);
	    preparedStatement.setInt(8, highest_pts_achvd);
	    preparedStatement.setFloat(9, avg_pts);
	    preparedStatement.setString(10, email);
	    preparedStatement.setString(11, password);

	    int rows = preparedStatement.executeUpdate();

	    if (rows > 0) {
	        System.out.println("New user " + username + " created successfully.");
	    }
	}
    
    private void Login() throws SQLException {
    	// Email
    	System.out.print("Login\n\nEmail: ");
    	String email_input = sc.nextLine();
    	
    	// Password
    	while (true) {
    	System.out.print("Password: ");
    	String password_input = sc.nextLine();
    		
    	// Verifying password to log user in.	
    	Statement statement = null;
    	ResultSet resultSet = null;
    	
    	statement = connection.createStatement();
    	
    	String retrieve_password = "SELECT password FROM user WHERE email = '" + email_input + "'";
    	resultSet = statement.executeQuery(retrieve_password);
    	
    	String verify_password = null;
    	if(resultSet.next() ) {
    		verify_password = resultSet.getString("password");
    	}   	
    		if (password_input.equals(verify_password)) {
    			System.out.println("Log-in successful.");
    			break;
    		}
    		else {
    			System.out.println("Password is incorrect, please try again.");
    			continue;
    		}
    	}
    	
    	// Display user that logged in.
    	Statement statement2 = null;
    	ResultSet resultSet2 = null;
    	
    	statement2 = connection.createStatement();
    	
    	String retrieve_username_via_email = "SELECT username FROM user WHERE email = '" + email_input + "'";
    	resultSet2 = statement2.executeQuery(retrieve_username_via_email);
    	
    	String display_user = null;
    	if(resultSet2.next()) {
    		display_user = resultSet2.getString("username");
    	}
    	System.out.println("Welcome back, " + display_user);

    }
}
