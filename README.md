# CSC311 Student Information System UI 

Java-based UI for managing a student information database, developed as a semester‑long CSC311 assignment.

## 🔑 Key Features

UI State Management

Context‑sensitive buttons and menu items: “Add,” “Edit,” “Delete” enable/disable based on selection and form validity.

Form Enhancements

Advanced regex validation for all fields.

“Major” field as an enum dropdown (CS, CPIS, English).

Import / Export

Read/write .txt files for bulk data handling.

Search & Filter

Real-time search and filtering of table data.

Themes

Built‑in Gray and Light‑Green themes.

Data Wipe

Clear all records with one action.

User Feedback

Status bar messages for add/update operations.

Thread Safety & Preferences

Thread-safe UserSession.

User login preferences saved on sign‑in.

## ⚙️ Setup

Clone the repository

git clone https://github.com/Angel-Adames/CSC311_DB_UI_semesterlongproject.git
cd CSC311_DB_UI_semesterlongproject

Configure the database

Update connection settings in src/main/resources/db.properties.

Run the SQL script:

mysql -u USER -p DATABASE_NAME < schema.sql

Build & Run

./mvnw clean package
./mvnw exec:java -Dexec.mainClass="com.example.Main"

## 📂 Project Structure

.
├── .idea/              # IDE config files
├── .mvn/               # Maven wrapper files
├── schema.sql          # Database schema definition
├── pom.xml             # Maven configuration
├── README.md           # Project documentation
└── src/
    ├── main/
    │   ├── java/       # Java source code
    │   └── resources/  # db.properties, CSS files
    └── test/           # Unit tests

_Extra features (10%): custom themes, .txt import/export, data wipe, search filter—all designed to enhance branding, usability, and presentation._
