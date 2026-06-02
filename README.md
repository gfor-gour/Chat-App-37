# Real-Time Multi-Module Chat Application (Java & Socket.IO)

A high-performance, real-time desktop chat application built using a multi-module Maven architecture in Java. The project features a distributed Client-Server model powered by **Socket.IO** (via Netty) for asynchronous real-time events, and **PostgreSQL** with **HikariCP** connection pooling for resilient database persistence. The UI is designed using Java Swing, styled with **FlatLaf (FlatDarkLaf)** for a premium dark-themed aesthetic, and laid out dynamically using **MigLayout**.

---

## 🏗️ System Architecture & Modules

The project is structured into three distinct Maven modules to enforce separation of concerns, compile-time safety, and code reusability:

```
socketio-chatapp (Root POM)
 ├── 📦 shared  - Common DTOs, request/response models, and validators
 ├── 🖥️ client  - Swing-based desktop chat application utilizing FlatLaf
 └── ⚙️ server  - Netty-Socket.IO server with PostgreSQL persistence
```

### 1. `shared` Module
Contains plain Java objects (POJOs), Data Transfer Objects (DTOs), and core validators (like `InputValidator`) that are shared between client and server. This prevents duplication and ensures JSON serialization schemas remain consistent across network boundaries.
*   **Key components:** `UserAccountDto`, `SendMessageRequest`, `ReceiveMessageResponse`, `MessageType`.

### 2. `client` Module
A lightweight Java desktop GUI designed for chat interaction.
*   **UI Core:** Swing, structured using container components like `ChatPanel` (divided into header, body, and input sub-panels) and `SidebarPanel` for active user lists.
*   **Networking:** Leverages the official `socket.io-client` Java library, encapsulated within a custom `ConnectionManager` implementing thread-safe listener updates.

### 3. `server` Module
A non-blocking, event-driven WebSocket backend.
*   **WebSocket Engine:** Built using `netty-socketio`, which runs on top of the high-performance Netty framework.
*   **Admin UI:** Features an integrated Java Swing monitoring dashboard displaying real-time connection status logs, initialization status, and shutdown triggers.

---

## 🔌 WebSocket Design & Real-Time Protocol

The application relies heavily on WebSockets to support bidirectional, real-time message passing. Below is the technical breakdown of the WebSocket stack:

### Connection Management & State Machine
The client-side `ConnectionManager` is a singleton class managing connection states (`DISCONNECTED`, `CONNECTING`, `CONNECTED`, `RECONNECTING`). 
*   **Custom Reconnection Loop:** Instead of relying on default socket.io reconnection, the application implements its own **exponential backoff algorithm** using a Java `ScheduledExecutorService` daemon thread. Reconnection delays double with each failure (e.g., $2^{\text{attempts}}$ seconds) up to a maximum cap of 30 seconds.
*   **Heartbeats:** Configured server-side with a 25-second ping interval and a 60-second ping timeout to detect dead sockets early without overloading the network.

### Multi-Event Architecture
Concerns on the server are segregated into decoupled event handlers:
*   `AuthEventHandler`: Handles user login (`login` event) and registration (`register` event), checking details against PostgreSQL.
*   `MessageEventHandler`: Manages messaging event forwarding (`send_to_user` event) and updates active sessions.
*   `FileEventHandler`: Processes file request and upload streams.
*   `ConnectionEventHandler`: Intercepts socket level connection/disconnection hooks and registers status changes on the monitor UI.

### Large Payload Handling
To support dynamic file transfers (images, documents), the server configuration is customized:
*   Max Frame Payload Length and Max HTTP Content Length are bound to a configurable limit (`file.max.size.mb` default to 50MB) to prevent Heap exhaustion while accommodating large buffers.

---

## 🗄️ PostgreSQL Database Integration

The persistence layer is optimized for transactional reliability, thread safety, and query performance.

### Schema Design
The schema uses a normalized structure consisting of authentication tables and account status details:
*   `user` Table: Houses primary keys, unique usernames, and secure passphrases.
*   `user_account` Table: Stores metadata such as gender, image binary data, and status indicators. It links to `user` via a foreign key constraint with cascading updates and deletions.
*   `files` Table: Logs uploaded file metadata, formats, blur hashes (for visual placeholders), and validation flags.

```sql
CREATE TABLE "user" (
  UserID SERIAL PRIMARY KEY,
  UserName VARCHAR(255) UNIQUE DEFAULT NULL,
  Password VARCHAR(72) NOT NULL
);

CREATE TABLE user_account (
  UserID INT PRIMARY KEY,
  UserName VARCHAR(255) DEFAULT NULL,
  Gender CHAR(1) NOT NULL DEFAULT '',
  Image BYTEA,
  ImageString VARCHAR(255) DEFAULT '',
  Status CHAR(1) NOT NULL DEFAULT '1',
  CONSTRAINT fk_user_account_user FOREIGN KEY (UserID) REFERENCES "user" (UserID) ON DELETE CASCADE ON UPDATE CASCADE
);
```

