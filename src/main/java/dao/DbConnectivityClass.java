package dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Person;
import service.MyLogger;
import service.UserSession;

import java.sql.*;

public class DbConnectivityClass {
    final static String DB_NAME="CSC311_BD_TEMP";
        final static String SQL_SERVER_URL = "jdbc:mysql://csc311adamesjava65.mysql.database.azure.com/";//update this server name
        final static String DB_URL = "jdbc:mysql://csc311adamesjava65.mysql.database.azure.com/" + DB_NAME;//update this database name
        final static String USERNAME = "adamy";// update this username
        final static String PASSWORD = "tester123456!";// update this password

        private final ObservableList<Person> data = FXCollections.observableArrayList();

    public ObservableList<Person> getData() {
        data.clear();
        String sql = "SELECT * FROM users ";
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = conn.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            if (!resultSet.isBeforeFirst()) {
                MyLogger.makeLog("No data found in users table."); // More specific log
            }
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String first_name = resultSet.getString("first_name");
                String last_name = resultSet.getString("last_name");
                String department = resultSet.getString("department");
                String major = resultSet.getString("major");
                String email = resultSet.getString("email");
                String imageURL = resultSet.getString("imageURL");
                data.add(new Person(id, first_name, last_name, department, major, email, imageURL));
            }
        } catch (SQLException e) {
            MyLogger.makeLog("SQL Error retrieving data: " + e.getMessage()); // Log the error
            e.printStackTrace(); // Keep debugging
            // Consider showing an error dialog to the user from the calling code if data is critical
        } catch (Exception e) { // Catch other potential exceptions like ClassNotFoundException
            MyLogger.makeLog("Error retrieving data: " + e.getMessage());
            e.printStackTrace();
        }
        return data;
    }

    public boolean connectToDatabase() {
        boolean hasRegistredUsers = false;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(SQL_SERVER_URL, USERNAME, PASSWORD);
                 Statement statement = conn.createStatement()) {
                statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
                MyLogger.makeLog("Database '" + DB_NAME + "' checked/created.");
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
                 Statement statement = conn.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS users ("
                        + "id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,"
                        + "first_name VARCHAR(200) NOT NULL,"
                        + "last_name VARCHAR(200) NOT NULL,"
                        + "department VARCHAR(200),"
                        + "major VARCHAR(200),"
                        + "email VARCHAR(200) NOT NULL UNIQUE,"
                        + "imageURL VARCHAR(1024))";
                statement.executeUpdate(sql);
                MyLogger.makeLog("Table 'users' checked/created.");

                String userAccountsTableSql = "CREATE TABLE IF NOT EXISTS user_accounts ("
                        + "username VARCHAR(255) NOT NULL PRIMARY KEY,"
                        + "password_hash VARCHAR(255) NOT NULL,"
                        + "privileges VARCHAR(50) NOT NULL DEFAULT 'USER'"
                        + ")";
                statement.executeUpdate(userAccountsTableSql);
                MyLogger.makeLog("Table 'user_accounts' checked/created.");

                try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM users")) {
                    if (resultSet.next()) {
                        int numUsers = resultSet.getInt(1);
                        if (numUsers > 0) {
                            hasRegistredUsers = true;
                            MyLogger.makeLog("Found " + numUsers + " existing users.");
                        } else {
                            MyLogger.makeLog("No users found in the table.");
                        }
                    }
                } // resultSet automatically closed
            } // conn and statement automatically closed

        } catch (ClassNotFoundException e) {
            MyLogger.makeLog("MySQL JDBC Driver not found: " + e.getMessage());
            e.printStackTrace();
            // This is often a critical error, might want to prevent the app from continuing
        } catch (SQLException e) {
            MyLogger.makeLog("Database connection or setup error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) { // Catch other potential runtime errors
            MyLogger.makeLog("An unexpected error occurred during database setup: " + e.getMessage());
            e.printStackTrace();
        }
            return hasRegistredUsers;
        }

    public UserSession validateLogin(String username, String plainPassword) {
        String sql = "SELECT password_hash, privileges FROM user_accounts WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String privileges = rs.getString("privileges");

                    // --- !! Use BCrypt (or your chosen library) to check the password !! ---
                    // Example using BCrypt (add the dependency first!)
                    // if (BCrypt.checkpw(plainPassword, storedHash)) {
                    // For now, using plain text comparison (REPLACE THIS!)
                    if (plainPassword.equals(storedHash)) { // !! REPLACE WITH HASH CHECK !!
                        MyLogger.makeLog("Login successful for user: " + username);
                        // Don't store the plain password in the session!
                        // You might not even need the hash in the session, just username and privileges.
                        return UserSession.getInstace(username, storedHash, privileges); // Or just username, privileges
                    } else {
                        MyLogger.makeLog("Login failed for user: " + username + " (Incorrect password)");
                        return null; // Password mismatch
                    }
                } else {
                    MyLogger.makeLog("Login failed: Username '" + username + "' not found.");
                    return null; // Username not found
                }
            }
        } catch (SQLException e) {
            MyLogger.makeLog("SQL Error during login validation for '" + username + "': " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) { // Catch hashing library errors too if applicable
            MyLogger.makeLog("Unexpected error during login validation for '" + username + "': " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void queryUserByLastName(String name) {

        String sql = "SELECT * FROM users WHERE last_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

            preparedStatement.setString(1, name);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                boolean found = false;
                while (resultSet.next()) {
                    found = true;
                    int id = resultSet.getInt("id");
                    String first_name = resultSet.getString("first_name");
                    String last_name = resultSet.getString("last_name");
                    String major = resultSet.getString("major");
                    String department = resultSet.getString("department");

                    MyLogger.makeLog("ID: " + id + ", Name: " + first_name + " " + last_name + " "
                            + ", Major: " + major + ", Department: " + department);
                }
                if (!found) {
                    MyLogger.makeLog("No user found with last name: " + name);
                }
            }

        } catch (SQLException e) {
            MyLogger.makeLog("Error querying user by last name: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void listAllUsers() {

        String sql = "SELECT * FROM users ";
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = conn.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            boolean found = false;
            while (resultSet.next()) {
                found = true;
                int id = resultSet.getInt("id");
                String first_name = resultSet.getString("first_name");
                String last_name = resultSet.getString("last_name");
                String department = resultSet.getString("department");
                String major = resultSet.getString("major");
                String email = resultSet.getString("email");

                MyLogger.makeLog("ID: " + id + ", Name: " + first_name + " " + last_name + " "
                        + ", Department: " + department + ", Major: " + major + ", Email: " + email);
            }
            if (!found) {
                MyLogger.makeLog("No users found in the database.");
            }

        } catch (SQLException e) {
            MyLogger.makeLog("Error listing all users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean insertUser(Person person) {

        String sql = "INSERT INTO users (first_name, last_name, department, major, email, imageURL) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

            preparedStatement.setString(1, person.getFirstName());
            preparedStatement.setString(2, person.getLastName());
            preparedStatement.setString(3, person.getDepartment());
            preparedStatement.setString(4, person.getMajor());
            preparedStatement.setString(5, person.getEmail());
            preparedStatement.setString(6, person.getImageURL());

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                MyLogger.makeLog("A new user was inserted successfully: " + person.getEmail());
                return true;
            } else {
                MyLogger.makeLog("User insertion failed (0 rows affected) for: " + person.getEmail());
                return false;
            }
        } catch (SQLException e) {
            MyLogger.makeLog("Error inserting user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean editUser(int id, Person p) {

        String sql = "UPDATE users SET first_name=?, last_name=?, department=?, major=?, email=?, imageURL=? WHERE id=?";

        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

            preparedStatement.setString(1, p.getFirstName());
            preparedStatement.setString(2, p.getLastName());
            preparedStatement.setString(3, p.getDepartment());
            preparedStatement.setString(4, p.getMajor());
            preparedStatement.setString(5, p.getEmail());
            preparedStatement.setString(6, p.getImageURL());
            preparedStatement.setInt(7, id);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                MyLogger.makeLog("User with ID " + id + " updated successfully.");
                return true;
            } else {
                MyLogger.makeLog("User with ID " + id + " not found or data unchanged.");
                return false;
            }
        } catch (SQLException e) {
            MyLogger.makeLog("Error updating user with ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteRecord(Person person) {
        if (person == null || person.getId() <= 0) { // Basic validation
            MyLogger.makeLog("Attempted to delete invalid person object or person with invalid ID.");
            return false;
        }
        int id = person.getId();
        String sql = "DELETE FROM users WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                MyLogger.makeLog("User with ID " + id + " deleted successfully.");
                return true;
            } else {
                MyLogger.makeLog("User with ID " + id + " not found for deletion.");
                return false;
            }

        } catch (SQLException e) {
            MyLogger.makeLog("Error deleting user with ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public int retrieveId(Person p) {
        int id = -1;
        if (p == null || p.getEmail() == null || p.getEmail().isEmpty()) {
            MyLogger.makeLog("Cannot retrieve ID for invalid person or empty email.");
            return id;
        }
        String sql = "SELECT id FROM users WHERE email=?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

            preparedStatement.setString(1, p.getEmail());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    id = resultSet.getInt("id");
                    MyLogger.makeLog("Retrieved ID " + id + " for email " + p.getEmail());
                } else {
                    MyLogger.makeLog("No user found with email " + p.getEmail() + " to retrieve ID.");
                }
            }

        } catch (SQLException e) {
            MyLogger.makeLog("Error retrieving ID for email " + p.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return id;
    }

    public boolean createUserAccount(String username, String hashedPassword) {
        String checkSql = "SELECT COUNT(*) FROM user_accounts WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD); // Use existing constants
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    MyLogger.makeLog("Attempt Creating Account Failed: Username '" + username + "' Already Exists");
                    return false;
                }
            }

        } catch (SQLException e) {
            MyLogger.makeLog("SQL Error Checking Username Existence For '" + username + "'- " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            MyLogger.makeLog("Unexpected Error Checking Username Existence For '" + username + "'- " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        String insertSql = "INSERT INTO user_accounts (username, password_hash, privileges) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            insertStmt.setString(1, username);
            insertStmt.setString(2, hashedPassword);
            insertStmt.setString(3, "USER");

            int rowsAffected = insertStmt.executeUpdate();

            if (rowsAffected > 0) {
                MyLogger.makeLog("New User Account Created Successfully For: " + username);
                return true;
            } else {
                MyLogger.makeLog("User Account Insertion Failed (0 Rows Affected) For: " + username);
                return false;
            }

        } catch (SQLException e) {
            MyLogger.makeLog("SQL Error Inserting New User '" + username + "'- " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            MyLogger.makeLog("Unexpected Error Inserting New User '" + username + "'- " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}