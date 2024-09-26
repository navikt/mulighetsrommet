# Tiltaksadministrasjon (admin-flate)

Kildekode for flate for administrative oppgaver rundt opprettelse og redigering av avtaler og
tiltaksgjennomføringer.

## Demo

Se demo av løsningen her https://tiltaksadministrasjon.ekstern.dev.nav.no

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

`.env` må konfigureres med token for lokal autentisering. Se seksjonen
om [lokal autentisering](../../mulighetsrommet-api/README.md#autentisering):

```.env
# Setter Bearer token for HTTP-kall mot mulighetsrommet-api.
VITE_MULIGHETSROMMET_API_AUTH_TOKEN=<token>
```

Deretter kan appen startes mot lokal backend:

```sh
turbo run backend
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
