# Contact Manager: backend

Spring Boot REST API (Java 21, MySQL 8).

## Setup (once)

1. **Create the database user.** Open `db/create-user.sql`, replace `CHANGE_ME` with your own password
   in a local copy (do not commit it), then run it as a MySQL admin:

   ```bash
   mysql -u root -p < db/create-user.sql
   ```

2. **Create the tables:**

   ```bash
   mysql -u root -p < db/schema.sql
   ```

   In PowerShell `<` does not work; use `Get-Content db\schema.sql | mysql -u root -p`.

## Environment variables

Secrets are never stored in the repository.

| Variable | Meaning |
|---|---|
| `DB_PASSWORD` | the password chosen for `contact_user` |
| `JWT_SECRET` | any random text of at least 32 characters, used to sign login tokens |

PowerShell example (current terminal only):

```powershell
$env:DB_PASSWORD = "your-database-password"
$env:JWT_SECRET  = "a-long-random-text-of-at-least-32-characters"
```

## Run and test

```bash
./mvnw spring-boot:run    # starts the API on http://localhost:8080
./mvnw test               # runs the tests
```

On Windows use `.\mvnw`. The tests use an in-memory H2 database, so they need neither MySQL nor the
variables above.

## Maven Wrapper note

`mvnw` and `mvnw.cmd` are the standard Apache Maven Wrapper scripts and are not edited by hand.
The downloaded Maven distribution is verified with `distributionSha256Sum`
(`.mvn/wrapper/maven-wrapper.properties`).

If you use a private Maven mirror with `MVNW_USERNAME` / `MVNW_PASSWORD`, set `MVNW_REPOURL` **only to an
`https://` address**, and only to a mirror that does not redirect to another host. This project does not
need any of these variables.
