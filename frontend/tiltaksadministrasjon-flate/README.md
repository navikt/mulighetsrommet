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

For dette trenger man et access token definert i miljøvariabelen `VITE_MULIGHETSROMMET_API_AUTH_TOKEN`.
Denne kan du definere etter eget ønske, enten som en vanlig miljøvariabel, evt.
via [vite](https://vite.dev/guide/env-and-mode.html#env-files)
eller [mise](https://mise.jdx.dev/environments/#using-environment-variables).
For å generere dette gjør du følgende:

1. Naviger til lokal [Mock Oauth2 Server (azure)](http://localhost:8081/azure/debugger)
2. Trykk på knappen `Get a token`
3. Skriv inn hva som helst som `user/subject`
4. Legg inn dette i optional claims:
   ```json
   {
     "NAVident": "B123456",
     "aud": ["mulighetsrommet-api"],
     "oid": "0bab029e-e84e-4842-8a27-d153b29782cf",
     "uti": "0bab029e-e84e-4842-8a27-d153b29782cf",
     "groups": [
       "52bb9196-b071-4cc7-9472-be4942d33c4b"
     ]
   }
   ```
5. Trykk `Sign in`
6. Kopier verdien til `access_token` og benytt denne i nevnte miljøvariabel

##### Alternativt automatisk oppsett

Opprett følgende fil `frontend/tiltaksadministrasjon-flate/mise.local.toml`, eller set miljøvariablene på din egen måte.

```toml
[env]
MOCK_ISSUER_ID = "azure"
MOCK_CLAIMS_JSON = '{"NAVident":"B123456","aud":["mulighetsrommet-api"],"oid":"0bab029e-e84e-4842-8a27-d153b29782cf","uti":"0bab029e-e84e-4842-8a27-d153b29782cf","groups":["52bb9196-b071-4cc7-9472-be4942d33c4b"]}'
MOCK_BASE_URL = "http://localhost:8081"
MOCK_CLIENT_ID = "debugger"
MOCK_CLIENT_SECRET = "someSecret"
MOCK_SCOPE = "openid somescope"
MOCK_TOKEN_FILE = ".local/tiltaksadministrasjon-flate.json"
```

#### Kjør applikasjonen

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
