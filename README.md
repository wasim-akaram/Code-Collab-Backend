# CodeSync Backend

CodeSync is a cloud-native collaborative coding platform. This repository contains the backend infrastructure, built using a robust microservices architecture with Spring Boot, Docker, and Spring Cloud.

## Architecture & Services

The backend is decomposed into several specialized microservices. All services register with the Eureka Server for service discovery and use the API Gateway as the single entry point.

### Infrastructure Services
* **eureka-server**: Service Registry for the microservices ecosystem.
* **api-gateway**: Handles routing, CORS, load balancing, and acts as the edge service for all incoming client requests.
* **admin-server**: Spring Boot Admin dashboard to monitor the health, metrics, and logs of all registered microservices.

### Business Microservices
* **auth-service**: Manages user authentication, JWT generation, OTP verification, and OAuth2 integration (Google & GitHub).
* **project-service**: Manages the creation, metadata, and configuration of user code projects.
* **file-service**: Handles file system operations within projects (CRUD operations for files and folders).
* **collab-service**: Manages real-time WebSockets connections (STOMP/SockJS) for live collaborative editing.
* **comment-service**: Handles code comments, threads, and discussions attached to projects.
* **execution-service**: Executes arbitrary user code in a secure, sandboxed container environment. Supports multiple languages including Java, Python, Node.js, C/C++, and Go.
* **version-service**: Provides Git-like version control, snapshotting, and history tracking for project files.
* **payment-service**: Integrates with Razorpay to manage Pro subscription upgrades and payment webhooks.
* **notification-service**: Dispatches asynchronous email notifications via RabbitMQ message queues.

## Technology Stack

* **Language/Framework**: Java 17, Spring Boot 3.x, Spring Cloud
* **Databases**: PostgreSQL (Relational Data), Redis (Caching & Session Management)
* **Message Broker**: RabbitMQ (Asynchronous Event Processing)
* **Security**: Spring Security, JWT (JSON Web Tokens), OAuth2
* **Build Tool**: Maven
* **Containerization**: Docker, Docker Compose
* **Testing**: JUnit 5, Mockito, JaCoCo, SonarQube

## Prerequisites

To run this backend locally, ensure you have the following installed:
* Java 17 JDK
* Maven 3.8+
* Docker & Docker Compose
* Git

## Getting Started

Follow these steps to clone and run the backend microservices locally.

### 1. Clone the Repository

```bash
git clone <repository-url>
cd backend
```

### 2. Configure Environment Variables

The backend relies on an environment variable file. Create a file named `.env` in the root of the `backend` directory and populate it with your specific credentials:

```env
DB_URL=jdbc:postgresql://postgres:5432/codesync
DB_USERNAME=postgres
DB_PASSWORD=your_db_password
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379
REDIS_HOST=redis
REDIS_PORT=6379
SPRING_RABBITMQ_HOST=rabbitmq

JWT_SECRET=your_secure_base64_encoded_jwt_secret

MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret

RAZORPAY_KEY_ID=your_razorpay_key_id
RAZORPAY_KEY_SECRET=your_razorpay_key_secret

OAUTH2_REDIRECT_BASE_URI=http://localhost:8080
```

### 3. Build the Services

Before running Docker Compose, you must build the Java ARchive (.jar) files for all microservices. A root Maven command can be used to build the entire reactor:

```bash
mvn clean package -DskipTests
```
This will generate the required `.jar` files in the `target/` directory of each respective microservice.

### 4. Run the Infrastructure

Use Docker Compose to spin up the entire ecosystem, including the PostgreSQL database, Redis, RabbitMQ, and all the microservices.

```bash
docker compose up -d
```

### 5. Verify the Deployment

Once the containers are running, it will take a few moments for all services to start up and register with Eureka. You can verify the health of the system by accessing:

* **Eureka Dashboard**: `http://localhost:8761`
* **API Gateway**: `http://localhost:8080`
* **Spring Boot Admin**: `http://localhost:8088` (or the respective port configured for admin-server)

## Testing and Code Quality

To execute the test suite across all services:
```bash
mvn test
```

To generate coverage reports and run SonarQube analysis (requires a local or remote SonarQube server):
```bash
mvn clean verify sonar:sonar -Dsonar.projectKey=CodeCollab-Backend -Dsonar.host.url=http://localhost:9000 -Dsonar.login=your_sonar_token
```

## Contributing

1. Create a feature branch
2. Commit your changes
3. Push to the branch
4. Open a Pull Request for review
