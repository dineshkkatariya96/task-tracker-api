# Task Tracker API

A REST API built with Spring Boot for managing employee tasks with role-based access control.

## Technologies
- Java 17
- Spring Boot 3.2.5
- Spring Security + JWT Authentication
- Spring Data JPA + Hibernate
- MySQL Database
- Maven

## Features
- JWT based authentication
- Role based access (Admin / Employee)
- Task CRUD operations
- Assign tasks to employees
- Task status workflow (OPEN → IN_PROGRESS → DONE → OVERDUE)
- Daily scheduler to auto-mark overdue tasks
- Global exception handling
- Input validation

## API Endpoints

### Auth
| Method | Endpoint | Access |
|--------|----------|--------|
| POST | /api/auth/register | Public |
| POST | /api/auth/login | Public |

### Tasks
| Method | Endpoint | Access |
|--------|----------|--------|
| GET | /api/tasks | Both |
| GET | /api/tasks/my | Both |
| GET | /api/tasks/overdue | Both |
| POST | /api/tasks | Admin only |
| PUT | /api/tasks/{id} | Admin only |
| PATCH | /api/tasks/{id}/status | Both |
| DELETE | /api/tasks/{id} | Admin only |

### Users
| Method | Endpoint | Access |
|--------|----------|--------|
| GET | /api/users | Admin only |
| GET | /api/users/employees | Admin only |

## Setup Instructions

### Prerequisites
- Java 17
- MySQL 8.0
- Maven

### Steps
1. Clone the repository
2. Create MySQL database:
```sql
CREATE DATABASE tasktracker;
```
3. Update `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tasktracker
spring.datasource.username=your_username
spring.datasource.password=your_password
```
4. Run the application:
```
mvn spring-boot:run
```
5. Server starts at `http://localhost:8080`

## Default Admin Credentials
```
Email    : admin@tasktracker.com
Password : admin123
```

## Project Structure
```
src/main/java/com/tasktracker/
├── config/          ← JWT, Security, CORS config
├── controller/      ← REST controllers
├── dto/             ← Request/Response DTOs
├── entity/          ← JPA entities
├── enums/           ← TaskStatus, Priority, Role
├── exception/       ← Global exception handler
├── repository/      ← JPA repositories
├── scheduler/       ← Overdue task scheduler
└── service/         ← Business logic
```