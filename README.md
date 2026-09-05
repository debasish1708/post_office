# PostOffice Secure Letter Portal

PostOffice is a modern, responsive, and visually stunning web application that allows two users, **Nutan** and **Debasish**, to securely send letters (PDFs/images) and attachments with raw text messages to each other. The application features a coin-based wallet system, routing logic, multiple delivery speeds, status tracking, OTP verification for downloads, and email notifications.

It is built with **Spring Boot (Java 21)** for the backend, **PostgreSQL** for the database, and **Vanilla HTML5, CSS3 (Glassmorphism, CSS variables), and JavaScript (ES6)** for the frontend.

---

## 🚀 Features

1. **Dual User Dashboard**: Easily switch between **Nutan** and **Debasish** sessions with a single click. The profile dashboard automatically updates to reflect the active user's view.
2. **Locked Receiver Safety**: Senders are restricted from sending letters to themselves. The receiver is automatically locked to the other user (Nutan can only send to Debasish, and vice versa).
3. **Wallet System**:
   - Each user starts with a wallet balance of **100 coins**.
   - Coins are deducted dynamically depending on the selected delivery service.
4. **Delivery Service Breakdown**:
   - **Normal Post**: 3 coins cost, 4 days delivery.
     - *Breakdown*: 1.00 delivery, 0.50 stamp duty, 1.50 processing fee.
   - **Speed Post**: 20 coins cost, 1 day delivery.
     - *Breakdown*: 10.00 delivery, 2.00 stamp duty, 8.00 processing fee.
   - **Superfast Post**: 50 coins cost, 10 minutes delivery.
     - *Breakdown*: 30.00 delivery, 5.00 stamp duty, 15.00 processing fee.
5. **Database-Driven Routing (Journey Stepper)**:
   - Displays journey nodes loaded dynamically from the PostgreSQL database.
   - **Nutan to Debasish**: `Angul` ➔ `node2` ➔ `node3` ➔ `node4` ➔ `Balasore` ➔ `Remuna`.
   - **Debasish to Nutan**: `Remuna` ➔ `Balasore` ➔ `node4` ➔ `node3` ➔ `node2` ➔ `Angul` (reverse route).
   - High-fidelity visual stepper highlights nodes based on elapsed transit time.
6. **In-Transit Service Upgrade**:
   - Up to the point of delivery, a sender can upgrade the shipping speed of a letter (e.g., from Normal to Speed).
   - Upgrading incurs a **5 coins surcharge fee** + the difference in cost of the chosen service.
   - *Example*: Upgrading Normal (3 coins) to Speed (20 coins) deducts `(20 - 3) + 5 = 22 coins` additional from the sender's wallet.
7. **Simulated Delivery Console**:
   - Skip waiting for real-time delivery with a manual "Simulate Delivery" fast-forward button.
8. **Secure OTP Verification**:
   - When a letter is delivered, the system sends an email to the receiver containing a unique **6-digit OTP**.
   - The letter contents are protected on the dashboard. The receiver must input the correct OTP to decrypt and view the letter.
9. **Automatic Downloads & Acknowledgment**:
   - Upon entering the correct OTP, the letter file (and optional attachment) is **automatically downloaded** to the receiver's computer.
   - The system sends a delivery acknowledgment email to the sender's email.
10. **Interactive Simulation logs**:
    - A floating drawer labeled **"Simulated Email Client"** captures all outgoing SMTP emails. Copy the OTPs and read receipts instantly in the UI without setting up a real email server!

---

## 🛠️ Tech Stack & Requirements

- **Backend**: Java 21, Spring Boot (Web, JPA, Mail, Validation)
- **Database**: PostgreSQL (v12 or higher)
- **Frontend**: Vanilla HTML5, CSS3 (Glassmorphism, animations), Javascript ES6 (Fetch APIs)
- **Build Tool**: Maven Wrapper (included, no global Maven required)

---

## ⚙️ Installation & Database Setup

### Step 1: Initialize PostgreSQL Database
1. Open your PostgreSQL console or client (e.g., pgAdmin, psql).
2. Create a new database named `postoffice`:
   ```sql
   CREATE DATABASE postoffice;
   ```
3. The database tables, initial users, routes, and services are seeded automatically when the application starts, using [schema.sql](file:///e:/PostOffice/src/main/resources/schema.sql).

### Step 2: Establish Database Credentials
1. Open the [application.properties](file:///e:/PostOffice/src/main/resources/application.properties) configuration file.
2. Edit the database url, username, and password credentials to match your local PostgreSQL server config:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/postoffice
   spring.datasource.username=your_postgres_username
   spring.datasource.password=your_postgres_password
   ```

---

## 🚀 Building & Running the Project

Since the project contains a Maven Wrapper (`mvnw`), you do not need Maven installed on your system. Run these commands from the workspace root:

### For Windows (PowerShell / Command Prompt)
Run the application locally:
```powershell
.\mvnw.cmd spring-boot:run
```

### For macOS / Linux (Terminal)
Give execute permissions and run:
```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Once started, open your web browser and navigate to:
```text
http://localhost:8080
```

---

## 📂 Project Structure

```text
PostOffice/
│
├── schema.sql                         # PostgreSQL schema script (copy for reference)
├── pom.xml                            # Maven Build and Spring Boot Starter Configuration
├── mvnw / mvnw.cmd                    # Maven Wrapper scripts
│
├── src/
│   └── main/
│       ├── java/com/postoffice/
│       │   ├── PostOfficeApplication.java  # Main execution entry-point
│       │   │
│       │   ├── model/
│       │   │   ├── User.java              # Profile: name, email, wallet
│       │   │   ├── PostType.java          # Service types & breakdown details
│       │   │   ├── RouteNode.java         # Database-driven path nodes
│       │   │   ├── Letter.java            # Letter parameters, files, status, OTP
│       │   │   └── Transaction.java       # Wallet ledger history & breakdowns
│       │   │
│       │   ├── repository/
│       │   │   ├── UserRepository.java
│       │   │   ├── PostTypeRepository.java
│       │   │   ├── RouteNodeRepository.java
│       │   │   ├── LetterRepository.java
│       │   │   └── TransactionRepository.java
│       │   │
│       │   ├── service/
│       │   │   ├── LetterService.java     # Core shipping, surcharges, OTP logic
│       │   │   └── MailService.java       # Real SMTP + UI inbox simulator
│       │   │
│       │   └── controller/
│       │       ├── UserController.java    # Exposes balances and ledger history
│       │       ├── LetterController.java  # Exposes booking, upgrades, and downloads
│       │       ├── RouteController.java   # Exposes sender-based route nodes
│       │       └── MockMailController.java # Exposes simulated inbox logs
│       │
│       └── resources/
│           ├── schema.sql                 # Seeding classpath schema (runs on startup)
│           ├── application.properties     # DB settings, SMTP configs, file limits
│           └── static/
│               ├── index.html             # High-end Single Page App interface
│               ├── css/
│               │   └── style.css          # Harmonious dark/glassmorphic layout styling
│               └── js/
│                   └── app.js             # Client controller, steppers, events mapping
```
