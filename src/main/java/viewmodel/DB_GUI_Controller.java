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

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;

public class DB_GUI_Controller implements Initializable {

    private static final String CSV_SEPARATOR = ",";
    private static final String CSV_HEADER = "FirstName,LastName,Department,Major,Email,ImageURL";
    private final DbConnectivityClass cnUtil = new DbConnectivityClass();
    private final ObservableList<Person> data = cnUtil.getData();
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
    private MenuItem importCSV;
    @FXML
    private MenuItem exportCSV;
    @FXML
    private Label statusLb;
    @FXML
    private TableColumn<Person, Integer> tv_id;
    @FXML
    private TableColumn<Person, String> tv_fn, tv_ln, tv_department, tv_major, tv_email;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        data.setAll(cnUtil.getData());
        tv.setItems(data);

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
                if (newSelection != null) {
                    populateForm(newSelection);
                } else {
                    clearForm();
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

    @FXML
    private void handleImport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open CSV File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        Stage stage = (Stage) menuBar.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(menuBar.getScene().getWindow());

        if (selectedFile != null) {
            statusLb.setText("Importing From " + selectedFile.getName() + "...");
            List<Person> importPersons = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            int lineNum = 0;
            boolean headerSkip = false;

            try (BufferedReader br = new BufferedReader(new FileReader(selectedFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lineNum++;
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (!headerSkip && line.startsWith("FirstName") || line.startsWith("ID")) {
                        MyLogger.makeLog("Skipping Header Row: " + line);
                        headerSkip = true;
                        continue;
                    }

                    String[] fields = line.split(CSV_SEPARATOR, -1);

                    if (fields.length != 6) {
                        errors.add("Line " + lineNum + ": Invalid Number Fields (Expected 6, Found " + fields.length + ")");
                        continue;
                    }

                    String firstName = fields[0].trim();
                    String lastName = fields[1].trim();
                    String department = fields[2].trim();
                    String major = fields[3].trim();
                    String email = fields[4].trim();
                    String imageURL = fields[5].trim();

                    if (firstName.isBlank() || lastName.isBlank() || department.isBlank() || major.isBlank() || email.isBlank() || imageURL.isBlank()) {
                        errors.add("Line " + line + ": Missing Required Field(s)");
                        continue;
                    }
                    if (!firstName.matches("^[a-zA-Z]+$") || !lastName.matches("^[a-zA-Z]+$") || !department.matches("^[a-zA-Z]+$")) {
                        errors.add("Line " + line + ": Name or Department Fields Must Contain Only Letters");
                        continue;
                    }
                    if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
                        errors.add("Line " + line + ": Invalid Email Format For '" + email + "'");
                        continue;
                    }
                    if (!imageURL.matches("^[^0-9]+$")) { // Your image URL validation
                        errors.add("Line " + line + ": Invalid Image URL (Not Contain Numbers) For '" + imageURL + "'");
                        continue;
                    }

                    Major selectedMajor = null;
                    try {
                        selectedMajor = Major.fromDisplayString(major);
                        if (selectedMajor == null) {
                            errors.add("Line " + line + ": Invalid Major '" + major + "'");
                            continue;
                        }
                    } catch (IllegalArgumentException e) {
                        errors.add("Line " + line + ": Invalid Major '" + major + "'");
                        continue;
                    }

                    Person p = new Person(firstName, lastName, department, selectedMajor.toString(), email, imageURL);
                    importPersons.add(p);
                }
            } catch (IOException e) {
                MyLogger.makeLog("Error Handling Import: " + e.getMessage());
                showErrorAlert("File Read Error", "Could Not Read The CSV File: " + e.getMessage());
                statusLb.setText("Error Handling Import");
                return;
            }

            int successCount = 0;
            int failCount = 0;
            if (!importPersons.isEmpty()) {
                statusLb.setText("Inserting " + importPersons.size() + " Valid Records Into Database . . .");
                for (Person p : importPersons) {
                    try {
                        boolean inserted = cnUtil.insertUser(p);
                        if (inserted) {
                            int retrieveId = cnUtil.retrieveId(p);
                            if (retrieveId > 0) {
                                p.setId(retrieveId);
                                Platform.runLater(() -> data.add(p));
                                successCount++;
                            } else {
                                errors.add("Record Inserted For " + p.getEmail() + " But Failed To Retrieve ID");
                                failCount++;
                            }
                        } else {
                            errors.add("Failed To Insert Record For " + p.getEmail() + "(Database Error Or Duplicate Email");
                            failCount++;
                        }
                    } catch (Exception e) {
                        MyLogger.makeLog("Error Inserting Imported User " + p.getEmail() + ": " + e.getMessage());
                        errors.add("Failed To Insert Record For " + p.getEmail() + ": " + e.getMessage());
                        failCount++;
                    }
                }
            }

            String summary = "Import Finished " + successCount + " Records Added";
            if (failCount > 0) {
                summary += " (" + failCount + " Failed)";
            }
            if (!errors.isEmpty()) {
                summary += " See Details Below Or In Log";
                showImportErrors(errors);
            }
            statusLb.setText(summary);
            MyLogger.makeLog(summary + (errors.isEmpty() ? "" : " Errors: " + String.join("; ", errors)));
        } else {
            statusLb.setText("CSV Import Cancelled");
        }
    }

