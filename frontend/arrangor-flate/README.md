# Arrangørflate for utbetalinger

Flate for tiltaksarrangører som skal be om utbetalinger for arbeidsmarkedstiltak

## Konfigurasjon

Noen miljøvariabler kan settes for å styre deler av applikasjonen.

Standard-verdier for disse er allerede satt satt på relevante scripts i `package.json`,
men de kan evt. overstyres via `.env` (eller andre metoder for å sette miljøvariabler).

```
# Når satt til "true" så blir ikke dekoratøren rendret i applikasjonen
DISABLE_DEKORATOREN=true

# Når satt til "true" så vil applikasjonen bruke mock serveren i stedet for å kontakte backend
VITE_MULIGHETSROMMET_API_MOCK=true

# Base-url til API
VITE_MULIGHETSROMMET_API_BASE=<url>
# Hardkodet token mot lokal backend
VITE_MULIGHETSROMMET_API_AUTH_TOKEN=<token>
```

## Lokal utvikling med mock-data

```
turbo run dev
```

## Lokal utvikling mot lokal backend

For dette trenger man et access token definert i miljøvariabelen `VITE_MULIGHETSROMMET_API_AUTH_TOKEN`.
Denne kan du definere etter eget ønske, enten som en vanlig miljøvariabel, evt.
via [vite](https://vite.dev/guide/env-and-mode.html#env-files)
eller [mise](https://mise.jdx.dev/environments/#using-environment-variables).
For å generere dette gjør du følgende:

1. Naviger til lokal [Mock Oauth2 Server](http://localhost:8081/tokenx/debugger)
2. Trykk på knappen `Get a token`
3. Skriv inn hva som helst i toppen
4. Legg inn dette i optional claims:
   ```json
   {
     "pid": "11830348931"
   }
   ```
5. Trykk `Sign in`
6. Kopier verdien til `access_token` og benytt denne i nevnte miljøvariabel

```
turbo run backend
```

## Tilgang i dev og prod

Tjenesten bruker Altinn for å sjekke om innlogget bruker har tilgang eller ikke.

### Dev

For å få tilgang i dev-miljø gjør du følgende:

1. Gå til https://tt02.altinn.no/
2. Trykk på Logg inn -> TestID på høyt nivå
3. Trykk på knappen «Hent tilfeldig daglig leder» - Ta vare på fnr for daglig leder -> Trykk på autentiser
4. Logg ut igjen
5. Logg inn -> TestID på høyt nivå
6. Trykk på knappen «Hent tilfeldig person» - Ta vare på fnr og etternavn til brukeren -> Trykk på autentiser
7. Hvis du får beskjed om å legge til tlf og epost for brukeren så velg noen tulleverdier.
8. Logg ut igjen
9. Logg inn og denne gangen bruker du daglig leder fra steg 3.
10. Trykk på en underenhet som daglig leder representerer
    1. Hvis du får beskjed om å legge til tlf og epost for brukeren så velg noen tulleverdier.
11. Trykk på «Profil»
12. Velg «Andre med rettigheter til virksomheten»
13. Velg «Legge til ny person eller virksomhet»
14. Søk opp fødselsnr på ansatt fra steg 6 og skriv inn etternavn og velg neste
15. Velg så «Gi tilgang til enkelttjenester»
16. Søk opp tjenesten «Tiltaksarrangør utbetaling - Nav Arbeidsmarkedstiltak» og velg «Legg til»
17. Trykk Gå videre
18. Velg så «Fullfør delegering» og trykk deg til ferdig

### Prod

For prod så er det bedriftene selv som må delegere tilgangen til ansatte.
Tilgangen har navnet **Tiltaksarrangør utbetaling - Nav Arbeidsmarkedstiltak**.
