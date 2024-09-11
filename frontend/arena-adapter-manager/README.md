# MAAM

Hjemmesnekra backoffice for diverse greier.

## Lokalt utvikling

Lokal dev-server kjøres med følgende kommando:

```
pnpm run dev
```

Proxy-instillinger er konfigurert i `vite.config.ts`.
For autentiserte requester kan du legge til følgende variabel i `.env`:

```
# Dette tokenet kan genereres med å følge guiden beskrevet i README.md til mulighetsrommet-api.
# Husk å sørge for at tokenet inkluderer riktig payload avhengig av hvilke apps du går mot lokalt.
VITE_AUTH_TOKEN=<token>
```
