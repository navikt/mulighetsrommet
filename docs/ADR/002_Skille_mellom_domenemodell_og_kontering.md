# ADR: Skille mellom tiltakets domenemodell og økonomisk kontering

## Status

Akseptert

## Kontekst

Tiltaksadministrasjon og tiltaksøkonomi beskriver forskjellige deler av domenet. Tiltaksadministrasjon bruker blant annet `Tiltakskode` og `TilsagnType` for å beskrive tiltak og tilsagn. Tiltaksøkonomi trenger identifikatorer som beskriver hvordan bestillinger skal konteres i OeBS/Planning.

Tidligere bruk av `tiltakskode` og `tilskuddstype` i meldingskontrakten koblet økonomisk kontering til begreper fra tiltaksadministrasjon. Dette gjorde det uklart hvilke egenskaper som var forretningsmessige klassifiseringer, og hvilke som identifiserte kontoer.

## Beslutning

Bestillingskontrakten bruker to økonomifaglige begreper: `Artskonto` og `Regnskapskonto`. Begrepene har ulik betydning og ulike regler for hvordan de kobles til konkrete kontonumre.

### Artskonto

`Artskonto` identifiserer en bestemt artskonto i OeBS/Planning for en gitt periode.

Innenfor samme periode skal én enumverdi alltid representere samme konkrete artskonto, uavhengig av hvilken `Regnskapskonto` den kombineres med. Kontonummeret kan endres mellom perioder.

Selv om enumverdiene i dag ligner `Tiltakskode`, er `Artskonto` et selvstendig økonomibegrep. Endringer i klassifiseringen av tiltak skal ikke automatisk endre betydningen av en artskonto.

### Regnskapskonto

`Regnskapskonto` er en konseptuell klassifisering av utgiften, for eksempel driftstilskudd, investeringer eller opplæringstilskudd.

Enumverdien identifiserer ikke alene ett konkret kontonummer. Den konkrete regnskapskontoen bestemmes av kombinasjonen av `Regnskapskonto`, `Artskonto` og gyldighetsperiode.

Eksempelvis blir driftstilskudd for VTA og driftstilskudd for AFT ført på forskjellige regnskapskontoer, selv om begge bruker `TILTAK_DRIFTSTILSKUDD`.

### Ansvarsdeling

Tiltaksadministrasjon oversetter sine domenebegreper til økonomiidentifikatorene i meldingskontrakten. Tiltaksøkonomi eier oppslaget fra disse identifikatorene til konkrete kontonumre for aktuell periode.

Begge identifikatorene lagres på bestillingen. Meldingskontrakten publiserer feltene `artskonto` og `regnskapskonto`, mens eksplisitt kompatibilitetslogikk støtter lesing av eldre meldinger.

## Konsekvenser

- Tiltaksadministrasjon og tiltaksøkonomi kan videreutvikle sine begreper uten at enumene må være identiske.
- Kontonumre kan endres over tid uten å endre meldingskontrakten.
- Samme `Artskonto` skal ikke ha forskjellige konkrete artskontoer for samme periode avhengig av `Regnskapskonto`. Dagens konfigurasjonstabell håndhever ikke denne regelen.
- Historiske bestillinger mangler opprinnelig informasjon om `Regnskapskonto`. Etterfylling basert på tiltakskode alene er derfor ikke tilstrekkelig til å identifisere blant annet investeringer; disse blir etterfylt i etterkant.
