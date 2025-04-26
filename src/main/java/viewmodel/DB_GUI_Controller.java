package viewmodel;

import dao.DbConnectivityClass;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Person;
import service.MyLogger;

import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

public class DB_GUI_Controller implements Initializable {

    @FXML
    TextField first_name, last_name, department, email, imageURL;
    @FXML
    ComboBox<Major> major;
    @FXML
    ImageView img_view;
    @FXML
    MenuBar menuBar;
    @FXML
    private TableView<Person> tv;
    @FXML
    private Button editBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Button addBtn;
    @FXML
    private MenuItem editItem;
    @FXML
    private MenuItem deleteItem;
    @FXML
    private MenuItem addItem;
    @FXML
    private Label statusLb;
    @FXML
    private TableColumn<Person, Integer> tv_id;
    @FXML
    private TableColumn<Person, String> tv_fn, tv_ln, tv_department, tv_major, tv_email;
    private final DbConnectivityClass cnUtil = new DbConnectivityClass();
    private final ObservableList<Person> data = cnUtil.getData();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            tv_id.setCellValueFactory(new PropertyValueFactory<>("id"));
            tv_fn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
            tv_ln.setCellValueFactory(new PropertyValueFactory<>("lastName"));
            tv_department.setCellValueFactory(new PropertyValueFactory<>("department"));
            tv_major.setCellValueFactory(new PropertyValueFactory<>("major"));
            tv_email.setCellValueFactory(new PropertyValueFactory<>("email"));
            tv.setItems(data);

            if (editBtn != null) {
                editBtn.setDisable(true);
            }

            if (deleteBtn != null) {
                deleteBtn.setDisable(true);
            }

            if (editItem != null) {
                editItem.setDisable(true);
            }

            if (deleteItem != null) {
                deleteItem.setDisable(true);
            }