### High-Performance Optimization & Security
1.  **HikariCP Connection Pool:** Configured to manage database connections dynamically. Standard PostgreSQL performance tweaks are applied to the pool configuration:
    ```java
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    ```
2.  **Prepared Statements & Indexes:** Prevents SQL injection attacks and improves execution plan caching. Indexes are explicitly declared on high-frequency query filters (`idx_user_username` and `idx_user_account_status`).
3.  **BCrypt Password Hashing:** Uses `at.favre.lib:bcrypt` with a strong work factor to hash user credentials before they are written to PostgreSQL. Password verification is performed securely during authentication.

---

## 🛠️ Debugging Case Study: The Messaging Bug

### The Issue
A critical bug existed where users were unable to view incoming messages. While they would receive top-corner toast notifications and could see online users, no messages appeared in the chat bubbles. Furthermore, the sender's own sent messages would disappear if they switched chat screens in the sidebar.

### Root Cause Analysis
1.  **Missing Field Initialization:** On the client-side UI, when a user typed a message and sent it, a `SendMessageRequest` DTO was populated with the receiver's `userID`, the message content, and type. However, the client did not set the `fromUserID` field.
2.  **Zero-Value Propagation:** Due to this, the `fromUserID` defaulted to `0` in the payload. The Socket.IO server received this DTO, processed it, and dispatched a `ReceiveMessageResponse` to the recipient with `fromUserID = 0`.
3.  **Validation Failure:** Upon receiving the event, the recipient client inspected the message to decide which chat panel to append it to:
    ```java
    if (chatPanel.getCurrentUser() != null && chatPanel.getCurrentUser().getUserID() == data.getFromUserID()) {
        chatPanel.getBody().addMessage(data.getText(), MessageBubble.Alignment.LEFT);
    } else {
        toast.showToast("New message received", ToastNotification.Type.SUCCESS);
    }
    ```
    Since `data.getFromUserID()` was always `0`, it never matched the actual sender's User ID (e.g., `101`). Consequently, the condition evaluated to `false`, executing the `else` block which triggered the success toast notification but skipped rendering the message in the chat body.
4.  **UI Switch Clearance:** In Java Swing, to swap chat windows, `ChatPanel` triggers `bodyPanel.clear()` when a new user is selected. Since client-side caching of conversation threads was not implemented, clearing the body caused all temporary message components to flush.

### The Fix
The client code was updated inside `MainFrame.java` to fetch the current authenticated client ID from the `AuthService` state singleton and map it explicitly before transmission:

```diff
         chatPanel = new ChatPanel(text -> {
             UserAccountDto current = chatPanel.getCurrentUser();
             if (current != null) {
                 SendMessageRequest req = new SendMessageRequest();
+                req.setFromUserID(AuthService.getInstance().getCurrentUser().getUserID());
                 req.setToUserID(current.getUserID());
                 req.setText(text);
                 req.setMessageType(MessageType.TEXT);
```

Once applied, the socket payload delivered the authentic sender identity, allowing correct evaluation on the receiver's end and making the chat interface fully functional.

---

## 🚀 Getting Started

### Prerequisites
*   **Java JDK 17**
*   **PostgreSQL 14+**
*   **Apache Maven 3.8+**

### Database Setup
1.  Create a PostgreSQL database named `chat_application`.
2.  Import the schema file:
    ```bash
    psql -U postgres -d chat_application -f db/chat_application.sql
    ```

### Configuration
Edit the configuration file under `server/src/main/resources/application.properties` to match your local database credentials:
```properties
db.url=jdbc:postgresql://localhost:5432/chat_application
db.username=your_postgres_username
db.password=your_postgres_password
```

### Building the Project
From the root directory, run a clean Maven build:
```bash
mvn clean install -DskipTests
```

### Running the Application

> [!IMPORTANT]
> **PowerShell Token Splitting Workaround**
> When running Maven plugins in Windows PowerShell, wrap the `-Dexec.mainClass` argument in double quotes to prevent PowerShell from incorrectly splitting the dot parameters as lifecycle phases.

#### 1. Start the Server Monitor
```powershell
mvn -pl server exec:java "-Dexec.mainClass=com.raven.main.Main"
```

#### 2. Start Chat Clients (Run multiple times for testing)
```powershell
mvn -pl client exec:java "-Dexec.mainClass=com.raven.client.ChatClientApplication"
```
