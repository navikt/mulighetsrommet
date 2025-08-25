# Arbeidsmarkedstiltak (veileder-flate)

Visning av arbeidsmarkedstiltak til veiledere og Nav-ansatte. Applikasjonen bygges og distribueres i flere varianter
avhengig av bruksområde.

**MODIA**

- En variant rettet mot veiledere ved arbeidsrettet oppfølging.
    - Kjører med en bruker (borger) i kontekst og inkluderer en del funksjoner rettet mot samhandling mellom
      Nav-veileder og bruker.
    - Lar deg finne relevante arbeidsmarkedstiltak for bruker i kontekst.
    - Gir deg oversikt over brukers tiltakshistorikk og integrerer med påmeldingsløsning for grintegrerer med
      påmeldingsløsning for gruppetiltak.
- Blir distribuert som en [Web Component](https://developer.mozilla.org/en-US/docs/Web/API/Web_components) og inkludert
  som en microfrontend i [veilarbpersonflate](https://github.com/navikt/veilarbpersonflatefs).

**NAV**

- En variant som er tilgjengelig for alle Nav-ansatte.
    - Gir deg en oversikt over arbeidsmarkedstiltak i Nav.
- Inneholder også en modus for forhåndsvisning av redaksjonelt innhold.
    - Tilgjengelig for administratorer/redaktører innen tiltaksadministrasjon.
    - Inkluderer noen mock-varianter av funksjoner som ellers kun er tilgjenglige for veiledere (via `MODIA`-varianten)
      slik at man kan forhåndsvise innhold uten å ha en reell bruker i kontekst.

**LOKAL**

- Lar deg navigere mellom de to overnevnte variantene.

## Lokal utvikling

Som standard vil `LOKAL`-appen startes lokalt. Om ønskelig kan dette overstyres via miljøvariabelen `APP` i `.env`:

```.env
APP=LOKAL # Eller MODIA, NAV
```

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

1. Naviger til lokal [Mock Oauth2 Server](http://localhost:8081/azure/debugger)
2. Trykk på knappen `Get a token`
3. Skriv inn hva som helst som `user/subject`
4. Legg inn dette i optional claims:
   ```json
   {
     "NAVident": "B123456"
   }
   ```
5. Trykk `Sign in`
6. Kopier verdien til `access_token` og benytt denne i nevnte miljøvariabel

```sh
turbo run backend
```

### Lokal testing av MODIA-varianten

Denne varianten av applikasjonen blir lastet på en særegen måte i `veilarbpersonflate`.
Siden `veilarbpersonflate` består av flere microfrontends ønsker vi å enkapsulere styling per app slik at de ikke
påvirker hverandre.
Dette gjøres ved at `MODIA`-varianten blir bygget som en Web Component og styling blir lastet inn under en egen Shadow
DOM.
Følgende script (miljøvariabler) kan brukes for å kjøre `MODIA`-varianten lokalt:

```sh
# Build app
APP=MODIA VITE_DEMO_FNR='12345678910' VITE_DEMO_ENHET='0315' turbo run build

# Serve app
turbo run preview
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

- `MODIA`-varianten lastes opp til Nav CDN og importeres direkte
  i [veilarbpersonflate](https://github.com/navikt/veilarbpersonflatefs).
- `NAV`-varianten hostes via egen instans av [POAO-frontend](https://github.com/navikt/poao-frontend).
