# IAC (Infrastructure as Code) for Mulighetsrommet

Mulighetsrommet bruker Kafka for å tilgjengeliggjøre data på topics som andre konsumenter kan benytte seg av.
I denne mappen man våre oppsett av kafka-topics og kafka-manager.

## Kafka topics
Topics ligger i mappen `/kafka-topics` og inneholder konfigurasjon for dev-miljø og prod-miljø.

### Deployment av topics
Deployment av topic skjer automatisk via Github Actions når man merger kode relatert til topicens yaml-fil på main-branchen.

## Kafka manager
Kafka manager er en applikasjon som kan nås på følgende url'er:

**Dev**: https://mulighetsrommet-kafka-manager.dev.intern.nav.no
**Prod**: https://mulighetsrommet-kafka-manager.intern.nav.no

Man kan bruke manageren for å lese meldinger på topics som er definert i konfigurasjonen.

Deployment av kafka-manager skjer via Github Actions ved merge til main-branch.