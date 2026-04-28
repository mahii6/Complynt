<div align="center">

<img src="https://img.shields.io/badge/Complynt-v1.0.0-4A90D9?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZmlsbD0id2hpdGUiIGQ9Ik0xMiAyQzYuNDggMiAyIDYuNDggMiAxMnM0LjQ4IDEwIDEwIDEwIDEwLTQuNDggMTAtMTBTMTcuNTIgMiAxMiAyem0tMiAxNWwtNS01IDEuNDEtMS40MUwxMCAxNC4xN2w3LjU5LTcuNTlMMTkgOGwtOSA5eiIvPjwvc3ZnPg==" alt="Complynt" />

# 🛡️ Complynt

### Unified Customer Complaint Communication Dashboard

**Gen-AI powered complaint management that aggregates, categorises, and resolves customer issues at scale**

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![HTML5](https://img.shields.io/badge/HTML5-CSS3-E34F26?style=flat-square&logo=html5&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/HTML)
[![JavaScript](https://img.shields.io/badge/JavaScript-ES6+-F7DF1E?style=flat-square&logo=javascript&logoColor=black)](https://developer.mozilla.org/en-US/docs/Web/JavaScript)

---

[**Live Demo**](#-demo) · [**Features**](#-features) · [**Architecture**](#️-architecture) · [**Quick Start**](#️-quick-start) ·  [**Contributing**](#-contributing)

</div>

---

## 📽️ Demo

> ```
> [![Demo Video](https://img.youtube.com/vi/YOUR_VIDEO_ID/maxresdefault.jpg)](https://www.youtube.com/watch?v=YOUR_VIDEO_ID)
> ```
>

---

## 🌟 Overview

**Complynt** is a full-stack, Gen-AI powered Customer Complaint Communication Dashboard built to solve a core enterprise challenge: complaint chaos across multiple channels. It aggregates complaints from all sources into a **single, unified platform** and uses NLP and Generative AI to automatically handle classification, prioritisation, routing, deduplication, and resolution — leaving agents free to focus on what actually matters.

Inspired by **SAP Fiori design principles**, Complynt delivers a polished, enterprise-grade UX out of the box.

```
Customer submits complaint  →  AI classifies & routes  →  Agent resolves  →  Compliance report generated
      (any channel)              (auto, instant)           (guided by AI)       (one click)
```

---

## ✨ Features

### 🤖 Gen-AI & NLP Engine
- **Auto-classification** by complaint type, product, severity, and sentiment
- **Key issue extraction** from free-form customer text
- **Duplicate & related complaint detection** across channels
- **AI-drafted response templates** for agent review before sending
- **Root cause identification** across historical complaint data
- **Trend analysis** to surface emerging issues before they escalate

### 🎛️ Admin Dashboard
| Module | Description |
|---|---|
| **Dashboard** (`index.html`) | Real-time KPIs: total complaints, SLA breaches, severity distribution, daily trends |
| **Complaints** (`complaints.html`) | Filterable master list across products, severities, and statuses |
| **Complaint Detail** (`complaint-detail.html`) | SAP Object Page-style deep dive — tabs for Details, Actions, Communications, SLA, Customer 360 |
| **Agent Management** (`agents.html`) | Team structure, workload visibility, and auto-routing logic |
| **Customer 360°** (`customers.html`) | Unified customer profiles linking all complaints to a single identity |
| **SLA Monitor** (`sla.html`) | Overdue ticket tracking with configurable SLA rules per severity |
| **Reports & Compliance** (`reports.html`) | One-click CSV export for regulatory reporting (e.g., RBI Ombudsman) |

### 🌐 Customer Portal
- Clean, distraction-free complaint submission form (`new-complaint.html`)
- Auto ticket number generation
- Severity and channel auto-assigned on ingestion
- No login required — designed for zero friction

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        FRONTEND                             │
│   Admin Dashboard (HTML/CSS/JS)  |  Customer Portal         │
│   SAP Fiori-inspired Design System (style.css)              │
│   Async REST Client (api.js) | UI Helpers (common.js)       │
└───────────────────────────┬─────────────────────────────────┘
                            │ REST /api/*
┌───────────────────────────▼─────────────────────────────────┐
│                    SPRING BOOT BACKEND                       │
│                                                             │
│  ┌─────────────────┐    ┌──────────────────┐               │
│  │ ComplaintService│    │  IngestionService │               │
│  │ (core CRUD &    │    │  (dedup + C360    │               │
│  │  orchestration) │    │   identity merge) │               │
│  └────────┬────────┘    └────────┬─────────┘               │
│           │                      │                          │
│  ┌────────▼────────┐    ┌────────▼─────────┐               │
│  │  RoutingService │    │    SlaService     │               │
│  │ (auto-assign to │    │  (dynamic SLA     │               │
│  │  least-loaded   │    │   deadlines &     │               │
│  │   agent)        │    │   breach flags)   │               │
│  └─────────────────┘    └──────────────────┘               │
│                                                             │
│  ┌─────────────────┐    ┌──────────────────┐               │
│  │ AuditLogService │    │  AiGatewayService │ ─── AI stub  │
│  │ (immutable      │    │  (NLP, sentiment, │    awaiting  │
│  │  ledger for     │    │   entity extract, │    Python    │
│  │  compliance)    │    │   auto-response)  │    service   │
│  └─────────────────┘    └──────────────────┘               │
└───────────────────────────┬─────────────────────────────────┘
                            │ JPA
┌───────────────────────────▼─────────────────────────────────┐
│                        MySQL 8.0+                           │
│         Complaints | Agents | Customers | SLA Rules         │
│         Audit Logs | Actions | Communication History        │
└─────────────────────────────────────────────────────────────┘
```

### Backend Services (Spring Boot — Controller → Service → Repository → Entity)

| Service | Responsibility |
|---|---|
| `ComplaintService` | Core orchestrator for creation, filtering, and status updates |
| `IngestionService` | Handles deduplication and Customer 360 identity resolution (email/phone/account) |
| `RoutingService` | Auto-assigns complaints to the least-loaded agent in the correct product team |
| `SlaService` | Dynamically calculates deadlines from `SlaRule` config; flags breaches |
| `AuditLogService` | Immutable change ledger for every status change, assignment, and action |
| `AiGatewayService` | Gateway to the AI/NLP pipeline — classification, sentiment, draft responses *(stub, pending Python microservice)* |

---


## ⚙️ Quick Start

### Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | 17 or higher |
| MySQL Server | 8.0+ (running on `localhost:3306`) |
| Maven | Bundled via `mvnw` wrapper — no install needed |

### 1. Clone the Repository

```bash
git clone https://github.com/Swaraj1657/Complynt.git
cd Complynt
```

### 2. Configure the Database

Create a MySQL database, then update `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/complynt_db
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD_HERE
spring.jpa.hibernate.ddl-auto=update
```

> ⚠️ **Important:** Do not commit real credentials. Add `application.properties` to `.gitignore` or use environment variables for production.

### 3. Run the Application

**On Windows:**
```cmd
.\mvnw.cmd spring-boot:run
```

**On macOS / Linux:**
```bash
./mvnw spring-boot:run
```

### 4. Open in Browser

| Portal | URL |
|---|---|
| 🖥️ **Admin Dashboard** | http://localhost:8080/ |
| 🌐 **Customer Complaint Portal** | http://localhost:8080/new-complaint.html |

---

## 📁 Project Structure

```
Complynt/
├── src/
│   └── main/
│       ├── java/
│       │   └── .../
│       │       ├── controller/        # REST API controllers
│       │       ├── service/           # Business logic (see Architecture)
│       │       ├── repository/        # JPA repositories
│       │       └── entity/            # JPA entities (Complaint, Agent, Customer, etc.)
│       └── resources/
│           ├── static/
│           │   ├── index.html         # Admin Dashboard
│           │   ├── complaints.html    # Complaint list
│           │   ├── complaint-detail.html  # Complaint deep-dive
│           │   ├── agents.html        # Agent management
│           │   ├── customers.html     # Customer 360 view
│           │   ├── sla.html           # SLA monitor
│           │   ├── reports.html       # Compliance reporting
│           │   ├── new-complaint.html # Customer portal
│           │   ├── style.css          # SAP Fiori-inspired design system
│           │   └── js/
│           │       ├── api.js         # Async REST client
│           │       └── common.js      # UI helpers & layout init
│           └── application.properties # App configuration
├── pom.xml
└── mvnw / mvnw.cmd                    # Maven wrapper
```

---

## 🗺️ Roadmap

- [x] Core complaint CRUD and status management
- [x] Auto-routing to least-loaded agent
- [x] Dynamic SLA calculation and breach detection
- [x] Customer 360 identity resolution (email/phone/account)
- [x] Immutable audit log for compliance
- [x] CSV export for regulatory reporting
- [x] AI Gateway service stub (classification, sentiment, draft responses)
- [ ] Python AI microservice integration (NLP pipeline)
- [ ] Real-time notifications (WebSocket)
- [ ] Multi-channel ingestion (Email, WhatsApp, Twitter/X)
- [ ] Trend analysis and root cause dashboard
- [ ] Role-based access control (RBAC)
- [ ] Docker / Docker Compose setup

---

## 🤝 Contributing

Contributions are welcome! Here's how to get started:

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/your-feature-name`
3. **Commit** your changes: `git commit -m "feat: add your feature"`
4. **Push** to your branch: `git push origin feature/your-feature-name`
5. **Open a Pull Request** — describe what you've done and why

Please follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages.

---

## 👤 Author

Mahi · [@mahii6](https://github.com/mahii6)

---

<div align="center">

Made with ❤️ for better customer experiences

⭐ **Star this repo if you find it useful!**

</div>
