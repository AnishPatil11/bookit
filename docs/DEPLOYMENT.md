# Deploy BookIt (free tier): Neon + Render + Vercel

Order matters: database first, then backend, then frontend, then one fix-up so the backend accepts the frontend.

## 1. Database on Neon

1. Sign up at neon.tech, create a project (any region near you), database name `bookit`.
2. Open **Connection details**. Turn **off** "Connection pooling" so you get the **direct** host (it does not contain `-pooler`). Flyway migrations work best on a direct connection.
3. Note the host, database, user and password. Build the JDBC URL like this:

```
jdbc:postgresql://<host>/<database>?sslmode=require
```

Example: `jdbc:postgresql://ep-cool-sun-123456.ap-southeast-1.aws.neon.tech/bookit?sslmode=require`

The user and password go in **separate** variables, so special characters in the password cause no trouble (the problem you hit with Atlas).

## 2. Backend on Render

1. Push the whole `bookit` folder to a GitHub repository (never commit `.env` files).
2. Render, **New**, **Web Service**, pick the repo.
3. Settings:
   - **Root Directory:** `backend`
   - **Runtime / Language:** Docker (Render finds `backend/Dockerfile`)
   - **Instance type:** Free
   - **Health Check Path:** `/actuator/health`
4. **Environment** variables:

| Key | Value |
|---|---|
| `DB_URL` | the JDBC URL from step 1 |
| `DB_USER` | your Neon user |
| `DB_PASSWORD` | your Neon password |
| `JWT_SECRET` | a random string of 40+ characters |
| `SEED_ADMIN_PASSWORD` | a strong password of your own (this is your live admin login) |
| `CORS_ALLOWED_ORIGINS` | leave as a placeholder for now, set in step 4 |

5. Deploy. The first build takes several minutes. When the log shows `Started BookItApplication`, open `https://YOUR-SERVICE.onrender.com/swagger-ui.html`.

Free instances sleep after about 15 minutes idle; the first request afterwards takes around a minute. Mention this in your README so a recruiter is not surprised, or ping `/actuator/health` with UptimeRobot.

If the log says `permission denied to create extension "btree_gist"`, set `SPRING_FLYWAY_LOCATIONS` to `classpath:db/migration` to skip the PostgreSQL-only constraint (the app-level locking still prevents double bookings).

## 3. Frontend on Vercel

1. Vercel, **Add New Project**, import the same repo.
2. **Root Directory:** `frontend`. Framework preset: Vite.
3. **Environment Variables:** `VITE_API_URL` = `https://YOUR-SERVICE.onrender.com` (no trailing slash, no `/api`).
4. Deploy. `frontend/vercel.json` already makes page refreshes on routes like `/bookings` work.

## 4. Let the backend accept the frontend

In Render, set `CORS_ALLOWED_ORIGINS` to your exact Vercel origin, for example `https://bookit-yourname.vercel.app` (no trailing slash, no path). Save; Render redeploys. Without this the browser blocks every API call.

## 5. Check it

- Open the Vercel URL, the three demo restaurants appear (the first load may be slow while Render wakes).
- Register, book a table, sign in as admin and see it.
- Copy both URLs into the README, the resume and LinkedIn.

## Common problems

| Symptom | Cause and fix |
|---|---|
| Browser console: CORS error | `CORS_ALLOWED_ORIGINS` is missing, has a trailing slash, or has the wrong domain. |
| Frontend says "Cannot reach the server" | `VITE_API_URL` is wrong or was added after the build. Fix it, then **Redeploy** the Vercel project. |
| Render log: password authentication failed | Wrong `DB_USER` or `DB_PASSWORD`. Re-copy from Neon. |
| Render log: `JWT_SECRET must be at least 32 characters` | Make the secret longer. |
| Render log: Flyway validation error after you edited a migration | Never edit an applied migration. Add a new `V3__...sql` file instead, or reset the Neon database for a fresh demo. |
