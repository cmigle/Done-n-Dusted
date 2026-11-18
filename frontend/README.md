# Done n Dusted – web client

React 19 + TypeScript + Vite frontend for the Done n Dusted API. Pixel-art RPG look: "Sweetie-16" palette,
Pixelify Sans, pixelarticons, procedurally generated pixel avatars.

## Run locally

The API must be running on `http://localhost:8080` (`./mvnw spring-boot:run` in the repository root).

```bash
npm install
npm run dev          # http://localhost:5173, /api is proxied to :8080
```

In the cloud the API address comes from `VITE_API_URL` at build time, e.g.
`VITE_API_URL=https://<app>.azurewebsites.net/api npm run build`.

## Checks

```bash
npm run lint         # oxlint
npm run build        # tsc + production bundle
npm test             # Vitest unit tests (API client, components, forms)
npm run test:e2e     # Playwright end-to-end tests against the running app
```

End-to-end tests need the API and the dev server running. Locally they can use the installed Edge instead of
downloading a browser: `E2E_CHANNEL=msedge npm run test:e2e`.

`npm run screenshots -- <baseUrl> <outDir>` saves desktop and mobile screenshots of the main pages.
