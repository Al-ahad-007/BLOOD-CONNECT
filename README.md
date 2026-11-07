# BloodConnect – A Web-Based Blood Donor and Emergency Finder System

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Jakarta EE](https://img.shields.io/badge/Jakarta_Servlet-6.0-2C5E8A?logo=eclipseide&logoColor=white)](https://jakarta.ee/)
[![Tomcat](https://img.shields.io/badge/Apache_Tomcat-10.1-F8DC75?logo=apachetomcat&logoColor=black)](https://tomcat.apache.org/)
[![MySQL](https://img.shields.io/badge/Database-MySQL_8.0+-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)

BloodConnect is a centralized healthcare platform that connects people searching for emergency blood units directly with registered voluntary blood donors in their regional area. Built for an academic Java Object-Oriented Programming (OOP) project, it strictly uses core Java, Jakarta Servlets 6.0, JDBC, and MySQL without external frameworks like Spring Boot, React, or Angular.

---

## 1. Project Overview

BloodConnect addresses the acute logistical challenge of locating compatible voluntary blood donors during hospital emergencies.

### Target Users & Roles
1. **Voluntary Blood Donors (`DONOR`)**: Register blood group and location, manage live availability status (Available/Unavailable), view emergency requests, and accept/decline donation opportunities.
2. **Requesters (`REQUESTER`)**: Search for nearby active donors by blood group and location, submit emergency blood requests specifying hospital and urgency, and track responses.
3. **Administrators (`ADMIN`)**: Access a centralized management dashboard to monitor system KPIs, moderate user accounts, inspect donors, and update blood request fulfillment statuses.

> **Important Medical Notice**: BloodConnect is a donor discovery and communication platform, not a medical compatibility or blood bank management system. Final donor eligibility, health screening, and compatibility testing must always be conducted by qualified healthcare professionals and licensed blood banks.

---

## 2. Key Features

- **Object-Oriented Architecture**: Clear separation of concerns utilizing Models, Data Access Objects (DAOs), Service Interfaces, and Servlets.
- **Role-Based Authentication & Authorization**: Session-based security with custom filters (`AuthenticationFilter`, `AdminAuthorizationFilter`).
- **Cryptographic Security**: Passwords hashed using industry-standard **BCrypt** (`gensalt(10)`); no plaintext passwords stored.
- **SQL Injection Prevention**: 100% of database operations use parameterized `PreparedStatement`.
- **Privacy Controls**: Donor contact information is hidden from unauthenticated guests and only visible to logged-in users.
- **Real-Time Availability Toggle**: Donors can update their availability status in real-time with automatic database synchronization.
- **Dynamic Emergency Broadcasts**: Submit urgent blood requirements with hospital details, required units, and urgency levels (`CRITICAL`, `URGENT`, `NORMAL`).
- **Interactive Responses**: Donors can accept requests with custom arrival times and messages; requesters can view contact details in real-time.
- **Zero-Config Fallback Support**: Fully configured for native MySQL on port 3306 with an automated H2 fallback mode for zero-setup evaluation.

---

## 3. Technology Stack

| Layer | Technology | Details |
|---|---|---|
| **Frontend** | HTML5, CSS3, Vanilla JavaScript | Reusable CSS components, responsive design (Desktop, Tablet, Mobile) |
| **Backend** | Java (JDK 17+) | Jakarta Servlet API 6.0 (Tomcat 10.1 compliant) |
| **Database** | MySQL 8.0+ / MariaDB 10.5+ | Normalized 4-table relational schema with foreign key constraints |
| **Persistence** | JDBC (Java Database Connectivity) | `Connection`, `PreparedStatement`, `ResultSet`, `DriverManager` |
| **Security** | jBCrypt 0.4 | Salted BCrypt password hashing |
| **JSON Serialization** | Google Gson 2.11.0 | Standard JSON communication between frontend and servlets |
| **Server** | Apache Tomcat 10.1 | Embedded runner & standalone `BloodConnect.war` |
| **Build Tool** | Apache Maven 3.9+ | Standard Maven WAR packaging |

---

## 4. System Architecture

```
                    +------------------------------------+
                    |        Web Browser (Client)        |
                    |   (HTML5, CSS3, Vanilla JS Fetch)  |
                    +-----------------+------------------+
                                      | HTTP / JSON
                                      v
                    +------------------------------------+
                    |       Tomcat 10.1 Web Server       |
                    |  +------------------------------+  |
                    |  |   Authentication Filters     |  |
                    |  +--------------+---------------+  |
                    |                 |                  |
                    |  +--------------v---------------+  |
                    |  |     Jakarta Servlets (6.0)   |  |
                    |  | (Auth, Donor, Request, Admin)|  |
                    |  +--------------+---------------+  |
                    |                 |                  |
                    |  +--------------v---------------+  |
                    |  |        Service Layer         |  |
                    |  | (AuthService, DonorService)  |  |
                    |  +--------------+---------------+  |
                    |                 |                  |
                    |  +--------------v---------------+  |
                    |  |    DAO Layer (JDBC Prepared) |  |
                    |  | (UserDAO, DonorDAO, ReqDAO)  |  |
                    |  +--------------+---------------+  |
                    +-----------------+------------------+
                                      | JDBC Driver
                                      v
                    +------------------------------------+
                    |     MySQL Database (3306)          |
                    |       bloodconnect_db              |
                    |  (users, donors, blood_requests,   |
                    |        donor_responses)            |
                    +------------------------------------+
```

---

## 5. Project Folder Structure

```
AHAD/
├── pom.xml                                    # Maven project configuration
├── README.md                                  # Complete system documentation
├── database.sql                               # MySQL schema and seed demonstration data
├── run.bat                                    # One-click Windows runner script
├── build.bat                                  # One-click Windows WAR packaging script
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/bloodconnect/
│       │       ├── config/
│       │       │   └── DatabaseConnection.java       # JDBC Singleton connection manager
│       │       ├── exception/
│       │       │   ├── ApplicationException.java     # Base application exception
│       │       │   ├── AuthenticationException.java  # Credential & login errors
│       │       │   ├── DatabaseException.java        # JDBC & SQL errors
│       │       │   └── ValidationException.java      # Input validation errors
│       │       ├── model/
│       │       │   ├── User.java                     # Base User model (Encapsulation)
│       │       │   ├── Donor.java                    # Inherits User (Inheritance)
│       │       │   ├── Admin.java                    # Inherits User
│       │       │   ├── BloodRequest.java             # Emergency Request model
│       │       │   └── DonorResponse.java            # Donor coordination response
│       │       ├── dao/
│       │       │   ├── UserDAO.java                  # User CRUD JDBC operations
│       │       │   ├── DonorDAO.java                 # Donor search & profile JDBC
│       │       │   ├── BloodRequestDAO.java          # Blood Request JDBC operations
│       │       │   └── DonorResponseDAO.java         # Donor Response JDBC operations
│       │       ├── service/
│       │       │   ├── AuthService.java              # Abstraction interface
│       │       │   ├── DonorService.java             # Abstraction interface
│       │       │   ├── BloodRequestService.java      # Abstraction interface
│       │       │   └── impl/
│       │       │       ├── AuthServiceImpl.java      # BCrypt & validation logic
│       │       │       ├── DonorServiceImpl.java     # Search & stats logic
│       │       │       └── BloodRequestServiceImpl.java # Emergency request logic
│       │       ├── servlet/
│       │       │   ├── RegisterServlet.java          # /api/auth/register
│       │       │   ├── LoginServlet.java             # /api/auth/login, /api/auth/me
│       │       │   ├── LogoutServlet.java            # /api/auth/logout
│       │       │   ├── UserServlet.java              # /api/user/profile, password
│       │       │   ├── DonorSearchServlet.java       # /api/donors/search
│       │       │   ├── DonorServlet.java             # /api/donor/*
│       │       │   ├── BloodRequestServlet.java      # /api/requests/*
│       │       │   ├── AdminServlet.java             # /api/admin/*
│       │       │   └── StatsServlet.java             # /api/stats/public
│       │       ├── filter/
│       │       │   ├── AuthenticationFilter.java     # Route protection filter
│       │       │   └── AdminAuthorizationFilter.java # Role check filter
│       │       ├── util/
│       │       │   └── ApiResponse.java              # JSON output streaming helper
│       │       └── runner/
│       │           └── TomcatServer.java             # Standalone Tomcat 10.1 launcher
│       │
│       ├── resources/
│       │   ├── application.properties                # Active database configuration
│       │   └── application.properties.example        # Configuration template
│       │
│       └── webapp/
│           ├── index.html                            # Public Landing Page
│           ├── login.html                            # User Login with demo quick-fill
│           ├── register.html                         # Donor & Requester Registration
│           ├── find-donors.html                      # Donor Search with filters
│           ├── donor-dashboard.html                  # Donor Dashboard (toggle, requests)
│           ├── request-blood.html                    # Emergency Blood Request Form
│           ├── my-requests.html                      # Track Requests & View Responses
│           ├── admin-dashboard.html                  # Administrator Console
│           │
│           ├── assets/
│           │   ├── css/
│           │   │   ├── style.css                     # Design tokens & typography
│           │   │   ├── components.css                # Reusable UI components
│           │   │   └── responsive.css                # Media queries (mobile, tablet)
│           │   └── js/
│           │       ├── main.js                       # Session management & UI utils
│           │       ├── auth.js                       # Login & registration logic
│           │       ├── donor.js                      # Search & donor dashboard
│           │       ├── requests.js                   # Blood request & response logic
│           │       └── admin.js                      # Administrator console logic
│           │
│           └── WEB-INF/
│               └── web.xml                           # Jakarta Servlet 6.0 deployment descriptor
```

---

## 6. Prerequisites

To run BloodConnect locally, ensure you have:
1. **Java JDK 17 or higher** (JDK 17, 21, or 25).
2. **Apache Maven 3.8+** (Included and configured in PATH).
3. **MySQL 8.0+ or MariaDB 10.5+** running on `localhost:3306`.

---

## 7. Database Setup

1. Open your terminal or MySQL command line:
   ```bash
   mysql -u root -p
   ```
2. Run the provided initialization script:
   ```bash
   SOURCE c:/path/to/AHAD/database.sql;
   ```
   *Or from PowerShell:*
   ```powershell
   Get-Content database.sql | mysql -u root -proot
   ```
3. Verify that the four tables were created:
   ```sql
   USE bloodconnect_db;
   SHOW TABLES;
   ```

---

## 8. Database Configuration

Database configuration is stored in `src/main/resources/application.properties`:

```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/bloodconnect_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.user=root
db.password=root

# Fallback in-memory database if MySQL server is not started during demonstration
db.fallback.h2=true
```

You can also override credentials via environment variables:
`DB_URL`, `DB_USER`, `DB_PASSWORD`, `DB_DRIVER`.

---

## 9. How to Build & Run

### Option A: Using the Embedded Tomcat 10.1 Runner (Recommended)

1. Open PowerShell in the project directory (`AHAD`).
2. Run the application directly with Maven:
   ```bash
   mvn exec:java
   ```
   *Or simply double-click `run.bat`.*
3. Open your browser and navigate to:
   **`http://localhost:8080/BloodConnect/`**
   *(Note: Visiting `http://localhost:8080/` automatically redirects to `/BloodConnect/`)*

### Option B: Deploying to External Apache Tomcat 10.1

1. Package the WAR file using Maven:
   ```bash
   mvn clean package -DskipTests
   ```
   *Or double-click `build.bat`.*
2. Copy the resulting file `target/BloodConnect.war` into your Tomcat installation's `webapps/` folder:
   ```bash
   copy target\BloodConnect.war C:\apache-tomcat-10.1.x\webapps\
   ```
3. Start Tomcat (`bin/startup.bat`) and open `http://localhost:8080/BloodConnect/`.

---

## 10. Pre-Configured Demo Accounts

For academic evaluation and quick testing, the login page (`login.html`) includes **one-click quick-fill buttons**:

| Role | Email Address | Password | Details |
|---|---|---|---|
| **System Administrator** | `admin@bloodconnect.org` | `Admin@123` | Full access to Admin Console |
| **Registered Donor** | `rohit.sharma@example.com` | `Donor@123` | O+ Donor in Bandra, Mumbai |
| **Registered Requester** | `rahul.mehta@example.com` | `Requester@123` | Patient family in Pune/Mumbai |

---

## 11. Testing & User Flows

### Flow 1: Donor Registration & Availability Toggle
1. Navigate to `/register.html` and select **Blood Donor**.
2. Fill in details (e.g., Blood Group: `B-`, City: `Nagpur`, Phone: `9822334455`).
3. Submit and verify automatic redirect to `/donor-dashboard.html`.
4. Click the **Toggle Availability** button: observe the live badge change from **Available** to **Unavailable** and verify the change in MySQL:
   ```sql
   SELECT email, availability_status FROM users u JOIN donors d ON u.user_id=d.user_id WHERE email='vikas.kumar@example.com';
   ```

### Flow 2: Donor Search with Filters
1. Open `/find-donors.html`.
2. Filter by Blood Group (e.g. `O+`) or City (e.g. `Mumbai`).
3. Observe responsive donor cards with privacy-shielded phone numbers.
4. Click **Login to Contact** or log in to view direct contact coordinates.

### Flow 3: Emergency Blood Request & Response Coordination
1. Log in as Requester (`rahul.mehta@example.com` / `Requester@123`).
2. Go to `/request-blood.html` and fill out an emergency request for patient `Sunita Sharma`, Blood Group `O+`, `CRITICAL` urgency.
3. Submit request; observe the generated unique Request ID confirmation modal.
4. Log out and log in as Donor Rohit Sharma (`rohit.sharma@example.com` / `Donor@123`).
5. In the **Incoming Requests** feed on `/donor-dashboard.html`, click **Accept Request** and enter an estimated arrival time.
6. Verify in MySQL that `blood_requests` status updated to `ACCEPTED` and a row was inserted in `donor_responses`.

### Flow 4: Administrator Management Console
1. Log in as Admin (`admin@bloodconnect.org` / `Admin@123`).
2. Navigate to `/admin-dashboard.html`.
3. Review total counts (Registered Users, Available Donors, Active Requests).
4. Update a Blood Request status to **FULFILLED**.
5. Test suspending or activating a user account.

### Flow 5: Security & Role Validation
1. Try accessing `/donor-dashboard.html` without logging in: verify redirect to `/login.html?error=unauthorized`.
2. Try accessing `/admin-dashboard.html` with a normal requester session: verify access blocked with `403 Forbidden`.
3. Try registering with an existing email: verify rejection with field-level duplicate prevention error.

---

## 12. Common Troubleshooting

| Issue | Cause | Solution |
|---|---|---|
| `CommunicationsException: Connection refused` | MySQL service is not running on port 3306. | Start MySQL/MariaDB service (`mysqld.exe`). BloodConnect will automatically fall back to H2 if configured. |
| `Access denied for user 'root'@'localhost'` | Database password mismatch. | Update `db.password` in `src/main/resources/application.properties` to match your local MySQL root password. |
| `java.lang.ClassNotFoundException: jakarta.servlet.*` | Using older Tomcat 9 instead of Tomcat 10. | BloodConnect uses Jakarta EE 10 (Tomcat 10.1). Ensure you run with `mvn exec:java` or deploy to Tomcat 10.1+. |
| `Address already in use: bind` | Port 8080 is occupied by another process. | Change `app.port=8081` in `application.properties` or terminate the existing process using port 8080. |

---

## 13. License & Academic Attribution
Developed as an academic demonstration of Java OOP, Jakarta Servlets, JDBC CRUD operations, and relational database management.

