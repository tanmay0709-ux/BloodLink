# BloodLink: Academic Project Report Progress Tracker

**Document:** Comprehensive Academic Project Report (Target: ~60 Pages)  
**Project:** BloodLink: Hospital Blood Bank Management and Emergency Blood Distribution System  
**Last Updated:** 2026-11-01  
**Status:** Inception & Architectural Mapping Complete — Page Allocation Proposed

---

## 1. Project Reference & Architectural Baseline
- **Academic Standard:** Modeled after SRM Institute of Science and Technology Department of Computer Science & Engineering Major/Minor Technical Project Report.
- **Frontend Stack:** React 18.3.1, JavaScript (ES6+), Vite 5.4.10, Axios, Responsive Custom CSS, Inter typography.
- **Backend Stack:** Java 21 LTS, Spring Boot 3.3.5, Spring Security 6 (Stateless JWT via JJWT 0.12.3), Spring Data JPA / Hibernate ORM, Maven.
- **Database:** MySQL relational engine (`bloodlink_db`), multi-table relational schema with strict foreign key integrity and transactional boundaries.
- **Architecture Pattern:** Enterprise Modular Monolith with layered separation of concerns (Controllers → Services → Repositories → Domain Entities).
- **Core User Roles:** `SYSTEM_ADMIN`, `HOSPITAL_STAFF`, `BLOOD_BANK_STAFF`.
- **Domain Modules:**
  1. Authentication & Role-Based Facility Binding (JWT)
  2. Hospital & Blood Bank Facility Administration
  3. Donor Registration, Screening & 56-Day Cooldown Eligibility Engine
  4. Perishable Blood Unit Ingestion, Component Shelf-Life (PRBC 42d, Platelets 5d, FFP 365d), and Status Tracking
  5. Emergency Blood Requests, Priority Triaging & Educational ABO/Rh Compatibility Matrix
  6. Two-Phase Allocation, Cold-Chain Dispatch Verification & Hospital Receipt Confirmation
  7. Real-Time In-App Alerting & Notification Dispatch Subsystem
  8. Executive Operational Analytics, Inventory Aggregation & Fulfillment Metrics Dashboard

---

## 2. Target 60-Page Allocation Matrix

