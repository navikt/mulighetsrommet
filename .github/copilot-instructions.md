# Copilot Instructions for mulighetsrommet

Mulighetsrommet er et monorepo for forvaltning, visning og deling av arbeidsmarkedstiltak, inkludert tilsagn og utbetalinger.

## Repooversikt

| Område | Katalog | Hva den gjør |
|--------|---------|--------------|
| API og domene | `mulighetsrommet-api/` | Tiltakstyper, avtaler, gjennomføringer, tilsagn, utbetalinger og interne jobber. |
| Veilederflate | `frontend/arbeidsmarkedstiltak-flate/` | Tiltaksoversikt for Nav-ansatte, inkludert Modia-variant. |
| Tiltaksadministrasjon | `frontend/tiltaksadministrasjon-flate/` | Administrasjon av tiltak, avtaler, tilsagn og utbetalinger. |
| Arrangørflate | `frontend/arrangor-utbetalinger-flate/` | Arrangørers tilsagn, utbetalingshistorikk og krav om utbetaling. |
| Tiltakshistorikk | `mulighetsrommet-tiltakshistorikk/` | Felles API for deltakelseshistorikk fra flere kilder. |
| Tiltaksøkonomi | `mulighetsrommet-tiltaksokonomi/` | Integrasjon mot OeBS via Kafka/HTTP. |
| Arena-adapter | `mulighetsrommet-arena-adapter/` | Integrasjon mellom Arena og resten av plattformen. |
| Driftsflate (MAAM) | `frontend/arena-adapter-manager/` | Intern driftsflate for blant annet Arena-adapteren og API-et. |
| CMS | `frontend/mulighetsrommet-cms/` | Redaksjonelt innhold i Sanity Studio. |
| API-klienter | `frontend/arbeidsmarkedstiltak-api-client/`, `frontend/tiltaksadministrasjon-api-client/`, `frontend/arrangor-utbetalinger-api-client/` | OpenAPI-spesifikasjoner og genererte klienter for veilederflaten, tiltaksadministrasjonen og arrangørflaten. |
| Fellesbiblioteker | `common/` | Kafka, database, klienter, validering og logging. |
| Dokumentasjon | `docs/` | Arkitekturbeslutninger i `docs/ADR/`, Kafka-topics og datadeling på Datamarkedsplassen. |
| PDF-generering | `mulighetsrommet-pdfgenrs/` | PDF-maler, testdata og oppsett for `pdfgenrs`. |
| Infrastruktur | `iac/` | Kafka-topics, Datastream og Prometheus-varsler. |

### Moduler i `mulighetsrommet-api/`

| Modul | Innhold |
|-------|---------|
| `server/` | Applikasjonsoppstart, Ktor-plugins og HTTP-ruter, samt tjenester, integrasjoner og jobber. |
| `admin/` | Tjenester, spørringer og modeller for tiltaksadministrasjon. |
| `veilederflate/` | Tjenester og spørringer for veilederflaten, inkludert brukeroppfølging og deling av tiltak. |
| `domain/` | Domenetyper og felles domenelogikk. |
| `persistence/` | Felles databasestøtte og Flyway-migrasjoner i `src/main/resources/db/migration/`. |
| `contracts/` | Delte kontrakter og DTO-er. |
| `clients/` | Klienter mot andre tjenester. |

Dette er ikke et rent lagdelt oppsett. Ruter for admin- og veilederflaten ligger i `server/`, og flere tjenester, modeller, spørringer og klienter ligger også der. Finn ut hvor koden for fagområdet ligger før du velger modul. Se `settings.gradle.kts` for moduloversikt og modulens `build.gradle.kts` for avhengigheter.

## Teknologi

- Backend: Kotlin, Ktor og kotlinx.serialization.
- Database: PostgreSQL, Kotliquery og Flyway.
- Frontend: Node.js, React, TypeScript, Aksel og TanStack Query.
- Veilederflaten og tiltaksadministrasjonen bruker Vite. Arrangørflaten bruker React Router med serverkode.
- Frontend-workspace og bygg: pnpm og Turbo.

## Bygg og test

Kjør kommandoene fra repoets rot.

### Backend

