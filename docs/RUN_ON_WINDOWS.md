# Run BookIt on Windows (beginner guide)

Follow the steps in order. Use **Command Prompt** (press Windows key, type `cmd`, Enter) for every command here. It avoids the PowerShell script error you saw with TaskFlow.

## 0. Install the tools (one time)

| Tool | Get it | Check it worked |
|---|---|---|
| **Java 17 JDK** | adoptium.net, choose "Temurin 17 (LTS)", Windows `.msi`. On the install screen tick **"Set JAVA_HOME variable"** and **"Add to PATH"**. | `java -version` shows 17 |
| **Maven** | maven.apache.org/download.cgi, download the **Binary zip**, unzip to `C:\tools\apache-maven-3.9.9`. Add `C:\tools\apache-maven-3.9.9\bin` to your PATH (Start menu, search "Environment Variables", Path, Edit, New). | `mvn -version` shows Maven and Java 17 |
| **Node.js 20** | nodejs.org (you already have it from TaskFlow) | `node -v` |
| **VS Code extension** | in VS Code install "Extension Pack for Java" | Java files get colours and hints |

Close and reopen Command Prompt after changing PATH.

## 1. Start the backend (no database needed)

```
cd path\to\bookit\backend
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

The first run downloads dependencies and takes a few minutes. Wait for a line like `Started BookItApplication`. Then open:

- http://localhost:8080/actuator/health  should show `{"status":"UP"}`
- http://localhost:8080/swagger-ui.html  interactive API docs
- http://localhost:8080/api/restaurants  shows three demo restaurants

This mode keeps data in memory, so everything resets when you stop it (Ctrl + C). That is fine for trying things out.

## 2. Start the frontend (second Command Prompt window)

```
cd path\to\bookit\frontend
npm install
npm run dev
```

Open http://localhost:5173.

- Customer: `demo@bookit.dev` / `Demo@12345` (the login page has a "Fill demo account" button)
- Admin: `admin@bookit.dev` / `Admin@12345`

Try this: open a restaurant, pick tomorrow, choose a time, confirm. Then sign in as admin in a private window and watch the booking appear under Admin, Bookings. Order a dish and move it through Start preparing, Mark ready, Mark served.

## 3. Run the tests

```
cd path\to\bookit\backend
mvn test
cd ..\frontend
npm test
```

## 4. Use real PostgreSQL (optional, needs Docker Desktop)

```
cd path\to\bookit
docker compose up -d db
cd backend
mvn spring-boot:run
```

Without the `h2` profile the app uses PostgreSQL on `localhost:5432` (user, password and database are all `bookit`). Data now survives restarts, and the database-level overlap constraint is active.

Whole stack in containers: `docker compose up --build`, then open http://localhost:3000.

## Troubleshooting

| Message | Fix |
|---|---|
| `'mvn' is not recognized` | Maven `bin` folder is not on PATH. Re-check step 0 and reopen Command Prompt. |
| `release version 17 not supported` or `invalid target release` | `java -version` is older than 17. Install Temurin 17 and make sure it is first on PATH. |
| `Port 8080 was already in use` | Another copy is running. Close that window, or run with `set PORT=8081` first and set the frontend proxy target in `vite.config.ts` to match. |
| Frontend shows "Cannot reach the server" | The backend is not running, or it crashed. Look at its window. |
| `Connection to localhost:5432 refused` | You ran without `-Dspring-boot.run.profiles=h2` and PostgreSQL is not running. Use step 1, or start the database as in step 4. |
| `JWT_SECRET must be at least 32 characters` | Only appears if you set a short `JWT_SECRET`. Use a longer one. |
| Blank page after login | Press F12, Console tab, and send me the red error. |

If a command fails, copy the **first red error** in the window and send it to me; the last lines are usually not the cause.
