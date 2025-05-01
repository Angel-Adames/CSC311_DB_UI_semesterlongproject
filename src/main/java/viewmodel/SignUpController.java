package viewmodel;

import dao.DbConnectivityClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import service.MyLogger;

import java.util.Objects;

public class SignUpController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label statusLabel;

    private final DbConnectivityClass dbConnectivity = new DbConnectivityClass();

    @FXML
    public void createNewAccount(ActionEvent actionEvent) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username == null || username.trim().isEmpty()) {
            statusLabel.setText("Username Cannot Be Empty");
            usernameField.requestFocus();
            return;
        }
        if (password == null || password.isEmpty()) {
            statusLabel.setText("Password Cannot Be Empty");
            passwordField.requestFocus();
            return;
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            statusLabel.setText("Please confirm your password.");
            confirmPasswordField.requestFocus(); // Set focus to confirm password field
            return; // Stop processing
        }
        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Password Do Not Match");
            passwordField.clear();
            confirmPasswordField.clear();
            passwordField.requestFocus();
            return;
        }

        String hashedPassword = password;

        try {
            boolean success = dbConnectivity.createUserAccount(username, hashedPassword);

            if (success) {
                MyLogger.makeLog("Account Successfully Created For User: " + username);
                usernameField.clear();
                passwordField.clear();
                confirmPasswordField.clear();
                statusLabel.setText("Account Created Successfully!");

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Account Created");
                alert.setHeaderText("Success!");
                alert.setContentText("Your Account Was Successfully Created - You Can Now Log In.");
                alert.showAndWait();
                goBack(actionEvent);
            } else {
                statusLabel.setText("Could Not Create Account - Username Might Already Exist.");
                usernameField.requestFocus();
            }
        } catch (Exception e) {
            statusLabel.setText("An Error Occurred - Please Try Again Later.");
            MyLogger.makeLog("Error Creating Account For: " + username + " - " + e.getMessage());
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Account Creation Error");
            errorAlert.setHeaderText("An Unexpected Error Occurred");
            errorAlert.setContentText("Could Not Create The Account Due To An Internal Error.");
            errorAlert.showAndWait();
        }
    }

    public void goBack(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/view/login.fxml")));
            Scene scene = new Scene(root, 900, 600);

            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/lightTheme.css")).toExternalForm());

            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.setTitle("Login");
            window.show();
        } catch (Exception e) {
            MyLogger.makeLog("Error Navigating Back To Login Screen: " + e.getMessage());
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Navigation Error");
            errorAlert.setHeaderText("Could Not Load Login Screen.");
            errorAlert.setContentText("Unexpected Error Occurred: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }
}
