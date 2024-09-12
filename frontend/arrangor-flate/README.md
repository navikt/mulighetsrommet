# Arrangørflate for refusjoner

Flate for tiltaksarrangører som skal be om refusjon for arbeidsmarkedstiltak

## Lokal utvikling

Noen pakker under `@navikt` hentes fra Github sitt NPM-repository. For at dette skal fungere må du først autentisere mot Github:

```
pnpm login --registry https://npm.pkg.github.com
```

Brukernavn er Github-brukernavnet ditt. Passordet er et [Personal Access Token](https://github.com/settings/tokens) med `read:packages`-scope. Tokenet må autentiseres med SSO mot navikt-organisasjonen.

Når du er logget inn kan du kjøre:

```
pnpm install
```

For å starte utviklingsserveren, kjør:

```
pnpm run dev
```
