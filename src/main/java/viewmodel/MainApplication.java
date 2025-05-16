package viewmodel;

import dao.DbConnectivityClass;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.MyLogger;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class MainApplication extends Application {

    private static Scene scene;
    private static DbConnectivityClass cnUtil;
    private Stage primaryStage;

    public static void main(String[] args) {
        cnUtil = new DbConnectivityClass();
        launch(args);

    }

    @Override
    public void start(Stage primaryStage) {
        MyLogger.makeLog("Application Starting...");
        DbConnectivityClass dbConnectivity = new DbConnectivityClass();
        try {
            MyLogger.makeLog("Attempting to connect to database and ensure tables exist...");
            boolean dbReady = dbConnectivity.connectToDatabase(); // Call the method
            // You might use the 'dbReady' boolean later if needed,
            // but the main goal here is table creation.
            MyLogger.makeLog("Database connection and table check complete.");

            // --- Load the initial FXML (e.g., login screen) ---
            // Ensure the path to your login FXML is correct
            URL loginFxmlUrl = getClass().getResource("/view/login.fxml");
            if (loginFxmlUrl == null) {
                throw new IOException("Cannot find login.fxml resource");
            }
            Parent root = FXMLLoader.load(loginFxmlUrl);

            Scene scene = new Scene(root, 900, 600);

            URL cssUrl = getClass().getResource("/css/lightTheme.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                MyLogger.makeLog("Warning: Could not find default CSS stylesheet.");
            }

            primaryStage.setTitle("Login"); // Set the initial window title
            primaryStage.setScene(scene);
            primaryStage.show();
            MyLogger.makeLog("Login screen displayed.");

        }  catch (IOException e) {
            MyLogger.makeLog("FATAL: Could not load the initial FXML scene: " + e.getMessage());
            e.printStackTrace();
            showFatalError("Application Error", "Could not load the main application interface.", e.getMessage());
        } catch (Exception e) {
            // Catch potential errors from connectToDatabase or other startup issues
            MyLogger.makeLog("FATAL: An unexpected error occurred during application startup: " + e.getMessage());
            e.printStackTrace();
            showFatalError("Application Startup Error", "An unexpected error occurred during startup.", e.getMessage());
        }
    }

    private void showFatalError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText("The application cannot continue.\nDetails: " + content);
        alert.showAndWait();
        // Optionally call Platform.exit() here if the error is truly unrecoverable
    }

    private void showScene1() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/view/splashscreen.fxml")));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/lightTheme.css")).toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.show();
            changeScene();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeScene() {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/view/login.fxml")).toURI().toURL());
            Scene currentScene = primaryStage.getScene();
            Parent currentRoot = currentScene.getRoot();
            currentScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/lightTheme.css")).toExternalForm());
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(3), currentRoot);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                Scene newScene = new Scene(newRoot, 900, 600);
                primaryStage.setScene(newScene);
                primaryStage.show();
            });
            fadeOut.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}