            tv.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                boolean isItemSelected = newSelection != null;
                if (editBtn != null) {
                    editBtn.setDisable(!isItemSelected);
                }
                if (deleteBtn != null) {
                    deleteBtn.setDisable(!isItemSelected);
                }
                if (editItem != null) {
                    editItem.setDisable(!isItemSelected);
                }
                if (deleteItem != null) {
                    deleteItem.setDisable(!isItemSelected);
                }
            });

            major.setItems(FXCollections.observableArrayList(Major.values()));

            addTextFieldListeners();

            MyLogger.makeLog("Data Retrieved: " + data.size() + " Records");
        } catch (Exception e) {
            System.err.println("Error During Initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addTextFieldListeners() {
        Predicate<String> nameValidator = s -> s != null && !s.isBlank() && s.matches("^[a-zA-Z]+$");
        Predicate<String> emailValidator = s -> s != null && !s.isBlank() && s.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");
        Predicate<String> departmentValidator = s -> s != null && !s.isBlank() && s.matches("^[a-zA-Z]+$");
        Predicate<String> imageValidator = s -> s != null && !s.isBlank() && s.matches("^[^0-9]+$");

        first_name.textProperty().addListener((obs, oldVal, newVal) -> updateAddButtonState(nameValidator.test(newVal)));
        last_name.textProperty().addListener((obs, oldVal, newVal) -> updateAddButtonState(nameValidator.test(newVal)));
        department.textProperty().addListener((obs, oldVal, newVal) -> updateAddButtonState(departmentValidator.test(newVal)));
        email.textProperty().addListener((obs, oldVal, newVal) -> updateAddButtonState(emailValidator.test(newVal)));
        imageURL.textProperty().addListener((obs, oldVal, newVal) -> updateAddButtonState(imageValidator.test(newVal)));
    }

    private void updateAddButtonState(boolean isValid) {
        boolean allFieldsValid =
                !first_name.getText().isBlank() && !last_name.getText().isBlank() &&
                        !department.getText().isBlank() &&
                        !email.getText().isBlank() && !imageURL.getText().isBlank() &&
                        major.getValue() != null &&
                        isValid;

        addBtn.setDisable(!allFieldsValid);
        addItem.setDisable(!allFieldsValid);
    }

    @FXML
    protected void addNewRecord() {

        String firstName = first_name.getText();
        String lastName = last_name.getText();
        String emailText = email.getText();
        String dept = department.getText();
        String image = imageURL.getText();
        Major selectedMajor = major.getValue();

        String namePattern = "^[a-zA-Z]+$"; // Only allow letters
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$"; // Must contain '@'
        String departmentPattern = "^[a-zA-Z]+$"; // Only allow letters
        String imagePattern = "^[^0-9]+$"; // Do not allow numbers

        if (firstName == null || firstName.isBlank() ||
                lastName == null || lastName.isBlank() ||
                dept == null || dept.isBlank() ||
                selectedMajor == null ||
                emailText == null || emailText.isBlank() ||
                image == null || image.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Missing Information");
            alert.setContentText("Please Fill In All Fields");
            alert.showAndWait();
            return;
        }

        if (!firstName.matches(namePattern)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Invalid First Name");
            alert.setContentText("First Name Must Contain Only Letters.");
            alert.showAndWait();
            return;
        }

        if (!lastName.matches(namePattern)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Invalid Last Name");
            alert.setContentText("Last Name Must Contain Only Letters.");
            alert.showAndWait();
            return;
        }

        if (!emailText.matches(emailPattern)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Invalid Email");
            alert.setContentText("Email Must Contain '@' Symbol.");
            alert.showAndWait();
            return;
        }

        if (!dept.matches(departmentPattern)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Invalid Department");
            alert.setContentText("Department Must Contain Only Letters.");
            alert.showAndWait();
            return;
        }

        if (!image.matches(imagePattern)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Invalid Image URL");
            alert.setContentText("Image URL Must Not Contain Numbers.");
            alert.showAndWait();
            return;
        }
        Person p = new Person(firstName, lastName, dept, selectedMajor.toString(), emailText, image);

        try {
            boolean successful = cnUtil.insertUser(p);
            if (successful) {
                int retrieveId = cnUtil.retrieveId(p);

                if (retrieveId > 0) {
                p.setId(retrieveId);

                data.add(p);

                clearForm();
                System.out.println("Record Added Successfully For ID: " + retrieveId);
                statusLb.setText("Record Added Successfully For ID: " + retrieveId);
                } else {
                    System.err.println("Record Might Be Inserted, But Failed To Retrieve ID For: " + p.getFirstName());
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Database Warning");
                    alert.setHeaderText("Record Added - ID Not Retrieved");
                    alert.setContentText("Record Might Be Inserted, But Failed To Retrieve ID For: " + p.getFirstName());
                    alert.showAndWait();
                    data.setAll(cnUtil.getData());
                    clearForm();
                    statusLb.setText("Failed To Retrieve ID For: " + p.getFirstName());
                }
            } else {
                System.err.println("Failed Inserting Record For: " + p.getFirstName());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Database Error");
                alert.setHeaderText("Failed To Add Record");
                alert.setContentText("Could Not Save Record To Database.");
                alert.showAndWait();
                statusLb.setText("Failed To Add Record For: " + p.getFirstName());
            }
        } catch (Exception e) {
            System.err.println("Error During Database Operation For addNewRecord: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Application Error");
            alert.setHeaderText("Error Adding Record");
            alert.setContentText("Unexpected Error Occurred While Adding the record: " + e.getMessage());
            alert.showAndWait();
            statusLb.setText("Error Adding Record: " + e.getMessage());
        }
    }

    @FXML
    protected void clearForm() {
        first_name.setText("");
        last_name.setText("");
        department.setText("");
        major.setValue(null);
        email.setText("");
        imageURL.setText("");
        addBtn.setDisable(true);
        addItem.setDisable(true);
        statusLb.setText("Ready");
    }

    @FXML
    protected void logOut(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/view/login.fxml")));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/lightTheme.css")).getFile());
            Stage window = (Stage) menuBar.getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void closeApplication() {
        System.exit(0);
    }

    @FXML
    protected void displayAbout() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/view/about.fxml")));
            Stage stage = new Stage();
            Scene scene = new Scene(root, 600, 500);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void editRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();
        if (p == null) {
            System.err.println("Edit Button Clicked But No Item Selected");
            statusLb.setText("No Item Selected For Edit");
            return;
        }

        int index = data.indexOf(p);

        Major maj = major.getValue();

        if (maj == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Invalid Major");
            alert.setContentText("Please Select a Major.");
            alert.showAndWait();
            return;
        }

        Person p2 = new Person(p.getId(), first_name.getText(), last_name.getText(), department.getText(),
                maj.name(), email.getText(),  imageURL.getText());

        boolean successful = cnUtil.editUser(p.getId(), p2);

        if (successful) {
            data.set(index, p2);
            tv.getSelectionModel().select(index);
            clearForm();
            statusLb.setText("Record Updated Successfully For ID: " + p.getId());
        } else {
            System.err.println("Failed To Update Record In Database For ID: " + p.getId());
            Alert alert = new Alert(Alert.AlertType.ERROR, "CANNOT UPDATE RECORD");
            alert.showAndWait();
            statusLb.setText("Failed To Update Record For ID: " + p.getId());
        }
    }

    @FXML
    protected void deleteRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();

        if (p == null) {
            System.err.println("Delete Button Clicked But No Item Selected");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Record?");
        confirmation.setContentText("Are You Sure You Want To Delete The Record For "
                + p.getFirstName() + " " + p.getLastName() + "?");

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            int index = data.indexOf(p);
            boolean successful = cnUtil.deleteRecord(p);

            if (successful) {
                data.remove(index);
                clearForm();
                statusLb.setText("Record Delete Successfully For ID: " + p.getId());
            } else {
                System.err.println("Failed To Delete Record From Data For ID: " + p.getId());
                Alert alert = new Alert(Alert.AlertType.ERROR, "CANNOT DELETE RECORD");
                alert.showAndWait();
                statusLb.setText("Failed To Delete Record For ID: " + p.getId());
            }
        }
    }

    @FXML
    protected void showImage() {
        File file = (new FileChooser()).showOpenDialog(img_view.getScene().getWindow());
        if (file != null) {
            img_view.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    protected void addRecord() {
        showSomeone();
    }

    @FXML
    protected void selectedItemTV(MouseEvent mouseEvent) {
        Person p = tv.getSelectionModel().getSelectedItem();
        if (p != null) {
            first_name.setText(p.getFirstName());
            last_name.setText(p.getLastName());
            department.setText(p.getDepartment());
            major.setValue(Major.valueOf(p.getMajor())); // Set the ComboBox to the current major
            email.setText(p.getEmail());
            imageURL.setText(p.getImageURL());
        }
    }

    public void lightTheme(ActionEvent actionEvent) {
        try {
            Scene scene = menuBar.getScene();
            Stage stage = (Stage) scene.getWindow();
            stage.getScene().getStylesheets().clear();
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/lightTheme.css")).toExternalForm());
            stage.setScene(scene);
            stage.show();
            System.out.println("light " + scene.getStylesheets());
            statusLb.setText("Light Theme Applied");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void darkTheme(ActionEvent actionEvent) {
        try {
            Stage stage = (Stage) menuBar.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/darkTheme.css")).toExternalForm());
            statusLb.setText("Dark Theme Applied");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showSomeone() {
        Dialog<Results> dialog = new Dialog<>();
        dialog.setTitle("New User");
        dialog.setHeaderText("Please specify…");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField textField1 = new TextField("Name");
        TextField textField2 = new TextField("Last Name");
        TextField textField3 = new TextField("Email ");
        ObservableList<Major> options =
                FXCollections.observableArrayList(Major.values());
        ComboBox<Major> comboBox = new ComboBox<>(options);
        comboBox.getSelectionModel().selectFirst();
        dialogPane.setContent(new VBox(8, textField1, textField2, textField3, comboBox));
        Platform.runLater(textField1::requestFocus);
        dialog.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK) {
                return new Results(textField1.getText(),
                        textField2.getText(), comboBox.getValue());
            }
            return null;
        });
        Optional<Results> optionalResult = dialog.showAndWait();
        optionalResult.ifPresent((Results results) -> {
            MyLogger.makeLog(
                    results.fname + " " + results.lname + " " + results.major);
        });
    }

    private enum Major {
        CS("Computer Science"),
        CPIS("Computer Information Systems"),
        CE("Computer Engineering"),
        ART("Art"),
        ENGLISH("English"),
        BUSINESS("Business"),
        MATH("Mathematics"),
        PHILOSOPHY("Philosophy"),
        PHYSICS("Physics"),
        CHEMISTRY("Chemistry"),
        BIOLOGY("Biology"),
        STATISTICS("Statistics");

        private final String display;

        Major(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    private static class Results {

        String fname;
        String lname;
        Major major;

        public Results(String name, String date, Major venue) {
            this.fname = name;
            this.lname = date;
            this.major = venue;
        }
    }

}