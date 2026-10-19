# Copilot Instructions for mulighetsrommet

Mulighetsrommet er en applikasjonsportfølje som skal hjelpe brukere og veiledere til å få en helhetlig oversikt
over alle arbeidsmarkedstiltak Nav kan tilby.

Dette repoet samler løsningene som trengs for å forvalte, vise og distribuere data om arbeidsmarkedstiltak:

- Henter, validerer og eksponerer tiltaksdata via API-er og Kafka.
- Gir flater for veiledere, saksbehandlere og tiltaksarrangører.
- Integrerer med eksterne systemer som Arena, OeBS, Sanity og Altinn.
- Håndterer støttefunksjoner som PDF-generering, datadeling og automatiske jobber.

## Repository Overview

| Område | Katalog | Hva den gjør |
|--------|---------|--------------|
| API og domene | `mulighetsrommet-api/` | Kjernetjeneste for tiltakstyper, avtaler, gjennomføringer, tilsagn, utbetaling, OpenAPI og interne jobber. |
| Veilederflate | `frontend/arbeidsmarkedstiltak-flate/` | Viser arbeidsmarkedstiltak for veiledere og Nav-ansatte (inkludert Modia-variant). |
| Tiltaksadministrasjon | `frontend/tiltaksadministrasjon-flate/` | Administrativ flate for opprettelse og redigering av tiltakstyper, avtaler, gjennomføringer, tilsagn og utbetalinger mot tiltaksarrangører og innbyggere |
| Arrangør utbetalinger ('arrangor-flate') | `frontend/arrangor-utbetalinger-flate/` | Flate der tiltaksarrangører får oversikt over sine tilsagn, tidligere utbetalinger og ber om utbetalinger. |
| Tiltakshistorikk | `mulighetsrommet-tiltakshistorikk/` | Samler deltakelseshistorikk fra flere kilder og eksponerer ett felles API. |
| Tiltaksøkonomi | `mulighetsrommet-tiltaksokonomi/` | Anti-corruption layer mot OeBS for bestillinger, fakturaer, annulleringer og kvitteringer via Kafka/HTTP. |
| Arena-adapter | `mulighetsrommet-arena-adapter/` | Anti-corruption layer mellom Arena og resten av plattformen. |
| CMS | `frontend/mulighetsrommet-cms/` | Forvaltning av redaksjonelt innhold i Sanity Studio. Fases snart ut. |
| Fellesbiblioteker | `common/` | Delte moduler for blant annet Kafka, database, klienter, validering og logging. |
| Datadeling og fagdokumentasjon | `docs/` | Dokumentasjon av Kafka-topics og datadeling på Datamarkedsplassen. |
| PDF-generering | `mulighetsrommet-pdfgenrs/` | Maler, testdata og oppsett for generering av PDF-er med `pdfgenrs`. |
| Datastream, GCP ressurser| `iac/` | Konfigurasjon av Kafka topics, Datastream, Prometheus-alerts |

## Tech Stack

- Kotlin
- Ktor
- Node.js/TypeScript
- React-Router
- React

## Key Patterns


### Authentication flow

#### 1) Nav-ansatt (frontend + API)
- Innlogging skjer med **Azure AD**.
- API-et validerer JWT mot Azure issuer/JWKS og forventet audience.
- Autorisasjon gjøres videre med roller/gruppetilhørighet (Entra-grupper mappes til `Rolle` i applikasjonen).
- På routes brukes kombinasjonen:
  - `authenticate(AuthProvider.NAV_ANSATT)` for autentisert Nav-ansatt
  - `authenticate(AuthProvider.NAV_ANSATT_WITH_ROLES)` + `authorize(Rolle.X)` når rollen er påkrevd

#### 2) Arrangørflate (sluttbruker)
- Arrangørflate bruker **TokenX**.
- API-et validerer TokenX-token og forventer `pid` claim, som mappes til `NorskIdent`.
- Access type blir OBO med TokenX-token ved videre kall.