    @FXML
    private void handleExport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Data To CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("student_data_export.csv");
        Stage stage = (Stage) menuBar.getScene().getWindow();
        File selectedFile = fileChooser.showSaveDialog(stage);

        if (selectedFile != null) {
            statusLb.setText("Exporting Data To " + selectedFile.getName() + "...");
            try (PrintWriter writer = new PrintWriter(new FileWriter(selectedFile))) {
                writer.println("ID,FirstName,LastName,Department,Major,Email,ImageURL");

                for (Person p : data) {
                    writer.println(formatPersonAsCSV(p));
                }

                statusLb.setText("Data Successfully Exported To " + selectedFile.getName());
                MyLogger.makeLog("Data Exported To " + selectedFile.getName());
                showErrorAlert("Export Successful", "Data Exported To:\n" + selectedFile.getAbsolutePath());
            } catch (IOException e) {
                MyLogger.makeLog("Error Exporting Data: " + e.getMessage());
                showErrorAlert("Export Error", "Could Not Export Data: " + e.getMessage());
                statusLb.setText("Error Exporting File");
            }
        } else {
            statusLb.setText("CSV Export Cancelled");
        }
    }

    private String formatPersonAsCSV(Person p) {
        return p.getId() + CSV_SEPARATOR +
                p.getFirstName() + CSV_SEPARATOR +
                p.getLastName() + CSV_SEPARATOR +
                p.getDepartment() + CSV_SEPARATOR +
                p.getMajor() + CSV_SEPARATOR +
                p.getEmail() + CSV_SEPARATOR +
                p.getImageURL();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showImportErrors(List<String> errors) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Import Issues");
        alert.setHeaderText("Some Records Could Not Be Imported - See Details");

        TextArea textArea = new TextArea(String.join("\n", errors));
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefSize(480, 320);
        alert.showAndWait();
    }

    private void populateForm(Person p) {
        if (p != null) {
            first_name.setText(p.getFirstName());
            last_name.setText(p.getLastName());
            department.setText(p.getDepartment());
            try {
                Major selectedMajor = Major.fromDisplayString(p.getMajor());
                major.setValue(selectedMajor);
            } catch (IllegalArgumentException e) {
                MyLogger.makeLog("Error Populating Form: " + e.getMessage());
                major.setValue(null);
            }
            email.setText(p.getEmail());
            imageURL.setText(p.getImageURL());
        }
    }

    @FXML
    protected void selectedItemTV(MouseEvent mouseEvent) {
        Person p = tv.getSelectionModel().getSelectedItem();
        if (p != null) {
            populateForm(p);
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
                maj.name(), email.getText(), imageURL.getText());

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

        public static Major fromDisplayString(String display) {
            for (Major m : Major.values()) {
                if (m.display.equalsIgnoreCase(display)) {
                    return m;
                }
            }
            throw new IllegalArgumentException("No constant with display text " + display + " found");
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