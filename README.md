# Lossless.fm Backend API

Java/Spring Boot backend API for Lossless.fm — a mobile-first prototype for uploading, publishing, discovering, and playing lossless audio tracks.

## Project Status

Prototype / work sample. Core backend functionality is implemented, but the project is not production-ready yet.

## Related Repositories

- Mobile App: `https://github.com/chursa-andrey/lossless-front`

---

## Features

- Email authentication
- Google/Facebook social login integration in development mode
- JWT access tokens
- Refresh token rotation
- Logout and token revocation
- WAV/FLAC upload
- Audio metadata extraction
- Genre and purchase link support
- Cursor-based track feed
- Authenticated audio streaming endpoint
- Structured API error responses

## Tech Stack

- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- JWT
- Maven
- Docker
- AWS S3-ready storage abstraction

## Architecture Overview

The backend follows a layered structure:

- Controllers handle HTTP requests and response mapping.
- Services contain business logic.
- Repositories handle database access.
- DTOs separate API contracts from JPA entities.
- Security layer manages JWT authentication and refresh token lifecycle.
- Storage abstraction allows replacing local file storage with S3-compatible storage later.

## Main API Areas

### Authentication

- Email login/registration flow
- Google/Facebook social login integration
- JWT access token generation
- Refresh token rotation
- Hashed refresh token storage
- Logout and token revocation

### Tracks

- Multipart WAV/FLAC upload
- File validation
- Audio metadata extraction
- Genre assignment
- Purchase links
- Audio file storage
- Track feed API
- Audio streaming endpoint

## Local Development

### Requirements

- Java 17
- Maven
- Docker
- PostgreSQL

### Run Database

```sh
docker compose up -d