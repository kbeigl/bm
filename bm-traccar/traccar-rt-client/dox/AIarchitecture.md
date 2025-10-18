# Traccar API Client Architecture (Revised)

## Key Architectural Characteristics

- **Separation of Concerns**: The architecture separates generated API code (`traccar-openapitools-client`) from business logic and integration code (`traccar-api-client`).
- **Generated API Layer**: All REST API models and client classes are generated from the Traccar OpenAPI spec and maintained in `traccar-openapitools-client`.
- **Extensible Client**: `traccar-api-client` wraps and extends the generated API, providing a stable interface for application developers.

---

## Project Structure

The following diagram shows the relationship between the two main projects:

```mermaid
graph TD
    A[traccar-api-client] -->|Depends on| B[traccar-openapitools-client]
    B -->|Generated API Code| C[REST API]
```

---

## Component Interaction (Runtime Flow)

This sequence diagram illustrates how components interact at runtime for REST API calls:

```mermaid
sequenceDiagram
    participant UserApp
    participant ApiClient as traccar-api-client
    participant OpenApi as traccar-openapitools-client
    participant TraccarServer

    UserApp->>ApiClient: Call business method
    ApiClient->>OpenApi: Use generated REST client
    OpenApi->>TraccarServer: HTTP Request
    TraccarServer-->>OpenApi: HTTP Response
    OpenApi-->>ApiClient: API Model/DTO
    ApiClient-->>UserApp: Result/DTO
```

---

## Deployment Architecture

The deployment diagram below shows how the components are deployed and interact with the Traccar server:

```mermaid
flowchart LR
    subgraph Client Host
        A1[User Application]
        A2[traccar-api-client]
        A3[traccar-openapitools-client]
    end

    subgraph Server Host
        B1[Traccar Server]
    end

    A1-->|API Calls|A2
    A2-->|Uses|A3
    A3-->|HTTP|B1
```

---

## Component Types

- **traccar-api-client**: Contains business logic, integration code, and references the generated API.
- **traccar-openapitools-client**: Contains only generated REST API models and client classes.

---
