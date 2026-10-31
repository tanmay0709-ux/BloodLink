# BloodLink 🩸

**Hospital Blood Bank Management and Emergency Blood Distribution System**

BloodLink is a full-stack web application that connects hospitals and blood banks, enabling real-time inventory management and rapid response to emergency blood requests.

---

## Tech Stack

| Layer     | Technology                                      |
|-----------|-------------------------------------------------|
| Backend   | Java 21, Spring Boot 3.x, Spring Security, JWT  |
| Frontend  | React 18, Vite, React Router v6, Axios          |
| Database  | MySQL 8.x                                       |
| Build     | Maven (backend), npm (frontend)                 |

---

## Folder Structure

```
BloodLInk/
├── backend/                        # Spring Boot application
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/bloodlink/
│           │   ├── BloodLinkApplication.java
│           │   ├── config/
│           │   │   ├── SecurityConfig.java
│           │   │   └── CorsConfig.java
│           │   ├── controller/
│           │   │   ├── AuthController.java
│           │   │   ├── BloodInventoryController.java
│           │   │   └── EmergencyRequestController.java
│           │   ├── dto/
│           │   │   ├── LoginRequest.java
│           │   │   ├── RegisterRequest.java
│           │   │   ├── JwtResponse.java
│           │   │   └── ApiResponse.java
│           │   ├── model/
│           │   │   ├── User.java
│           │   │   ├── BloodInventory.java
│           │   │   └── EmergencyRequest.java
│           │   ├── repository/
│           │   │   ├── UserRepository.java
│           │   │   ├── BloodInventoryRepository.java
│           │   │   └── EmergencyRequestRepository.java
│           │   ├── security/
│           │   │   ├── JwtTokenProvider.java
│           │   │   ├── JwtAuthenticationFilter.java
│           │   │   └── CustomUserDetailsService.java
│           │   └── service/
│           │       ├── AuthService.java
│           │       ├── BloodInventoryService.java
│           │       └── EmergencyRequestService.java
│           └── resources/
│               └── application.properties
│
├── frontend/                       # Vite + React application
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── main.jsx
│       ├── App.jsx
│       ├── index.css
│       ├── components/
│       │   └── Navbar.jsx
│       ├── context/
│       │   └── AuthContext.jsx
│       ├── pages/
│       │   ├── Home.jsx
│       │   ├── Login.jsx
│       │   ├── Register.jsx
│       │   ├── Dashboard.jsx
│       │   └── NotFound.jsx
│       └── services/
│           ├── api.js
│           └── authService.js
│
└── README.md
```

---

## Environment Setup

### 1. MySQL Database

Ensure MySQL 8.x is running, then create the database (Spring Boot will auto-create tables via `ddl-auto=update`):

```sql
CREATE DATABASE IF NOT EXISTS bloodlink_db;
```

### 2. Backend Configuration

Edit `backend/src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
app.jwt.secret=YOUR_SECURE_RANDOM_SECRET_AT_LEAST_256_BITS
```

---

## Running the Backend

```bash
cd backend
mvn spring-boot:run
```

The API will be available at **http://localhost:8080**

### Key API Endpoints

| Method | Endpoint                      | Description              |
|--------|-------------------------------|--------------------------|
| POST   | `/api/auth/register`          | Register a new user      |
| POST   | `/api/auth/login`             | Login and receive JWT    |
| GET    | `/api/inventory`              | List all blood inventory |
| POST   | `/api/inventory`              | Add inventory record     |
| PATCH  | `/api/inventory/{id}/adjust`  | Adjust blood quantity    |
| GET    | `/api/emergency`              | List emergency requests  |
| POST   | `/api/emergency`              | Create emergency request |
| PATCH  | `/api/emergency/{id}/status`  | Update request status    |

---

## Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

The app will be available at **http://localhost:5173**

Vite is configured to proxy `/api` requests to `http://localhost:8080`, so no CORS issues during development.

---

## User Roles

| Role                | Description                            |
|---------------------|----------------------------------------|
| `ADMIN`             | Full system access                     |
| `HOSPITAL_STAFF`    | Manage requests for their hospital     |
| `BLOOD_BANK_MANAGER`| Manage inventory and approve requests  |
| `VIEWER`            | Read-only access (default for new users)|

---

## Notes

- JWT tokens expire after **24 hours** (configurable via `app.jwt.expiration`)
- The security config currently permits all routes for development ease. Tighten role-based access before deploying to production.
- Password hashing uses **BCrypt**.