| Section / Chapter | Content Scope & Key Technical Focus | Target Pages | Running Page Range | Status |
| :--- | :--- | :---: | :---: | :---: |
| **Front Matter** | Title Page, Bonafide Certificate, Acknowledgement, Executive Abstract | 4 | i – iv | **COMPLETED & SAVED TO DISK** |
| **Preliminary Lists** | Table of Contents, List of Figures, List of Tables, List of Abbreviations & Acronyms | 4 | v – viii | **COMPLETED & SAVED TO DISK** |
| **Chapter 1** | **Introduction & Domain Context**<br>• 1.1 Historic Evolution of Blood Transfusion & Logistics<br>• 1.2 Digital Transformation of Blood Supply Networks<br>• 1.3 Biomedical & Logistical Principles of Blood Cold-Chain and Allocation<br>• 1.4 Scope and System Boundaries of BloodLink<br>• 1.5 Structure & Organization of the Dissertation | 4 | 1 – 4 | **COMPLETED & SAVED TO DISK** |
| **Chapter 2** | **Project Objectives & Requirements Engineering**<br>• 2.1 Primary Healthcare & Operational Objectives<br>• 2.2 Technical, Non-Functional, and Reliability Objectives<br>• 2.3 Stakeholder Analysis & User Personas Matrix<br>• 2.4 Functional Requirements Specification (IEEE 830 Use Case Index & Details)<br>• 2.5 Non-Functional Requirements & Architectural Quality Attributes | 6 | 5 – 10 | **COMPLETED & SAVED TO DISK** |
| **Chapter 3** | **Problem Statement & Domain Challenges**<br>• 3.1 Structural Inefficiencies in Paper/Phone-Based Blood Logistics<br>• 3.2 The Perishable Component Dilemma & Expiry Wastage<br>• 3.3 Emergency Search Latency & Stockout Hazards<br>• 3.4 Concurrency Bottlenecks & Double-Allocation Race Conditions<br>• 3.5 Cold-Chain Integrity, Custody Transfers & State Desynchronization | 4 | 11 – 14 | **COMPLETED & SAVED TO DISK** |
| **Chapter 4** | **Literature Review & Theoretical Foundations**<br>• 4.1 Comparative Taxonomy of Healthcare Supply Chain Systems<br>• 4.2 ABO/Rh Immunological Compatibility Matrices & Clinical Screening Protocols<br>• 4.3 First-Expiry-First-Out (FEFO) vs. FIFO in Perishable Medical Inventory<br>• 4.4 Architectural Evolution: Monolith vs. Microservice in Healthcare Information Systems<br>• 4.5 State-of-the-Art in Real-Time Web Push Protocols & Notification Pipelines | 6 | 15 – 20 | **COMPLETED & SAVED TO DISK** |
| **Chapter 5** | **Proposed Methodology & Engineering Process**<br>• 5.1 Agile-Scrum Software Development Life Cycle (Sprint breakdown)<br>• 5.2 Domain-Driven Design (DDD) & Ubiquitous Healthcare Language<br>• 5.3 Layered Enterprise Architecture Methodology<br>• 5.4 Defensive Transactional Protocols, Concurrency Locks & Role-Based Access Control<br>• 5.5 Quality Assurance & Static Code Analysis Pipeline | 4 | 21 – 24 | **COMPLETED & SAVED TO DISK** |
| **Chapter 6** | **System Design, Architecture & Data Modeling**<br>• 6.1 High-Level Enterprise Modular Monolith Architecture Topology<br>• 6.2 Relational Data Dictionary (users, hospitals, blood_banks, donors, donations, blood_units, blood_requests, blood_allocations, blood_dispatches, notifications)<br>• 6.3 Comprehensive Entity-Relationship (ER) Schema Model<br>• 6.4 UML Structural Class Model<br>• 6.5 UML Behavioral State Machine & Activity Models (Request Lifecycle & Blood Unit States) | 7 | 25 – 31 | **COMPLETED & SAVED TO DISK** |
| **Chapter 7** | **Implementation Details & Codebase Analysis**<br>• 7.1 System Technology Stack & Dependency Hierarchy (`pom.xml`, `package.json`)<br>• 7.2 Domain Entity Models Implementation<br>• 7.3 Data Access Layer & Custom JPQL Queries<br>• 7.4 Service Layer Business Logic & Transaction Management<br>• 7.5 REST API Controller Implementation & JWT Security Filter Chain<br>• 7.6 Responsive React 18 Frontend Engineering<br>• 7.7 Role-Based Authorization, Security Context & Input Validation | 7 | 32 – 38 | **COMPLETED & SAVED TO DISK** |
| **Chapter 8** | **Operational Mechanics & Execution Workflows**<br>• 8.1 User Authentication, Role Assignment & Facility Binding<br>• 8.2 Donor Registration & 56-Day Cooldown Eligibility Logic<br>• 8.3 Blood Unit Ingestion, Shelf-Life Auto-Calculation & Stock Management<br>• 8.4 Emergency Request Lifecycle, ABO/Rh Component Compatibility Filtering & Two-Phase Unit Reservation<br>• 8.5 Multi-Facility Allocation, Cold-Chain Dispatch Verification & Hospital Receipt Confirmation<br>• 8.6 Executive Analytics Aggregation & Real-Time Dashboard Rendering | 5 | 39 – 43 | **COMPLETED & SAVED TO DISK** |
| **Chapter 9** | **Results, Performance Benchmarking & Discussion**<br>• 9.1 Functional Verification Matrix (Exhaustive Test Scenarios)<br>• 9.2 Transactional Concurrency & Race-Condition Resistance Audit<br>• 9.3 System Latency, Throughput & Profiling Benchmarks<br>• 9.4 Clinical Compatibility Screening Verification<br>• 9.5 Comparative Feature Evaluation Matrix with Legacy Systems | 5 | 44 – 48 | **COMPLETED & SAVED TO DISK** |
| **Chapter 10 & 11** | **Conclusion & Future Scope**<br>• 10.1 Synthesis of Achievements<br>• 10.2 Architectural Strengths and Practical Healthcare Value<br>• 10.3 Final Concluding Remarks<br>• 11.1 Real-Time IoT Temperature Sensor & GPS Courier Tracking<br>• 11.2 Full-Duplex WebSockets / Push Notifications Migration<br>• 11.3 Machine Learning Demand Forecasting for Epidemics & Mass Casualties<br>• 11.4 Integration with National Health Stacks (e-RaktKosh / ABDM)<br>• 11.5 Donor Mobile Application & Automated WhatsApp Alert Daemon | 3 | 49 – 51 | **COMPLETED & SAVED TO DISK** |
| **Chapter 12** | **Comprehensive Academic & Industry References**<br>• 30+ peer-reviewed citations in strict APA 7th format | 2 | 52 – 53 | **COMPLETED & SAVED TO DISK** |
| **Appendices** | **Appendices A through E**<br>• Appendix A: Complete Installation, Setup, Build & Operations Manual (2 pp)<br>• Appendix B: Complete RESTful API Specifications & Curl Payload Samples (2 pp)<br>• Appendix C: Relational SQL DDL Schemas, Seed Catalogs & Query Book (2 pp)<br>• Appendix D: Annotated Source Code Listings for Core Production Units (2 pp)<br>• Appendix E: Technical Glossary & Enterprise Healthcare Acronym Directory (1 p) | 9 | 54 – 62 | Ready to Generate |
| **Total Target** | | **~62** | **Full Treatise** | |

---

## 3. Technical Invariants & Verification Checklist
- [x] **Request Quantity vs. Unit Quantity:** Explicitly differentiated across all sections: Blood Request quantity indicates discrete unit bags; Blood Unit quantity indicates fluid volume in milliliters (ml).
- [x] **ABO/Rh Compatibility Notice:** Strictly framed as educational decision-support screening; clearly stated that clinical transfusion requires crossmatching and laboratory validation.
- [x] **Database Schema Alignment:** All tables match actual entities: `users`, `hospitals`, `blood_banks`, `donors`, `donations`, `blood_units`, `blood_requests`, `blood_allocations`, `blood_dispatches`, `blood_dispatch_units`, `notifications`.
- [x] **Role Hierarchy Alignment:** Correctly reflects `SYSTEM_ADMIN`, `HOSPITAL_STAFF`, and `BLOOD_BANK_STAFF`.

---

## 4. Current Work State & Exact Next Continuation Step
- **Current Step:** Batches 1, 2, 3, and 4 Completed (Front Matter, Chapters 1 through 12 written and saved to `BloodLink_Report_Draft.md`).
- **Next Step:** Generate Batch 5 (Appendices A through E: Complete Installation & Operations Manual, RESTful API Specs & Payloads, Relational SQL DDL Schemas, Annotated Java Production Units, and Technical Glossary).
