# Web Quiz Engine (Spring Boot, Kotlin)

This project demonstrates a RESTful backend built with Kotlin and Spring Boot, focusing on maintainable architecture and testability.
Provides REST endpoints for registering users, creating, retrieving, deleting quizzes, and validating user answers.

## Tech stack

- Kotlin
- Spring Boot
- Spring Data JPA
- Spring Security
- H28
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

## API endpoints & request examples with HTTPie

### Register a new user

`POST /api/register`
```shell
http POST :8889/api/register email=<email> password=<pass>
```

### Create a new quiz

`POST /api/quizzes`
```shell
http POST :8889/api/quizzes \
    title="My name" \
    text="What is my name?" \
    options:='["Vanya", "Joe"]' \
    answer:='[0]' \
    -a <email> and <pass>
```

### Retrieve a quiz

`GET /api/quizzes/{id}`
```shell
http :8889/api/quizzes/<id> -a <email> and <pass>
```

### Retrieve all quizzes, paginated

`GET /api/quizzes`
```shell
http :8889/api/quizzes -a <email> and <pass>
```

### Submit an answer

`POST /api/quizzes/{id}/solve`
```shell
http POST :8889/api/quizzes/1/solve answer:='[<answerId>]' -a <email> and <pass>
```

### Delete a quiz

`DELETE /api/quizzes/{id}`
```shell
http DELETE :8889/api/quizzes/<quizId> -a <email> and <pass>
```

### Get all quiz completions for the user, paginated

`GET /api/quizzes/completed`
```shell
http :8889/api/quizzes/completed?page=<pageNumber> -a <email> and <pass>
```

## Future improvements

- Handle authentication exceptions
- Specify authorization for each /api/** endpoint explicitly
- Replace H2 with PostgreSQL for production-like persistence
