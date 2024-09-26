# Arbeidsmarkedstiltak (veileder-flate)

Visning av arbeidsmarkedstiltak til veiledere og NAV-ansatte. Applikasjonen bygges og distribueres i flere varianter
avhengig av bruksområde.

**MODIA**

- En variant rettet mot veiledere ved arbeidsrettet oppfølging.
    - Kjører med en bruker (borger) i kontekst og inkluderer en del funksjoner rettet mot samhandling mellom
      NAV-veileder og bruker.
    - Lar deg finne relevante arbeidsmarkedstiltak for bruker i kontekst.
    - Gir deg oversikt over brukers tiltakshistorikk og integrerer med påmeldingsløsning for grintegrerer med
      påmeldingsløsning for gruppetiltak.
- Blir distribuert som en [Web Component](https://developer.mozilla.org/en-US/docs/Web/API/Web_components) og inkludert
  som en microfrontend i [veilarbpersonflate](https://github.com/navikt/veilarbpersonflatefs).

**NAV**

- En variant som er tilgjengelig for alle NAV-ansatte.
    - Gir deg en oversikt over arbeidsmarkedstiltak i NAV.
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

- `MODIA`-varianten lastes opp til NAV CDN og importeres direkte
  i [veilarbpersonflate](https://github.com/navikt/veilarbpersonflatefs).
- `NAV`-varianten hostes via egen instans av [POAO-frontend](https://github.com/navikt/poao-frontend).
