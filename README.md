# Web Quiz Engine (Spring Boot, Kotlin)

This project demonstrates a RESTful backend built with Kotlin and Spring Boot, focusing on maintainable architecture and testability.
Provides REST endpoints for registering users, creating, retrieving, deleting quizzes, and validating user answers.

## Tech stack

- Kotlin
- Spring Boot
- Spring Data JPA
- Spring Security
- H2
- Gradle

## Architecture

The application follows a layered architecture separating web, business logic, and persistence concerns to ensure maintainability and testability.

## Features

- RESTful API design
- Input validation using Spring validation
- Centralized exception handling
- Separation of domain models, DTOs and persistence entities
- Unit and integration tests
- Basic HTTP authentication
- New user registration with custom user store
- Pagination

## Requirements

- Java 23. Higher versions not tested.

## Run

### Linux/Mac

```bash
./gradlew bootRun
```

### Windows

```
gradlew.bat bootRun
```

## Default URL

http://localhost:8889

## API Overview

- POST /api/register — register a new user
- POST /api/quizzes — create a quiz
- GET /api/quizzes/{id} — retrieve a quiz
- GET /api/quizzes — retrieve all quiz, paginated
- POST /api/quizzes/{id}/solve — submit an answer
- DELETE /api/quizzes/{id} — delete a quiz
- GET /api/quizzes/completed — get all quiz completions for the user, paginated

## Future improvements

- Handle authentication exceptions
- Specify authorization for each /api/** endpoint explicitly
- Replace H2 with PostgreSQL for production-like persistence
