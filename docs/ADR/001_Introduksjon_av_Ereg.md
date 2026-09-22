# ADR: Ereg som kilde for å hente ut organisasjonsinformasjon

## Status

Utkast

## Kontekst

Vi har lagt til Ereg register, primært for å støtte Team Fagers integrasjon mot Tiltaksøkonomi, en ACL mot OeBS.
Ereg (QA) er koblet opp mot bl.a. Altinn (TT02) og Tenor (Skatt testbedrifter), som gir enklere flyter for test-data på tvers av systemer.

I dag bruker vi Brønnøysundregisteret (prod) som kilde for å hente ut organisasjonsinformasjon, som ikke kjenner til test organisasjonene.
Enkelte unntak for test organisasjoner og noen utenlandske bedrifter har vi lagt direkte inn i `arrangor`-tabellen.

Vi lytter på `amt.virksomheter-v1` for å få hint om at organisasjoner i Brreg har en endring.
Kaller deretter Brreg for å få oppdatert informasjon, og lagrer dette i `arrangor`-tabellen.

## Beslutning

< uavklart >

## Konsekvenser

Om vi bytter til å bruke Ereg for alt, må vi finne en løsning for å vedlikeholde informasjonen vår, på lik linje med hvordan vi gjør det i dag via `amt.virksomheter-v1.

## Referanser:

- https://github.com/navikt/ereg-services (APIet til Ereg)
