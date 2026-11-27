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

## Trash clean-up

Deleted contacts can be recovered for 30 days. A scheduled job (`TrashCleanupJob`) removes older ones
permanently: it runs one minute after the application starts and then every 24 hours. It locks the rows
it is about to delete, so it cannot remove a contact that is being restored at the same moment. No manual
step is needed.

## Maven Wrapper note

`mvnw` and `mvnw.cmd` are the Apache Maven Wrapper scripts (3.3.4) with a few **local changes**, marked
`local change` in the files:

- a user name and password are sent only over HTTPS, and only to the host of the download address
  (not to another host it redirects to);
- `mvnw.cmd` works in folders whose name contains an apostrophe, and stops when the download fails.

Regenerating the wrapper (`mvn wrapper:wrapper`) overwrites these changes.
The downloaded Maven distribution is verified with `distributionSha256Sum`
(`.mvn/wrapper/maven-wrapper.properties`).

`MVNW_REPOURL`, `MVNW_USERNAME` and `MVNW_PASSWORD` are only for private Maven mirrors. This project does
not need any of them.
