# Tiltaksadministrasjon (admin-flate)

Kildekode for flate for administrative oppgaver rundt opprettelse og redigering av avtaler og
gjennomføringer.

## Oppsett

### Installere avhengigheter

```
pnpm install
```

### Lokal utvikling med mock-data

```sh
turbo run dev
```

### Lokal utvikling mot lokal backend

`backend`-scriptet henter nå access token automatisk via headless `authorization_code`-flyt mot mock-oauth2-server (`azure`).
Du trenger ikke lenger å bruke `/azure/debugger` manuelt.

Valgfritt kan du overstyre lokal konfig i en lokal (ikke commitet) `.mise.local.toml` i repo-root:

```toml
[env]
MOCK_BASE_URL = "http://localhost:8081"
CLIENT_ID = "debugger"
CLIENT_SECRET = "someSecret"
SCOPE = "openid somescope"
TOKEN_FILE = ".local/mock-oauth-token-azure.json"
```

For å overstyre claims kan du sette `CLAIMS_JSON` som miljøvariabel med syntetiske testdata.

```sh
turbo run backend
```

Fallback til gammel flyt finnes fortsatt med:

```sh
pnpm run backend:manual
```

## Testing og linting

Koden lintes og formatteres med `eslint` og `prettier`.

```
turbo run lint

# Fiks det som kan gjøres automatisk, bl.a. kode-formattering
turbo run lint:fix
```

E2E-tester er skrevet med `playwright`. Ved lokal testing kan det være behjelpelig å kjøre `playwright` med UI'et:

```
# Kjør tester
turbo run playwright

# Kjør tester med UI
turbo run playwright:open
```

## Deploy

Ved merge til main-branch deployes appen til dev og prod.