```bash
./gradlew test         # Kjør tester
./gradlew ktlintCheck  # Kontroller kodeformatering
./gradlew build        # Bygg
```

Ved avgrensede endringer kan du kjøre tester for den berørte modulen, for eksempel `./gradlew :mulighetsrommet-api:server:test`. Kjør også tester i moduler som bruker koden du endrer.

### Frontend

Bruk `name` fra appens `package.json` som `<frontend-name>`, for eksempel `@tiltaksadministrasjon/flate`.
Kjør bare oppgaver som finnes i den valgte pakkens `scripts`. Veilederflaten har for eksempel Playwright-tester, men ikke et `test`-script.

```bash
pnpm exec turbo run typecheck --filter=<frontend-name>  # Typekontroller appen
pnpm exec turbo run lint --filter=<frontend-name>       # Kjør lint-kontroller
pnpm exec turbo run test --filter=<frontend-name> -- --run  # Kjør Vitest uten watch-modus
pnpm exec turbo run playwright --filter=<frontend-name> # Kjør Playwright-tester
```

Turbo bygger workspace-avhengigheter før typekontroll, inkludert OpenAPI-klienter. Arrangørflaten genererer også React Router-typer.
`--run` i testkommandoen gjelder pakker med `test: vitest`. Se README i aktuell tjeneste for lokalt oppsett.

## Tilgangskontroll

- Nav-ansatte bruker Entra ID (Azure AD), med Entra-grupper mappet til `Rolle`. Følg eksisterende bruk av `AuthProvider` og `authorize(Rolle.X)` i aktuell rutegruppe.
- Arrangørflaten bruker TokenX mot API-et på vegne av innlogget bruker. TokenX-identitet gir ikke alene tilgang til arrangørressurser. Bruk eksisterende Altinn-kontroll for organisasjonsnummeret ressursen tilhører, før lesing eller endring.
- Maskinelle endepunkter bruker app-token fra Entra ID og app-roller. Nais `accessPolicy` erstatter ikke API-ets autentisering og rollekontroller.
- Maskinporten brukes for enkelte eksterne integrasjoner. Les eksisterende klientoppsett før du endrer tokenbruk.
- Se `mulighetsrommet-api/server/src/main/kotlin/no/nav/mulighetsrommet/api/plugins/Authentication.kt`, aktuell rutegruppe og tjenestens `.nais/`-filer for gjeldende tilgangsmodell.

## Datatilgang og hendelser

- Bruk tjenestens database-wrapper og eksisterende `*Queries`-/repository-mønster. Samle oppdateringer som må være atomiske i samme `db.transaction`.
- I `mulighetsrommet-api` skal databaseendringer og tilhørende outbox-hendelser lagres i samme transaksjon. Følg eksisterende `QueryContext.outbox`-mønster fremfor å publisere direkte til Kafka.
- Bevar eksisterende transaksjonsgrenser og advisory locks når du endrer slike flyter.

## API og generert kode

- Følg eksisterende rutegruppe for aktuell flate, inkludert OpenAPI-metadata og tilgangskontroller.
- Ikke rediger genererte OpenAPI-spesifikasjoner eller klienter direkte. Endre API-kilden og kjør `./gradlew generateOpenApi` fra repoets rot for å regenerere `openapi.yaml`-filene. Regenerer klientene gjennom eksisterende frontend-byggoppsett. Se `mulighetsrommet-api/README.md`.
- Genereringsflyten er API-kilde → OpenAPI-spesifikasjon → frontend-klient. Oppgaven i `mulighetsrommet-api/server/build.gradle.kts` skriver `openapi.yaml` til de tre API-klientpakkene i repooversikten. Den skriver også den offentlige spesifikasjonen til `mulighetsrommet-api/server/src/main/resources/web/openapi-public.yaml`. Klientpakkenes `build`-script genererer klientkode fra spesifikasjonene.
- Bruk eksisterende `StatusException`, `ProblemDetail` og `ValidationError` fremfor å innføre et nytt feilformat.

## Avgrens endringer

Endre bare det oppgaven krever. Unngå navneendringer, omstrukturering og annen refaktorering utenfor oppgaven.
