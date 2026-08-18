# ♟️ FIDE Chess Matchmaking & Tournament System

An automated, intelligent tournament management engine designed to resolve the inefficiencies of manual pairing, rigid play formats, and informal tie-breaking. Built for tournament organizers and arbiters, it dynamically manages player data, venue seating, and strict FIDE Swiss matchmaking rules.

---

## 📖 Overview
The automated Chess Matchmaking System is powered by a robust backend architecture utilizing Java Spring Boot to manage core application logic and RESTful APIs for seamless communication. Data persistence and integrity are handled by a highly normalized PostgreSQL database, which securely stores critical entities like players, tournaments, and match histories using Spring Data JPA and Hibernate. 

To ensure reliable state updates, transaction management guarantees ACID-compliant database operations during complex score calculations. Security is enforced through Spring Security, implementing Role-Based Access Control and JSON Web Tokens to restrict sensitive matchmaking actions to authorized arbiters while keeping leaderboard data public. To handle real-time performance demands, Redis serves as an in-memory caching layer that tracks players currently engaged in active matches. Furthermore, the system integrates a Python-driven microservice utilizing LangChain and a vector database to provide an advanced Retrieval-Augmented Generation (RAG) AI assistant, allowing organizers to instantly query the official rulebook during live events.

## ✨ Key Features

### 🏆 FIDE Swiss Matchmaking Engine
- **Deterministic Pairings:** Custom algorithm utilizing bucketing and backtracking to generate perfect $S_1$ vs $S_2$ pairings based on FIDE rules.
- **Strict Constraint Enforcement:** Hard constraints (no playing the same opponent twice, no three same colors in a row) and soft constraints (color floaters, downfloaters) handled systematically.
- **Bye Management & Tie-Breaking:** Automatic handling of odd-player byes and dynamic tie-break score calculation.

### 🛡️ Secure Arbiter Dashboard
- **Role-Based Access Control (RBAC):** Distinct roles for `ADMIN`, `ARBITER`, and `GUEST`.
- **Stateless Authentication:** Secured via JSON Web Tokens (JWT).
- **Protected Match States:** Only authorized personnel can trigger the matchmaking engine or submit official board results.

### 🤖 RAG-Powered AI Arbiter Assistant
- **FIDE Rulebook Vectorization:** Complete official chess rules stored in a semantic Vector Database.
- **Instant Query Resolution:** Python/LangChain microservice allows arbiters to ask natural language questions regarding discrepancies, returning context-aware rulings.

## 🛠️ Tech Stack

**Backend System**
- Java 17+
- Spring Boot (Web, Data JPA, Security)
- Hibernate ORM
- JWT (JSON Web Tokens)

**Data & Caching**
- PostgreSQL (Primary Relational Database)
- Redis (In-memory cache for active match states)

**AI Microservice**
- Python
- LangChain
- Vector Database (e.g., Pinecone / Chroma)
- LLM API integration

## 🏗️ System Architecture

```text
User/Client ── HTTP/REST ──> Spring Boot API Gateway (Controllers)
                                │
                                ├──> Spring Security (JWT Filter / RBAC)
                                │
                                ├──> Services (Matchmaking Engine, Transactional Logic)
                                │       ├──> PostgreSQL (Players, Matches, Standings)
                                │       └──> Redis (Active Match Cache)
                                │
                                └──> Internal REST Bridge ──> Python AI Service
                                                                └──> LangChain + Vector DB
```

## 🚀 API Endpoints (Core)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/tournaments` | Initialize a new tournament | `ARBITER` |
| `POST` | `/api/v1/tournaments/{tid}/players` | Enroll a player in the tournament | `ARBITER` |
| `POST` | `/api/v1/tournaments/{tid}/rounds/generate`| Trigger the FIDE Dutch engine | `ARBITER` |
| `PUT` | `/api/v1/matches/{match_id}/result` | Submit match result (Transactional update) | `ARBITER` |
| `GET` | `/api/v1/tournaments/{tid}/standings` | View the live leaderboard | `PUBLIC` |

## 🔮 Future Enhancements
- **Agentic AI Architecture:** Upgrading the RAG assistant to autonomously verify submitted match results against known chess logic.
- **External AI Integration:** Providing hooks to connect local Python/Pygame-based chess AI engines directly to the matchmaking service for bot-tournaments.
- **Online Platform Hooks:** Collaboration APIs for major chess platforms (e.g., Chess.com) for hybrid over-the-board/online events.

---
*Architected and developed with a focus on database normalization, algorithmic precision, and scalable system design.*
