# Cloud Storage Web Application

A web-based application for storing, organizing, and downloading user files and folders. This is a personal pet project developed to gain practical experience in building a full-scale server architecture using Java, integrated with S3-compatible storage, distributed session management, and modern isolated testing tools.

---

## Features

### 🔐 User Management, Sessions, and Security
* **Authentication and Authorization:** Full resource access control and endpoint protection implemented via **Spring Security**.
* **Sessions and Cookies:** User state management handled through session mechanics. Powered by **Spring Session**, session data is transparently delegated to an external Redis cache, making the core application completely stateless.

### 🌤️ File Structure and Storage Operations
* **S3 Object Storage:** Physical placement and structuring of user assets within an S3-compatible cloud storage environment.
* **Upload Management:** Supports uploading individual files as well as entire folders containing nested files and directories.
    * **Size Limits:** The maximum size limit is restricted to 2500 MB for a single file, and 5000 MB for total files uploaded per batch.
    * **Smart Conflict Resolution:** If a file being uploaded matches an existing file name in the target directory, the application automatically handles the conflict by appending a special character to the newly uploaded file's name to prevent overwriting.
* **Download Management:** Supports downloading separate files or entire folders. Directory structures are dynamically compiled and downloaded as ZIP archives.
* **Directory Management:** Seamless navigation, creation of virtual folders, renaming and moving files or directories, and recursive folder deletion.
* **REST API & Swagger:** Comprehensive documentation and interactive endpoint testing of all available storage controllers through an embedded OpenAPI/Swagger UI interface.

---

## Tech Stack

* **Programming Language:** Java 17
* **Build System:** Gradle
* **Server Technologies:** Spring Boot, Spring Security, Spring Session
* **Database Management:** PostgreSQL, Spring Data JPA
* **Database Migrations:** Liquibase
* **Object Storage:** S3 (MinIO)
* **Session Cache:** Redis
* **Infrastructure:** Docker (containers, images, volumes), Docker Compose
* **Testing Suite:** JUnit, Testcontainers

---

## Project Architecture & Learning Highlights

* **Decentralized Session Management:** Successfully migrated session storage from the local memory of a specific Tomcat server instance into a centralized Redis cluster. This architecture ensures high availability, fault tolerance, and allows the web application to scale horizontally seamlessly.
* **Automated Database Management (Liquibase):** All changes to the database schema are structurally recorded within dedicated project changelog files. When the application boots up in any server or machine environment, these migrations apply automatically. This design guarantees that the database layer remains completely identical and error-free across all deployment targets.
* **Reliable Integration Testing:** Secured critical code paths and core components using isolated integration tests powered by the Testcontainers library, eliminating environment conflicts during test executions.

---

## Running the Project Locally (Production Setup)

### Prerequisites
Ensure you have [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

### 1. Prepare Configuration
To deploy the ready-made cloud storage stack, you only need the infrastructure configuration files. [Copy](https://github.com/Olegarh86/CloudStorage) `compose.prod.yaml` and `.env.example` into your local working directory.

### 2. Configure the Environment
Rename `.env.example` to `.env` and set up your specific configuration credentials to connect PostgreSQL and the S3 storage bucket:
```properties
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password

MINIO_ROOT_USER=minio_admin
MINIO_ROOT_PASSWORD=your_secure_s3_password
```

### 3. Start the storage
Launch the complete platform with a single command. Docker will automatically fetch the public application image from Docker Hub and spin up the necessary infrastructure dependencies:
```bash
docker compose -f compose.prod.yaml up -d
```
Upon startup, the application verifies and updates the schema using Liquibase. It will then be ready to handle web traffic at http://localhost:8090.

---

## Development Setup

If you want to modify the codebase, execute tests, or build your own custom Docker image from scratch:

### Prerequisites
* JDK 17
* Docker Desktop up and running

### 1. Clone the Repository
```bash
git clone https://github.com
cd CloudStorage
```

### 2. Run Tests and Compile the Artifact
Build the executable Spring Boot `.jar` binary file:
```bash
chmod +x gradlew
./gradlew clean bootJar
```

### 3. Local Image Build and Launch
Deploy your freshly compiled local code integrated with the development infrastructure layer at http://localhost:8092:
```bash
docker compose up -d --build
```
