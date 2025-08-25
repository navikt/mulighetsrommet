<h1 align="center">Mulighetsrommet 🎯</h1>

## Introduksjon

Mulighetsrommet er en applikasjonsportfølje som skal hjelpe brukere og veiledere til å få en helhetlig oversikt
over alle arbeidsmarkedstiltak Nav kan tilby.

## Oppsett

Dette prosjektet er strukturert som et monorepo, der både backend- og frontend-kode er organisert i samme kodebase.
Enn så lenge benytter vi følgende tooling for å kjøre tasks for henholdsvis backend og frontend:

- [Gradle](https://gradle.org/) med subprojects
- [Turborepo](https://turborepo.org/) i kombinasjon med [PNPM workspaces](https://pnpm.io/workspaces)

**MERK: Det er lenket til flere programmer under, men vær oppmerksom om du følger lenkene og installerer disse
programmene på egen maskin. Dobbeltsjekk at lenkene er oppdaterte og troverdige før du blindt følger noen som helst
guider (også denne) og gjør endringer på egen maskin!**

### Tooling via mise-en-place

Om ønskelig så kan [mise](https://mise.jdx.dev/) benyttes til å installere verktøyene som trengs for å kjøre dette
prosjektet lokalt. Dette prosjektet inkluderer en `mise` [mise.toml](https://mise.jdx.dev/configuration.html#mise-toml)
-fil som spesifiserer versjoner for bygg- og runtime-avhengigheter som matcher det vi kjører på Github Actions (CI) og
på NAIS.

For å benytte `mise` så må du [installere programmet](https://mise.jdx.dev/getting-started.html#getting-started) og
deretter kan du kjøre følgende kommando:

```bash
# Installer tooling
mise install

# Oppdater verktøy til ny versjon, f.eks.
mise use node@24
```

**MERK:** `mise` er et verktøy som gjør det enklere å administrere ting som er felles på tvers av prosjektet. Unngå å
legge til unødvendig med verktøy i konfigurasjonen som er sjekket inn i repoet.
Benytt heller [mise.local.toml](https://mise.jdx.dev/configuration.html#mise-toml) om du ønsker å benytte verktøyet til
flere formål, så unngår du å dytte dine egne vaner på naboen.

### Gradle

Det anbefales å ha `gradle` installert, det gjør det lettere å kjøre kommandoer uavhengig av hvilket prosjekt du jobber
med. Hvis du ikke har installert `gradle` via `asdf` så kan det
også [installeres manuelt](https://gradle.org/install/).

Det ligger et [gradlew](./gradlew) script i repoet som oppgraderes ved nye versjoner og `gradle` plukker automatisk opp
dette. Dette lar oss kjøre scripts i forskjellige prosjekter uten å måtte referere direkte til `gradlew`, f.eks:

```sh
# I mappen mulighetsrommet-api
gradle run
```

### Turborepo

Turborepo benyttes til å kjøre kommandoer på tvers av workspaces. Det tar seg blandt annet av å cache output fra bygg og
å kjøre bygge-script i avhengigheter om det er behov for det. Det kan være en fordel å installere globalt for å gjøre
lokal utvikling enklere:

```sh
npm i -g turbo
```

Deretter kan npm-scripts kjøres direkte via `turbo`. Fordelen med å gjøre dette er bl.a. at interne avhengigheter bygges
automatisk:

```sh
turbo run dev
turbo run build
# osv ...
````

Se [turbo.json](./turbo.json)
og [Configuring tasks](https://turbo.build/repo/docs/crafting-your-repository/configuring-tasks) for hvordan man kan
utvide støtten med flere scripts.

### Token for pnpm install av private pakker

Noen pakker under `@navikt` hentes fra Github sitt NPM-repository. For at dette skal fungere må du først autentisere mot
Github:

```
pnpm login --registry https://npm.pkg.github.com
```

Brukernavn er Github-brukernavnet ditt. Passordet er et [Personal Access Token](https://github.com/settings/tokens) med
`read:packages`-scope. Tokenet må autentiseres med SSO mot navikt-organisasjonen.

#### Github token er utdatert.

1. Gå til [Personal Access Token på Github](https://github.com/settings/tokens)
2. Trykk `Tokens (classic)`
3. Trykk `Generate new token` --> `Generate new token (classic)`
4. Skriv noe som `Nav IT` under `Note`
5. Velg hvor lenge du vil at det skal vare under `Expiration`
6. Under `Select scope` velg `repo` og `read:packages`
7. Trykk `Generate token`
8. Kopier `ghp_x...` tokenet og putt det i `.npmrc` filen på maskinen din
9. Trykk `Configure SSO`
10. Trykk `Authorize` på `navikt`
11. Ferdig!

### Docker

For å gjøre utvikling på lokal maskin enklere benytter vi Docker og Docker Compose til å kjøre databaser og mocks av
tredjeparts tjenester.
Sørg for å ha [Docker](https://www.docker.com) installert, se instruksjoner
for [Mac](https://docs.docker.com/desktop/mac/install) eller [Ubuntu](https://docs.docker.com/engine/install/ubuntu).

Når installasjon er fullført kan du bl.a. benytte følgende kommandoer til å administrere containere definert
i `docker-compose.yaml`:

```bash
# Starter alle containere som trengs for lokal utvikling
docker compose --profile dev up -d

# Stopper alle containere
docker compose -p mulighetsrommet down

# Stopper alle containere og sletter samtidig tilhørende volumer
docker compose -p mulighetsrommet down -v
```

## Utvikling

### Kodeformatering og linting i backend

Vi bruker `ktlint` for kodeformatering og linting av kotlin-kode. Følgende kommandoer kan benyttes til å sjekke og fikse
lintefeil:

```sh
# Sjekk lintefeil
gradle ktlintCheck

# Sjekk lintefeil og fiks de som kan fikses automatisk
gradle ktlintFormat
```

### Kodeformatering og linting i frontend

Vi bruker `prettier` for kodeformatering og eslint for linting. Følgende kommandoer kan benyttes til å sjekke og fikse
lintefeil:

```sh
# Sjekk lintefeil
turbo run lint

# Sjekk lintefeil og fiks de som kan fikses automatisk
turbo run lint:fix
```

### Mocks via Wiremock

Vi har en rekke mocks for tredjeparts tjenester som blir administrert via Wiremock
i [docker-compose.yaml](docker-compose.yaml) og som blir
benyttet når du kjører tjenestene i dette prosjektet på lokal maskin.
Se konfigurasjonen der for hvor mockene er definert hvis du ønsker å utvide med flere responser.

Følgende endepunkter kan være kjekke for benytte under testing:

- Get all mocks: `curl -XGET http://localhost:8090/__admin/mappings`
- Reload mocks: `curl -I -XPOST http://localhost:8090/__admin/mappings/reset`

## Overvåking av løsninger

Det finnes noen tilgjengelige dashboards, men nytten med disse kan variere:

- [Innsikt i logger](<https://logs.adeo.no/app/dashboards#/view/6927d260-00ed-11ed-9b1a-4723a5e7a9db?_g=(filters:!(),refreshInterval:(pause:!t,value:0),time:(from:now-15m,to:now))>)
- [Opentelemetry-metrikker fra frontend](https://grafana.nav.cloud.nais.io/d/k8g_nks4z/frontend-web-vitals?orgId=1&var-app=mulighetsrommet-veileder-flate&var-env=prod-gcp-loki&from=now-15m&to=now)
- [Metrikker fra frontend](<https://logs.adeo.no/app/dashboards#/view/b9e91b00-01ba-11ed-9b1a-4723a5e7a9db?_a=(viewMode:edit)&_g=(filters:!(),refreshInterval:(pause:!t,value:0),time:(from:now-15m,to:now))>)
- [Metrikker fra backend](https://grafana.nais.io/d/8W2DNq6nk/mulighetsrommet-api?orgId=1&refresh=5m&var-datasource=prod-gcp&var-duration=30m&var-team=team-mulighetsrommet&from=now-15m&to=now)

## Overvåking av automatiske jobber

Vi har satt opp to Slack-bots som kan gi beskjed til oss på Slack i kanalen #team-valp-monitoring dersom det oppstår
feil under kjøring av de automatiske jobbene.

Botene finner man her:

- Dev-monitorering: https://api.slack.com/apps/A04PW7S8J94/general
- Prod-monitorering: https://api.slack.com/apps/A04Q2NNABDZ

## Datadeling på Datamarkedsplassen

[Se dokumentasjon](./docs/datamarkedsplassen/README.md)

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles via issues her på GitHub.

### For Nav-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team-valp