#### 3) Tjeneste-til-tjeneste (M2M)
- Interne klientkall mellom apper bruker **Azure app tokens** + NAIS `accessPolicy`.
- Inbound-regler i `.nais/*.yaml` definerer hvilke apper/roller som kan kalle hvilke tjenester.
- JWT med app-roller brukes for maskinelle endepunkter (f.eks. arena-adapter/gjennomføring).

#### 4) Eksterne integrasjoner
- **Maskinporten** brukes for enkelte eksterne tjenester (f.eks. Altinn-scope i NAIS-config).
- Token providers settes opp sentralt i dependency injection og brukes per klient med riktig scope/resource.

#### 5) Lokal utvikling
- Lokal auth bruker mock-oauth2 (azure/tokenx/maskinporten) med claims i lokale scripts/README-oppsett.

### Data access pattern

#### 1) QueryContext + session/transaction
- Datatilgang går gjennom database-wrappere (`ApiDatabase`, `OkonomiDatabase`, `TiltakshistorikkDatabase`).
- Standard mønster:
  - `db.session { ... }` for lesing/enkle operasjoner
  - `db.transaction { ... }` for skriveoperasjoner og konsistente oppdateringer
- Hver context eksponerer `queries` (SQL-klasser) og i `mulighetsrommet-api` også `repository`.

#### 2) Kotliquery + eksplisitt SQL
- SQL ligger i dedikerte `*Queries`-klasser og kjører via `kotliquery` (`queryOf`, `session.list/single/update/execute`).
- Mapping fra `Row` til domene/DTO gjøres i samme query-lag.

#### 3) Transaksjon + eventpublisering
- I `mulighetsrommet-api` finnes `outbox` i `QueryContext`, brukt til å publisere hendelser på en robust måte sammen med databaseendringer.
- For kritiske sekvenser finnes også advisory lock-støtte (`acquireAdvisoryLock`) i transaksjonscontext.

#### 4) Migrasjoner og skjema
- Flyway brukes for migrasjoner, og migrering kjøres ved oppstart av applikasjoner.
- PostgreSQL er standard databaselager i tjenestene.

### API conventions

#### 1) Routing og struktur
- Ktor-ruter er gruppert per domene/flate, med tydelige prefikser under `/api`.
- Typiske områder:
  - `/api/tiltaksadministrasjon`
  - `/api/veilederflate`
  - `/api/arrangorflate`
  - versjonerte endepunkter som `/api/v1/...` og enkelte `/api/v2/...`

#### 2) OpenAPI-først i routes
- Endepunkter defineres med OpenAPI-metadata i route-definisjonene (`tags`, `operationId`, request/response-schema).
- Spesifikasjoner genereres automatisk og publiseres både som YAML og via `swagger-ui`.
- Egen OpenAPI-spec per flate brukes i tillegg til public spec.

#### 3) Feilhåndtering
- `StatusException` brukes for kontrollerte feil i routes/services.
- Felles `StatusPages` oversetter exceptions til `ProblemDetail`-respons (`application/problem+json`), med `traceId`.
- Valideringsfeil returneres som `ValidationError` (ProblemDetail med feltfeil i `extensions.errors`).

#### 4) Request/response-kontrakter
- DTO-er er `@Serializable` (kotlinx serialization).
- Requests leses med `call.receive<T>()`, svar sendes med `call.respond(...)` eller `respondWithStatusResponse(...)`.
- Paginering bruker query-parametere (`page`, `size`) og standardisert `PaginatedResponse`.

#### 5) Sikkerhet per endpoint
- Auth og autorisasjon er eksplisitt på route-nivå (`authenticate` + `authorize`) i stedet for implicit globale regler.
- Dette gjør tilgangsmodell synlig nær API-kontrakten.

## Minimal Editing

When fixing a bug or implementing a feature, change only what is necessary.
Do not rename variables, restructure working code, or refactor beyond the task at hand.
Keep diffs small and focused so they are easy to review.

