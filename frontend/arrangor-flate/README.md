# Arrangørflate for refusjoner

Flate for tiltaksarrangører som skal be om refusjon for arbeidsmarkedstiltak

## Utvikling mot mock server

```
turbo run dev
```

## Utvikling mot backend

For dette trenger man et access token definert i `VITE_MULIGHETSROMMET_API_AUTH_TOKEN`. For å generere denne:
1. gå til `http://localhost:8081/tokenx/debugger`
2. Trykk på knappen `Get a token`
3. Skriv inn hva som helst i toppen
4. Legg inn dette i optional claims (pid kan du velge selv men påkrevd)
```json
{
 "pid": "11830348931",
 "aud": "mulighetsrommet-api"
}
```
5. Legg `access_token` verdien inn i env variabelen din


```
turbo run backend
```


## Miljøer
- [Flate for arrangører i dev-miljø](https://arrangor-refusjon.intern.dev.nav.no/)
- PROD-miljø er per 23.09.24 ikke satt opp enda.

## Tilgang i dev og prod
Tjenesten bruker Altinn for å sjekke om innlogget bruker har tilgang eller ikke
For å få tilgang i dev-miljø gjør du følgende:
1. Gå til https://tt02.altinn.no/
2. Trykk på Logg inn -> TestID på høyt nivå
3. Trykk på knappen «Hent tilfeldig daglig leder» - Ta vare på fnr for daglig leder -> Trykk på autentiser
4. Logg ut igjen
5. Logg inn -> TestID på høyt nivå
6. Trykk på knappen «Hent tilfeldig person» - Ta vare på fnr og etternavn til brukeren  -> Trykk på autentiser
7. Hvis du får beskjed om å legge til tlf og epost for brukeren så velg noen tulleverdier.
8. Logg ut igjen
9. Logg inn og denne gangen bruker du daglig leder fra steg 3.
10. Trykk på en underenhet som daglig leder representerer
    1. Hvis du får beskjed om å legge til tlf og epost for brukeren så velg noen tulleverdier.
11. Trykk på «Profil»
12. Velg «Andre med rettigheter til virksomheten»
13. Velg «Legge til ny person eller virksomhet»
14. Søk opp fødselsnr på ansatt fra steg 6 og skriv inn etternavn og velg neste
15.  Velg så «Gi tilgang til enkelttjenester»
16. Søk opp tjenesten «Tiltaksarrangør refusjon - NAV Arbeidsmarkedstiltak» og velg «Legg til»
17. Trykk Gå videre
18. Velg så «Fullfør delegering» og trykk deg til ferdig

For prod så er det bedriftene selv som delegerer korrekt tilgang. Korrekt tilgang er ***Tiltaksarrangør refusjon - NAV Arbeidsmarkedstiltak***
