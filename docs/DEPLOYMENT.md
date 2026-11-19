# Done n Dusted - deployment

**Live: <https://done-n-dusted.azurewebsites.net>.** The UI, the API (`/api`), Swagger UI
(`/swagger-ui.html`) and the health check (`/actuator/health`) are served from one address.

```
Browser ──HTTPS──► Azure App Service (Italy North, Linux, Java 21, B1)
                     Spring Boot "prod" + React build inside (classpath:/static)
                         │ JDBC, sslmode=require
                         ▼
                   Azure Database for PostgreSQL 17 (Poland Central, Burstable B1ms, 32 GB)
```

| Decision | Why |
| --- | --- |
| Regions | The subscription policy allows only five regions: Poland Central, Italy North, Austria East, Belgium Central and Denmark East |
| App Service in Italy North | The B1 App Service quota in Poland Central is 0 for this subscription; Italy North allows it |
| UI in the same App Service | Azure Static Web Apps only runs in regions the policy does not allow. So the React build is packaged into the jar and Spring Boot serves it (`FrontendConfig`): one address, no CORS, Swagger on the same domain |
| PostgreSQL Flexible Server | The same PostgreSQL as locally; Flyway creates the schema and demo data on first start |
| Configuration in environment variables | `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (48 random characters), `CORS_ORIGINS`, `PORT`/`WEBSITES_PORT=8080`, all in the App Service settings, not in Git |
| Security | HTTPS only, TLS 1.2 or later, FTP disabled; the database accepts only Azure services and only over SSL |
| Always On | The app does not go to sleep, so the first response is fast |

## Deploying

```bash
scripts/package.sh                                   # React build -> static, mvn package
az webapp deploy -g rg-done-n-dusted -n done-n-dusted --type jar \
  --src-path target/done-n-dusted-0.3.0.jar
```

**Verified in the cloud:** the Postman collection passes all 176 checks and Playwright passes 10 of 10
scenarios (`E2E_BASE_URL=https://done-n-dusted.azurewebsites.net npm run test:e2e`). The database was
recreated after the test runs, so the live demo has clean demo data.
