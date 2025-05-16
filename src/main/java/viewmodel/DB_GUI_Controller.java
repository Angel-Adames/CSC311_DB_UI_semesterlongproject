package viewmodel;

import dao.DbConnectivityClass;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
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
import javafx.collections.transformation.FilteredList;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Person;
import org.jetbrains.annotations.NotNull;
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
    private MenuItem importTXT;
    @FXML
    private MenuItem exportTXT;
    @FXML
    private Label statusLb;
    @FXML
    private TextField searchField;
    @FXML
    private TableColumn<Person, Integer> tv_id;
    @FXML
    private TableColumn<Person, String> tv_fn, tv_ln, tv_department, tv_major, tv_email;
    private FilteredList<Person> filteredData;

    private final Predicate<String> nameValidator = s -> s != null && !s.isBlank() && s.matches("^[a-zA-Z]+$");
    private final Predicate<String> emailValidator = s -> s != null && !s.isBlank() && s.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");
    private final Predicate<String> departmentValidator = s -> s != null && !s.isBlank() && s.matches("^[a-zA-Z]+$");
    // Allow any non-blank string, as the actual image loading will validate the path/URL
    private final Predicate<String> imageValidator = s -> s != null && !s.isBlank();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        try {
            tv_id.setCellValueFactory(new PropertyValueFactory<>("id"));
            tv_fn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
            tv_ln.setCellValueFactory(new PropertyValueFactory<>("lastName"));
            tv_department.setCellValueFactory(new PropertyValueFactory<>("department"));
            tv_major.setCellValueFactory(new PropertyValueFactory<>("major"));
            tv_email.setCellValueFactory(new PropertyValueFactory<>("email"));

            if (editBtn != null) editBtn.setDisable(true);
            if (deleteBtn != null) deleteBtn.setDisable(true);
            if (addBtn != null) addBtn.setDisable(true);
            if (editItem != null) editItem.setDisable(true);
            if (deleteItem != null) deleteItem.setDisable(true);
            if (addItem != null) addItem.setDisable(true);

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
                updateAddButtonState();
            });

            filteredData = new FilteredList<>(data, p -> true);
            SortedList<Person> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(tv.comparatorProperty());
            tv.setItems(sortedData);

            if (searchField != null) {
                searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                    String query = (newValue == null) ? "" : newValue.trim(); // Keep the original case for the display but normalize for search

                    // 1. Update Predicate for FilteredList (your existing logic is good here)
                    filteredData.setPredicate(person -> {
                        if (query.isEmpty()) {
                            return true; // Show all if the search is empty
                        }
                        String lowerCaseFilter = query.toLowerCase();
                        if (person.getFirstName() != null && person.getFirstName().toLowerCase().contains(lowerCaseFilter)) {
                            return true;
                        } else if (person.getLastName() != null && person.getLastName().toLowerCase().contains(lowerCaseFilter)) {
                            return true;
                        } else if (person.getEmail() != null && person.getEmail().toLowerCase().contains(lowerCaseFilter)) {
                            return true;
                        } else
                            return person.getDepartment() != null && person.getDepartment().toLowerCase().contains(lowerCaseFilter);
                    });

                    if (!query.isEmpty()) {
                        Comparator<Person> relevanceComparator = (p1, p2) -> {
                            int score1 = calculateRelevanceScore(p1, query); // Pass the original query for scoring logic
                            int score2 = calculateRelevanceScore(p2, query);
                            int relevanceDiff = Integer.compare(score2, score1);
                            if (relevanceDiff != 0) {
                                return relevanceDiff;
                            }
                            Comparator<Person> tableSort = tv.getComparator();
                            if (tableSort != null) {
                                return tableSort.compare(p1, p2);
                            }
                            if (p1.getId() != null && p2.getId() != null) {
                                return Integer.compare(p1.getId(), p2.getId());
                            }
                            return 0;
                        };
                        sortedData.comparatorProperty().unbind(); // Unbind from a table's default comparator
                        sortedData.setComparator(relevanceComparator);
                    } else {
                        // No search query, revert to the table's default sorting behavior
                        sortedData.comparatorProperty().unbind(); // Ensure it's unbound
                        sortedData.setComparator(null);           // Clear custom comparator
                        sortedData.comparatorProperty().bind(tv.comparatorProperty()); // Re-bind to table column sorting
                    }
                });
            }

            major.setItems(FXCollections.observableArrayList(Major.values()));
            addTextFieldListeners();

            updateAddButtonState();

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

                    if (!headerSkip && line.toLowerCase().startsWith("firstname, lastname") || line.toLowerCase().startsWith("id, firstname")) {
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
                    String majorString = fields[3].trim();
                    String emailCSV = fields[4].trim();
                    String imageURLCSV = fields[5].trim();

                    if (!nameValidator.test(firstName)) {
                        errors.add("Line " + lineNum + ": Invalid First Name '" + firstName + "'");
                        continue;
                    }
                    if (!nameValidator.test(lastName)) {
                        errors.add("Line " + lineNum + ": Invalid Last Name '" + lastName + "'");
                        continue;
                    }
                    if (!departmentValidator.test(department)) {
                        errors.add("Line " + lineNum + ": Invalid Department '" + department + "'");
                        continue;
                    }
                    if (!emailValidator.test(emailCSV)) {
                        errors.add("Line " + lineNum + ": Invalid Email Format For '" + emailCSV + "'");
                        continue;
                    }
                    if (!imageValidator.test(imageURLCSV)) {
                        errors.add("Line " + lineNum + ": Invalid Image URL (No Numbers) For '" + imageURLCSV + "'");
                        continue;
                    }

                    Major selectedMajorEnum; // Renamed
                    try {
                        selectedMajorEnum = Major.fromDisplayString(majorString);
                    } catch (IllegalArgumentException e) {
                        errors.add("Line " + lineNum + ": Invalid Major '" + majorString + "'");
                        continue;
                    }

                    Person p = new Person(firstName, lastName, department, selectedMajorEnum.toString(), emailCSV, imageURLCSV);
                    importPersons.add(p);
                }
            } catch (IOException e) {
                MyLogger.makeLog("Error Handling Import IO: " + e.getMessage());
                showErrorAlert("File Read Error", "Could Not Read The CSV File: " + e.getMessage());
                statusLb.setText("Error Handling Import");
                return;
            } catch (Exception e) {
                MyLogger.makeLog("Unexpected Error During CSV Parsing: " + e.getMessage());
                showErrorAlert("Import Error", "An unexpected error occurred during CSV processing: " + e.getMessage());
                statusLb.setText("Error Processing CSV");
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
                                MyLogger.makeLog("Record Inserted For " + p.getEmail() + " But Failed To Retrieve ID");
                                failCount++;
                            }
                        } else {
                            errors.add("Failed To Insert Record For " + p.getEmail() + "(Database Error Or Duplicate Email");
                            MyLogger.makeLog("DB insert failed for: " + p.getEmail());
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
            MyLogger.makeLog("CSV Import Cancelled");
        }
    }

    @FXML
    private void handleImportTXT (ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open TXT File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File selectedFile = fileChooser.showOpenDialog(menuBar.getScene().getWindow());

        if (selectedFile != null) {
            statusLb.setText("Importing Fomr " + selectedFile.getName() + "...");
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

                    if (!headerSkip && line.toLowerCase().startsWith("firstname\tlastname")) {
                        MyLogger.makeLog("Skipping Header Row: " + line);
                        headerSkip = true;
                        continue;
                    }

                    String[] fields = line.split("\t", -1);

                    if (fields.length != 6) {
                        errors.add("Line " + lineNum + ": Invalid Number Fields (Expected 6, Found " + fields.length + ")");
                        continue;
                    }

                    String firstName = fields[0].trim();
                    String lastName = fields[1].trim();
                    String department = fields[2].trim();
                    String majorString = fields[3].trim();
                    String emailTXT = fields[4].trim();
                    String imageURLTXT = fields[5].trim();

                    // Validate fields using existing validators
                    if (!nameValidator.test(firstName)) {
                        errors.add("Line " + lineNum + ": Invalid First Name '" + firstName + "'");
                        continue;
                    }
                    if (!nameValidator.test(lastName)) {
                        errors.add("Line " + lineNum + ": Invalid Last Name '" + lastName + "'");
                        continue;
                    }
                    if (!departmentValidator.test(department)) {
                        errors.add("Line " + lineNum + ": Invalid Department '" + department + "'");
                        continue;
                    }
                    if (!emailValidator.test(emailTXT)) {
                        errors.add("Line " + lineNum + ": Invalid Email Format For '" + emailTXT + "'");
                        continue;
                    }
                    if (!imageValidator.test(imageURLTXT)) {
                        errors.add("Line " + lineNum + ": Invalid Image URL For '" + imageURLTXT + "'");
                        continue;
                    }

                    Major selectedMajorEnum;
                    try {
                        selectedMajorEnum = Major.fromDisplayString(majorString);
                    } catch (IllegalArgumentException e) {
                        errors.add("Line " + lineNum + ": Invalid Major '" + majorString + "'");
                        continue;
                    }

                    Person p = new Person(firstName, lastName, department, selectedMajorEnum.toString(), emailTXT, imageURLTXT);
                    importPersons.add(p);
                }
            } catch (IOException e) {
                MyLogger.makeLog("Error Handling Import IO: " + e.getMessage());
                showErrorAlert("File Read Error", "Could Not Read The TXT File: " + e.getMessage());
                statusLb.setText("Error Handling Import");
                return;
            }
            processImportedRecords(importPersons, errors);
        } else {
            statusLb.setText("TXT Import Cancelled");
            MyLogger.makeLog("TXT Import Cancelled");
        }
    }

    @FXML
    private void handleExportTXT(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save TXT File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File file = fileChooser.showSaveDialog(menuBar.getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Write header
                writer.println("FirstName\tLastName\tDepartment\tMajor\tEmail\tImageURL");

                // Write data
                for (Person person : tv.getItems()) {
                    writer.println(formatPersonAsTXT(person));
                }

                statusLb.setText("Data Exported Successfully to " + file.getName());
                MyLogger.makeLog("Data exported successfully to " + file.getAbsolutePath());
                new Alert(Alert.AlertType.INFORMATION, "TXT File Exported Successfully to:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showErrorAlert("Export Error", "Failed to export data: " + e.getMessage());
                MyLogger.makeLog("Export error: " + e.getMessage());
                statusLb.setText("Export Failed");
            }
        } else {
            statusLb.setText("TXT Export Cancelled");
            MyLogger.makeLog("TXT Export Cancelled");
        }
    }

    @NotNull
    private String formatPersonAsTXT(@NotNull Person p) {
        return String.join("\t",
                p.getFirstName(),
                p.getLastName(),
                p.getDepartment(),
                p.getMajor(),
                p.getEmail(),
                p.getImageURL());
    }

    private void processImportedRecords(@NotNull List<Person> importPersons, List<String> errors) {
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
                            MyLogger.makeLog("Record Inserted For " + p.getEmail() + " But Failed To Retrieve ID");
                            failCount++;
                        }
                    } else {
                        errors.add("Failed To Insert Record For " + p.getEmail() + "(Database Error Or Duplicate Email");
                        MyLogger.makeLog("DB insert failed for: " + p.getEmail());
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
    }

    private int calculateRelevanceScore(Person person, String query) {
        if (query == null || query.trim().isEmpty() || person == null) {
            return 0;
        }

        String normalizedQuery = query.trim().toLowerCase();
        if (normalizedQuery.isEmpty()) return 0;

        String[] searchTerms = normalizedQuery.split("\\s+"); // Split the query into terms
        int totalScore = 0;

        for (String term : searchTerms) {
            if (term.isEmpty()) continue;
            int termScore = 0;

            // Score first name
            if (person.getFirstName() != null) {
                String firstName = person.getFirstName().toLowerCase();
                if (firstName.equals(term)) termScore += 100;
                else if (firstName.startsWith(term)) termScore += 50;
                else if (firstName.contains(term)) termScore += 10;
            }

            // Score last name
            if (person.getLastName() != null) {
                String lastName = person.getLastName().toLowerCase();
                if (lastName.equals(term)) termScore += 100;
                else if (lastName.startsWith(term)) termScore += 50;
                else if (lastName.contains(term)) termScore += 10;
            }

            // Score email
            if (person.getEmail() != null) {
                String email = person.getEmail().toLowerCase();
                if (email.equals(term)) termScore += 80; // Exact email match is good
                else if (email.startsWith(term)) termScore += 40;
                else if (email.contains(term)) termScore += 5;
            }

            // Optionally, score other fields like department or major with lower weights
            if (person.getDepartment() != null && person.getDepartment().toLowerCase().contains(term)) {
                termScore += 2;
            }
            if (person.getMajor() != null && person.getMajor().toLowerCase().contains(term)) {
                termScore += 2;
            }
            totalScore += termScore;
        }
        return totalScore;
    }

    @FXML
    private void handleExport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Data To CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("student_data_export.csv");
        File selectedFile = fileChooser.showSaveDialog(menuBar.getScene().getWindow());

        if (selectedFile != null) {
            statusLb.setText("Exporting Data To " + selectedFile.getName() + "...");
            try (PrintWriter writer = new PrintWriter(new FileWriter(selectedFile))) {
                writer.println(CSV_HEADER);

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

    @NotNull
    private String formatPersonAsCSV(@NotNull Person p) {
        // This should match the CSV_HEADER order, excluding ID if it's not part of it
        return escapeCsvField(p.getFirstName()) + CSV_SEPARATOR +
                escapeCsvField(p.getLastName()) + CSV_SEPARATOR +
                escapeCsvField(p.getDepartment()) + CSV_SEPARATOR +
                escapeCsvField(p.getMajor()) + CSV_SEPARATOR +
                escapeCsvField(p.getEmail()) + CSV_SEPARATOR +
                escapeCsvField(p.getImageURL());
    }

    @NotNull
    private String escapeCsvField(String field) {
        if (field == null) return "";
        String escapedField = field.replace("\"", "\"\""); // Escape double quotes
        if (escapedField.contains(CSV_SEPARATOR) || escapedField.contains("\"") || escapedField.contains("\n") || escapedField.contains("\r")) {
            return "\"" + escapedField + "\""; // Enclose in double quotes if it contains separator, quotes, or newlines
        }
        return escapedField;
    }

    void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION); // Default to INFORMATION
        if ("Validation Error".equals(title) || "Database Error".equals(title) || "Export Error".equals(title) || title.contains("Error") || title.contains("Warning")) {
            alert.setAlertType(Alert.AlertType.WARNING); // Use WARNING for errors/warnings for better visual cue
        }
        alert.setTitle(title);
        alert.setHeaderText(null); // Or a more descriptive header if needed
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
        alert.getDialogPane().setPrefSize(500, 350);
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
            updateAddButtonState();
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
        // Predicates are now class members, no need to redefine them here.

        first_name.textProperty().addListener((obs, oldVal, newVal) -> updateAddButtonState());
        last_name.textProperty().addListener((obs, oldVal, newVal) -> updateAddButtonState());
        department.textProperty().addListener((obs, oldVal, newVal) -> updateAddButtonState());
        email.textProperty().addListener((obs, oldVal, newVal) -> updateAddButtonState());
        imageURL.textProperty().addListener((obs, oldVal, newVal) -> updateAddButtonState());

        // Add listener for ComboBox
        if (major != null) {
            major.valueProperty().addListener((obs, oldVal, newVal) -> updateAddButtonState());
        }
    }

    private void updateAddButtonState() {
        boolean fnValid = nameValidator.test(first_name.getText());
        boolean lnValid = nameValidator.test(last_name.getText());
        boolean deptValid = departmentValidator.test(department.getText());
        boolean emailValid = emailValidator.test(email.getText());
        boolean imgValid = imageValidator.test(imageURL.getText());
        boolean majorSelected = major.getValue() != null;

        boolean allConditionsMet = fnValid && lnValid && deptValid && emailValid && imgValid && majorSelected;

        if (addBtn != null) {
            addBtn.setDisable(!allConditionsMet);
        }
        if (addItem != null) {
            addItem.setDisable(!allConditionsMet);
        }
    }

    @FXML
    protected void addNewRecord() {

        String firstName = first_name.getText();
        String lastName = last_name.getText();
        String emailText = email.getText();
        String dept = department.getText();
        String image = imageURL.getText();
        Major selectedMajor = major.getValue();

        if (!nameValidator.test(firstName)) {
            showErrorAlert("Validation Error", "First Name Must Contain Only Letters and cannot be blank.");
            return;
        }
        if (!nameValidator.test(lastName)) {
            showErrorAlert("Validation Error", "Last Name Must Contain Only Letters and cannot be blank.");
            return;
        }
        if (!emailValidator.test(emailText)) {
            showErrorAlert("Validation Error", "Invalid Email Format or email is blank.");
            return;
        }
        if (!departmentValidator.test(dept)) {
            showErrorAlert("Validation Error", "Department Must Contain Only Letters and cannot be blank.");
            return;
        }
        if (!imageValidator.test(image)) {
            showErrorAlert("Validation Error", "Image URL Must Not Contain Numbers and cannot be blank.");
            return;
        }
        if (selectedMajor == null) {
            showErrorAlert("Validation Error", "Please Select a Major.");
            return;
        }

        Person p = new Person(firstName, lastName, dept, selectedMajor.toString(), emailText, image);

        try {
            boolean successful = cnUtil.insertUser(p);
            if (successful) {
                int retrieveId = cnUtil.retrieveId(p);
                if (retrieveId > 0) {
                    p.setId(retrieveId);
                    data.add(p); // Add to the master data list
                    clearForm(); // This will call updateAddButtonState()
                    MyLogger.makeLog("Record Added Successfully For ID: " + retrieveId);
                    statusLb.setText("Record Added Successfully For ID: " + retrieveId);
                } else {
                    MyLogger.makeLog("Record Inserted, But Failed To Retrieve ID For: " + p.getEmail());
                    showErrorAlert("Database Warning", "Record Added - ID Not Retrieved\nRecord Might Be Inserted, But Failed To Retrieve ID For: " + p.getEmail());
                    data.setAll(cnUtil.getData()); // Refresh data from DB if ID retrieval fails post-insert
                    clearForm();
                    statusLb.setText("Failed To Retrieve ID For: " + p.getEmail());
                }
            } else {
                MyLogger.makeLog("Failed Inserting Record For: " + p.getEmail());
                showErrorAlert("Database Error", "Failed To Add Record\nCould Not Save Record To Database (Possible duplicate email or other constraint).");
                statusLb.setText("Failed To Add Record For: " + p.getEmail());
            }
        } catch (Exception e) {
            MyLogger.makeLog("Error During Database Operation For addNewRecord: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Application Error", "Error Adding Record\nUnexpected Error Occurred While Adding the record: " + e.getMessage());
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
        statusLb.setText("Ready");
        updateAddButtonState();
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
        MyLogger.makeLog("Application exit requested by user.");
        Platform.exit();
    }

    @FXML
    protected void displayAbout() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/view/about.fxml")));
            Stage stage = new Stage();
            stage.setTitle("About This Application");
            Scene scene = new Scene(root);
            String cssPath = "/css/lightTheme.css";
            URL cssURL = getClass().getResource(cssPath);
            if (cssURL != null) scene.getStylesheets().add(cssURL.toExternalForm());

            stage.initOwner(menuBar.getScene().getWindow()); // Set owner for modality if needed
            stage.initModality(Modality.APPLICATION_MODAL); // Makes the about window block input to other windows

            stage.setScene(scene);
            stage.showAndWait();
            MyLogger.makeLog("About dialog displayed.");
        } catch (IOException e) {
            MyLogger.makeLog("Error loading about.fxml: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Could not load the about screen.");
        } catch (NullPointerException e) {
            MyLogger.makeLog("Error: about.fxml resource not found. Check path. " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Resource Error", "Could not find necessary files for the about screen.");
        }
    }

    @FXML
    protected void editRecord() {
        Person selectedPerson = tv.getSelectionModel().getSelectedItem();
        if (selectedPerson == null) {
            MyLogger.makeLog("Edit Button Clicked But No Item Selected");
            statusLb.setText("No Item Selected For Edit");
            return;
        }

        int index = data.indexOf(selectedPerson); // Use the master data list for index

        Major selectedMajorEnum = major.getValue(); // Get from ComboBox
        // Validate all fields before creating the new Person object for update
        String firstName = first_name.getText();
        String lastName = last_name.getText();
        String dept = department.getText();
        String emailText = email.getText();
        String imageUrlText = imageURL.getText();

        if (!nameValidator.test(firstName)) {
            showErrorAlert("Validation Error", "First Name Must Contain Only Letters and cannot be blank.");
            return;
        }
        if (!nameValidator.test(lastName)) {
            showErrorAlert("Validation Error", "Last Name Must Contain Only Letters and cannot be blank.");
            return;
        }
        if (selectedMajorEnum == null) { // Check after name validation for better UX flow
            showErrorAlert("Validation Error", "Please Select a Major.");
            return;
        }
        if (!departmentValidator.test(dept)) {
            showErrorAlert("Validation Error", "Department Must Contain Only Letters and cannot be blank.");
            return;
        }
        if (!emailValidator.test(emailText)) {
            showErrorAlert("Validation Error", "Invalid Email Format or email is blank.");
            return;
        }
        if (!imageValidator.test(imageUrlText)) {
            showErrorAlert("Validation Error", "Image URL Must Not Contain Numbers and cannot be blank.");
            return;
        }

        Person updatedPerson = new Person(selectedPerson.getId(), firstName, lastName, dept,
                selectedMajorEnum.toString(), emailText, imageUrlText);

        boolean successful = cnUtil.editUser(selectedPerson.getId(), updatedPerson);

        if (successful) {
            data.set(index, updatedPerson); // Update in the master list
            clearForm(); // This will also update button states
            statusLb.setText("Record Updated Successfully For ID: " + selectedPerson.getId());
            MyLogger.makeLog("Record Updated Successfully For ID: " + selectedPerson.getId());
        } else {
            MyLogger.makeLog("Failed To Update Record In Database For ID: " + selectedPerson.getId());
            showErrorAlert("Database Error", "CANNOT UPDATE RECORD\nFailed to update record in the database.");
            statusLb.setText("Failed To Update Record For ID: " + selectedPerson.getId());
        }
    }

    @FXML
    protected void deleteRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();

        if (p == null) {
            MyLogger.makeLog("Delete Button Clicked But No Item Selected");
            showErrorAlert("No Selection", "Please select a record to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Record?");
        confirmation.setContentText("Are You Sure You Want To Delete The Record For "
                + p.getFirstName() + " " + p.getLastName() + "? This action cannot be undone.");

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            MyLogger.makeLog("User confirmed deletion for ID: " + p.getId());
            // int index = data.indexOf(p); // Index in the master list
            boolean successful = cnUtil.deleteRecord(p);

            if (successful) {
                data.remove(p); // Remove from the master list, FilteredList will update
                clearForm(); // Clears form and updates button states
                statusLb.setText("Record Deleted Successfully For ID: " + p.getId());
                MyLogger.makeLog("Record Deleted Successfully From DB and UI For ID: " + p.getId());
            } else {
                MyLogger.makeLog("Failed To Delete Record From Database For ID: " + p.getId());
                showErrorAlert("Database Error", "CANNOT DELETE RECORD\nFailed to delete record from the database.");
                statusLb.setText("Failed To Delete Record For ID: " + p.getId());
            }
        } else {
            MyLogger.makeLog("User cancelled deletion for ID: " + p.getId());
            statusLb.setText("Deletion Cancelled.");
        }
    }

    @FXML
    protected void showImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fileChooser.showOpenDialog(img_view.getScene().getWindow());
        if (file != null) {
            try {
                String imagePath = file.toURI().toString();
                img_view.setImage(new Image(imagePath));
                imageURL.setText(imagePath); // Update the imageURL TextField
                MyLogger.makeLog("Image selected: " + imagePath);
                // updateAddButtonState(); // imageURL text listener will call this
            } catch (Exception e) {
                MyLogger.makeLog("Error loading image: " + e.getMessage());
                showErrorAlert("Image Load Error", "Could not load the selected image: " + e.getMessage());
            }
        } else {
            MyLogger.makeLog("Image selection cancelled.");
        }
    }

    @FXML
    protected void addRecord() {
        MyLogger.makeLog("Add Menu Item Clicked, showing 'showSomeone' dialog.");
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

    public void lightGreenTheme(ActionEvent actionEvent) {
        try {
            Stage stage = (Stage) menuBar.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/lightGreenTheme.css")).toExternalForm());
            stage.setScene(scene);
            statusLb.setText("Light Green Theme Applied");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void grayTheme(ActionEvent actionEvent) {
        try {
            Stage stage = (Stage) menuBar.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/grayTheme.css")).toExternalForm());
            statusLb.setText("Gray Theme Applied");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showSomeone() {
        Dialog<Results> dialog = new Dialog<>();
        dialog.setTitle("Add New Person (Dialog)");
        dialog.setHeaderText("Please enter the person's details below.");
        dialog.initOwner(menuBar.getScene().getWindow()); // Set owner

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField tfFirstName = new TextField();
        tfFirstName.setPromptText("First Name");
        TextField tfLastName = new TextField();
        tfLastName.setPromptText("Last Name");
        TextField tfEmailDialog = new TextField(); // Renamed to avoid conflict
        tfEmailDialog.setPromptText("Email");
        TextField tfDepartmentDialog = new TextField();
        tfDepartmentDialog.setPromptText("Department");
        TextField tfImageURLDialog = new TextField();
        tfImageURLDialog.setPromptText("Image URL (optional)");


        ObservableList<Major> options = FXCollections.observableArrayList(Major.values());
        ComboBox<Major> cbMajor = new ComboBox<>(options);
        cbMajor.setPromptText("Select Major");
        cbMajor.getSelectionModel().selectFirst();

        VBox content = new VBox(10,
                new Label("First Name:"), tfFirstName,
                new Label("Last Name:"), tfLastName,
                new Label("Department:"), tfDepartmentDialog,
                new Label("Major:"), cbMajor,
                new Label("Email:"), tfEmailDialog,
                new Label("Image URL:"), tfImageURLDialog
        );

        content.setPadding(new javafx.geometry.Insets(10));
        dialogPane.setContent(content);
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable updateOkButtonState = () -> {
            boolean fnValidDialog = nameValidator.test(tfFirstName.getText());
            boolean lnValidDialog = nameValidator.test(tfLastName.getText());
            boolean emailValidDialog = emailValidator.test(tfEmailDialog.getText());
            boolean deptValidDialog = departmentValidator.test(tfDepartmentDialog.getText());
            boolean imgValidDialog = imageValidator.test(tfImageURLDialog.getText()) || tfImageURLDialog.getText().isBlank();
            boolean majorSelectedDialog = cbMajor.getValue() != null;
            okButton.setDisable(!(fnValidDialog && lnValidDialog && emailValidDialog && deptValidDialog && majorSelectedDialog));
        };

        tfFirstName.textProperty().addListener((obs, o, n) -> updateOkButtonState.run());
        tfLastName.textProperty().addListener((obs, o, n) -> updateOkButtonState.run());
        tfEmailDialog.textProperty().addListener((obs, o, n) -> updateOkButtonState.run());
        tfDepartmentDialog.textProperty().addListener((obs, o, n) -> updateOkButtonState.run());
        cbMajor.valueProperty().addListener((obs, o, n) -> updateOkButtonState.run());
        tfImageURLDialog.textProperty().addListener((obs, o, n) -> updateOkButtonState.run());

        Platform.runLater(tfFirstName::requestFocus);

        dialog.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK) {
                return new Results(tfFirstName.getText(), tfLastName.getText(), cbMajor.getValue(),
                        tfDepartmentDialog.getText(), tfEmailDialog.getText(), tfImageURLDialog.getText());
            }
            return null;
        });

        Optional<Results> optionalResult = dialog.showAndWait();
        optionalResult.ifPresent((Results results) -> {
            MyLogger.makeLog("Dialog Add: " + results.fname + " " + results.lname + ", Major: " + results.major + ", Email: " + results.email);
            // Create Person object from dialog results
            Person pFromDialog = new Person(results.fname, results.lname, results.department,
                    results.major.toString(), results.email, results.imageURL);
            // Attempt to insert this person (similar to addNewRecord logic)
            try {
                boolean successful = cnUtil.insertUser(pFromDialog);
                if (successful) {
                    int retrieveId = cnUtil.retrieveId(pFromDialog);
                    if (retrieveId > 0) {
                        pFromDialog.setId(retrieveId);
                        data.add(pFromDialog); // Add to the main list
                        statusLb.setText("Dialog: Record Added Successfully For ID: " + retrieveId);
                        MyLogger.makeLog("Dialog: Record Added Successfully For ID: " + retrieveId);
                    } else {
                        statusLb.setText("Dialog: Record Added, Failed To Retrieve ID For: " + pFromDialog.getEmail());
                        MyLogger.makeLog("Dialog: Record Inserted, But Failed To Retrieve ID For: " + pFromDialog.getEmail());
                        data.setAll(cnUtil.getData()); // Refresh
                    }
                } else {
                    statusLb.setText("Dialog: Failed To Add Record For: " + pFromDialog.getEmail());
                    MyLogger.makeLog("Dialog: Failed Inserting Record For: " + pFromDialog.getEmail());
                    showErrorAlert("Dialog Add Error", "Could not save record from dialog to database.");
                }
            } catch (Exception e) {
                MyLogger.makeLog("Dialog: Error During DB Operation: " + e.getMessage());
                e.printStackTrace();
                showErrorAlert("Dialog Add Error", "Unexpected error adding record from dialog: " + e.getMessage());
            }
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

        @NotNull
        public static Major fromDisplayString(String display) {
            for (Major m : Major.values()) {
                if (m.display.equalsIgnoreCase(display) || m.name().equalsIgnoreCase(display)) {
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

    @FXML
    private void handleWipeStudentData(ActionEvent event) {
        // Instantiate your helper controller, passing necessary dependencies
        WipeStudentDataController wiper = new WipeStudentDataController(
                this,         // Pass the instance of DB_GUI_Controller
                this.cnUtil,
                this.data,        // The ObservableList of persons
                this.statusLb,
                this.menuBar
        );
        wiper.confirmAndWipeData(); // Call the method to perform the wipe
    }

    private static class Results {
        String fname;
        String lname;
        Major major;
        String department;
        String email;
        String imageURL;

        public Results(String fname, String lname, Major major, String department, String email, String imageURL) {
            this.fname = fname;
            this.lname = lname;
            this.major = major;
            this.department = department;
            this.email = email;
            this.imageURL = imageURL;
        }
    }
}