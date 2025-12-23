# SentimentScribe

SentimentScribe is a desktop diary app that lets users create, save, edit, and organize personal entries. It uses natural language processing (NLP) to analyze each entry’s sentiment, identify the user’s emotional state, and recommend songs and movies that reflect the overall tone and themes of the writing.

## Table of Contents

1. [Overview](#overview)
2. [Target Architecture / API](#target-architecture--api)
3. [Features](#features)
4. [How It Works (with Screenshots)](#how-it-works-with-screenshots)
5. [Getting Started](#getting-started)
6. [Technologies & APIs](#technologies--apis)
7. [Future Features](#future-features-not-in-mvp)

## Target Architecture / API

SentimentScribe is moving to a Spring Boot web backend. The initial REST API surface will cover:

- Auth/session: login, verify password (or equivalent strategy)
- Diary entries: create, list, load, update, delete
- Analysis: analyze keywords/sentiment
- Recommendations: songs + movies for an entry

Persistence choice for the first web iteration: file-based storage.  
Frontend approach: separate SPA consuming the backend API.

## Features

- **Create Diary Entries** – Write and store personal diary entries.
- **Manage Entries** – Manually save your writing to a chosen folder and delete entries you no longer want, keeping your diary organized and relevant.
- **Load Entries** – Reopen past entries to read or edit them.
- **Password Protection** – Lock the diary with a password so entries remain private and secure.
- **Analyze Writing** – Use NLP to extract keywords and phrases from your writing and summarize the main ideas and topics.
- **Get Recommendations** – Receive music (Spotify) and movie (TMDb) suggestions that reflect the overall themes and tone of your writing.


## Getting Started

### Prerequisites

- Java 21
- Maven
- Access tokens / API keys for:
  - Spotify Web API
  - TMDb API

### Local Development

Backend:

```
cd backend
mvn spring-boot:run
```

Optional local Postgres (profile is still file-based by default):

```
docker compose up -d postgres
```

```
cd backend
SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run
```

Defaults (override with env vars): `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`.

Frontend:

```
cd frontend
npm install
npm run dev
```

### Production Build / Deployment

Frontend build (static hosting):

```
cd frontend
npm install
npm run build
```

- Set `VITE_API_BASE_URL` in the frontend build environment to the deployed backend URL.
- Deploy the frontend build output from `frontend/dist`.

Backend deployment:

```
cd backend
mvn clean package
```

- Deploy the generated Spring Boot artifact and configure CORS for the frontend origin.
- Set `SENTIMENTSCRIBE_CORS_ORIGIN` (or `sentimentscribe.cors.allowed-origins`) to the deployed frontend URL.

Release checklist:

- Backend API is reachable from the frontend host.
- Backend CORS allows the frontend origin.
- Frontend `VITE_API_BASE_URL` points to the backend URL.
- Spotify/TMDb keys are set in the backend environment.

## Technologies & APIs

- **Language:** Java
- **Build Tool:** Maven
- **NLP:** [Stanford CoreNLP](https://stanfordnlp.github.io/CoreNLP/)
- **Music Recommendations:** [Spotify Web API](https://developer.spotify.com/documentation/web-api)
- **Movie Recommendations:** [TMDb API](https://developer.themoviedb.org/docs)

---
