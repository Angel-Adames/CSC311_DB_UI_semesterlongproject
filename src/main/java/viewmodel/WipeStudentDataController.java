package viewmodel;

import dao.DbConnectivityClass;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import model.Person;
import service.MyLogger;

import java.util.Optional;

public class WipeStudentDataController {

    private final DB_GUI_Controller mainController;
    private final DbConnectivityClass cnUtil;
    private final ObservableList<Person> personDataList;
    private final Label statusLabel;
    private final MenuBar mainMenuBar;

    public WipeStudentDataController(DB_GUI_Controller mainController,
                                     DbConnectivityClass cnUtil,
                                     ObservableList<Person> personDataList,
                                     Label statusLabel,
                                     MenuBar mainMenuBar) {
        this.mainController = mainController;
        this.cnUtil = cnUtil;
        this.personDataList = personDataList;
        this.statusLabel = statusLabel;
        this.mainMenuBar = mainMenuBar;
    }

    public void confirmAndWipeData() {
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Database Wipe");
        confirmationAlert.setHeaderText("WARNING: IRREVERSIBLE ACTION!");
        confirmationAlert.setContentText("Are you sure you want to delete ALL student records from the database?\nThis action cannot be undone.");
        if (mainMenuBar != null && mainMenuBar.getScene() != null) {
            confirmationAlert.initOwner(mainMenuBar.getScene().getWindow());
        }

        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            MyLogger.makeLog("User confirmed wiping all student data.");
            try {
                // Corrected method call here:
                boolean success = cnUtil.deleteAllRecords();
                if (success) {
                    personDataList.clear(); // Clear the ObservableList
                    mainController.clearForm();  // Call clearForm on the main controller instance
                    statusLabel.setText("All student data has been wiped successfully.");
                    MyLogger.makeLog("All student data wiped from database and UI.");
                    mainController.showErrorAlert("Operation Successful", "All student data has been successfully deleted from the database.");
                } else {
                    statusLabel.setText("Failed to wipe student data from the database.");
                    MyLogger.makeLog("Failed to wipe student data from the database (DbConnectivityClass returned false).");
                    mainController.showErrorAlert("Database Error", "Could not wipe all student data. Check logs for details.");
                }
            } catch (Exception e) {
                MyLogger.makeLog("Exception occurred during wiping student data: " + e.getMessage());
                e.printStackTrace();
                statusLabel.setText("Error occurred while wiping data.");
                mainController.showErrorAlert("Application Error", "An unexpected error occurred: " + e.getMessage());
            }
        } else {
            MyLogger.makeLog("User cancelled wiping all student data.");
            statusLabel.setText("Database wipe operation cancelled by user.");
        }
    }
}